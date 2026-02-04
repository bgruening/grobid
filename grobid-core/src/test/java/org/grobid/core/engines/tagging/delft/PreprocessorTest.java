package org.grobid.core.engines.tagging.delft;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for Preprocessor, particularly the feature extraction logic.
 * 
 * These tests validate that:
 * 1. Feature indices map correctly to vocabulary lookups
 * 2. The compact features array structure works as expected
 */
public class PreprocessorTest {

    private Preprocessor preprocessor;

    // Mock vocabulary and feature mappings
    private Map<String, Integer> charVocab;
    private Map<Integer, String> tagIndex;
    private List<Integer> featuresIndices;
    private Map<Integer, Map<String, Integer>> featuresMapToIndex;

    @Before
    public void setUp() {
        // Simple char vocabulary
        charVocab = new HashMap<>();
        charVocab.put("<PAD>", 0);
        charVocab.put("<UNK>", 1);
        charVocab.put("a", 2);
        charVocab.put("b", 3);
        charVocab.put("c", 4);

        // Simple tag index
        tagIndex = new HashMap<>();
        tagIndex.put(0, "<PAD>");
        tagIndex.put(1, "O");
        tagIndex.put(2, "B-TITLE");
        tagIndex.put(3, "I-TITLE");

        // Feature indices matching Python's 1-based indexing (columns 9, 10, 11 in
        // file)
        featuresIndices = Arrays.asList(9, 10, 11);

        // Feature vocabularies for each column
        featuresMapToIndex = new HashMap<>();

        // Column 9 vocabulary (e.g., BLOCK features)
        Map<String, Integer> col9Vocab = new HashMap<>();
        col9Vocab.put("BLOCKSTART", 1);
        col9Vocab.put("BLOCKIN", 2);
        col9Vocab.put("BLOCKEND", 3);
        featuresMapToIndex.put(9, col9Vocab);

        // Column 10 vocabulary (e.g., LINE features)
        Map<String, Integer> col10Vocab = new HashMap<>();
        col10Vocab.put("LINESTART", 1);
        col10Vocab.put("LINEIN", 2);
        col10Vocab.put("LINEEND", 3);
        featuresMapToIndex.put(10, col10Vocab);

        // Column 11 vocabulary (e.g., ALIGN features)
        Map<String, Integer> col11Vocab = new HashMap<>();
        col11Vocab.put("ALIGNEDLEFT", 1);
        col11Vocab.put("ALIGNEDRIGHT", 2);
        col11Vocab.put("CENTERED", 3);
        featuresMapToIndex.put(11, col11Vocab);

        preprocessor = new Preprocessor(charVocab, tagIndex, 30, featuresIndices, featuresMapToIndex);
    }

    @Test
    public void testHasFeatures_returnsTrue() {
        assertTrue(preprocessor.hasFeatures());
    }

    @Test
    public void testGetFeaturesIndices() {
        List<Integer> indices = preprocessor.getFeaturesIndices();
        assertEquals(3, indices.size());
        assertEquals(Integer.valueOf(9), indices.get(0));
        assertEquals(Integer.valueOf(10), indices.get(1));
        assertEquals(Integer.valueOf(11), indices.get(2));
    }

    @Test
    public void testGetNumFeatures() {
        assertEquals(3, preprocessor.getNumFeatures());
    }

    /**
     * Test that tokensToFeatureIndices correctly maps feature values to vocabulary
     * indices.
     * 
     * The features array is a COMPACT structure where:
     * - features[tokenIdx][0] contains the value for column featuresIndices[0]
     * (column 9)
     * - features[tokenIdx][1] contains the value for column featuresIndices[1]
     * (column 10)
     * etc.
     */
    @Test
    public void testTokensToFeatureIndices_mapsCorrectly() {
        // Compact features array: features[token][featureSlot]
        // Slot 0 = value for column 9, Slot 1 = value for column 10, Slot 2 = value for
        // column 11
        String[][] features = {
                { "BLOCKSTART", "LINESTART", "ALIGNEDLEFT" }, // Token 0
                { "BLOCKIN", "LINEIN", "ALIGNEDRIGHT" }, // Token 1
        };

        long[][] result = preprocessor.tokensToFeatureIndices(features, 2);

        assertNotNull(result);
        assertEquals(2, result.length); // seqLength = 2
        assertEquals(3, result[0].length); // 3 features

        // Token 0: BLOCKSTART(1), LINESTART(1), ALIGNEDLEFT(1)
        assertEquals(1, result[0][0]); // BLOCKSTART -> 1
        assertEquals(1, result[0][1]); // LINESTART -> 1
        assertEquals(1, result[0][2]); // ALIGNEDLEFT -> 1

        // Token 1: BLOCKIN(2), LINEIN(2), ALIGNEDRIGHT(2)
        assertEquals(2, result[1][0]); // BLOCKIN -> 2
        assertEquals(2, result[1][1]); // LINEIN -> 2
        assertEquals(2, result[1][2]); // ALIGNEDRIGHT -> 2
    }

    @Test
    public void testTokensToFeatureIndices_unknownValueMapsToZero() {
        String[][] features = {
                { "UNKNOWN_BLOCK", "LINESTART", "ALIGNEDLEFT" },
        };

        long[][] result = preprocessor.tokensToFeatureIndices(features, 1);

        // Unknown value maps to 0 (default)
        assertEquals(0, result[0][0]); // UNKNOWN_BLOCK -> 0
        assertEquals(1, result[0][1]); // LINESTART -> 1
        assertEquals(1, result[0][2]); // ALIGNEDLEFT -> 1
    }

    @Test
    public void testTokensToFeatureIndices_nullValueMapsToZero() {
        String[][] features = {
                { null, "LINESTART", null },
        };

        long[][] result = preprocessor.tokensToFeatureIndices(features, 1);

        // Null value keeps default 0
        assertEquals(0, result[0][0]); // null -> 0
        assertEquals(1, result[0][1]); // LINESTART -> 1
        assertEquals(0, result[0][2]); // null -> 0
    }

    @Test
    public void testTokensToFeatureIndices_padsToSeqLength() {
        String[][] features = {
                { "BLOCKSTART", "LINESTART", "ALIGNEDLEFT" },
        };

        // Request seqLength=4, but only 1 token
        long[][] result = preprocessor.tokensToFeatureIndices(features, 4);

        assertEquals(4, result.length); // Padded to seqLength

        // Token 0 should have correct values
        assertEquals(1, result[0][0]);

        // Tokens 1-3 should be all zeros (padding)
        for (int i = 1; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(0, result[i][j]);
            }
        }
    }

    @Test
    public void testTokensToFeatureIndices_truncatesToSeqLength() {
        String[][] features = {
                { "BLOCKSTART", "LINESTART", "ALIGNEDLEFT" },
                { "BLOCKIN", "LINEIN", "ALIGNEDRIGHT" },
                { "BLOCKEND", "LINEEND", "CENTERED" },
        };

        // Request seqLength=2, but 3 tokens provided
        long[][] result = preprocessor.tokensToFeatureIndices(features, 2);

        assertEquals(2, result.length); // Truncated to seqLength

        // Token 0 and 1 should have correct values
        assertEquals(1, result[0][0]); // BLOCKSTART
        assertEquals(2, result[1][0]); // BLOCKIN
    }

    @Test
    public void testTokensToFeatureIndices_handlesShortFeatureArray() {
        // Features array shorter than expected (missing slot 2)
        String[][] features = {
                { "BLOCKSTART", "LINESTART" }, // Only 2 elements, should be 3
        };

        long[][] result = preprocessor.tokensToFeatureIndices(features, 1);

        // Should not throw, and missing feature should be 0
        assertEquals(1, result[0][0]); // BLOCKSTART
        assertEquals(1, result[0][1]); // LINESTART
        // Slot 2 is missing in input, should remain 0
        assertEquals(0, result[0][2]);
    }

    @Test
    public void testNoFeatures_returnsNull() {
        // Create preprocessor without features
        Preprocessor noFeaturesPreprocessor = new Preprocessor(charVocab, tagIndex, 30);

        assertFalse(noFeaturesPreprocessor.hasFeatures());
        assertNull(noFeaturesPreprocessor.tokensToFeatureIndices(new String[][] {}, 1));
    }

    @Test
    public void testCreateMask() {
        boolean[] mask = preprocessor.createMask(3, 5);

        assertEquals(5, mask.length);
        assertTrue(mask[0]);
        assertTrue(mask[1]);
        assertTrue(mask[2]);
        assertFalse(mask[3]);
        assertFalse(mask[4]);
    }

    @Test
    public void testGetTagIndex() {
        Map<Integer, String> tags = preprocessor.getTagIndex();

        assertEquals("O", tags.get(1));
        assertEquals("B-TITLE", tags.get(2));
        assertEquals("I-TITLE", tags.get(3));
    }
}
