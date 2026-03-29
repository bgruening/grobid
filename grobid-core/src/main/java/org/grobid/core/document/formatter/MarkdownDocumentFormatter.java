package org.grobid.core.document.formatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.data.*;
import org.grobid.core.document.model.*;

/**
 * Produces Markdown output from a {@link GrobidDocument} data model.
 *
 * <p>The output structure follows the TEI2Markdown format from grobid-client-python:</p>
 * <ul>
 *   <li>{@code # Title} as H1</li>
 *   <li>Authors listed one per line</li>
 *   <li>Affiliations comma-separated</li>
 *   <li>{@code Published on Month DD, YYYY}</li>
 *   <li>Abstract text</li>
 *   <li>Sections as {@code ### heading}</li>
 *   <li>References as {@code **[N]** ...}</li>
 * </ul>
 */
public class MarkdownDocumentFormatter {

    /**
     * Format a complete GrobidDocument as Markdown.
     */
    public String format(GrobidDocument grobidDoc) {
        StringBuilder md = new StringBuilder();

        // Title
        formatTitle(md, grobidDoc);

        // Authors
        formatAuthors(md, grobidDoc);

        // Affiliations
        formatAffiliations(md, grobidDoc);

        // Publication date
        formatPublicationDate(md, grobidDoc);

        // Abstract
        formatAbstract(md, grobidDoc);

        // Body
        formatBody(md, grobidDoc);

        // Annex (acknowledgements, etc.)
        formatAnnex(md, grobidDoc);

        // References
        formatReferences(md, grobidDoc);

        return md.toString();
    }

    private void formatTitle(StringBuilder md, GrobidDocument grobidDoc) {
        BiblioItem header = grobidDoc.getHeader();
        if (header != null && StringUtils.isNotBlank(header.getTitle())) {
            md.append("# ").append(header.getTitle().trim()).append("\n\n");
        }
    }

    private void formatAuthors(StringBuilder md, GrobidDocument grobidDoc) {
        BiblioItem header = grobidDoc.getHeader();
        if (header == null || header.getFullAuthors() == null)
            return;

        for (Person author : header.getFullAuthors()) {
            String name = formatPersonName(author);
            if (StringUtils.isNotBlank(name)) {
                md.append(name).append("\n");
            }
        }

        if (!header.getFullAuthors().isEmpty()) {
            md.append("\n");
        }
    }

    private void formatAffiliations(StringBuilder md, GrobidDocument grobidDoc) {
        BiblioItem header = grobidDoc.getHeader();
        if (header == null || header.getFullAffiliations() == null)
            return;

        StringBuilder affiliations = new StringBuilder();
        for (Affiliation aff : header.getFullAffiliations()) {
            String affText = formatAffiliation(aff);
            if (StringUtils.isNotBlank(affText)) {
                if (affiliations.length() > 0) {
                    affiliations.append(", ");
                }
                affiliations.append(affText);
            }
        }

        if (affiliations.length() > 0) {
            md.append(affiliations).append("\n\n");
        }
    }

    private void formatPublicationDate(StringBuilder md, GrobidDocument grobidDoc) {
        BiblioItem header = grobidDoc.getHeader();
        if (header == null)
            return;

        Date pubDate = header.getNormalizedPublicationDate();
        if (pubDate != null) {
            String isoDate = Date.toISOString(pubDate);
            if (StringUtils.isNotBlank(isoDate)) {
                String formatted = formatDateString(isoDate);
                md.append("Published on ").append(formatted).append("\n\n");
                return;
            }
        }

        if (StringUtils.isNotBlank(header.getPublicationDate())) {
            md.append("Published on ").append(header.getPublicationDate()).append("\n\n");
        }
    }

    private void formatAbstract(StringBuilder md, GrobidDocument grobidDoc) {
        BiblioItem header = grobidDoc.getHeader();
        if (header == null || StringUtils.isBlank(header.getAbstract()))
            return;

        md.append(header.getAbstract().trim()).append("\n\n");
    }

    private void formatBody(StringBuilder md, GrobidDocument grobidDoc) {
        if (CollectionUtils.isEmpty(grobidDoc.getBody()))
            return;

        for (BodyElement element : grobidDoc.getBody()) {
            formatBodyElement(md, element);
        }
    }

    private void formatBodyElement(StringBuilder md, BodyElement element) {
        switch (element) {
            case Section section -> {
                if (StringUtils.isNotBlank(section.getHeading())) {
                    md.append("### ").append(section.getHeading().trim()).append("\n\n");
                }
                for (BodyElement child : section.getChildren()) {
                    formatBodyElement(md, child);
                }
            }
            case Paragraph paragraph -> {
                String text = inlineContentToMarkdown(paragraph.content());
                if (StringUtils.isNotBlank(text)) {
                    md.append(text.trim()).append("\n\n");
                }
            }
            case ListBlock listBlock -> {
                for (ListItem item : listBlock.items()) {
                    String text = inlineContentToMarkdown(item.content());
                    md.append("- ").append(text.trim()).append("\n");
                }
                md.append("\n");
            }
            case EquationBlock eqBlock -> {
                Equation eq = eqBlock.equation();
                if (eq != null && StringUtils.isNotBlank(eq.getContent())) {
                    md.append("*").append(eq.getContent().trim()).append("*");
                    if (StringUtils.isNotBlank(eq.getLabel())) {
                        md.append(" ").append(eq.getLabel().trim());
                    }
                    md.append("\n\n");
                }
            }
            case OtherBlock other -> {
                // Skip other blocks in Markdown
            }
        }
    }

    private void formatAnnex(StringBuilder md, GrobidDocument grobidDoc) {
        // Acknowledgement
        if (CollectionUtils.isNotEmpty(grobidDoc.getAcknowledgement())) {
            md.append("### Acknowledgements\n\n");
            for (BodyElement element : grobidDoc.getAcknowledgement()) {
                formatBodyElement(md, element);
            }
        }

        // Annex content
        if (CollectionUtils.isNotEmpty(grobidDoc.getAnnex())) {
            for (BodyElement element : grobidDoc.getAnnex()) {
                formatBodyElement(md, element);
            }
        }
    }

    private void formatReferences(StringBuilder md, GrobidDocument grobidDoc) {
        if (CollectionUtils.isEmpty(grobidDoc.getReferences()))
            return;

        md.append("## References\n\n");

        for (int i = 0; i < grobidDoc.getReferences().size(); i++) {
            BibDataSet bds = grobidDoc.getReferences().get(i);
            BiblioItem bib = bds.getResBib();
            if (bib == null)
                continue;

            String refText = formatSingleReference(bib, i + 1);
            md.append(refText).append("\n");
        }
    }

    /**
     * Format a single bibliographic reference in Markdown.
     * Follows the Python TEI2Markdown._format_reference() pattern.
     */
    private String formatSingleReference(BiblioItem bib, int refNum) {
        StringBuilder ref = new StringBuilder();
        ref.append("**[").append(refNum).append("]**");

        // Title
        if (StringUtils.isNotBlank(bib.getTitle())) {
            ref.append(" ").append(bib.getTitle().trim());
        }

        // Authors
        String authors = formatReferenceAuthors(bib);
        if (StringUtils.isNotBlank(authors)) {
            ref.append(" *").append(authors).append("*");
        }

        // Journal / venue
        if (StringUtils.isNotBlank(bib.getJournal())) {
            ref.append(" *").append(bib.getJournal().trim()).append("*");
        } else if (StringUtils.isNotBlank(bib.getBookTitle())) {
            ref.append(" *").append(bib.getBookTitle().trim()).append("*");
        }

        // Year
        if (StringUtils.isNotBlank(bib.getPublicationDate())) {
            ref.append(" (").append(bib.getPublicationDate().trim()).append(")");
        }

        // Volume
        if (StringUtils.isNotBlank(bib.getVolume())) {
            ref.append(" ").append(bib.getVolume().trim());
        }

        // Pages
        if (StringUtils.isNotBlank(bib.getPageRange())) {
            ref.append(" pp. ").append(bib.getPageRange().trim());
        }

        // DOI
        if (StringUtils.isNotBlank(bib.getDOI())) {
            ref.append(" https://doi.org/").append(bib.getDOI().trim());
        }

        // Ensure proper ending punctuation
        String result = ref.toString();
        if (!result.endsWith(".")) {
            result += ".";
        }

        return result;
    }

    // -- Helpers --

    /**
     * Convert a list of InlineContent to Markdown text.
     * Reference markers are kept as plain text (e.g. "[1]").
     */
    private String inlineContentToMarkdown(List<InlineContent> content) {
        if (content == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (InlineContent ic : content) {
            switch (ic) {
                case TextSpan ts -> sb.append(ts.text());
                case CitationRef cr -> sb.append(cr.markerText());
                case FigureRef fr -> sb.append(fr.markerText());
                case TableRef tr -> sb.append(tr.markerText());
                case EquationRef er -> sb.append(er.markerText());
                case FootnoteRef fnr -> sb.append(fnr.markerText());
                case URLRef ur -> sb.append(ur.text());
            }
        }
        return sb.toString();
    }

    private String formatPersonName(Person person) {
        StringBuilder name = new StringBuilder();
        if (person.getFirstName() != null) {
            name.append(person.getFirstName().trim());
        }
        if (person.getLastName() != null) {
            if (name.length() > 0)
                name.append(" ");
            name.append(person.getLastName().trim());
        }
        return name.toString();
    }

    private String formatAffiliation(Affiliation aff) {
        if (aff == null)
            return null;
        // Prefer the full affiliation string
        if (StringUtils.isNotBlank(aff.getAffiliationString())) {
            return aff.getAffiliationString().trim();
        }
        // Fallback to institution name
        if (aff.getInstitutions() != null && !aff.getInstitutions().isEmpty()) {
            return aff.getInstitutions().get(0);
        }
        return null;
    }

    private String formatReferenceAuthors(BiblioItem bib) {
        if (bib.getFullAuthors() == null || bib.getFullAuthors().isEmpty()) {
            return null;
        }

        StringBuilder authors = new StringBuilder();
        List<Person> authorList = bib.getFullAuthors();

        for (int i = 0; i < authorList.size(); i++) {
            if (i > 0) {
                if (i == authorList.size() - 1) {
                    authors.append(" and ");
                } else {
                    authors.append(", ");
                }
            }
            authors.append(formatPersonName(authorList.get(i)));
        }

        return authors.toString();
    }

    /**
     * Format an ISO date string (e.g. "2024-01-15") to "January 15, 2024".
     */
    private String formatDateString(String isoDate) {
        if (isoDate == null)
            return "";
        try {
            // Try full date
            LocalDate date = LocalDate.parse(isoDate);
            return date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH));
        } catch (DateTimeParseException e) {
            // Try year-month
            try {
                if (isoDate.length() == 7) { // "2024-01"
                    LocalDate date = LocalDate.parse(isoDate + "-01");
                    return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
                }
            } catch (DateTimeParseException e2) {
                // Fall through
            }
            return isoDate;
        }
    }
}
