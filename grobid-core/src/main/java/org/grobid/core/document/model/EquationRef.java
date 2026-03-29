package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.BoundingBox;

/**
 * An inline reference to an equation (e.g. "Eq. (1)", "(3)").
 *
 * @param markerText  the display text of the marker
 * @param targetId    the equation identifier (e.g. "formula_0"), or null if unresolved
 * @param coords      bounding box coordinates in the PDF, if available
 */
public record EquationRef(
        String markerText,
        String targetId,
        List<BoundingBox> coords) implements InlineContent {
}
