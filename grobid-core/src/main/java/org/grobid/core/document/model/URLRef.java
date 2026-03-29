package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.BoundingBox;

/**
 * An inline URL or hyperlink detected in the text.
 *
 * @param text         the display text of the link
 * @param destination  the URL target (may differ from display text if PDF annotation provides it)
 * @param coords       bounding box coordinates in the PDF, if available
 */
public record URLRef(
        String text,
        String destination,
        List<BoundingBox> coords) implements InlineContent {
}
