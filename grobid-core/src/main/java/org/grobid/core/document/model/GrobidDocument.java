package org.grobid.core.document.model;

import java.util.ArrayList;
import java.util.List;

import org.grobid.core.data.*;
import org.grobid.core.engines.citations.CalloutAnalyzer.MarkerType;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.layout.Page;

/**
 * The complete structured representation of a processed document.
 *
 * <p>This is the intermediate data model populated by {@code DocumentStructureBuilder}
 * and consumed by the three output formatters (TEI, JSON, Markdown). It reuses existing
 * Grobid data classes directly ({@link BiblioItem}, {@link Figure}, {@link Table},
 * {@link Equation}, {@link Note}, {@link Funding}, {@link Affiliation}, {@link Person},
 * {@link BibDataSet}, {@link Page}) and introduces new types only for body structure
 * ({@link BodyElement}, {@link InlineContent}) which previously had no data model.</p>
 */
public class GrobidDocument {

    // -- Metadata --

    private String language;
    private String md5;
    private List<MarkerType> markerTypes;
    private GrobidAnalysisConfig config;

    // -- Header (reuse BiblioItem directly — 200+ fields already modeled) --

    private BiblioItem header;

    // -- Structured abstract --

    private List<BodyElement> abstractContent;

    // -- Body & Annex (the new structured representation) --

    private List<BodyElement> body;
    private List<BodyElement> annex;

    // -- Back matter sections (structured as body elements) --

    private List<BodyElement> acknowledgement;
    private List<BodyElement> fundingSection;
    private List<BodyElement> availability;
    private List<BodyElement> conflictOfInterest;
    private List<BodyElement> authorContribution;

    // -- Extracted structured data from back matter --

    private List<Funding> fundings;
    private List<Affiliation> acknowledgedAffiliations;
    private List<Affiliation> acknowledgedInfrastructures;

    // -- Content elements (reuse existing classes) --

    private List<Figure> figures;
    private List<Figure> annexFigures;
    private List<Table> tables;
    private List<Table> annexTables;
    private List<Equation> equations;
    private List<Equation> annexEquations;
    private List<Note> footnotes;
    private List<Note> marginNotes;

    // -- References (reuse existing) --

    private List<BibDataSet> references;

    // -- Layout --

    private List<Page> pages;

    public GrobidDocument() {
        this.abstractContent = new ArrayList<>();
        this.body = new ArrayList<>();
        this.annex = new ArrayList<>();
        this.acknowledgement = new ArrayList<>();
        this.fundingSection = new ArrayList<>();
        this.availability = new ArrayList<>();
        this.conflictOfInterest = new ArrayList<>();
        this.authorContribution = new ArrayList<>();
        this.fundings = new ArrayList<>();
        this.acknowledgedAffiliations = new ArrayList<>();
        this.acknowledgedInfrastructures = new ArrayList<>();
        this.figures = new ArrayList<>();
        this.annexFigures = new ArrayList<>();
        this.tables = new ArrayList<>();
        this.annexTables = new ArrayList<>();
        this.equations = new ArrayList<>();
        this.annexEquations = new ArrayList<>();
        this.footnotes = new ArrayList<>();
        this.marginNotes = new ArrayList<>();
        this.references = new ArrayList<>();
        this.pages = new ArrayList<>();
        this.markerTypes = new ArrayList<>();
    }

    // -- Metadata accessors --

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public List<MarkerType> getMarkerTypes() {
        return markerTypes;
    }

    public void setMarkerTypes(List<MarkerType> markerTypes) {
        this.markerTypes = markerTypes;
    }

    public GrobidAnalysisConfig getConfig() {
        return config;
    }

    public void setConfig(GrobidAnalysisConfig config) {
        this.config = config;
    }

    // -- Header --

    public BiblioItem getHeader() {
        return header;
    }

    public void setHeader(BiblioItem header) {
        this.header = header;
    }

    // -- Abstract --

    public List<BodyElement> getAbstractContent() {
        return abstractContent;
    }

    public void setAbstractContent(List<BodyElement> abstractContent) {
        this.abstractContent = abstractContent;
    }

    // -- Body & Annex --

    public List<BodyElement> getBody() {
        return body;
    }

    public void setBody(List<BodyElement> body) {
        this.body = body;
    }

    public List<BodyElement> getAnnex() {
        return annex;
    }

    public void setAnnex(List<BodyElement> annex) {
        this.annex = annex;
    }

    // -- Back matter sections --

    public List<BodyElement> getAcknowledgement() {
        return acknowledgement;
    }

    public void setAcknowledgement(List<BodyElement> acknowledgement) {
        this.acknowledgement = acknowledgement;
    }

    public List<BodyElement> getFundingSection() {
        return fundingSection;
    }

    public void setFundingSection(List<BodyElement> fundingSection) {
        this.fundingSection = fundingSection;
    }

    public List<BodyElement> getAvailability() {
        return availability;
    }

    public void setAvailability(List<BodyElement> availability) {
        this.availability = availability;
    }

    public List<BodyElement> getConflictOfInterest() {
        return conflictOfInterest;
    }

    public void setConflictOfInterest(List<BodyElement> conflictOfInterest) {
        this.conflictOfInterest = conflictOfInterest;
    }

    public List<BodyElement> getAuthorContribution() {
        return authorContribution;
    }

    public void setAuthorContribution(List<BodyElement> authorContribution) {
        this.authorContribution = authorContribution;
    }

    // -- Extracted structured data from back matter --

    public List<Funding> getFundings() {
        return fundings;
    }

    public void setFundings(List<Funding> fundings) {
        this.fundings = fundings;
    }

    public List<Affiliation> getAcknowledgedAffiliations() {
        return acknowledgedAffiliations;
    }

    public void setAcknowledgedAffiliations(List<Affiliation> acknowledgedAffiliations) {
        this.acknowledgedAffiliations = acknowledgedAffiliations;
    }

    public List<Affiliation> getAcknowledgedInfrastructures() {
        return acknowledgedInfrastructures;
    }

    public void setAcknowledgedInfrastructures(List<Affiliation> acknowledgedInfrastructures) {
        this.acknowledgedInfrastructures = acknowledgedInfrastructures;
    }

    // -- Content elements --

    public List<Figure> getFigures() {
        return figures;
    }

    public void setFigures(List<Figure> figures) {
        this.figures = figures;
    }

    public List<Figure> getAnnexFigures() {
        return annexFigures;
    }

    public void setAnnexFigures(List<Figure> annexFigures) {
        this.annexFigures = annexFigures;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public List<Table> getAnnexTables() {
        return annexTables;
    }

    public void setAnnexTables(List<Table> annexTables) {
        this.annexTables = annexTables;
    }

    public List<Equation> getEquations() {
        return equations;
    }

    public void setEquations(List<Equation> equations) {
        this.equations = equations;
    }

    public List<Equation> getAnnexEquations() {
        return annexEquations;
    }

    public void setAnnexEquations(List<Equation> annexEquations) {
        this.annexEquations = annexEquations;
    }

    public List<Note> getFootnotes() {
        return footnotes;
    }

    public void setFootnotes(List<Note> footnotes) {
        this.footnotes = footnotes;
    }

    public List<Note> getMarginNotes() {
        return marginNotes;
    }

    public void setMarginNotes(List<Note> marginNotes) {
        this.marginNotes = marginNotes;
    }

    // -- References --

    public List<BibDataSet> getReferences() {
        return references;
    }

    public void setReferences(List<BibDataSet> references) {
        this.references = references;
    }

    // -- Layout --

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }
}
