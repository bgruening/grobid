package org.grobid.core.document.formatter;

import java.util.List;

import com.google.common.base.Joiner;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.data.*;
import org.grobid.core.document.model.*;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.layout.Page;
import org.grobid.core.utilities.TextUtilities;

/**
 * Produces TEI XML from a {@link GrobidDocument} data model.
 *
 * <p>This formatter walks the structured data model and generates TEI XML output.
 * For Phase 1, the header and references sections delegate to the existing
 * {@code BiblioItem.toTEI()} and related methods to ensure backward compatibility.
 * The body section is generated directly from the new data model.</p>
 */
public class TEIDocumentFormatter {

    /**
     * Format a complete GrobidDocument as TEI XML.
     */
    public String format(GrobidDocument grobidDoc) {
        StringBuilder tei = new StringBuilder();
        GrobidAnalysisConfig config = grobidDoc.getConfig();

        tei.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        tei.append("<TEI xml:lang=\"")
                .append(grobidDoc.getLanguage() != null ? grobidDoc.getLanguage() : "en")
                .append("\"");
        tei.append(" xmlns=\"http://www.tei-c.org/ns/1.0\">\n");

        // Header — delegate to BiblioItem.toTEI() for backward compatibility
        // Note: full header formatting is complex (2000+ lines in TEIFormatter)
        // For now, this is a simplified version. Full backward compat will be achieved
        // by comparing output against snapshots.
        tei.append("<teiHeader>\n");
        formatHeader(tei, grobidDoc);
        tei.append("</teiHeader>\n");

        // Body
        tei.append("<text>\n");
        formatBody(tei, grobidDoc);
        tei.append("</text>\n");

        tei.append("</TEI>\n");
        return tei.toString();
    }

    /**
     * Format just the body section from a GrobidDocument.
     * This is useful for testing the body formatter in isolation.
     */
    public String formatBodyOnly(GrobidDocument grobidDoc) {
        StringBuilder tei = new StringBuilder();
        formatBody(tei, grobidDoc);
        return tei.toString();
    }

    private void formatHeader(StringBuilder tei, GrobidDocument grobidDoc) {
        // Delegate to existing BiblioItem.toTEI() for full backward compatibility
        BiblioItem header = grobidDoc.getHeader();
        if (header != null) {
            tei.append(header.toTEI(0, 0, grobidDoc.getConfig()));
        }
    }

    private void formatBody(StringBuilder tei, GrobidDocument grobidDoc) {
        GrobidAnalysisConfig config = grobidDoc.getConfig();

        // Body text
        if (CollectionUtils.isNotEmpty(grobidDoc.getBody())) {
            tei.append("\t\t<body>\n");
            formatBodyElements(tei, grobidDoc.getBody(), config, "\t\t\t");

            // Figures
            for (Figure figure : grobidDoc.getFigures()) {
                tei.append(figure.toTEI(config, null, null, grobidDoc.getMarkerTypes()));
                tei.append("\n");
            }

            // Tables
            for (Table table : grobidDoc.getTables()) {
                tei.append(table.toTEI(config, null, null, grobidDoc.getMarkerTypes()));
                tei.append("\n");
            }

            tei.append("\t\t</body>\n");
        }

        // Back matter
        tei.append("\t\t<back>\n");

        // Acknowledgement
        if (CollectionUtils.isNotEmpty(grobidDoc.getAcknowledgement())) {
            tei.append("\t\t\t<div type=\"acknowledgement\">\n");
            formatBodyElements(tei, grobidDoc.getAcknowledgement(), config, "\t\t\t\t");
            tei.append("\t\t\t</div>\n");
        }

        // Annex
        if (CollectionUtils.isNotEmpty(grobidDoc.getAnnex())) {
            tei.append("\t\t\t<div type=\"annex\">\n");
            formatBodyElements(tei, grobidDoc.getAnnex(), config, "\t\t\t\t");

            for (Figure figure : grobidDoc.getAnnexFigures()) {
                tei.append(figure.toTEI(config, null, null, grobidDoc.getMarkerTypes()));
                tei.append("\n");
            }
            for (Table table : grobidDoc.getAnnexTables()) {
                tei.append(table.toTEI(config, null, null, grobidDoc.getMarkerTypes()));
                tei.append("\n");
            }
            tei.append("\t\t\t</div>\n");
        }

        // References
        formatReferences(tei, grobidDoc);

        tei.append("\t\t</back>\n");
    }

    /**
     * Format a list of body elements (sections, paragraphs, etc.) as TEI XML.
     */
    private void formatBodyElements(
            StringBuilder tei,
            List<BodyElement> elements,
            GrobidAnalysisConfig config,
            String indent) {
        for (BodyElement element : elements) {
            switch (element) {
                case Section section -> formatSection(tei, section, config, indent);
                case Paragraph paragraph -> formatParagraph(tei, paragraph, config, indent);
                case ListBlock listBlock -> formatList(tei, listBlock, config, indent);
                case EquationBlock equationBlock -> formatEquation(tei, equationBlock, config, indent);
                case OtherBlock otherBlock -> formatOther(tei, otherBlock, config, indent);
            }
        }
    }

    private void formatSection(
            StringBuilder tei,
            Section section,
            GrobidAnalysisConfig config,
            String indent) {
        tei.append(indent).append("<div");
        if (section.getHeadingId() != null) {
            tei.append(" xml:id=\"").append(section.getHeadingId()).append("\"");
        }
        tei.append(">\n");

        if (section.getHeading() != null) {
            tei.append(indent).append("\t<head");
            if (section.getHeadingNumber() != null) {
                tei.append(" n=\"").append(section.getHeadingNumber()).append("\"");
            }
            if (CollectionUtils.isNotEmpty(section.getHeadingCoords())) {
                tei.append(" coords=\"").append(Joiner.on(";").join(section.getHeadingCoords())).append("\"");
            }
            tei.append(">");
            tei.append(TextUtilities.HTMLEncode(section.getHeading()));
            tei.append("</head>\n");
        }

        formatBodyElements(tei, section.getChildren(), config, indent + "\t");

        tei.append(indent).append("</div>\n");
    }

    private void formatParagraph(
            StringBuilder tei,
            Paragraph paragraph,
            GrobidAnalysisConfig config,
            String indent) {
        tei.append(indent).append("<p");
        if (paragraph.id() != null) {
            tei.append(" xml:id=\"").append(paragraph.id()).append("\"");
        }
        if (CollectionUtils.isNotEmpty(paragraph.coords())) {
            tei.append(" coords=\"").append(Joiner.on(";").join(paragraph.coords())).append("\"");
        }
        tei.append(">");

        formatInlineContent(tei, paragraph.content());

        tei.append("</p>\n");
    }

    private void formatList(
            StringBuilder tei,
            ListBlock listBlock,
            GrobidAnalysisConfig config,
            String indent) {
        tei.append(indent).append("<list>\n");
        for (ListItem item : listBlock.items()) {
            tei.append(indent).append("\t<item>");
            formatInlineContent(tei, item.content());
            tei.append("</item>\n");
        }
        tei.append(indent).append("</list>\n");
    }

    private void formatEquation(
            StringBuilder tei,
            EquationBlock equationBlock,
            GrobidAnalysisConfig config,
            String indent) {
        Equation eq = equationBlock.equation();
        if (eq != null) {
            tei.append(indent).append("<formula");
            if (eq.getId() != null) {
                tei.append(" xml:id=\"formula_").append(eq.getId()).append("\"");
            }
            tei.append(">");
            if (eq.getContent() != null) {
                tei.append(TextUtilities.HTMLEncode(eq.getContent()));
            }
            if (eq.getLabel() != null) {
                tei.append("<label>").append(TextUtilities.HTMLEncode(eq.getLabel())).append("</label>");
            }
            tei.append("</formula>\n");
        }
    }

    private void formatOther(
            StringBuilder tei,
            OtherBlock otherBlock,
            GrobidAnalysisConfig config,
            String indent) {
        if (StringUtils.isNotBlank(otherBlock.text())) {
            tei.append(indent).append("<note type=\"other\">");
            tei.append(TextUtilities.HTMLEncode(otherBlock.text()));
            tei.append("</note>\n");
        }
    }

    /**
     * Format a sequence of inline content elements as TEI XML.
     */
    private void formatInlineContent(StringBuilder tei, List<InlineContent> content) {
        if (content == null)
            return;

        for (InlineContent ic : content) {
            switch (ic) {
                case TextSpan ts -> tei.append(TextUtilities.HTMLEncode(ts.text()));
                case CitationRef cr -> {
                    tei.append("<ref type=\"bibr\"");
                    if (cr.targetBibId() != null) {
                        tei.append(" target=\"#").append(cr.targetBibId()).append("\"");
                    }
                    if (CollectionUtils.isNotEmpty(cr.coords())) {
                        tei.append(" coords=\"").append(Joiner.on(";").join(cr.coords())).append("\"");
                    }
                    tei.append(">");
                    tei.append(TextUtilities.HTMLEncode(cr.markerText()));
                    tei.append("</ref>");
                }
                case FigureRef fr -> {
                    tei.append("<ref type=\"figure\"");
                    if (fr.targetId() != null) {
                        tei.append(" target=\"#").append(fr.targetId()).append("\"");
                    }
                    if (CollectionUtils.isNotEmpty(fr.coords())) {
                        tei.append(" coords=\"").append(Joiner.on(";").join(fr.coords())).append("\"");
                    }
                    tei.append(">");
                    tei.append(TextUtilities.HTMLEncode(fr.markerText()));
                    tei.append("</ref>");
                }
                case TableRef tr -> {
                    tei.append("<ref type=\"table\"");
                    if (tr.targetId() != null) {
                        tei.append(" target=\"#").append(tr.targetId()).append("\"");
                    }
                    if (CollectionUtils.isNotEmpty(tr.coords())) {
                        tei.append(" coords=\"").append(Joiner.on(";").join(tr.coords())).append("\"");
                    }
                    tei.append(">");
                    tei.append(TextUtilities.HTMLEncode(tr.markerText()));
                    tei.append("</ref>");
                }
                case EquationRef er -> {
                    tei.append("<ref type=\"formula\"");
                    if (er.targetId() != null) {
                        tei.append(" target=\"#").append(er.targetId()).append("\"");
                    }
                    if (CollectionUtils.isNotEmpty(er.coords())) {
                        tei.append(" coords=\"").append(Joiner.on(";").join(er.coords())).append("\"");
                    }
                    tei.append(">");
                    tei.append(TextUtilities.HTMLEncode(er.markerText()));
                    tei.append("</ref>");
                }
                case FootnoteRef fnr -> {
                    tei.append("<ref type=\"foot\"");
                    if (fnr.noteId() != null) {
                        tei.append(" target=\"#").append(fnr.noteId()).append("\"");
                    }
                    tei.append(">");
                    tei.append(TextUtilities.HTMLEncode(fnr.markerText()));
                    tei.append("</ref>");
                }
                case URLRef ur -> {
                    tei.append("<ref type=\"url\" target=\"")
                            .append(TextUtilities.HTMLEncode(ur.destination()))
                            .append("\"");
                    if (CollectionUtils.isNotEmpty(ur.coords())) {
                        tei.append(" coords=\"").append(Joiner.on(";").join(ur.coords())).append("\"");
                    }
                    tei.append(">");
                    tei.append(TextUtilities.HTMLEncode(ur.text()));
                    tei.append("</ref>");
                }
            }
        }
    }

    private void formatReferences(StringBuilder tei, GrobidDocument grobidDoc) {
        GrobidAnalysisConfig config = grobidDoc.getConfig();
        List<BibDataSet> references = grobidDoc.getReferences();

        if (CollectionUtils.isEmpty(references)) {
            return;
        }

        tei.append("\t\t\t<div type=\"references\">\n");
        tei.append("\t\t\t\t<listBibl>\n");

        for (BibDataSet bds : references) {
            BiblioItem bib = bds.getResBib();
            if (bib != null) {
                tei.append("\n").append(bib.toTEI(bds.getResBib().getOrdinal(), 0, config));
            }
        }

        tei.append("\n\t\t\t\t</listBibl>\n");
        tei.append("\t\t\t</div>\n");
    }

    /**
     * Format page information as TEI.
     */
    private void formatPages(StringBuilder tei, GrobidDocument grobidDoc) {
        if (CollectionUtils.isEmpty(grobidDoc.getPages()))
            return;

        for (Page page : grobidDoc.getPages()) {
            tei.append("\t\t\t<surface ");
            tei.append("n=\"").append(page.getNumber()).append("\" ");
            tei.append("ulx=\"0.0\" uly=\"0.0\" ");
            tei.append("lrx=\"").append(page.getWidth()).append("\" ");
            tei.append("lry=\"").append(page.getHeight()).append("\"");
            tei.append("/>\n");
        }
    }
}
