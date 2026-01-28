package org.grobid.core.engines.tagging.delft;

import ai.onnxruntime.OrtException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ONNX Runtime wrapper for running DeLFT encoder models.
 */
public class OnnxSequenceLabellingModel implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnnxSequenceLabellingModel.class);

    private final OnnxSequenceLabellingRunner modelRunner;
    private final CRFDecoder crfDecoder;
    private final Preprocessor preprocessor;
    private final WordEmbeddings embeddings;
    private final int maxSeqLength;

    public OnnxSequenceLabellingModel(Path modelDir) throws IOException, OrtException {
        // Read config.json
        Gson gson = new Gson();
        Path configPath = modelDir.resolve("config.json");
        JsonObject config;
        try (FileReader reader = new FileReader(configPath.toFile())) {
            config = gson.fromJson(reader, JsonObject.class);
        }

        int embeddingSize = config.get("wordEmbeddingSize").getAsInt();
        this.maxSeqLength = config.get("maxSequenceLength").getAsInt();
        String embeddingsName = config.get("embeddingsName").getAsString();

        // Embeddings are stored under delft installation path:
        // {delft}/data/db/{name}
        String delftPath = GrobidProperties.getDeLFTFilePath();
        Path embeddingsPath = Path.of(delftPath, "data", "db", embeddingsName);

        LOGGER.info("Loading ONNX model from: {}", modelDir);
        LOGGER.info("Loading embeddings from: {}", embeddingsPath);

        // Load components
        this.modelRunner = new OnnxSequenceLabellingRunner(modelDir.resolve("encoder.onnx"));
        this.crfDecoder = CRFDecoder.fromJson(modelDir.resolve("crf_params.json"));
        this.preprocessor = Preprocessor.fromJson(modelDir.resolve("vocab.json"));
        this.embeddings = WordEmbeddings.getInstance(embeddingsPath, embeddingSize);

        LOGGER.info("DeLFT model loaded from {}", modelDir);
        LOGGER.info("Model has features: {}", preprocessor.hasFeatures());
    }

    /**
     * Load a DeLFT model from exported directory.
     * 
     * @param modelDir       Directory containing encoder.onnx, crf_params.json,
     *                       vocab.json
     * @param embeddingsPath Path to LMDB embeddings database
     * @param embeddingSize  Dimension of word embeddings
     * @param maxSeqLength   Maximum sequence length
     */
    public OnnxSequenceLabellingModel(Path modelDir, Path embeddingsPath, int embeddingSize, int maxSeqLength)
            throws IOException, OrtException {

        this.maxSeqLength = maxSeqLength;

        // Load components
        this.modelRunner = new OnnxSequenceLabellingRunner(modelDir.resolve("encoder.onnx"));
        this.crfDecoder = CRFDecoder.fromJson(modelDir.resolve("crf_params.json"));
        this.preprocessor = Preprocessor.fromJson(modelDir.resolve("vocab.json"));
        this.embeddings = WordEmbeddings.getInstance(embeddingsPath, embeddingSize);

        LOGGER.info("DeLFT model loaded from {}", modelDir);
        LOGGER.info("Model has features: {}", preprocessor.hasFeatures());
    }

    /**
     * Annotate text with sequence labels (no features).
     * 
     * @param text Input text
     * @return Annotation result
     */
    public AnnotationResult annotate(String text) throws OrtException {
        List<LayoutToken> tokens = preprocessor.tokenize(text);
        String[] words = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            words[i] = tokens.get(i).getText();
        }

        return annotateTokens(words, null);
    }

    /**
     * Annotate tokens with features.
     * 
     * For BidLSTM_CRF_FEATURES models, features must be provided.
     * Each row in features corresponds to a token, with one value per feature
     * column.
     * 
     * @param tokens   Array of token strings
     * @param features Feature values per token [numTokens][numFeatures], can be
     *                 null for non-feature models
     * @return Annotation result
     */
    public AnnotationResult annotateTokens(String[] tokens, String[][] features) throws OrtException {
        int numTokens = Math.min(tokens.length, maxSeqLength);

        if (numTokens == 0) {
            return new AnnotationResult(null, new String[0], new String[0]);
        }

        // Truncate to max sequence length
        String[] words = new String[numTokens];
        System.arraycopy(tokens, 0, words, 0, numTokens);

        // Get embeddings [seq_len][embed_size]
        float[][] wordEmbs = embeddings.getEmbeddings(words);

        // Pad to maxSeqLength
        float[][] paddedEmbs = new float[maxSeqLength][embeddings.getEmbeddingSize()];
        for (int i = 0; i < numTokens; i++) {
            paddedEmbs[i] = wordEmbs[i];
        }

        // Get char indices [seq_len][max_char]
        List<LayoutToken> layoutTokens = new ArrayList<>();
        for (String word : words) {
            LayoutToken lt = new LayoutToken();
            lt.setText(word);
            layoutTokens.add(lt);
        }
        long[][] charIndices = preprocessor.tokensToCharIndices(layoutTokens, maxSeqLength);

        // Create batch of 1
        float[][][] batchEmbs = new float[][][] { paddedEmbs };
        long[][][] batchChars = new long[][][] { charIndices };

        // Handle features
        long[][][] batchFeatures = null;
        if (preprocessor.hasFeatures() && features != null) {
            long[][] featureIndices = preprocessor.tokensToFeatureIndices(features, maxSeqLength);
            batchFeatures = new long[][][] { featureIndices };
        }

        // Run model
        float[][][] emissions = modelRunner.runInference(batchEmbs, batchChars, batchFeatures);

        // CRF decode
        boolean[] mask = preprocessor.createMask(numTokens, maxSeqLength);
        int[] tagIndices = crfDecoder.decode(emissions[0], mask);

        // Convert to tag names (only for actual tokens)
        String[] tags = new String[numTokens];
        for (int i = 0; i < numTokens; i++) {
            tags[i] = delft2grobidLabel(
                    preprocessor.getTagIndex().getOrDefault(tagIndices[i], TaggingLabels.IOB_OTHER_LABEL));
        }

        return new AnnotationResult(String.join(" ", words), words, tags);
    }

    /**
     * Annotate multiple token sequences in a single batch inference call.
     * All sequences are padded to maxSeqLength.
     * 
     * @param tokensBatch   Array of token arrays [batchSize][numTokens]
     * @param featuresBatch Feature values per token
     *                      [batchSize][numTokens][numFeatures], can be null
     * @return Array of AnnotationResult, one per input sequence
     */
    public AnnotationResult[] annotateTokensBatch(String[][] tokensBatch, String[][][] featuresBatch)
            throws OrtException {
        int batchSize = tokensBatch.length;

        if (batchSize == 0) {
            return new AnnotationResult[0];
        }

        // Prepare arrays to store actual token counts for each sequence
        int[] numTokensPerSeq = new int[batchSize];

        // Prepare batch data
        float[][][] batchEmbs = new float[batchSize][maxSeqLength][embeddings.getEmbeddingSize()];
        long[][][] batchChars = new long[batchSize][maxSeqLength][];
        long[][][] batchFeatures = null;

        if (preprocessor.hasFeatures() && featuresBatch != null) {
            batchFeatures = new long[batchSize][][];
        }

        for (int b = 0; b < batchSize; b++) {
            String[] tokens = tokensBatch[b];
            int numTokens = Math.min(tokens.length, maxSeqLength);
            numTokensPerSeq[b] = numTokens;

            if (numTokens == 0) {
                // Initialize with zeros for empty sequences
                for (int i = 0; i < maxSeqLength; i++) {
                    batchEmbs[b][i] = new float[embeddings.getEmbeddingSize()];
                    batchChars[b][i] = new long[preprocessor.getMaxCharLength()];
                }
                if (batchFeatures != null) {
                    batchFeatures[b] = new long[maxSeqLength][preprocessor.getNumFeatures()];
                }
                continue;
            }

            // Truncate to max sequence length
            String[] words = new String[numTokens];
            System.arraycopy(tokens, 0, words, 0, numTokens);

            // Get embeddings and pad
            float[][] wordEmbs = embeddings.getEmbeddings(words);
            for (int i = 0; i < numTokens; i++) {
                batchEmbs[b][i] = wordEmbs[i];
            }
            for (int i = numTokens; i < maxSeqLength; i++) {
                batchEmbs[b][i] = new float[embeddings.getEmbeddingSize()];
            }

            // Get char indices
            List<LayoutToken> layoutTokens = new ArrayList<>();
            for (String word : words) {
                LayoutToken lt = new LayoutToken();
                lt.setText(word);
                layoutTokens.add(lt);
            }
            batchChars[b] = preprocessor.tokensToCharIndices(layoutTokens, maxSeqLength);

            // Handle features
            if (batchFeatures != null) {
                if (featuresBatch != null && featuresBatch[b] != null) {
                    batchFeatures[b] = preprocessor.tokensToFeatureIndices(featuresBatch[b], maxSeqLength);
                } else {
                    batchFeatures[b] = new long[maxSeqLength][preprocessor.getNumFeatures()];
                }
            }
        }

        // Run batch inference
        float[][][] emissions = modelRunner.runInference(batchEmbs, batchChars, batchFeatures);

        // Decode each sequence
        AnnotationResult[] results = new AnnotationResult[batchSize];
        for (int b = 0; b < batchSize; b++) {
            int numTokens = numTokensPerSeq[b];

            if (numTokens == 0) {
                results[b] = new AnnotationResult(null, new String[0], new String[0]);
                continue;
            }

            // CRF decode
            boolean[] mask = preprocessor.createMask(numTokens, maxSeqLength);
            int[] tagIndices = crfDecoder.decode(emissions[b], mask);

            // Convert to tag names (only for actual tokens)
            String[] tags = new String[numTokens];
            String[] words = new String[numTokens];
            System.arraycopy(tokensBatch[b], 0, words, 0, numTokens);

            for (int i = 0; i < numTokens; i++) {
                tags[i] = delft2grobidLabel(
                        preprocessor.getTagIndex().getOrDefault(tagIndices[i], TaggingLabels.IOB_OTHER_LABEL));
            }

            results[b] = new AnnotationResult(String.join(" ", words), words, tags);
        }

        return results;
    }

    /**
     * Check if this model requires features.
     */
    public boolean hasFeatures() {
        return preprocessor.hasFeatures();
    }

    /**
     * Get the number of features expected per token (0 if no features).
     */
    public int getNumFeatures() {
        return preprocessor.getNumFeatures();
    }

    /**
     * Maximum sequence length supported by the model.
     * <p>
     * For the DeLFT-exported ONNX bundles shipped with GROBID, this value is read
     * from the accompanying {@code config.json} (field {@code maxSequenceLength}).
     */
    public int getMaxSeqLength() {
        return maxSeqLength;
    }

    /**
     * Read the maximum sequence length from a DeLFT ONNX bundle directory.
     * <p>
     * This method only parses {@code config.json} and does not load ONNX Runtime,
     * embeddings (LMDB) or any other native dependency.
     *
     * @param modelDir directory containing {@code config.json}
     * @return maxSequenceLength from config.json
     */
    public static int readMaxSequenceLength(Path modelDir) throws IOException {
        Gson gson = new Gson();
        Path configPath = modelDir.resolve("config.json");
        try (FileReader reader = new FileReader(configPath.toFile())) {
            JsonObject config = gson.fromJson(reader, JsonObject.class);
            return config.get("maxSequenceLength").getAsInt();
        }
    }

    /**
     * Annotate multiple texts in batch.
     */
    public List<AnnotationResult> annotateBatch(List<String> texts) throws OrtException {
        List<AnnotationResult> results = new ArrayList<>();
        for (String text : texts) {
            results.add(annotate(text));
        }
        return results;
    }

    /**
     * Label GROBID-formatted input data with support for long sequences.
     * 
     * For sequences exceeding maxSeqLength, this method chunks the sequence,
     * runs inference on each chunk independently, and concatenates the results.
     * This matches the behavior of DeLFT's Python grobidTagger.tag() method.
     * 
     * Input format: token\tfeature1\tfeature2\t...\n
     * Output format: token\tfeature1\tfeature2\t...\tlabel\n
     * 
     * @param data GROBID feature data
     * @return Labeled output in GROBID format
     */
    public String labelGrobidInput(String data) {
        try {
            // Parse input into sequences (separated by empty lines)
            List<List<String>> sequences = new ArrayList<>();
            List<String> currentSequence = new ArrayList<>();

            String[] lines = data.split("\n", -1); // -1 to keep trailing empty strings

            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    if (!currentSequence.isEmpty()) {
                        sequences.add(currentSequence);
                        currentSequence = new ArrayList<>();
                    }
                } else {
                    currentSequence.add(line);
                }
            }
            // Don't forget the last sequence if it doesn't end with empty line
            if (!currentSequence.isEmpty()) {
                sequences.add(currentSequence);
            }

            if (sequences.isEmpty()) {
                return "";
            }

            // Process each sequence independently (with chunking if needed)
            List<List<String>> allSequenceLabels = new ArrayList<>();

            for (List<String> sequence : sequences) {
                List<String> sequenceLabels = labelSequenceWithChunking(sequence);
                allSequenceLabels.add(sequenceLabels);
            }

            // Rebuild output with original structure
            StringBuilder output = new StringBuilder();
            int seqIdx = 0;
            int tokenInSeqIdx = 0;
            boolean inSequence = false;

            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    if (inSequence && seqIdx < sequences.size()) {
                        // End of a sequence - add separator
                        output.append("\n");
                        seqIdx++;
                        tokenInSeqIdx = 0;
                        inSequence = false;
                    }
                } else {
                    inSequence = true;
                    if (seqIdx < allSequenceLabels.size() &&
                            tokenInSeqIdx < allSequenceLabels.get(seqIdx).size()) {
                        String label = allSequenceLabels.get(seqIdx).get(tokenInSeqIdx);
                        output.append(line).append("\t").append(label).append("\n");
                        tokenInSeqIdx++;
                    }
                }
            }

            return output.toString();
        } catch (OrtException e) {
            throw new RuntimeException("ONNX inference failed", e);
        }
    }

    /**
     * Label a single sequence, chunking if it exceeds maxSeqLength.
     * 
     * @param sequenceLines Lines of the sequence (token\tfeatures format)
     * @return List of labels, one per token
     */
    private List<String> labelSequenceWithChunking(List<String> sequenceLines) throws OrtException {
        // Parse tokens and features from the sequence
        List<String[]> tokensWithFeatures = new ArrayList<>();
        for (String line : sequenceLines) {
            String[] parts = line.split("[\\t\\s]+");
            tokensWithFeatures.add(parts);
        }

        int totalTokens = tokensWithFeatures.size();

        // If sequence fits in one chunk, process directly
        if (totalTokens <= maxSeqLength) {
            return labelTokensWithFeatures(tokensWithFeatures);
        }

        // Collect all chunks for batch processing
        List<List<String[]>> chunks = new ArrayList<>();
        int offset = 0;

        while (offset < totalTokens) {
            int chunkEnd = Math.min(offset + maxSeqLength, totalTokens);
            chunks.add(new ArrayList<>(tokensWithFeatures.subList(offset, chunkEnd)));
            offset = chunkEnd;
        }

        // Convert chunks to batch arrays
        int batchSize = chunks.size();
        String[][] tokensBatch = new String[batchSize][];
        String[][][] featuresBatch = new String[batchSize][][];

        for (int i = 0; i < batchSize; i++) {
            List<String[]> chunk = chunks.get(i);
            tokensBatch[i] = new String[chunk.size()];
            featuresBatch[i] = null;

            for (int j = 0; j < chunk.size(); j++) {
                String[] parts = chunk.get(j);
                tokensBatch[i][j] = parts[0];

                if (parts.length > 1) {
                    if (featuresBatch[i] == null) {
                        featuresBatch[i] = new String[chunk.size()][parts.length - 1];
                    }
                    for (int k = 1; k < parts.length; k++) {
                        featuresBatch[i][j][k - 1] = parts[k];
                    }
                }
            }
        }

        // Run batch inference
        AnnotationResult[] results = annotateTokensBatch(tokensBatch, featuresBatch);

        // Combine results
        List<String> allLabels = new ArrayList<>();
        for (AnnotationResult result : results) {
            allLabels.addAll(Arrays.asList(result.getLabels()));
        }

        return allLabels;
    }

    /**
     * Label a list of tokens with features.
     * 
     * @param tokensWithFeatures Each element is [token, feature1, feature2, ...]
     * @return List of labels
     */
    private List<String> labelTokensWithFeatures(List<String[]> tokensWithFeatures) throws OrtException {
        if (tokensWithFeatures.isEmpty()) {
            return new ArrayList<>();
        }

        // Extract tokens and features
        String[] tokens = new String[tokensWithFeatures.size()];
        String[][] features = null;

        for (int i = 0; i < tokensWithFeatures.size(); i++) {
            String[] parts = tokensWithFeatures.get(i);
            tokens[i] = parts[0];

            if (parts.length > 1 && features == null) {
                features = new String[tokensWithFeatures.size()][parts.length - 1];
            }
            if (features != null) {
                for (int j = 1; j < parts.length; j++) {
                    features[i][j - 1] = parts[j];
                }
            }
        }

        // Run annotation
        AnnotationResult result = annotateTokens(tokens, features);
        return Arrays.asList(result.getLabels());
    }

    @Override
    public void close() {
        if (modelRunner != null)
            modelRunner.close();
        if (embeddings != null)
            embeddings.close();
    }

    private static String delft2grobidLabel(String label) {
        if (label.equals(TaggingLabels.IOB_OTHER_LABEL)) {
            return TaggingLabels.OTHER_LABEL;
        } else if (label.startsWith(TaggingLabels.IOB_START_ENTITY_LABEL_PREFIX)) {
            return label.replace(TaggingLabels.IOB_START_ENTITY_LABEL_PREFIX,
                    TaggingLabels.GROBID_START_ENTITY_LABEL_PREFIX);
        } else if (label.startsWith(TaggingLabels.IOB_INSIDE_LABEL_PREFIX)) {
            return label.replace(TaggingLabels.IOB_INSIDE_LABEL_PREFIX,
                    TaggingLabels.GROBID_INSIDE_ENTITY_LABEL_PREFIX);
        }
        return label;
    }

    /**
     * Annotation result containing tokens and labels.
     */
    public static class AnnotationResult {
        private final String text;
        private final String[] tokens;
        private final String[] labels;

        public AnnotationResult(String text, String[] tokens, String[] labels) {
            this.text = text;
            this.tokens = tokens;
            this.labels = labels;
        }

        public String getText() {
            return text;
        }

        public String[] getTokens() {
            return tokens;
        }

        public String[] getLabels() {
            return labels;
        }

        /**
         * Represents an extracted entity with its label and text.
         */
        public static class Entity {
            public final String label;
            public final String text;
            public final int startToken;
            public final int endToken;

            public Entity(String label, String text, int startToken, int endToken) {
                this.label = label;
                this.text = text;
                this.startToken = startToken;
                this.endToken = endToken;
            }
        }

        /**
         * Extract entities from BIO-tagged sequence.
         * Groups consecutive tokens with the same label into entities.
         */
        public List<Entity> extractEntities() {
            List<Entity> entities = new ArrayList<>();
            if (tokens == null || tokens.length == 0) {
                return entities;
            }

            String currentLabel = null;
            int startIdx = -1;
            StringBuilder currentText = new StringBuilder();

            for (int i = 0; i < tokens.length; i++) {
                String label = labels[i];
                String baseLabel = getBaseLabel(label);

                // Logic for Grobid-like labels (I-<label> is start, <label> is inside)
                boolean isO = label.equals(TaggingLabels.OTHER_LABEL) || label.equals("O");
                boolean isBegin = label.startsWith(TaggingLabels.GROBID_START_ENTITY_LABEL_PREFIX);
                boolean isInside = !isO && !isBegin;

                if (isBegin || (isInside && !baseLabel.equals(currentLabel))) {
                    // Save previous entity if exists
                    if (currentLabel != null) {
                        entities.add(new Entity(currentLabel, currentText.toString().trim(), startIdx, i - 1));
                    }
                    // Start new entity
                    currentLabel = baseLabel;
                    startIdx = i;
                    currentText = new StringBuilder(tokens[i]);
                } else if (isInside && baseLabel.equals(currentLabel)) {
                    // Continue current entity
                    currentText.append(" ").append(tokens[i]);
                } else if (isO) {
                    // End current entity if exists
                    if (currentLabel != null) {
                        entities.add(new Entity(currentLabel, currentText.toString().trim(), startIdx, i - 1));
                        currentLabel = null;
                        startIdx = -1;
                        currentText = new StringBuilder();
                    }
                }
            }

            // Don't forget last entity
            if (currentLabel != null) {
                entities.add(new Entity(currentLabel, currentText.toString().trim(), startIdx, tokens.length - 1));
            }

            return entities;
        }

        /**
         * Get base label without B-/I- prefix (also removes angle brackets).
         */
        private String getBaseLabel(String label) {
            String base = label;
            if (label.startsWith(TaggingLabels.GROBID_START_ENTITY_LABEL_PREFIX)) { // I-
                base = label.substring(TaggingLabels.GROBID_START_ENTITY_LABEL_PREFIX.length());
            } else if (label.startsWith(TaggingLabels.IOB_START_ENTITY_LABEL_PREFIX)) { // B-
                base = label.substring(TaggingLabels.IOB_START_ENTITY_LABEL_PREFIX.length());
            } else if (label.startsWith(TaggingLabels.IOB_INSIDE_LABEL_PREFIX)) { // I- (original IOB)
                base = label.substring(TaggingLabels.IOB_INSIDE_LABEL_PREFIX.length());
            }

            // Remove angle brackets if present (e.g., <title> -> title)
            if (base.startsWith("<") && base.endsWith(">")) {
                return base.substring(1, base.length() - 1);
            }
            return base;
        }

        /**
         * Format entities as XML-like string.
         * E.g., "<title>Analysis of 10,478 cancer genomes</title><author>Ben
         * Kinnersley</author>"
         */
        public String toXmlString() {
            List<Entity> entities = extractEntities();
            StringBuilder sb = new StringBuilder();
            for (Entity entity : entities) {
                sb.append("<").append(entity.label).append(">")
                        .append(entity.text)
                        .append("</").append(entity.label).append(">");
            }
            return sb.toString();
        }

        public String toJson() {
            List<Entity> entities = extractEntities();

            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"text\": \"").append(escapeJson(text)).append("\",\n");
            sb.append("  \"tokens\": [");
            for (int i = 0; i < tokens.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append("\"").append(escapeJson(tokens[i])).append("\"");
            }
            sb.append("],\n");
            sb.append("  \"labels\": [");
            for (int i = 0; i < labels.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append("\"").append(labels[i]).append("\"");
            }
            sb.append("],\n");

            // Add XML-formatted entities
            sb.append("  \"entitiesXml\": \"").append(escapeJson(toXmlString())).append("\",\n");

            // Add structured entities list
            sb.append("  \"entities\": [");
            for (int i = 0; i < entities.size(); i++) {
                Entity e = entities.get(i);
                if (i > 0)
                    sb.append(", ");
                sb.append("\n    {\"label\": \"").append(e.label)
                        .append("\", \"text\": \"").append(escapeJson(e.text))
                        .append("\", \"start\": ").append(e.startToken)
                        .append(", \"end\": ").append(e.endToken).append("}");
            }
            if (!entities.isEmpty()) {
                sb.append("\n  ");
            }
            sb.append("]\n}");
            return sb.toString();
        }

        private String escapeJson(String s) {
            if (s == null)
                return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }
}
