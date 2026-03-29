package org.grobid.core.engines;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.GrobidConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Lexicon.class)
public class CitationParserNullHandlingTest {

    private CitationParser target;

    @Before
    public void setUp() throws Exception {
        PowerMock.mockStatic(Lexicon.class);
        Lexicon mockLexicon = EasyMock.createNiceMock(Lexicon.class);
        EasyMock.expect(Lexicon.getInstance()).andReturn(mockLexicon).anyTimes();
        PowerMock.replay(Lexicon.class);
        GrobidConfig.ModelParameters modelParameters = new GrobidConfig.ModelParameters();
        modelParameters.name = "bao";
        GrobidProperties.addModel(modelParameters);

        // Initialize minimal grobidConfig so BiblioItem serialization methods work
        GrobidConfig config = new GrobidConfig();
        config.grobid = new GrobidConfig.GrobidParameters();
        config.grobid.languageDetectorFactory = "org.grobid.core.lang.impl.CybozuLanguageDetectorFactory";
        Whitebox.setInternalState(GrobidProperties.class, "grobidConfig", config);

        target = new CitationParser(null, GrobidModels.DUMMY);
    }

    @After
    public void tearDown() throws Exception {
        Whitebox.setInternalState(GrobidProperties.class, "grobidConfig", (GrobidConfig) null);
    }

    @Test
    public void processingStringMultiple_nullInput_returnsNull() {
        List<BiblioItem> result = target.processingStringMultiple(null, 0);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void processingStringMultiple_emptyList_returnsNull() {
        List<BiblioItem> result = target.processingStringMultiple(new ArrayList<>(), 0);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void processingStringMultiple_allBlankStrings_returnsNull() {
        List<String> inputs = Arrays.asList("", "   ", "\t");
        List<BiblioItem> result = target.processingStringMultiple(inputs, 0);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void processingStringMultiple_nbspOnlyStrings_returnsNull() {
        List<String> inputs = Arrays.asList("\u00A0", "\u00A0\u00A0\u00A0");
        List<BiblioItem> result = target.processingStringMultiple(inputs, 0);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void processingLayoutTokenMultiple_allEmptyTokenLists_returnsNull() {
        List<List<LayoutToken>> tokenList = new ArrayList<>();
        tokenList.add(new ArrayList<>());
        tokenList.add(new ArrayList<>());
        List<BiblioItem> result = target.processingLayoutTokenMultiple(tokenList, 0);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void emptyBiblioItem_serializesSafely() {
        BiblioItem empty = new BiblioItem();
        GrobidAnalysisConfig config = GrobidAnalysisConfig.defaultInstance();

        String bibtex = empty.toBibTeX("0", config);
        assertThat(bibtex, is(notNullValue()));

        String tei = empty.toTEI(0, config);
        assertThat(tei, is(notNullValue()));
    }

    @Test
    public void emptyBiblioItem_isRejectedAsReference() {
        BiblioItem empty = new BiblioItem();
        assertThat(empty.rejectAsReference(), is(true));
    }
}
