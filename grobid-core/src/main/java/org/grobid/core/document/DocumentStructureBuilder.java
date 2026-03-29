package org.grobid.core.document;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.*;
import org.grobid.core.document.model.*;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.citations.CalloutAnalyzer.MarkerType;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.grobid.core.utilities.matching.EntityMatcherException;

/**
 * Builds a {@link GrobidDocument} data model from the raw parsing output.
 *
 * <p>This replaces the implicit structure building previously spread across
 * {@code TEIFormatter.toTEITextPiece()} (body), {@code TEIFormatter.getTeiNotes()} (notes),
 * and {@code FullTextParser.toTEI()} (back matter orchestration).</p>
 *
 * <p>The resulting {@link GrobidDocument} is format-agnostic and can be consumed by any
 * formatter (TEI, JSON, Markdown).</p>
 */
public class DocumentStructureBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentStructureBuilder.class);

    private static final Set<TaggingLabel> MARKER_LABELS = Sets.newHashSet(
            TaggingLabels.CITATION_MARKER,
            TaggingLabels.FIGURE_MARKER,
            TaggingLabels.TABLE_MARKER,
            TaggingLabels.EQUATION_MARKER);

    private final ReferenceResolver referenceResolver;

    public DocumentStructureBuilder() {
        this.referenceResolver = new ReferenceResolver();
    }

    public DocumentStructureBuilder(ReferenceResolver referenceResolver) {
        this.referenceResolver = referenceResolver;
    }

    /**
     * Build a complete {@link GrobidDocument} from the parsed document data.
     * This is the main entry point, replacing {@code FullTextParser.toTEI()}.
     */
    public GrobidDocument build(
            Document doc,
            String bodyLabellingResult,
            String annexLabellingResult,
            LayoutTokenization layoutTokenization,
            List<LayoutToken> tokenizationsAnnex,
            BiblioItem resHeader,
            List<Figure> bodyFigures,
            List<Table> bodyTables,
            List<Equation> bodyEquations,
            List<Figure> annexFigures,
            List<Table> annexTables,
            List<Equation> annexEquations,
            List<MarkerType> markerTypes,
            GrobidAnalysisConfig config) {

        GrobidDocument grobidDoc = new GrobidDocument();
        grobidDoc.setConfig(config);
        grobidDoc.setHeader(resHeader);
        grobidDoc.setLanguage(doc.getLanguage());
        grobidDoc.setMarkerTypes(markerTypes != null ? markerTypes : new ArrayList<>());

        List<BibDataSet> resCitations = doc.getBibDataSets();
        grobidDoc.setReferences(resCitations != null ? resCitations : new ArrayList<>());

        // Pages
        if (doc.getPages() != null) {
            grobidDoc.setPages(doc.getPages());
        }

        // Extract notes (footnotes and margin notes)
        List<Note> allNotes = extractNotes(doc);
        List<Note> footnotes = new ArrayList<>();
        List<Note> marginNotes = new ArrayList<>();
        for (Note note : allNotes) {
            if (note.getNoteType() == Note.NoteType.FOOT) {
                footnotes.add(note);
            } else {
                marginNotes.add(note);
            }
        }
        grobidDoc.setFootnotes(footnotes);
        grobidDoc.setMarginNotes(marginNotes);

        // Build body structure
        if (bodyLabellingResult != null && layoutTokenization != null
                && layoutTokenization.getTokenization() != null) {
            List<BodyElement> body = buildBodyElements(
                    bodyLabellingResult,
                    layoutTokenization,
                    resCitations,
                    true,
                    bodyFigures,
                    bodyTables,
                    bodyEquations,
                    allNotes,
                    markerTypes,
                    doc,
                    config);
            grobidDoc.setBody(body);
        }

        // Content elements
        grobidDoc.setFigures(bodyFigures != null ? bodyFigures : new ArrayList<>());
        grobidDoc.setTables(bodyTables != null ? bodyTables : new ArrayList<>());
        grobidDoc.setEquations(bodyEquations != null ? bodyEquations : new ArrayList<>());
        grobidDoc.setAnnexFigures(annexFigures != null ? annexFigures : new ArrayList<>());
        grobidDoc.setAnnexTables(annexTables != null ? annexTables : new ArrayList<>());
        grobidDoc.setAnnexEquations(annexEquations != null ? annexEquations : new ArrayList<>());

        // Build annex structure
        if (annexLabellingResult != null && tokenizationsAnnex != null) {
            List<BodyElement> annex = buildBodyElements(
                    annexLabellingResult,
                    new LayoutTokenization(tokenizationsAnnex),
                    resCitations,
                    true,
                    annexFigures,
                    annexTables,
                    annexEquations,
                    null,
                    markerTypes,
                    doc,
                    config);
            grobidDoc.setAnnex(annex);
        }

        return grobidDoc;
    }

    /**
     * Build the list of body elements from CRF labelling output.
     * This is the core port of {@code TEIFormatter.toTEITextPiece()}.
     */
    public List<BodyElement> buildBodyElements(
            String labellingResult,
            LayoutTokenization layoutTokenization,
            List<BibDataSet> bds,
            boolean keepUnsolvedCallout,
            List<Figure> figures,
            List<Table> tables,
            List<Equation> equations,
            List<Note> notes,
            List<MarkerType> markerTypes,
            Document doc,
            GrobidAnalysisConfig config) {

        List<BodyElement> result = new ArrayList<>();

        if (labellingResult == null || layoutTokenization == null) {
            return result;
        }

        List<LayoutToken> tokenizations = layoutTokenization.getTokenization();
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, labellingResult,
                tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        // State machine variables (mirroring toTEITextPiece)
        TaggingLabel lastClusterLabel = null;
        Section currentSection = createInitialSection(config);
        result.add(currentSection);
        Paragraph currentParagraph = null;
        List<LayoutToken> currentParagraphTokens = null;
        List<InlineContent> currentParagraphContent = null;
        ListBlock currentList = null;
        List<ListItem> currentListItems = null;
        int equationIndex = 0;

        boolean generateRefCoords = config.isGenerateTeiCoordinates("ref");
        boolean generateHeadCoords = config.isGenerateTeiCoordinates("head");
        boolean generateParagraphCoords = config.isGenerateTeiCoordinates("p");

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            if (clusterLabel.equals(TaggingLabels.SECTION)) {
                // Finalize previous paragraph if sentence segmentation is needed
                currentParagraph = finalizeParagraph(
                        currentParagraph,
                        currentParagraphContent,
                        currentParagraphTokens,
                        config);

                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());

                // Parse section number
                Pair<String, String> numb = getSectionNumber(clusterContent);
                String heading;
                String headingNumber = null;
                int level = 1;
                if (numb != null) {
                    heading = numb.a;
                    headingNumber = numb.b;
                    level = computeSectionLevel(headingNumber);
                } else {
                    heading = clusterContent;
                }

                String headingId = config.isGenerateTeiIds() ? "_" + KeyGen.getKey().substring(0, 7) : null;

                List<BoundingBox> headingCoords = null;
                if (generateHeadCoords) {
                    headingCoords = BoundingBoxCalculator.calculate(cluster.concatTokens());
                }

                currentSection = new Section(heading, headingNumber, level, headingCoords, headingId);
                result.add(currentSection);
                currentParagraph = null;
                currentParagraphContent = null;
                currentParagraphTokens = null;

            } else if (clusterLabel.equals(TaggingLabels.EQUATION)
                    || clusterLabel.equals(TaggingLabels.EQUATION_LABEL)) {
                int start = -1;
                if (CollectionUtils.isNotEmpty(cluster.concatTokens())) {
                    start = cluster.concatTokens().get(0).getOffset();
                }
                if (start != -1 && equations != null) {
                    for (int i = equationIndex; i < equations.size(); i++) {
                        Equation equation = equations.get(i);
                        if (equation.getStart() == start) {
                            currentSection.addChild(new EquationBlock(equation));
                            equationIndex = i;
                            break;
                        }
                    }
                }

            } else if (clusterLabel.equals(TaggingLabels.ITEM)) {
                String clusterContent = LayoutTokensUtil.normalizeText(cluster.concatTokens());
                ListItem item = new ListItem(
                        List.of(new TextSpan(clusterContent)),
                        cluster.concatTokens());

                if (!MARKER_LABELS.contains(lastClusterLabel) && lastClusterLabel != TaggingLabels.ITEM) {
                    // Start a new list
                    currentListItems = new ArrayList<>();
                    currentListItems.add(item);
                    currentList = new ListBlock(currentListItems);
                    currentSection.addChild(currentList);
                } else if (currentListItems != null) {
                    currentListItems.add(item);
                }

            } else if (clusterLabel.equals(TaggingLabels.OTHER)) {
                String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                currentSection.addChild(new OtherBlock(clusterContent, cluster.concatTokens()));

            } else if (clusterLabel.equals(TaggingLabels.PARAGRAPH)) {
                List<LayoutToken> clusterTokens = cluster.concatTokens();

                // Detect inline footnote callouts and URLs
                List<InlineContent> paragraphContent = buildParagraphInlineContent(
                        clusterTokens,
                        notes,
                        doc,
                        config);

                boolean isNewParagraph = isNewParagraph(lastClusterLabel, currentParagraph);

                if (isNewParagraph) {
                    // Finalize previous paragraph
                    currentParagraph = finalizeParagraph(
                            currentParagraph,
                            currentParagraphContent,
                            currentParagraphTokens,
                            config);

                    String paragraphId = config.isGenerateTeiIds() ? "_" + KeyGen.getKey().substring(0, 7) : null;

                    List<BoundingBox> paragraphCoords = null;
                    if (generateParagraphCoords) {
                        paragraphCoords = BoundingBoxCalculator.calculate(clusterTokens);
                    }

                    currentParagraphContent = new ArrayList<>(paragraphContent);
                    currentParagraphTokens = new ArrayList<>(clusterTokens);

                    // Create the paragraph (will be finalized later with potential sentence segmentation)
                    currentParagraph = new Paragraph(
                            currentParagraphContent,
                            currentParagraphTokens,
                            paragraphCoords,
                            paragraphId,
                            null // sentences populated later if segmentation enabled
                    );
                    currentSection.addChild(currentParagraph);
                } else {
                    // Append to existing paragraph
                    if (currentParagraphContent != null) {
                        currentParagraphContent.addAll(paragraphContent);
                    }
                    if (currentParagraphTokens != null) {
                        currentParagraphTokens.addAll(clusterTokens);
                    }
                }

            } else if (MARKER_LABELS.contains(clusterLabel)) {
                // Reference markers (citation, figure, table, equation)
                List<LayoutToken> refTokens = LayoutTokensUtil.dehyphenize(cluster.concatTokens());
                String chunkRefString = LayoutTokensUtil.toText(refTokens);

                // Add a space before the marker
                if (currentParagraphContent != null) {
                    currentParagraphContent.add(new TextSpan(" "));
                }

                MarkerType citationMarkerType = null;
                if (markerTypes != null && !markerTypes.isEmpty()) {
                    citationMarkerType = markerTypes.get(0);
                }

                List<InlineContent> resolvedRefs = null;
                try {
                    if (clusterLabel.equals(TaggingLabels.CITATION_MARKER)) {
                        resolvedRefs = referenceResolver.resolveCitationMarkers(
                                refTokens,
                                doc.getReferenceMarkerMatcher(),
                                generateRefCoords,
                                keepUnsolvedCallout,
                                citationMarkerType);

                        // Check if filtered-out superscript marker might be a footnote callout
                        if (resolvedRefs != null && resolvedRefs.size() == 1
                                && resolvedRefs.get(0) instanceof TextSpan) {
                            InlineContent footNoteRef = tryMatchFootnoteCallout(
                                    refTokens,
                                    chunkRefString,
                                    notes,
                                    citationMarkerType,
                                    config);
                            if (footNoteRef != null) {
                                resolvedRefs = List.of(footNoteRef);
                                if (chunkRefString.endsWith(" ")) {
                                    resolvedRefs = new ArrayList<>(resolvedRefs);
                                    resolvedRefs.add(new TextSpan(" "));
                                }
                            }
                        }

                    } else if (clusterLabel.equals(TaggingLabels.FIGURE_MARKER)) {
                        resolvedRefs = referenceResolver.resolveFigureMarkers(
                                chunkRefString,
                                refTokens,
                                figures,
                                generateRefCoords);
                    } else if (clusterLabel.equals(TaggingLabels.TABLE_MARKER)) {
                        resolvedRefs = referenceResolver.resolveTableMarkers(
                                chunkRefString,
                                refTokens,
                                tables,
                                generateRefCoords);
                    } else if (clusterLabel.equals(TaggingLabels.EQUATION_MARKER)) {
                        resolvedRefs = referenceResolver.resolveEquationMarkers(
                                chunkRefString,
                                refTokens,
                                equations,
                                generateRefCoords);
                    }
                } catch (EntityMatcherException e) {
                    LOGGER.warn("Error resolving references: " + e.getMessage());
                }

                if (resolvedRefs != null) {
                    if (currentParagraphContent != null) {
                        currentParagraphContent.addAll(resolvedRefs);
                    } else {
                        // Marker outside paragraph — attach to section as a standalone paragraph
                        Paragraph markerParagraph = new Paragraph(
                                new ArrayList<>(resolvedRefs), refTokens, null, null, null);
                        currentSection.addChild(markerParagraph);
                    }
                }

                if (currentParagraphTokens != null) {
                    currentParagraphTokens.addAll(cluster.concatTokens());
                }

            } else if (clusterLabel.equals(TaggingLabels.FIGURE) || clusterLabel.equals(TaggingLabels.TABLE)) {
                // Figures and tables are floating — they're appended as separate elements,
                // not inline in the body. Add space for paragraph reconnection.
                if (currentParagraphContent != null) {
                    currentParagraphContent.add(new TextSpan(" "));
                }
            }

            lastClusterLabel = cluster.getTaggingLabel();
        }

        // Finalize last paragraph
        finalizeParagraph(currentParagraph, currentParagraphContent, currentParagraphTokens, config);

        // Remove empty sections
        result.removeIf(element -> {
            if (element instanceof Section section) {
                return section.getChildren().isEmpty() && section.getHeading() == null;
            }
            return false;
        });

        return result;
    }

    /**
     * Build inline content for a paragraph, detecting footnote callouts and URLs.
     * Port of the footnote/URL detection logic in {@code toTEITextPiece()} for PARAGRAPH clusters.
     */
    private List<InlineContent> buildParagraphInlineContent(
            List<LayoutToken> clusterTokens,
            List<Note> notes,
            Document doc,
            GrobidAnalysisConfig config) {

        int clusterPage = Iterables.getLast(clusterTokens).getPage();
        boolean generateRefCoords = config.isGenerateTeiCoordinates("ref");

        // Match footnote callouts
        Map<String, Note> labels2Notes = new TreeMap<>();
        List<Triple<String, String, OffsetPosition>> matchedLabelPositions = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(notes)) {
            List<Note> notesSamePage = notes.stream()
                    .filter(f -> !f.isIgnored() && f.getPageNumber() == clusterPage)
                    .collect(Collectors.toList());

            int start = 0;
            for (Note note : notesSamePage) {
                List<LayoutToken> clusterReduced = clusterTokens.subList(start, clusterTokens.size());
                Optional<LayoutToken> matching = clusterReduced.stream()
                        .filter(t -> t.getText().equals(note.getLabel()) && t.isSuperscript())
                        .findFirst();

                if (matching.isPresent()) {
                    int idx = clusterReduced.indexOf(matching.get()) + start;
                    note.setIgnored(true);
                    OffsetPosition matchingPosition = new OffsetPosition();
                    matchingPosition.start = idx;
                    matchingPosition.end = idx + 1;
                    start = matchingPosition.end;
                    matchedLabelPositions.add(Triple.of(note.getIdentifier(), "note", matchingPosition));
                    labels2Notes.put(note.getIdentifier(), note);
                }
            }
        }

        // Match URLs
        List<org.apache.commons.lang3.tuple.Pair<OffsetPosition, String>> urlPositions = Lexicon
                .tokenPositionUrlPatternWithPdfAnnotations(clusterTokens, doc.getPDFAnnotations());

        urlPositions.forEach(opu -> {
            matchedLabelPositions.add(
                    Triple.of(
                            opu.getRight() != null ? opu.getRight()
                                    : LayoutTokensUtil.normalizeDehyphenizeText(
                                            clusterTokens.subList(opu.getLeft().start, opu.getLeft().end + 1)),
                            "url",
                            new OffsetPosition(opu.getLeft().start, opu.getLeft().end + 1)));
        });

        // If no inline references found, return simple text
        if (CollectionUtils.isEmpty(matchedLabelPositions)) {
            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(clusterTokens);
            return new ArrayList<>(List.of(new TextSpan(clusterContent)));
        }

        // Sort matches by position and build inline content
        List<Triple<String, String, OffsetPosition>> sorted = matchedLabelPositions.stream()
                .filter(a -> StringUtils.isNotBlank(a.getLeft()))
                .sorted(Comparator.comparingInt(m -> m.getRight().start))
                .collect(Collectors.toList());

        List<InlineContent> content = new ArrayList<>();
        int pos = 0;

        for (Triple<String, String, OffsetPosition> ref : sorted) {
            String type = ref.getMiddle();
            OffsetPosition matchingPosition = ref.getRight();

            if (pos > matchingPosition.start) {
                break;
            }

            // Text before the reference
            List<LayoutToken> before = clusterTokens.subList(pos, matchingPosition.start);
            String textBefore = LayoutTokensUtil.normalizeDehyphenizeText(before);

            if (CollectionUtils.isNotEmpty(before) && before.get(0).getText().equals(" ")) {
                content.add(new TextSpan(" "));
            }
            if (!textBefore.isEmpty()) {
                content.add(new TextSpan(textBefore));
            }

            // The reference itself
            List<LayoutToken> calloutTokens = clusterTokens.subList(matchingPosition.start, matchingPosition.end);

            if ("note".equals(type)) {
                Note note = labels2Notes.get(ref.getLeft());
                if (note != null) {
                    List<BoundingBox> coords = generateRefCoords ? BoundingBoxCalculator.calculate(calloutTokens)
                            : null;
                    content.add(new FootnoteRef(note.getLabel(), note.getIdentifier(), coords));
                }
            } else if ("url".equals(type)) {
                String destination = ref.getLeft();

                // Add space before URL if needed
                if (CollectionUtils.isNotEmpty(before)
                        && StringUtils.equalsAnyIgnoreCase(Iterables.getLast(before).getText(), " ", "\n")) {
                    content.add(new TextSpan(" "));
                }

                URLRef urlRef = referenceResolver.resolveURL(destination, calloutTokens, generateRefCoords);
                if (urlRef != null) {
                    content.add(urlRef);
                }
            }

            pos = matchingPosition.end;
        }

        // Remaining text after last reference
        List<LayoutToken> remaining = clusterTokens.subList(pos, clusterTokens.size());
        String remainingText = LayoutTokensUtil.normalizeDehyphenizeText(remaining);

        if (CollectionUtils.isNotEmpty(remaining) && remaining.get(0).getText().equals(" ")) {
            content.add(new TextSpan(" "));
        }
        if (!remainingText.isEmpty()) {
            content.add(new TextSpan(remainingText));
        }

        return content;
    }

    /**
     * Try to match a filtered-out superscript citation marker as a footnote callout.
     * Port of the footnote fallback logic in toTEITextPiece() for MARKER_LABELS.
     */
    private InlineContent tryMatchFootnoteCallout(
            List<LayoutToken> refTokens,
            String chunkRefString,
            List<Note> notes,
            MarkerType citationMarkerType,
            GrobidAnalysisConfig config) {

        if (citationMarkerType == null || citationMarkerType != MarkerType.SUPERSCRIPT_NUMBER) {
            if (!refTokens.isEmpty() && refTokens.get(0).isSuperscript()) {
                int clusterPage = Iterables.getLast(refTokens).getPage();
                if (notes != null && !notes.isEmpty()) {
                    List<Note> notesSamePage = notes.stream()
                            .filter(f -> !f.isIgnored() && f.getPageNumber() == clusterPage)
                            .collect(Collectors.toList());

                    for (Note note : notesSamePage) {
                        if (chunkRefString.trim().equals(note.getLabel())) {
                            note.setIgnored(true);
                            boolean generateRefCoords = config.isGenerateTeiCoordinates("ref");
                            List<BoundingBox> coords = generateRefCoords ? BoundingBoxCalculator.calculate(refTokens)
                                    : null;
                            return new FootnoteRef(chunkRefString.trim(), note.getIdentifier(), coords);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Determine whether a new paragraph should be started.
     * Port of {@code TEIFormatter.isNewParagraph()}.
     */
    private static boolean isNewParagraph(TaggingLabel lastClusterLabel, Paragraph currentParagraph) {
        return (!MARKER_LABELS.contains(lastClusterLabel)
                && lastClusterLabel != TaggingLabels.FIGURE
                && lastClusterLabel != TaggingLabels.TABLE)
                || currentParagraph == null;
    }

    /**
     * Create the initial (implicit) section for content before the first heading.
     */
    private Section createInitialSection(GrobidAnalysisConfig config) {
        String divId = config.isGenerateTeiIds() ? "_" + KeyGen.getKey().substring(0, 7) : null;
        return new Section(null, null, 0, null, divId);
    }

    /**
     * Finalize a paragraph — placeholder for future sentence segmentation support.
     * Returns null to signal that the current paragraph has been finalized.
     */
    private Paragraph finalizeParagraph(
            Paragraph paragraph,
            List<InlineContent> content,
            List<LayoutToken> tokens,
            GrobidAnalysisConfig config) {
        // TODO: sentence segmentation support (port of segmentIntoSentences)
        // For now, paragraphs are finalized as-is since they already contain their content.
        return null;
    }

    /**
     * Extract notes (footnotes and margin notes) from the document.
     * Reuses the existing logic from {@code TEIFormatter.getTeiNotes()}.
     */
    public List<Note> extractNotes(Document doc) {
        List<Note> notes = new ArrayList<>();

        SortedSet<DocumentPiece> footnoteParts = doc.getDocumentPart(SegmentationLabels.FOOTNOTE);
        notes.addAll(extractNotesOfType(doc, footnoteParts, Note.NoteType.FOOT));

        SortedSet<DocumentPiece> marginNoteParts = doc.getDocumentPart(SegmentationLabels.MARGINNOTE);
        notes.addAll(extractNotesOfType(doc, marginNoteParts, Note.NoteType.MARGIN));

        return notes;
    }

    private List<Note> extractNotesOfType(
            Document doc,
            SortedSet<DocumentPiece> documentNoteParts,
            Note.NoteType noteType) {
        List<Note> notes = new ArrayList<>();
        if (documentNoteParts == null) {
            return notes;
        }

        List<String> allNotes = new ArrayList<>();

        for (DocumentPiece docPiece : documentNoteParts) {
            List<LayoutToken> noteTokens = doc.getDocumentPieceTokenization(docPiece);
            if (CollectionUtils.isEmpty(noteTokens)) {
                continue;
            }

            String footText = doc.getDocumentPieceText(docPiece);
            footText = footText.replace("\n", " ");
            if (footText.length() < 6) {
                continue;
            }
            if (allNotes.contains(footText)) {
                continue;
            }

            allNotes.add(footText);

            // Delegate to TEIFormatter's makeNotes which handles note splitting
            // Note: this is a temporary bridge — ideally this logic would be extracted too
            List<Note> localNotes = TEIFormatter.makeNotesStatic(noteTokens, footText, noteType, notes.size());
            if (localNotes != null) {
                notes.addAll(localNotes);
            }
        }

        notes.forEach(n -> n.setText(TextUtilities.dehyphenize(n.getText())));

        return notes;
    }

    /**
     * Parse section number from heading text.
     * Port of {@code TEIFormatter.getSectionNumber()}.
     */
    static Pair<String, String> getSectionNumber(String text) {
        java.util.regex.Matcher m1 = BasicStructureBuilder.headerNumbering1.matcher(text);
        java.util.regex.Matcher m2 = BasicStructureBuilder.headerNumbering2.matcher(text);
        java.util.regex.Matcher m3 = BasicStructureBuilder.headerNumbering3.matcher(text);
        java.util.regex.Matcher m = null;
        String numb = null;
        if (m1.find()) {
            numb = m1.group(0);
            m = m1;
        } else if (m2.find()) {
            numb = m2.group(0);
            m = m2;
        } else if (m3.find()) {
            numb = m3.group(0);
            m = m3;
        }
        if (numb != null) {
            text = text.replace(numb, "").trim();
            numb = numb.replace(" ", "");
            return new Pair<>(text, numb);
        } else {
            return null;
        }
    }

    /**
     * Compute section depth from a section number string like "1.2.3".
     */
    static int computeSectionLevel(String headingNumber) {
        if (headingNumber == null || headingNumber.isEmpty()) {
            return 1;
        }
        int level = 1;
        for (char c : headingNumber.toCharArray()) {
            if (c == '.') {
                level++;
            }
        }
        return level;
    }
}
