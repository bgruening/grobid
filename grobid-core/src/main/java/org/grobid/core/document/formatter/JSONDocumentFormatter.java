package org.grobid.core.document.formatter;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.data.*;
import org.grobid.core.document.model.*;
import org.grobid.core.layout.BoundingBox;

/**
 * Produces JSON output from a {@link GrobidDocument} data model.
 *
 * <p>The output structure follows the TEI2LossyJSON format from grobid-client-python,
 * which is inspired by the CORD-19 dataset format. Key features:</p>
 * <ul>
 *   <li>{@code biblio} — header metadata with abstract passages including reference offsets</li>
 *   <li>{@code body_text} — array of passage objects with text, section headings, and inline reference offsets</li>
 *   <li>{@code figures_and_tables} — figures and tables with coordinates</li>
 *   <li>{@code references} — comprehensive bibliographic data per entry</li>
 * </ul>
 */
public class JSONDocumentFormatter {

    private final ObjectMapper mapper;

    public JSONDocumentFormatter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Format a complete GrobidDocument as JSON.
     */
    public String format(GrobidDocument grobidDoc) {
        try {
            ObjectNode root = mapper.createObjectNode();

            root.put("level", "paragraph");

            // Biblio (header metadata)
            ObjectNode biblio = buildBiblio(grobidDoc);
            root.set("biblio", biblio);

            // Body text
            ArrayNode bodyText = buildBodyText(grobidDoc);
            root.set("body_text", bodyText);

            // Figures and tables
            ArrayNode figuresAndTables = buildFiguresAndTables(grobidDoc);
            root.set("figures_and_tables", figuresAndTables);

            // References
            ArrayNode references = buildReferences(grobidDoc);
            root.set("references", references);

            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Error formatting GrobidDocument as JSON", e);
        }
    }

    private ObjectNode buildBiblio(GrobidDocument grobidDoc) {
        ObjectNode biblio = mapper.createObjectNode();
        BiblioItem header = grobidDoc.getHeader();

        if (header == null) {
            return biblio;
        }

        biblio.put("title", header.getTitle() != null ? header.getTitle() : "");

        // Authors
        ArrayNode authors = mapper.createArrayNode();
        if (header.getFullAuthors() != null) {
            for (Person author : header.getFullAuthors()) {
                String name = formatPersonName(author);
                if (StringUtils.isNotBlank(name)) {
                    authors.add(name);
                }
            }
        }
        biblio.set("authors", authors);

        // DOI
        if (header.getDOI() != null) {
            biblio.put("doi", header.getDOI());
        }

        // MD5
        if (grobidDoc.getMd5() != null) {
            biblio.put("hash", grobidDoc.getMd5());
        }

        // Publication date
        if (header.getNormalizedPublicationDate() != null) {
            biblio.put("publication_date", Date.toISOString(header.getNormalizedPublicationDate()));
        }
        if (header.getPublicationDate() != null) {
            biblio.put("publication_date_text", header.getPublicationDate());
        }

        // Publisher
        if (header.getPublisher() != null) {
            biblio.put("publisher", header.getPublisher());
        }

        // Journal
        if (header.getJournal() != null) {
            biblio.put("journal", header.getJournal());
        }
        if (header.getJournalAbbrev() != null) {
            biblio.put("journal_abbr", header.getJournalAbbrev());
        }

        // Abstract — formatted as array of passage objects
        ArrayNode abstractArray = buildAbstractPassages(grobidDoc);
        biblio.set("abstract", abstractArray);

        return biblio;
    }

    private ArrayNode buildAbstractPassages(GrobidDocument grobidDoc) {
        ArrayNode abstractArray = mapper.createArrayNode();

        BiblioItem header = grobidDoc.getHeader();
        if (header != null && header.getAbstract() != null) {
            ObjectNode passage = mapper.createObjectNode();
            passage.put("id", 0);
            passage.put("text", header.getAbstract());
            passage.set("coords", mapper.createArrayNode());
            passage.set("refs", mapper.createArrayNode());
            abstractArray.add(passage);
        }

        return abstractArray;
    }

    private ArrayNode buildBodyText(GrobidDocument grobidDoc) {
        ArrayNode bodyText = mapper.createArrayNode();

        if (CollectionUtils.isEmpty(grobidDoc.getBody())) {
            return bodyText;
        }

        String currentHeadSection = "";
        for (BodyElement element : grobidDoc.getBody()) {
            if (element instanceof Section section) {
                currentHeadSection = section.getHeading() != null ? section.getHeading() : "";
                // Recurse into section children
                buildPassagesFromElements(bodyText, section.getChildren(), currentHeadSection, null);
            } else if (element instanceof Paragraph paragraph) {
                ObjectNode passage = buildPassage(paragraph, currentHeadSection, null);
                bodyText.add(passage);
            }
        }

        return bodyText;
    }

    private void buildPassagesFromElements(
            ArrayNode bodyText,
            List<BodyElement> elements,
            String headSection,
            String headParagraph) {
        for (BodyElement element : elements) {
            switch (element) {
                case Section section -> {
                    String subHeading = section.getHeading() != null ? section.getHeading() : headSection;
                    buildPassagesFromElements(bodyText, section.getChildren(), subHeading, null);
                }
                case Paragraph paragraph -> {
                    ObjectNode passage = buildPassage(paragraph, headSection, headParagraph);
                    bodyText.add(passage);
                }
                case ListBlock listBlock -> {
                    // Convert list items to a paragraph-like passage
                    StringBuilder listText = new StringBuilder();
                    for (ListItem item : listBlock.items()) {
                        listText.append("- ").append(inlineContentToText(item.content())).append("\n");
                    }
                    ObjectNode passage = mapper.createObjectNode();
                    passage.put("text", listText.toString().trim());
                    passage.put("head_section", headSection);
                    passage.putNull("head_paragraph");
                    passage.set("coords", mapper.createArrayNode());
                    passage.set("refs", mapper.createArrayNode());
                    bodyText.add(passage);
                }
                case EquationBlock eq -> {
                    if (eq.equation() != null && eq.equation().getContent() != null) {
                        ObjectNode passage = mapper.createObjectNode();
                        passage.put("text", eq.equation().getContent());
                        passage.put("head_section", headSection);
                        passage.putNull("head_paragraph");
                        passage.set("coords", mapper.createArrayNode());
                        passage.set("refs", mapper.createArrayNode());
                        bodyText.add(passage);
                    }
                }
                case OtherBlock other -> {
                    // Skip "other" blocks in JSON
                }
            }
        }
    }

    /**
     * Build a passage object from a Paragraph, computing reference offsets.
     */
    private ObjectNode buildPassage(Paragraph paragraph, String headSection, String headParagraph) {
        ObjectNode passage = mapper.createObjectNode();

        if (paragraph.id() != null) {
            passage.put("id", paragraph.id());
        }

        // Compute text and reference offsets
        StringBuilder text = new StringBuilder();
        ArrayNode refs = mapper.createArrayNode();

        if (paragraph.content() != null) {
            for (InlineContent ic : paragraph.content()) {
                switch (ic) {
                    case TextSpan ts -> text.append(ts.text());
                    case CitationRef cr -> {
                        int start = text.length();
                        text.append(cr.markerText());
                        int end = text.length();
                        ObjectNode ref = mapper.createObjectNode();
                        ref.put("type", "bibr");
                        if (cr.targetBibId() != null) {
                            ref.put("target", "#" + cr.targetBibId());
                        }
                        ref.put("text", cr.markerText());
                        ref.put("offset_start", start);
                        ref.put("offset_end", end);
                        refs.add(ref);
                    }
                    case FigureRef fr -> {
                        int start = text.length();
                        text.append(fr.markerText());
                        int end = text.length();
                        ObjectNode ref = mapper.createObjectNode();
                        ref.put("type", "figure");
                        if (fr.targetId() != null) {
                            ref.put("target", "#" + fr.targetId());
                        }
                        ref.put("text", fr.markerText());
                        ref.put("offset_start", start);
                        ref.put("offset_end", end);
                        refs.add(ref);
                    }
                    case TableRef tr -> {
                        int start = text.length();
                        text.append(tr.markerText());
                        int end = text.length();
                        ObjectNode ref = mapper.createObjectNode();
                        ref.put("type", "table");
                        if (tr.targetId() != null) {
                            ref.put("target", "#" + tr.targetId());
                        }
                        ref.put("text", tr.markerText());
                        ref.put("offset_start", start);
                        ref.put("offset_end", end);
                        refs.add(ref);
                    }
                    case EquationRef er -> {
                        int start = text.length();
                        text.append(er.markerText());
                        int end = text.length();
                        ObjectNode ref = mapper.createObjectNode();
                        ref.put("type", "formula");
                        if (er.targetId() != null) {
                            ref.put("target", "#" + er.targetId());
                        }
                        ref.put("text", er.markerText());
                        ref.put("offset_start", start);
                        ref.put("offset_end", end);
                        refs.add(ref);
                    }
                    case FootnoteRef fnr -> text.append(fnr.markerText());
                    case URLRef ur -> text.append(ur.text());
                }
            }
        }

        passage.put("text", text.toString());
        passage.put("head_section", headSection != null ? headSection : "");
        if (headParagraph != null) {
            passage.put("head_paragraph", headParagraph);
        } else {
            passage.putNull("head_paragraph");
        }

        // Coordinates
        ArrayNode coords = mapper.createArrayNode();
        if (CollectionUtils.isNotEmpty(paragraph.coords())) {
            for (BoundingBox box : paragraph.coords()) {
                coords.add(boundingBoxToJson(box));
            }
        }
        passage.set("coords", coords);

        passage.set("refs", refs);

        return passage;
    }

    private ArrayNode buildFiguresAndTables(GrobidDocument grobidDoc) {
        ArrayNode result = mapper.createArrayNode();

        // Figures
        if (grobidDoc.getFigures() != null) {
            for (Figure figure : grobidDoc.getFigures()) {
                ObjectNode item = mapper.createObjectNode();
                item.put("id", figure.getId() != null ? figure.getId() : "");
                item.put("label", figure.getLabel() != null ? figure.getLabel() : "");
                item.put("head", figure.getHeader() != null ? figure.getHeader() : "");
                item.put("type", "figure");
                item.put("desc", figure.getCaption() != null ? figure.getCaption() : "");
                item.put("note", "");
                item.set("coords", mapper.createArrayNode());
                result.add(item);
            }
        }

        // Tables
        if (grobidDoc.getTables() != null) {
            for (Table table : grobidDoc.getTables()) {
                ObjectNode item = mapper.createObjectNode();
                item.put("id", table.getId() != null ? table.getId() : "");
                item.put("label", table.getLabel() != null ? table.getLabel() : "");
                item.put("head", table.getHeader() != null ? table.getHeader() : "");
                item.put("type", "table");
                item.put("desc", table.getCaption() != null ? table.getCaption() : "");
                item.put("note", table.getNote() != null ? table.getNote() : "");
                item.set("coords", mapper.createArrayNode());
                result.add(item);
            }
        }

        return result;
    }

    private ArrayNode buildReferences(GrobidDocument grobidDoc) {
        ArrayNode result = mapper.createArrayNode();

        if (CollectionUtils.isEmpty(grobidDoc.getReferences())) {
            return result;
        }

        for (int i = 0; i < grobidDoc.getReferences().size(); i++) {
            BibDataSet bds = grobidDoc.getReferences().get(i);
            BiblioItem bib = bds.getResBib();
            if (bib == null)
                continue;

            ObjectNode ref = mapper.createObjectNode();
            ref.put("id", "b" + bib.getOrdinal());

            if (bib.getTitle() != null)
                ref.put("title", bib.getTitle());

            // Authors
            ArrayNode authors = mapper.createArrayNode();
            if (bib.getFullAuthors() != null) {
                for (Person author : bib.getFullAuthors()) {
                    ObjectNode authorNode = mapper.createObjectNode();
                    if (author.getFirstName() != null)
                        authorNode.put("forename", author.getFirstName());
                    if (author.getLastName() != null)
                        authorNode.put("surname", author.getLastName());
                    authorNode.put("name", formatPersonName(author));
                    authors.add(authorNode);
                }
            }
            ref.set("authors", authors);

            if (bib.getJournal() != null)
                ref.put("journal", bib.getJournal());
            if (bib.getVolume() != null)
                ref.put("volume", bib.getVolume());
            if (bib.getIssue() != null)
                ref.put("issue", bib.getIssue());
            if (bib.getPageRange() != null)
                ref.put("pages", bib.getPageRange());
            if (bib.getPublicationDate() != null)
                ref.put("year", bib.getPublicationDate());
            if (bib.getDOI() != null)
                ref.put("doi", bib.getDOI());
            if (bib.getPublisher() != null)
                ref.put("publisher", bib.getPublisher());

            // Raw reference text
            if (bds.getRawBib() != null)
                ref.put("raw_reference", bds.getRawBib());

            result.add(ref);
        }

        return result;
    }

    // -- Helpers --

    private String formatPersonName(Person person) {
        StringBuilder name = new StringBuilder();
        if (person.getFirstName() != null) {
            name.append(person.getFirstName());
        }
        if (person.getMiddleName() != null) {
            if (name.length() > 0)
                name.append(" ");
            name.append(person.getMiddleName());
        }
        if (person.getLastName() != null) {
            if (name.length() > 0)
                name.append(" ");
            name.append(person.getLastName());
        }
        return name.toString();
    }

    private String inlineContentToText(List<InlineContent> content) {
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

    private ObjectNode boundingBoxToJson(BoundingBox box) {
        ObjectNode node = mapper.createObjectNode();
        node.put("p", box.getPage());
        node.put("x", box.getX());
        node.put("y", box.getY());
        node.put("w", box.getWidth());
        node.put("h", box.getHeight());
        return node;
    }
}
