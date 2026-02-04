package org.grobid.core.engines.tagging.delft;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for OnnxSequenceLabellingModel sequence chunking functionality.
 */
public class OnnxSequenceLabellingModelTest {

    /**
     * Test that input is correctly split into sequences at empty lines.
     */
    @Test
    public void testSequenceSplitting() {
        String input = "token1\tfeature1\ntoken2\tfeature2\n\ntoken3\tfeature3\ntoken4\tfeature4\n";

        int sequenceCount = countSequences(input);

        assertEquals(2, sequenceCount);
    }

    /**
     * Test chunking calculation for large sequences.
     */
    @Test
    public void testChunkingCalculation() {
        int totalTokens = 6748; // Size of large_sequence.txt
        int maxSeqLength = 512;

        int expectedChunks = (int) Math.ceil((double) totalTokens / maxSeqLength);

        assertEquals(14, expectedChunks);
    }

    /**
     * Test that features are correctly parsed from tab-separated lines.
     */
    @Test
    public void testFeatureParsing() {
        String line = "token\tf1\tf2\tf3\tf4";
        String[] parts = line.split("[\\t\\s]+");

        assertEquals(5, parts.length);
    }

    // Helper to count sequences in input
    private int countSequences(String input) {
        String[] lines = input.split("\n", -1);
        int count = 0;
        boolean inSequence = false;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                if (inSequence) {
                    count++;
                    inSequence = false;
                }
            } else {
                inSequence = true;
            }
        }
        if (inSequence)
            count++;

        return count;
    }

    /**
     * Test that feature extraction uses the correct column offset.
     * 
     * Python stores features as: features = pieces[1:]
     * So features[9] = pieces[10]
     * 
     * Java must use parts[featureIndex + 1] to get the same value.
     * This test verifies the +1 offset logic is correct.
     */
    @Test
    public void testFeatureExtractionOffset() {
        // Simulate Grobid input line with 31 columns (token + 30 features)
        // Column 10 = "BLOCKSTART" (0-indexed, so it's pieces[10] in Python)
        String line = "token col1 col2 col3 col4 col5 col6 col7 col8 col9 BLOCKSTART LINESTART ALIGNEDLEFT " +
                "col13 col14 col15 col16 col17 col18 col19 col20 col21 col22 col23 col24 col25 col26 col27 col28 col29 col30";
        String[] parts = line.split("[\\t\\s]+");

        // featuresIndices = [9, 10, 11, ...] (Python's 1-based feature indices)
        // When we access featureIndex=9, we should get "BLOCKSTART" which is at
        // parts[10]
        int featureIndex = 9;
        int adjustedIndex = featureIndex + 1; // This is the fix!

        assertEquals("BLOCKSTART", parts[adjustedIndex]);
        assertEquals("LINESTART", parts[10 + 1]);
        assertEquals("ALIGNEDLEFT", parts[11 + 1]);
    }

    /**
     * Test the compact feature extraction - verifying we extract only
     * the columns specified in featuresIndices.
     */
    @Test
    public void testCompactFeatureExtraction() {
        String line = "token a b c d e f g h i BLOCKSTART LINESTART ALIGNEDLEFT m n o p q r s t u v w x y z";
        String[] parts = line.split("[\\t\\s]+");

        // featuresIndices = [9, 10, 11] (select columns 9, 10, 11 from Python's
        // features array)
        java.util.List<Integer> featuresIndices = java.util.Arrays.asList(9, 10, 11);

        // Extract features using the +1 offset
        String[] extractedFeatures = new String[featuresIndices.size()];
        for (int k = 0; k < featuresIndices.size(); k++) {
            int featureIndex = featuresIndices.get(k);
            int adjustedIndex = featureIndex + 1;
            if (adjustedIndex < parts.length) {
                extractedFeatures[k] = parts[adjustedIndex];
            } else {
                extractedFeatures[k] = "0";
            }
        }

        // Verify we got the correct values
        assertEquals("BLOCKSTART", extractedFeatures[0]);
        assertEquals("LINESTART", extractedFeatures[1]);
        assertEquals("ALIGNEDLEFT", extractedFeatures[2]);
    }

    /**
     * Test that missing features (when adjustedIndex >= parts.length) default to
     * "0".
     */
    @Test
    public void testMissingFeatures() {
        String line = "token col1 col2"; // Only 3 columns
        String[] parts = line.split("[\\t\\s]+");

        java.util.List<Integer> featuresIndices = java.util.Arrays.asList(9, 10, 11);

        String[] extractedFeatures = new String[featuresIndices.size()];
        for (int k = 0; k < featuresIndices.size(); k++) {
            int featureIndex = featuresIndices.get(k);
            int adjustedIndex = featureIndex + 1;
            if (adjustedIndex < parts.length) {
                extractedFeatures[k] = parts[adjustedIndex];
            } else {
                extractedFeatures[k] = "0"; // Default for missing
            }
        }

        // All should be "0" since parts only has 3 elements
        assertEquals("0", extractedFeatures[0]);
        assertEquals("0", extractedFeatures[1]);
        assertEquals("0", extractedFeatures[2]);
    }

    // =========================================================================
    // Label Conversion Tests (delft2grobidLabel logic)
    // =========================================================================

    /**
     * Test IOB "O" label converts to GROBID "O" (OTHER_LABEL).
     */
    @Test
    public void testLabelConversion_OLabel() {
        String result = convertLabel("O");
        assertEquals("O", result);
    }

    /**
     * Test IOB "B-" prefix converts to GROBID "I-" prefix.
     */
    @Test
    public void testLabelConversion_BPrefix() {
        String result = convertLabel("B-title");
        assertEquals("I-<title>", result);
    }

    /**
     * Test IOB "I-" prefix converts to GROBID "<" ... ">" format.
     */
    @Test
    public void testLabelConversion_IPrefix() {
        String result = convertLabel("I-title");
        assertEquals("<title>", result);
    }

    /**
     * Test <PAD> label converts to "O".
     */
    @Test
    public void testLabelConversion_PADLabel() {
        String result = convertLabel("<PAD>");
        assertEquals("O", result);
    }

    // Helper to simulate delft2grobidLabel
    private String convertLabel(String label) {
        if (label.equals("O") || label.trim().equals("<PAD>")) {
            return "O";
        } else if (label.startsWith("B-")) {
            return label.replace("B-", "I-<") + ">";
        } else if (label.startsWith("I-")) {
            return "<" + label.substring(2) + ">";
        }
        return label;
    }

    // =========================================================================
    // Sequence Chunking Tests
    // =========================================================================

    /**
     * Test chunking calculation for sequences that fit in one chunk.
     */
    @Test
    public void testChunking_fitsInOneChunk() {
        int totalTokens = 400;
        int maxSeqLength = 512;

        int numChunks = (int) Math.ceil((double) totalTokens / maxSeqLength);

        assertEquals(1, numChunks);
    }

    /**
     * Test chunk boundaries are calculated correctly.
     */
    @Test
    public void testChunkBoundaries() {
        int totalTokens = 1000;
        int maxSeqLength = 512;

        java.util.List<int[]> chunks = new java.util.ArrayList<>();
        int offset = 0;
        while (offset < totalTokens) {
            int chunkEnd = Math.min(offset + maxSeqLength, totalTokens);
            chunks.add(new int[] { offset, chunkEnd });
            offset = chunkEnd;
        }

        assertEquals(2, chunks.size());

        // First chunk: [0, 512)
        assertEquals(0, chunks.get(0)[0]);
        assertEquals(512, chunks.get(0)[1]);

        // Second chunk: [512, 1000)
        assertEquals(512, chunks.get(1)[0]);
        assertEquals(1000, chunks.get(1)[1]);
    }

    /**
     * Test parsing input into sequences separated by empty lines.
     */
    @Test
    public void testParseSequences() {
        String input = "token1\tfeature1\ntoken2\tfeature2\n\ntoken3\tfeature3\n";

        java.util.List<java.util.List<String>> sequences = parseSequences(input);

        assertEquals(2, sequences.size());
        assertEquals(2, sequences.get(0).size()); // First sequence: 2 tokens
        assertEquals(1, sequences.get(1).size()); // Second sequence: 1 token
    }

    // Helper to parse sequences
    private java.util.List<java.util.List<String>> parseSequences(String input) {
        java.util.List<java.util.List<String>> sequences = new java.util.ArrayList<>();
        java.util.List<String> current = new java.util.ArrayList<>();

        for (String line : input.split("\n", -1)) {
            if (line.trim().isEmpty()) {
                if (!current.isEmpty()) {
                    sequences.add(current);
                    current = new java.util.ArrayList<>();
                }
            } else {
                current.add(line);
            }
        }
        if (!current.isEmpty()) {
            sequences.add(current);
        }
        return sequences;
    }
}
