package org.grobid.service.util;

public enum ExpectedResponseType {
    BIBTEX, XML, JSON, MARKDOWN;

    /**
     * Parse a format string (from REST API parameter) into an ExpectedResponseType.
     * Defaults to XML if the format is null or unrecognized.
     */
    public static ExpectedResponseType fromString(String format) {
        if (format == null)
            return XML;
        return switch (format.toLowerCase().trim()) {
            case "json" -> JSON;
            case "markdown", "md" -> MARKDOWN;
            case "bibtex", "bib" -> BIBTEX;
            default -> XML;
        };
    }
}
