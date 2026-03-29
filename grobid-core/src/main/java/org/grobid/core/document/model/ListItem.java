package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.LayoutToken;

/**
 * A single item within a {@link ListBlock}.
 *
 * @param content  the inline content of this list item
 * @param tokens   the original layout tokens for coordinate computation
 */
public record ListItem(
        List<InlineContent> content,
        List<LayoutToken> tokens) {
}
