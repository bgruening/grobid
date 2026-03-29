package org.grobid.core.document.model;

import org.grobid.core.data.Equation;

/**
 * A block-level equation element.
 * Wraps the existing {@link Equation} data class which holds content, label, and coordinates.
 *
 * @param equation  the equation data from Grobid's equation parser
 */
public record EquationBlock(Equation equation) implements BodyElement {
}
