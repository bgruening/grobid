package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;

/**
 * A paragraph containing a sequence of inline content elements (text, citations, refs, etc.).
 *
 * @param content    the inline content sequence (mixed text spans and typed references)
 * @param tokens     the original layout tokens, preserved for coordinate computation
 * @param coords     bounding box coordinates of this paragraph in the PDF
 * @param id         a unique identifier for this paragraph (may be null)
 * @param sentences  sentence-level segmentation, populated when sentence segmentation is enabled
 */
public record Paragraph(
        List<InlineContent> content,
        List<LayoutToken> tokens,
        List<BoundingBox> coords,
        String id,
        List<SentenceData> sentences) implements BodyElement {
}
