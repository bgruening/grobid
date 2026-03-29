package org.grobid.core.document.model;

/**
 * Sealed interface representing a block-level element in the document body.
 * Each variant corresponds to a distinct structural element produced by the CRF labelling.
 */
public sealed interface BodyElement permits Section, Paragraph, ListBlock, EquationBlock, OtherBlock {
}
