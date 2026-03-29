package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.BoundingBox;

/**
 * An inline reference to a figure (e.g. "Fig. 1", "Figure 2a").
 *
 * @param markerText  the display text of the marker
 * @param targetId    the figure identifier (e.g. "fig_0"), or null if unresolved
 * @param coords      bounding box coordinates in the PDF, if available
 */
public record FigureRef(
        String markerText,
        String targetId,
        List<BoundingBox> coords) implements InlineContent {
}
