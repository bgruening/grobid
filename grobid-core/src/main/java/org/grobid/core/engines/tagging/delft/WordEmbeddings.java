package org.grobid.core.engines.tagging.delft;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.lmdbjava.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Word embeddings lookup using LMDB database.
 * 
 * Reads embeddings from LMDB where values are raw float32 arrays
 * (little-endian).
 * Use convert_lmdb_embeddings.py to convert from pickled numpy format.
 * 
 * This class is a singleton per embeddings path - multiple models using the
 * same embeddings (e.g., glove-840B) share a single LMDB connection.
 * Use {@link #getInstance(Path, int)} to obtain instances.
 */
public class WordEmbeddings implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordEmbeddings.class);

    // Singleton registry keyed by absolute path
    private static final ConcurrentHashMap<String, WordEmbeddings> INSTANCES = new ConcurrentHashMap<>();

    // Default max cache size: 200K entries (~240MB for 300-dim embeddings)
    private static final int DEFAULT_CACHE_SIZE = 200_000;

    private final Env<ByteBuffer> env;
    private final Dbi<ByteBuffer> dbi;
    private final int embeddingSize;
    private final float[] zeroVector;
    private final AtomicInteger refCount = new AtomicInteger(0); // Track how many models are using this instance

    // LRU cache for embeddings - eliminates repeated LMDB lookups (using Guava)
    private final Cache<String, float[]> cache;

    // Instrumentation for LMDB access pattern analysis
    private final AtomicLong totalLookups = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong totalLookupTimeNs = new AtomicLong();
    private final AtomicLong misses = new AtomicLong(); // Words not found in DB
    private final ConcurrentHashMap<String, Boolean> uniqueWords = new ConcurrentHashMap<>();
    private final ScheduledExecutorService statsScheduler;
    private final String dbName;

    /**
     * Get a shared WordEmbeddings instance for the given path.
     * 
     * Multiple models using the same embeddings path will share a single LMDB
     * connection, reducing resource usage and reader slot contention.
     * 
     * @param dbPath        Path to the LMDB database directory
     * @param embeddingSize Dimension of the embeddings
     * @return Shared WordEmbeddings instance
     * @throws IOException if the database cannot be opened
     */
    public static WordEmbeddings getInstance(Path dbPath, int embeddingSize) throws IOException {
        String key = dbPath.toAbsolutePath().toString();

        WordEmbeddings instance = INSTANCES.get(key);
        if (instance != null) {
            instance.refCount.incrementAndGet();
            LOGGER.debug("Reusing existing WordEmbeddings instance for {} (refCount={})",
                    key, instance.refCount.get());
            return instance;
        }

        // Double-checked locking for thread-safe lazy initialization
        synchronized (INSTANCES) {
            instance = INSTANCES.get(key);
            if (instance == null) {
                instance = new WordEmbeddings(dbPath, embeddingSize);
                INSTANCES.put(key, instance);
                LOGGER.info("Created new WordEmbeddings singleton for {}", key);
            }
            instance.refCount.incrementAndGet();
            return instance;
        }
    }

    /**
     * Private constructor - use {@link #getInstance(Path, int)} instead.
     * 
     * @param dbPath        Path to the LMDB database directory
     * @param embeddingSize Dimension of the embeddings
     * @throws IOException if the database cannot be opened (missing path, LMDB
     *                     error, or native library issue)
     */
    private WordEmbeddings(Path dbPath, int embeddingSize) throws IOException {
        this.embeddingSize = embeddingSize;
        this.zeroVector = new float[embeddingSize];
        this.dbName = dbPath.getFileName().toString();

        // Initialize Guava Cache with LRU eviction (max 200K entries ~240MB for
        // 300-dim)
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(DEFAULT_CACHE_SIZE)
                .recordStats() // Enable cache statistics
                .build();

        // Check if path exists before trying to open
        if (!Files.exists(dbPath)) {
            throw new IOException("Embeddings database not found: " + dbPath.toAbsolutePath() +
                    "\nPlease provide a valid path to an LMDB embeddings database.");
        }
        if (!Files.isDirectory(dbPath)) {
            throw new IOException("Embeddings path is not a directory: " + dbPath.toAbsolutePath() +
                    "\nLMDB databases are directories containing 'data.mdb' and 'lock.mdb' files.");
        }

        try {
            // Open LMDB environment with increased reader slots for high concurrency
            this.env = Env.create()
                    .setMapSize(10_000_000_000L) // 10GB max
                    .setMaxReaders(512) // Support high concurrency (default is 126)
                    .setMaxDbs(1)
                    .open(dbPath.toFile());

            // Open the default database
            this.dbi = env.openDbi((String) null, DbiFlags.MDB_CREATE);
        } catch (LmdbException e) {
            throw new IOException("Failed to open LMDB database at " + dbPath.toAbsolutePath() +
                    ": " + e.getMessage(), e);
        } catch (UnsatisfiedLinkError e) {
            throw new IOException("LMDB native library failed to load. " +
                    "Ensure lmdbjava dependency includes native libraries for your platform. " +
                    "Error: " + e.getMessage(), e);
        }

        // Validate that the database contains raw float32 format (not pickled numpy)
        validateEmbeddingFormat(dbPath);

        // Start the stats logging scheduler (every 30 seconds)
        this.statsScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lmdb-stats-" + dbName);
            t.setDaemon(true);
            return t;
        });
        statsScheduler.scheduleAtFixedRate(this::logStats, 30, 30, TimeUnit.SECONDS);

        LOGGER.info("Opened LMDB database at {} (stats logging enabled every 30s)", dbPath);
    }

    /**
     * Log accumulated statistics about LMDB access patterns.
     * Called every 30 seconds by the stats scheduler.
     */
    private void logStats() {
        long lookups = totalLookups.get();
        if (lookups == 0) {
            return; // Don't log if no activity
        }

        long timeNs = totalLookupTimeNs.get();
        long missCount = misses.get();
        long hits = cacheHits.get();
        int uniqueCount = uniqueWords.size();
        long cacheSize = cache.size();

        // Calculate metrics
        double cacheHitRate = lookups > 0 ? 100.0 * hits / lookups : 0.0;
        double dbHitRate = lookups > 0 ? 100.0 * (lookups - missCount) / lookups : 0.0;
        double repeatRatio = uniqueCount > 0 ? (double) lookups / uniqueCount : 0.0;
        long lmdbLookups = lookups - hits;
        double avgLmdbTimeMs = lmdbLookups > 0 ? (timeNs / (double) lmdbLookups) / 1_000_000.0 : 0.0;

        LOGGER.info("LMDB [{}] stats: {} lookups, cache hit: {}% ({} entries), " +
                "DB hit: {}%, repeat ratio: {}x, avg LMDB lookup: {}ms",
                dbName, lookups,
                String.format("%.1f", cacheHitRate), cacheSize,
                String.format("%.1f", dbHitRate),
                String.format("%.1f", repeatRatio),
                String.format("%.3f", avgLmdbTimeMs));
    }

    /**
     * Look up embedding for a word.
     * 
     * @param word The word to look up
     * @return Embedding vector, or zero vector if not found
     * @throws RuntimeException if LMDB database access fails
     */
    public float[] getEmbedding(String word) {
        // Normalize digits to "0" like Python's _normalize_num
        String normalizedWord = normalizeNum(word);

        byte[] keyBytes = normalizedWord.getBytes(StandardCharsets.UTF_8);
        ByteBuffer keyBuffer = ByteBuffer.allocateDirect(keyBytes.length);
        keyBuffer.put(keyBytes).flip();

        // Retry logic for LMDB BadReaderLockException under high concurrency
        int maxRetries = 3;
        int retryDelayMs = 10;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try (Txn<ByteBuffer> txn = env.txnRead()) {
                ByteBuffer valueBuffer = dbi.get(txn, keyBuffer);

                if (valueBuffer == null) {
                    // Word not found, return zero vector
                    return zeroVector.clone();
                }

                // Parse float array from raw bytes (little-endian float32)
                valueBuffer.order(ByteOrder.LITTLE_ENDIAN);
                float[] embedding = new float[embeddingSize];
                for (int i = 0; i < embeddingSize; i++) {
                    embedding[i] = valueBuffer.getFloat();
                }
                return embedding;
            } catch (Txn.BadReaderLockException e) {
                if (attempt < maxRetries - 1) {
                    LOGGER.debug("LMDB reader lock contention (attempt {}), retrying", attempt + 1);
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during LMDB retry", ie);
                    }
                } else {
                    throw new RuntimeException(
                            "LMDB error after " + maxRetries + " retries for word '" + word + "': " + e.getMessage(),
                            e);
                }
            } catch (LmdbException e) {
                throw new RuntimeException(
                        "LMDB database error during embedding lookup for word '" + word + "': " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("LMDB lookup failed after retries for word '" + word + "'");
    }

    /**
     * Normalize digits in a word to "0" (matches Python's _normalize_num).
     * This is needed because the model was trained with this normalization.
     * 
     * @param word Input word
     * @return Word with all digits replaced by "0"
     */
    private String normalizeNum(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append('0');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Look up embeddings for a sequence of words.
     * 
     * Uses a single LMDB read transaction for all lookups to avoid
     * exhausting reader slots under high concurrency.
     * 
     * @param words Array of words
     * @return 2D array [seq_len][embedding_size]
     * @throws RuntimeException if LMDB database access fails
     */
    public float[][] getEmbeddings(String[] words) {
        float[][] result = new float[words.length][embeddingSize];

        // Retry logic for LMDB BadReaderLockException under high concurrency
        int maxRetries = 3;
        int retryDelayMs = 10;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try (Txn<ByteBuffer> txn = env.txnRead()) {
                for (int i = 0; i < words.length; i++) {
                    result[i] = getEmbeddingWithTxn(words[i], txn);
                }
                return result; // Success, return immediately
            } catch (Txn.BadReaderLockException e) {
                // Reader slot contention under high concurrency - retry after brief delay
                if (attempt < maxRetries - 1) {
                    LOGGER.debug("LMDB reader lock contention (attempt {}), retrying after {}ms",
                            attempt + 1, retryDelayMs);
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during LMDB retry", ie);
                    }
                } else {
                    throw new RuntimeException(
                            "LMDB database error after " + maxRetries + " retries: " + e.getMessage(), e);
                }
            } catch (LmdbException e) {
                throw new RuntimeException(
                        "LMDB database error during batch embedding lookup: " + e.getMessage(), e);
            }
        }

        // Should not reach here, but satisfy compiler
        throw new RuntimeException("LMDB lookup failed after retries");
    }

    /**
     * Look up embedding for a word using an existing transaction.
     * 
     * @param word The word to look up
     * @param txn  Active read transaction
     * @return Embedding vector, or zero vector if not found
     */
    private float[] getEmbeddingWithTxn(String word, Txn<ByteBuffer> txn) {
        // Normalize digits to "0" like Python's _normalize_num
        String normalizedWord = normalizeNum(word);

        // Track lookups and unique words
        totalLookups.incrementAndGet();
        uniqueWords.putIfAbsent(normalizedWord, Boolean.TRUE);

        // Check cache first - avoids LMDB disk I/O for repeated words
        float[] cached = cache.getIfPresent(normalizedWord);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached; // Return cached copy (no clone needed - embeddings are read-only)
        }

        // Cache miss - look up in LMDB
        long startNs = System.nanoTime();

        byte[] keyBytes = normalizedWord.getBytes(StandardCharsets.UTF_8);
        ByteBuffer keyBuffer = ByteBuffer.allocateDirect(keyBytes.length);
        keyBuffer.put(keyBytes).flip();

        ByteBuffer valueBuffer = dbi.get(txn, keyBuffer);

        totalLookupTimeNs.addAndGet(System.nanoTime() - startNs);

        if (valueBuffer == null) {
            // Word not found, cache and return zero vector
            misses.incrementAndGet();
            float[] zero = zeroVector.clone();
            cache.put(normalizedWord, zero);
            return zero;
        }

        // Parse float array from raw bytes (little-endian float32)
        valueBuffer.order(ByteOrder.LITTLE_ENDIAN);
        float[] embedding = new float[embeddingSize];
        for (int i = 0; i < embeddingSize; i++) {
            embedding[i] = valueBuffer.getFloat();
        }

        // Store in cache for future lookups
        cache.put(normalizedWord, embedding);
        return embedding;
    }

    /**
     * Check if a word exists in the database.
     * 
     * @throws RuntimeException if LMDB database access fails
     */
    public boolean contains(String word) {
        byte[] keyBytes = word.getBytes(StandardCharsets.UTF_8);
        ByteBuffer keyBuffer = ByteBuffer.allocateDirect(keyBytes.length);
        keyBuffer.put(keyBytes).flip();

        try (Txn<ByteBuffer> txn = env.txnRead()) {
            return dbi.get(txn, keyBuffer) != null;
        } catch (LmdbException e) {
            throw new RuntimeException("LMDB database error checking word '" + word + "': " + e.getMessage(), e);
        }
    }

    public int getEmbeddingSize() {
        return embeddingSize;
    }

    /**
     * Validate that the embeddings database contains raw float32 format.
     * 
     * If the database contains pickled numpy arrays (the old DeLFT format),
     * the bytes will be interpreted as garbage floats with extreme values.
     * This validation fails fast at startup with a clear error message.
     * 
     * @param dbPath Path to the database (for error messages)
     * @throws IOException if validation fails
     */
    private void validateEmbeddingFormat(Path dbPath) throws IOException {
        // Common test words that should exist in any GloVe/word2vec vocabulary
        String[] testWords = { "the", "and", "of", "to", "in" };
        final float MAX_VALID_VALUE = 10.0f; // GloVe values are typically < 5

        for (String testWord : testWords) {
            if (contains(testWord)) {
                float[] embedding = getEmbedding(testWord);

                for (int i = 0; i < embedding.length; i++) {
                    float value = embedding[i];

                    if (Float.isNaN(value) || Float.isInfinite(value) || Math.abs(value) > MAX_VALID_VALUE) {
                        close(); // Clean up before throwing
                        throw new IOException(
                                "Embeddings database at " + dbPath.toAbsolutePath() + " appears to contain " +
                                        "pickled numpy format instead of raw float32.\n" +
                                        "Found invalid embedding value for word '" + testWord + "': " +
                                        (Float.isNaN(value) ? "NaN" : Float.isInfinite(value) ? "Infinity" : value) +
                                        " at index " + i + ".\n" +
                                        "Please regenerate embeddings using:\n" +
                                        "  python3 grobid-home/scripts/preload_embeddings.py --embedding glove-840B");
                    }
                }

                LOGGER.debug("Embeddings format validation passed for word '{}'", testWord);
                return; // Validation passed for one word, that's enough
            }
        }

        LOGGER.warn("Could not validate embeddings format - none of the test words found in database");
    }

    @Override
    public void close() {
        // Reference counting: only close if this is the last reference
        int remaining = refCount.decrementAndGet();
        if (remaining > 0) {
            LOGGER.debug("WordEmbeddings [{}] close called, but {} references remain", dbName, remaining);
            return;
        }

        // Remove from singleton registry
        INSTANCES.values().remove(this);
        LOGGER.info("Closing WordEmbeddings singleton for {}", dbName);

        // Shutdown stats scheduler first
        if (statsScheduler != null) {
            statsScheduler.shutdown();
            try {
                if (!statsScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    statsScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                statsScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            // Log final stats before closing
            logStats();
        }

        if (dbi != null) {
            dbi.close();
        }
        if (env != null) {
            env.close();
        }
    }
}
