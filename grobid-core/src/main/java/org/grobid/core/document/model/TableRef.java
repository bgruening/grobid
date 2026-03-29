package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.BoundingBox;

/**
 * An inline reference to a table (e.g. "Table 1", "Tab. 2").
 *
 * @param markerText  the display text of the marker
 * @param targetId    the table identifier (e.g. "tab_0"), or null if unresolved
 * @param coords      bounding box coordinates in the PDF, if available
 */
public record TableRef(
        String markerText,
        String targetId,
        List<BoundingBox> coords) implements InlineContent {
}
