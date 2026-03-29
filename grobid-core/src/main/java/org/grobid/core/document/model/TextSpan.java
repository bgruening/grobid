package org.grobid.core.document.model;

/**
 * Plain text content within a paragraph.
 */
public record TextSpan(String text) implements InlineContent {
}
