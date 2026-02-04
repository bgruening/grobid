package org.grobid.core.engines.tagging.delft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Test to compare Java preprocessing with Python preprocessing.
 * 
 * This test loads the same input file (example.txt from delft) and
 * produces preprocessing output in JSON format that can be compared
 * with the Python output.
 */
public class PreprocessingComparisonTest {

    private static final String DELFT_PATH = System.getProperty("user.home") + "/development/github/delft";
    private static final String MODEL_DIR = DELFT_PATH + "/exported_models/header-BidLSTM_CRF_FEATURES.onnx";
    private static final String INPUT_FILE = DELFT_PATH + "/example.txt";
    private static final String OUTPUT_FILE = DELFT_PATH + "/java_preprocessing.json";

    private static Preprocessor preprocessor;

    @BeforeClass
    public static void setup() throws IOException {
        Path vocabPath = Paths.get(MODEL_DIR, "vocab.json");
        if (!Files.exists(vocabPath)) {
            System.out.println("SKIP: vocab.json not found at " + vocabPath);
            return;
        }
        preprocessor = Preprocessor.fromJson(vocabPath);
    }

    /**
     * Test that generates preprocessing output for comparison with Python.
     * Run with: mvn test -Dtest=PreprocessingComparisonTest#testExportPreprocessing
     */
    @Test
    public void testExportPreprocessing() throws IOException {
        if (preprocessor == null) {
            System.out.println("SKIP: Preprocessor not loaded");
            return;
        }

        // Read input file
        String content = Files.readString(Paths.get(INPUT_FILE));
        String[] lines = content.split("\n", -1);

        // Parse tokens and features
        List<String> tokens = new ArrayList<>();
        List<String[]> tokenFeatures = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().isEmpty())
                continue;
            String[] parts = line.split("[\\t\\s]+");
            if (parts.length > 0) {
                tokens.add(parts[0]);
                tokenFeatures.add(parts);
            }
        }

        int limit = Math.min(50, tokens.size());

        // Build output
        Map<String, Object> output = new LinkedHashMap<>();

        // Config
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("max_char", preprocessor.getMaxCharLength());
        config.put("num_features", preprocessor.getNumFeatures());
        config.put("features_indices", preprocessor.getFeaturesIndices());
        output.put("config", config);

        // Tokens
        output.put("tokens", tokens.subList(0, limit));

        // Char indices
        List<long[]> charIndices = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            // Create LayoutToken for this token
            org.grobid.core.layout.LayoutToken lt = new org.grobid.core.layout.LayoutToken();
            lt.setText(tokens.get(i));
            List<org.grobid.core.layout.LayoutToken> tokenList = Collections.singletonList(lt);

            // Get char indices (returned as [seq][char], we only have 1 token so take [0])
            long[][] indices = preprocessor.tokensToCharIndices(tokenList, 1);
            charIndices.add(indices[0]);
        }
        output.put("char_indices", charIndices);

        // Feature indices
        if (preprocessor.hasFeatures() && preprocessor.getFeaturesIndices() != null) {
            List<Integer> featuresIndices = preprocessor.getFeaturesIndices();
            List<List<Map<String, Object>>> featureOutput = new ArrayList<>();

            for (int i = 0; i < limit; i++) {
                String[] parts = tokenFeatures.get(i);
                List<Map<String, Object>> tokenFeatList = new ArrayList<>();

                // Build feature array for tokensToFeatureIndices
                String[][] singleTokenFeatures = new String[1][featuresIndices.size()];

                for (int j = 0; j < featuresIndices.size(); j++) {
                    int featureColumn = featuresIndices.get(j);
                    // Match the fix: use featureColumn + 1 since Python's features = pieces[1:]
                    int adjustedColumn = featureColumn + 1;
                    String featureValue = adjustedColumn < parts.length ? parts[adjustedColumn] : null;
                    singleTokenFeatures[0][j] = featureValue;
                }

                // Get mapped indices
                long[][] mappedIndices = preprocessor.tokensToFeatureIndices(singleTokenFeatures, 1);

                for (int j = 0; j < featuresIndices.size(); j++) {
                    int featureColumn = featuresIndices.get(j);
                    String featureValue = singleTokenFeatures[0][j];
                    long mappedIndex = mappedIndices != null ? mappedIndices[0][j] : 0;

                    Map<String, Object> featInfo = new LinkedHashMap<>();
                    featInfo.put("featureColumn", featureColumn);
                    featInfo.put("featureValue", featureValue);
                    featInfo.put("mappedIndex", mappedIndex);
                    tokenFeatList.add(featInfo);
                }

                featureOutput.add(tokenFeatList);
            }
            output.put("feature_indices", featureOutput);
        }

        // Write output
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(output);
        Files.writeString(Paths.get(OUTPUT_FILE), json);

        System.out.println("Java preprocessing output written to: " + OUTPUT_FILE);
        System.out.println("\nSample of first 5 tokens:");
        for (int i = 0; i < Math.min(5, limit); i++) {
            System.out.println("\nToken " + i + ": '" + tokens.get(i) + "'");
            System.out.print("  Char indices (first 10): [");
            long[] chars = charIndices.get(i);
            for (int j = 0; j < Math.min(10, chars.length); j++) {
                if (j > 0)
                    System.out.print(", ");
                System.out.print(chars[j]);
            }
            System.out.println("]");
        }

        assertTrue("Output file should exist", Files.exists(Paths.get(OUTPUT_FILE)));
    }
}
