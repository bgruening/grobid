package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.BoundingBox;

/**
 * A sentence within a paragraph, populated when sentence segmentation is enabled.
 *
 * @param content  the inline content sequence within this sentence
 * @param coords   bounding box coordinates of this sentence in the PDF
 * @param id       a unique identifier for this sentence (may be null)
 */
public record SentenceData(
        List<InlineContent> content,
        List<BoundingBox> coords,
        String id) {
}
