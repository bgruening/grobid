package org.grobid.core.engines.tagging.delft;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for CRFDecoder Viterbi decoding.
 * 
 * Tests verify:
 * - Basic Viterbi decoding finds optimal path
 * - Mask handling for variable length sequences
 * - Transition matrix usage
 * - Start/end transitions
 * - Batch decoding
 */
public class CRFDecoderTest {

    private CRFDecoder decoder;

    @Before
    public void setUp() {
        // Create a simple 3-tag CRF decoder (O, B-TITLE, I-TITLE)
        // Transition matrix [from_tag][to_tag]
        float[][] transitions = {
                // to: O, B-TITLE, I-TITLE
                { 0.5f, 0.3f, -1.0f }, // from O: prefer O or B-TITLE, penalize I-TITLE
                { 0.2f, 0.1f, 0.6f }, // from B-TITLE: prefer I-TITLE
                { 0.3f, 0.1f, 0.5f } // from I-TITLE: prefer continuing I-TITLE
        };

        // Start transitions: prefer starting with O or B-TITLE
        float[] startTransitions = { 0.5f, 0.4f, -1.0f };

        // End transitions: all tags can end
        float[] endTransitions = { 0.0f, 0.0f, 0.0f };

        decoder = new CRFDecoder(transitions, startTransitions, endTransitions);
    }

    @Test
    public void testGetNumTags() {
        assertEquals(3, decoder.getNumTags());
    }

    /**
     * Test basic decoding with strong emissions for each tag.
     */
    @Test
    public void testDecode_followsStrongEmissions() {
        // Emissions strongly favor: [O, B-TITLE, I-TITLE, O]
        float[][] emissions = {
                { 2.0f, -1.0f, -1.0f }, // Position 0: strongly O
                { -1.0f, 2.0f, -1.0f }, // Position 1: strongly B-TITLE
                { -1.0f, -1.0f, 2.0f }, // Position 2: strongly I-TITLE
                { 2.0f, -1.0f, -1.0f }, // Position 3: strongly O
        };

        int[] result = decoder.decode(emissions, null);

        assertEquals(4, result.length);
        assertEquals(0, result[0]); // O
        assertEquals(1, result[1]); // B-TITLE
        assertEquals(2, result[2]); // I-TITLE
        assertEquals(0, result[3]); // O
    }

    /**
     * Test that transitions influence decoding when emissions are ambiguous.
     */
    @Test
    public void testDecode_transitionsInfluenceDecoding() {
        // Emissions are all equal - transitions should decide
        float[][] emissions = {
                { 0.0f, 0.0f, 0.0f }, // Position 0: ambiguous
                { 0.0f, 0.0f, 0.0f }, // Position 1: ambiguous
        };

        int[] result = decoder.decode(emissions, null);

        assertEquals(2, result.length);
        // With our transition matrix, starting with O is preferred
        // (startTransitions[0]=0.5)
        // and O->O has good transition (0.5)
        assertEquals(0, result[0]); // Should start with O
    }

    /**
     * Test that I-TITLE cannot start a sequence (penalized by startTransitions).
     */
    @Test
    public void testDecode_cannotStartWithContinuation() {
        // Position 0 emissions favor I-TITLE, but start transitions penalize it
        float[][] emissions = {
                { 0.0f, 0.0f, 0.5f }, // Slightly favor I-TITLE
        };

        int[] result = decoder.decode(emissions, null);

        // Should NOT be I-TITLE (index 2) because start transitions penalize it
        assertNotEquals(2, result[0]);
    }

    /**
     * Test decoding with mask - only valid positions are decoded.
     */
    @Test
    public void testDecode_respectsMask() {
        float[][] emissions = {
                { 2.0f, -1.0f, -1.0f }, // Position 0: O
                { -1.0f, 2.0f, -1.0f }, // Position 1: B-TITLE
                { -1.0f, -1.0f, 2.0f }, // Position 2: I-TITLE (masked out)
                { 2.0f, -1.0f, -1.0f }, // Position 3: O (masked out)
        };

        boolean[] mask = { true, true, false, false };

        int[] result = decoder.decode(emissions, mask);

        // Only first 2 positions should be decoded
        assertEquals(2, result.length);
        assertEquals(0, result[0]); // O
        assertEquals(1, result[1]); // B-TITLE
    }

    /**
     * Test decoding empty sequence (all masked out).
     */
    @Test
    public void testDecode_emptySequence() {
        float[][] emissions = {
                { 2.0f, -1.0f, -1.0f },
                { -1.0f, 2.0f, -1.0f },
        };

        boolean[] mask = { false, false };

        int[] result = decoder.decode(emissions, mask);

        assertEquals(0, result.length);
    }

    /**
     * Test batch decoding.
     */
    @Test
    public void testDecodeBatch() {
        float[][][] emissions = {
                // Sequence 0: [O, B-TITLE]
                {
                        { 2.0f, -1.0f, -1.0f },
                        { -1.0f, 2.0f, -1.0f },
                },
                // Sequence 1: [B-TITLE, I-TITLE]
                {
                        { -1.0f, 2.0f, -1.0f },
                        { -1.0f, -1.0f, 2.0f },
                }
        };

        int[][] results = decoder.decodeBatch(emissions, null);

        assertEquals(2, results.length);

        assertEquals(2, results[0].length);
        assertEquals(0, results[0][0]); // O
        assertEquals(1, results[0][1]); // B-TITLE

        assertEquals(2, results[1].length);
        assertEquals(1, results[1][0]); // B-TITLE
        assertEquals(2, results[1][1]); // I-TITLE
    }

    /**
     * Test batch decoding with masks.
     */
    @Test
    public void testDecodeBatch_withMasks() {
        float[][][] emissions = {
                // Sequence 0: 3 positions, but only 2 valid
                {
                        { 2.0f, -1.0f, -1.0f },
                        { -1.0f, 2.0f, -1.0f },
                        { -1.0f, -1.0f, 2.0f },
                },
                // Sequence 1: 3 positions, only 1 valid
                {
                        { 2.0f, -1.0f, -1.0f },
                        { -1.0f, 2.0f, -1.0f },
                        { -1.0f, -1.0f, 2.0f },
                }
        };

        boolean[][] masks = {
                { true, true, false },
                { true, false, false }
        };

        int[][] results = decoder.decodeBatch(emissions, masks);

        assertEquals(2, results[0].length); // 2 valid positions
        assertEquals(1, results[1].length); // 1 valid position
    }

    /**
     * Test single position decoding.
     */
    @Test
    public void testDecode_singlePosition() {
        float[][] emissions = {
                { -1.0f, 2.0f, -1.0f }, // Only B-TITLE
        };

        int[] result = decoder.decode(emissions, null);

        assertEquals(1, result.length);
        assertEquals(1, result[0]); // B-TITLE
    }
}
