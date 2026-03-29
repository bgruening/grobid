package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.BoundingBox;

/**
 * An inline citation reference marker (e.g. "[1]", "(Smith 2020)").
 *
 * @param markerText   the display text of the marker as it appears in the document
 * @param targetBibId  the bibliography entry identifier (e.g. "b0"), or null if unresolved
 * @param bibOrdinal   the ordinal position in the bibliography (-1 if unresolved)
 * @param coords       bounding box coordinates in the PDF, if available
 */
public record CitationRef(
        String markerText,
        String targetBibId,
        int bibOrdinal,
        List<BoundingBox> coords) implements InlineContent {
}
