package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.BoundingBox;

/**
 * An inline footnote or marginal note callout (typically a superscript number or symbol).
 *
 * @param markerText  the display text of the callout (e.g. "1", "*")
 * @param noteId      identifier linking to the corresponding Note object
 * @param coords      bounding box coordinates in the PDF, if available
 */
public record FootnoteRef(
        String markerText,
        String noteId,
        List<BoundingBox> coords) implements InlineContent {
}
