package org.grobid.core.document.model;

import java.util.List;

import org.grobid.core.layout.LayoutToken;

/**
 * A block of text labeled as "other" by the CRF model — content that doesn't fit
 * into sections, paragraphs, lists, or equations (e.g. page headers, noise).
 *
 * @param text    the text content
 * @param tokens  the original layout tokens for coordinate computation
 */
public record OtherBlock(
        String text,
        List<LayoutToken> tokens) implements BodyElement {
}
