package org.grobid.core.document.model;

/**
 * Sealed interface representing an inline element within a paragraph.
 * Paragraphs contain a sequence of InlineContent elements — plain text
 * interspersed with typed references (citations, figures, tables, etc.).
 */
public sealed interface InlineContent
        permits TextSpan, CitationRef, FigureRef, TableRef, EquationRef, FootnoteRef, URLRef {
}
