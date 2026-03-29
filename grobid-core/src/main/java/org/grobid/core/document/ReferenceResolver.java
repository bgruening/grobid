package org.grobid.core.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.data.Equation;
import org.grobid.core.data.Figure;
import org.grobid.core.data.FigureTableType;
import org.grobid.core.data.Table;
import org.grobid.core.document.model.*;
import org.grobid.core.engines.citations.CalloutAnalyzer.MarkerType;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.matching.EntityMatcherException;
import org.grobid.core.utilities.matching.ReferenceMarkerMatcher;

/**
 * Format-agnostic reference resolution. Extracts the matching logic from
 * TEIFormatter's markReferences*TEI* methods and produces {@link InlineContent}
 * objects instead of XOM nodes.
 *
 * <p>This class is used by {@link DocumentStructureBuilder} to populate the data model.
 * The TEI, JSON, and Markdown formatters then render these InlineContent objects
 * in their respective formats.</p>
 */
public class ReferenceResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceResolver.class);

    private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");

    /**
     * Resolve citation markers in a token sequence against the bibliography.
     * Port of {@code TEIFormatter.markReferencesTEILuceneBased()}.
     *
     * @param refTokens          the layout tokens containing the citation marker text
     * @param markerMatcher      the Lucene-based reference marker matcher
     * @param generateCoordinates whether to compute bounding box coordinates
     * @param keepUnsolvedCallout whether to keep markers that couldn't be matched to a bibliography entry
     * @param citationMarkerType  constraint on marker type (superscript vs bracket), or null
     * @return list of inline content (CitationRef for matched markers, TextSpan for unmatched/plain text)
     */
    public List<InlineContent> resolveCitationMarkers(
            List<LayoutToken> refTokens,
            ReferenceMarkerMatcher markerMatcher,
            boolean generateCoordinates,
            boolean keepUnsolvedCallout,
            MarkerType citationMarkerType) throws EntityMatcherException {

        if (refTokens == null || refTokens.isEmpty()) {
            return null;
        }

        String text = LayoutTokensUtil.toText(refTokens);
        if (text == null || text.trim().isEmpty() || text.endsWith("</ref>") || text.startsWith("<ref")
                || markerMatcher == null) {
            return Collections.singletonList(new TextSpan(text));
        }

        boolean spaceEnd = false;
        text = text.replace("\n", " ");
        if (text.endsWith(" ")) {
            spaceEnd = true;
        }

        // Check constraints on global marker type
        if (citationMarkerType != null) {
            boolean hasSuperScriptNumber = false;
            for (LayoutToken refToken : refTokens) {
                if (refToken.isSuperscript()) {
                    hasSuperScriptNumber = true;
                    break;
                }
            }

            if (citationMarkerType == MarkerType.SUPERSCRIPT_NUMBER) {
                if (!hasSuperScriptNumber) {
                    return Collections.singletonList(new TextSpan(text));
                }
            } else {
                if (hasSuperScriptNumber) {
                    return Collections.singletonList(new TextSpan(text));
                }
            }
        }

        List<InlineContent> result = new ArrayList<>();
        List<ReferenceMarkerMatcher.MatchResult> matchResults = markerMatcher.match(refTokens);
        if (matchResults != null) {
            for (ReferenceMarkerMatcher.MatchResult matchResult : matchResults) {
                String markerText = LayoutTokensUtil.normalizeText(matchResult.getText());

                List<BoundingBox> coords = null;
                if (generateCoordinates && matchResult.getTokens() != null) {
                    coords = BoundingBoxCalculator.calculate(matchResult.getTokens());
                }

                boolean solved = matchResult.getBibDataSet() != null;
                if (solved) {
                    String targetId = "b" + matchResult.getBibDataSet().getResBib().getOrdinal();
                    int ordinal = matchResult.getBibDataSet().getResBib().getOrdinal();
                    result.add(new CitationRef(markerText, targetId, ordinal, coords));
                } else if (keepUnsolvedCallout) {
                    result.add(new CitationRef(markerText, null, -1, coords));
                } else {
                    result.add(new TextSpan(matchResult.getText()));
                }
            }
        }

        if (spaceEnd) {
            result.add(new TextSpan(" "));
        }
        return result;
    }

    /**
     * Convenience overload without citation marker type constraint.
     */
    public List<InlineContent> resolveCitationMarkers(
            List<LayoutToken> refTokens,
            ReferenceMarkerMatcher markerMatcher,
            boolean generateCoordinates,
            boolean keepUnsolvedCallout) throws EntityMatcherException {
        return resolveCitationMarkers(refTokens, markerMatcher, generateCoordinates, keepUnsolvedCallout, null);
    }

    /**
     * Resolve figure reference markers.
     * Port of {@code TEIFormatter.markReferencesFigureTEI()}.
     */
    public List<InlineContent> resolveFigureMarkers(
            String refText,
            List<LayoutToken> allRefTokens,
            List<Figure> figures,
            boolean generateCoordinates) {
        return resolveFigureOrTableMarkers(refText, allRefTokens, figures, FigureTableType.FIGURE, generateCoordinates);
    }

    /**
     * Resolve table reference markers.
     * Port of {@code TEIFormatter.markReferencesTableTEI()}.
     */
    public List<InlineContent> resolveTableMarkers(
            String refText,
            List<LayoutToken> allRefTokens,
            List<Table> tables,
            boolean generateCoordinates) {
        return resolveFigureOrTableMarkers(refText, allRefTokens, tables, FigureTableType.TABLE, generateCoordinates);
    }

    /**
     * Resolve figure or table reference markers.
     * Port of {@code TEIFormatter.markReferencesFigureOrTableTEI()}.
     */
    private List<InlineContent> resolveFigureOrTableMarkers(
            String refText,
            List<LayoutToken> allRefTokens,
            List<? extends Figure> figuresOrTables,
            FigureTableType type,
            boolean generateCoordinates) {

        if (refText == null || refText.trim().isEmpty()) {
            return null;
        }

        List<InlineContent> result = new ArrayList<>();

        if (refText.trim().length() == 1 && TextUtilities.fullPunctuations.contains(refText.trim())) {
            result.add(new TextSpan(refText));
            return result;
        }

        // Split compound references (e.g. "Fig. 1 and 2") on separators
        List<Pair<String, List<LayoutToken>>> labels = null;

        List<List<LayoutToken>> allYs = LayoutTokensUtil
                .split(allRefTokens, ReferenceMarkerMatcher.FIGURE_TABLES_REF_SEPARATORS, true);
        if (allYs.size() > 1) {
            labels = new ArrayList<>();
            for (List<LayoutToken> ys : allYs) {
                labels.add(new Pair<>(LayoutTokensUtil.toText(LayoutTokensUtil.dehyphenize(ys)), ys));
            }
        } else {
            labels = ReferenceMarkerMatcher.getNumberedLabels(allRefTokens, false);
        }

        if (labels == null || labels.size() <= 1) {
            Pair<String, List<LayoutToken>> localLabel = new Pair<>(refText, allRefTokens);
            labels = new ArrayList<>();
            labels.add(localLabel);
        }

        for (Pair<String, List<LayoutToken>> theLabel : labels) {
            String text = theLabel.a;
            List<LayoutToken> refTokens = theLabel.b;

            String textLow = text.toLowerCase().trim();
            String bestId = null;

            // First pass: exact match on label
            if (figuresOrTables != null) {
                for (Figure figureOrTable : figuresOrTables) {
                    if (StringUtils.isNotBlank(figureOrTable.getLabel())) {
                        String label = TextUtilities.cleanField(figureOrTable.getLabel(), false);
                        if (StringUtils.isNotBlank(label) && textLow.equals(label.toLowerCase())) {
                            bestId = figureOrTable.getId();
                            break;
                        }
                    }
                }
                // Second pass: relaxed matching (contains)
                if (bestId == null) {
                    for (int i = figuresOrTables.size() - 1; i >= 0; i--) {
                        Figure figureOrTable = figuresOrTables.get(i);
                        if (StringUtils.isNotBlank(figureOrTable.getLabel())) {
                            String label = TextUtilities.cleanField(figureOrTable.getLabel(), false);
                            if (StringUtils.isNotBlank(label) && textLow.contains(label.toLowerCase())) {
                                bestId = figureOrTable.getId();
                                break;
                            }
                        }
                    }
                }
            }

            boolean spaceEnd = false;
            boolean spaceStart = false;
            text = text.replace("\n", " ");
            if (text.endsWith(" ")) {
                spaceEnd = true;
            }
            if (!text.equals(" ") && text.startsWith(" ")) {
                spaceStart = true;
            }
            text = text.trim();

            if (StringUtils.isBlank(text)) {
                if (spaceStart) {
                    result.add(new TextSpan(" "));
                }
                result.add(new TextSpan(text));
                if (spaceEnd) {
                    result.add(new TextSpan(" "));
                }
                continue;
            }

            // Handle trailing separators ("and", "&", ",")
            String andWordString = null;
            if (text.endsWith("and") || text.endsWith("&") || text.endsWith(",")) {
                if (text.equals("and") || text.equals("&") || text.equals(",")) {
                    if (spaceStart) {
                        result.add(new TextSpan(" "));
                    }
                    result.add(new TextSpan(text));
                    if (spaceEnd) {
                        result.add(new TextSpan(" "));
                    }
                    continue;
                } else if (text.endsWith("and")) {
                    text = text.substring(0, text.length() - 3);
                    andWordString = "and";
                    refTokens = refTokens.subList(0, refTokens.size() - 1);
                } else if (text.endsWith("&")) {
                    text = text.substring(0, text.length() - 1);
                    andWordString = "&";
                    refTokens = refTokens.subList(0, refTokens.size() - 1);
                } else if (text.endsWith(",")) {
                    text = text.substring(0, text.length() - 1);
                    andWordString = ",";
                    refTokens = refTokens.subList(0, refTokens.size() - 1);
                }

                if (text.endsWith(" ")) {
                    andWordString = " " + andWordString;
                    refTokens = refTokens.subList(0, refTokens.size() - 1);
                }
                text = text.trim();
            }

            List<BoundingBox> coords = null;
            if (generateCoordinates && refTokens != null) {
                coords = BoundingBoxCalculator.calculate(refTokens);
            }

            // Build the target ID with proper prefix
            String targetId = null;
            if (bestId != null) {
                if (type == FigureTableType.TABLE) {
                    targetId = "tab_" + bestId;
                } else if (type == FigureTableType.FIGURE) {
                    targetId = "fig_" + bestId;
                }
            }

            if (spaceStart) {
                result.add(new TextSpan(" "));
            }

            if (type == FigureTableType.FIGURE) {
                result.add(new FigureRef(text, targetId, coords));
            } else {
                result.add(new TableRef(text, targetId, coords));
            }

            if (andWordString != null) {
                result.add(new TextSpan(andWordString));
            }

            if (spaceEnd) {
                result.add(new TextSpan(" "));
            }
        }
        return result;
    }

    /**
     * Resolve equation reference markers.
     * Port of {@code TEIFormatter.markReferencesEquationTEI()}.
     */
    public List<InlineContent> resolveEquationMarkers(
            String text,
            List<LayoutToken> refTokens,
            List<Equation> equations,
            boolean generateCoordinates) {

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        text = TextUtilities.cleanField(text, false);
        String textNumber = null;
        Matcher m = PATTERN_NUMBER.matcher(text);
        if (m.find()) {
            textNumber = m.group();
        }

        List<InlineContent> result = new ArrayList<>();

        String textLow = text.toLowerCase();
        String bestFormulaId = null;
        if (equations != null) {
            for (Equation equation : equations) {
                if (StringUtils.isNotBlank(equation.getLabel())) {
                    String label = TextUtilities.cleanField(equation.getLabel(), false);
                    Matcher m2 = PATTERN_NUMBER.matcher(label);
                    String labelNumber = null;
                    if (m2.find()) {
                        labelNumber = m2.group();
                    }
                    if ((labelNumber != null && textNumber != null && !labelNumber.isEmpty()
                            && labelNumber.equals(textNumber))
                            || (!label.isEmpty() && textLow.equals(label.toLowerCase()))) {
                        bestFormulaId = equation.getId();
                        break;
                    }
                }
            }
        }

        boolean spaceEnd = false;
        text = text.replace("\n", " ");
        if (text.endsWith(" ")) {
            spaceEnd = true;
        }
        text = text.trim();

        List<BoundingBox> coords = null;
        if (generateCoordinates && refTokens != null) {
            coords = BoundingBoxCalculator.calculate(refTokens);
        }

        String targetId = bestFormulaId != null ? "formula_" + bestFormulaId : null;
        result.add(new EquationRef(text, targetId, coords));

        if (spaceEnd) {
            result.add(new TextSpan(" "));
        }
        return result;
    }

    /**
     * Create a URL reference from detected URL tokens.
     * Port of {@code TEIFormatter.generateURLRef()}.
     */
    public URLRef resolveURL(
            String destination,
            List<LayoutToken> refTokens,
            boolean generateCoordinates) {

        if (StringUtils.isEmpty(destination)) {
            return null;
        }

        String cleanText = StringUtils.trim(LayoutTokensUtil.toText(refTokens).replace("\n", " "));
        String cleanDestination = StringUtils.trim(destination.replace("\n", " ").replace(" ", ""));

        List<BoundingBox> coords = null;
        if (generateCoordinates && refTokens != null) {
            coords = BoundingBoxCalculator.calculate(refTokens);
        }

        return new URLRef(cleanText, cleanDestination, coords);
    }
}
