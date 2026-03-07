package org.grobid.core.data.util;

import org.grobid.core.data.Affiliation;
import org.grobid.core.data.Person;
import org.grobid.core.layout.LayoutToken;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class AuthorAffiliationAssignerTest {

    // --- Distribution tests ---

    @Test
    public void testSingleAuthorSingleAff() {
        List<Person> authors = authors("Doe");
        List<Affiliation> affs = affiliations("University of Nowhere");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getRawAffiliationString(),
                is("University of Nowhere"));
        assertFalse(affs.get(0).getFailAffiliation());
    }

    @Test
    public void testSingleAuthorMultipleAffs() {
        List<Person> authors = authors("Doe");
        List<Affiliation> affs = affiliations("Univ A", "Univ B", "Univ C");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(authors.get(0).getAffiliations(), hasSize(3));
        for (Affiliation aff : affs) {
            assertFalse(aff.getFailAffiliation());
        }
    }

    @Test
    public void testMultipleAuthorsSingleAff() {
        List<Person> authors = authors("Smith", "Jones", "Wang");
        List<Affiliation> affs = affiliations("MIT");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        for (Person aut : authors) {
            assertThat(aut.getAffiliations(), hasSize(1));
            assertThat(aut.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        }
        assertFalse(affs.get(0).getFailAffiliation());
    }

    // --- Marker matching tests (string-search approach) ---

    @Test
    public void testMarkerMatching_simple() {
        List<Person> authors = authors("Smith", "Wesson");

        Affiliation aff1 = affiliation("University of One");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("University of Two");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        // Simulate the original author string with markers as superscripts
        String originalAuthors = "Smith 1, Wesson 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getMarker(), is("1"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getMarker(), is("2"));
    }

    @Test
    public void testMarkerMatching_compound() {
        // An author with markers "1,2" should get both affiliations
        List<Person> authors = authors("Smith", "Jones");

        Affiliation aff1 = affiliation("University of One");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("University of Two");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        // Smith has markers 1,2 and Jones has marker 2
        String originalAuthors = "Smith 1,2, Jones 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith gets aff 1 via marker "1" AND aff 2 via first occurrence of "2" (nearest to Smith)
        assertThat(authors.get(0).getAffiliations(), hasSize(2));
        // Jones gets aff 2 via second occurrence of "2"
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
    }

    @Test
    public void testMarkerMatching_relaxed() {
        // Marker "*" on affiliation, present in original authors string
        Person a1 = person("Burda");
        Person a2 = person("Edwards");
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("OpenAI");
        aff1.setMarker("*");
        Affiliation aff2 = affiliation("Another Lab");
        aff2.setMarker("†");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        String originalAuthors = "Burda *, Edwards †";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("Another Lab"));
    }

    @Test
    public void testMarkerMatching_noOriginalAuthors() {
        // When originalAuthors is null, marker matching is skipped and
        // fallback strategies should handle assignment
        List<Person> authors = authors("Smith", "Jones");
        // Give them layout tokens for proximity matching
        authors.get(0).setLayoutTokens(tokensAt(100, 50, 1));
        authors.get(1).setLayoutTokens(tokensAt(100, 200, 1));

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        aff1.setLayoutTokens(tokensAt(100, 60, 1));
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        aff2.setLayoutTokens(tokensAt(100, 190, 1));
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // Proximity fallback should assign
        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testMarkerMatching_preventsSingleDigitMatchingDoubleDigit() {
        // Marker "1" should not match inside "11" in the originalAuthors string
        List<Person> authors = authors("Smith", "Jones");

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff11 = affiliation("Stanford");
        aff11.setMarker("11");
        List<Affiliation> affs = Arrays.asList(aff1, aff11);

        String originalAuthors = "Smith 1, Jones 11";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith should get "1" (MIT), Jones should get "11" (Stanford)
        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    // --- Proximity matching tests ---

    @Test
    public void testProximity_sharedAffiliation() {
        // Two authors close to same affiliation, one distant affiliation.
        // Proximity assigns both to OpenAI, then orphan rescue assigns
        // Stanford to the nearest author (Edwards at y=70 is closer to y=500).
        Person a1 = person("Burda");
        a1.setLayoutTokens(tokensAt(100, 50, 1));
        Person a2 = person("Edwards");
        a2.setLayoutTokens(tokensAt(100, 70, 1));
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("OpenAI");
        aff1.setLayoutTokens(tokensAt(100, 60, 1));
        Affiliation aff2 = affiliation("Stanford");
        aff2.setLayoutTokens(tokensAt(100, 500, 1));
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // Both get OpenAI via proximity, Stanford is rescued to nearest author
        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        // Edwards gets OpenAI + rescued Stanford
        assertThat(a2.getAffiliations(), hasSize(2));
        // No affiliation should be orphaned
        assertFalse(aff1.getFailAffiliation());
        assertFalse(aff2.getFailAffiliation());
    }

    @Test
    public void testProximity_interleaved() {
        // Authors and affiliations are interleaved vertically
        Person burda = person("Burda");
        burda.setLayoutTokens(tokensAt(100, 100, 1));

        Person edwards = person("Edwards");
        edwards.setLayoutTokens(tokensAt(100, 120, 1));

        Person storkey = person("Storkey");
        storkey.setLayoutTokens(tokensAt(100, 200, 1));

        Person klimov = person("Klimov");
        klimov.setLayoutTokens(tokensAt(100, 260, 1));

        List<Person> authors = Arrays.asList(burda, edwards, storkey, klimov);

        Affiliation openai = affiliation("OpenAI");
        openai.setLayoutTokens(tokensAt(100, 140, 1));

        Affiliation edinburgh = affiliation("Univ. of Edinburgh");
        edinburgh.setLayoutTokens(tokensAt(100, 220, 1));

        List<Affiliation> affs = Arrays.asList(openai, edinburgh);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(burda.getAffiliations(), hasSize(1));
        assertThat(burda.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        assertThat(edwards.getAffiliations(), hasSize(1));
        assertThat(edwards.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        assertThat(storkey.getAffiliations(), hasSize(1));
        assertThat(storkey.getAffiliations().get(0).getRawAffiliationString(), is("Univ. of Edinburgh"));
        assertThat(klimov.getAffiliations(), hasSize(1));
        assertThat(klimov.getAffiliations().get(0).getRawAffiliationString(), is("Univ. of Edinburgh"));
    }

    @Test
    public void testProximity_interleavedWithMarkerMatch() {
        // Marker-matched authors (via originalAuthors string), rest by proximity
        Person burda = person("Burda");
        burda.setLayoutTokens(tokensAt(100, 100, 1));

        Person edwards = person("Edwards");
        edwards.setLayoutTokens(tokensAt(100, 120, 1));

        Person storkey = person("Storkey");
        storkey.setLayoutTokens(tokensAt(100, 200, 1));

        Person klimov = person("Klimov");
        klimov.setLayoutTokens(tokensAt(100, 260, 1));

        List<Person> authors = Arrays.asList(burda, edwards, storkey, klimov);

        Affiliation openai = affiliation("OpenAI");
        openai.setMarker("*");
        openai.setLayoutTokens(tokensAt(100, 140, 1));

        Affiliation edinburgh = affiliation("Univ. of Edinburgh");
        edinburgh.setLayoutTokens(tokensAt(100, 220, 1));

        List<Affiliation> affs = Arrays.asList(openai, edinburgh);

        String originalAuthors = "Burda *, Edwards *, Storkey, Klimov";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Burda and Edwards match OpenAI via marker "*"
        assertThat(burda.getAffiliations(), hasSize(1));
        assertThat(burda.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));
        assertThat(edwards.getAffiliations(), hasSize(1));
        assertThat(edwards.getAffiliations().get(0).getRawAffiliationString(), is("OpenAI"));

        // Storkey and Klimov get Edinburgh via proximity
        assertThat(storkey.getAffiliations(), hasSize(1));
        assertThat(storkey.getAffiliations().get(0).getRawAffiliationString(), is("Univ. of Edinburgh"));
        assertThat(klimov.getAffiliations(), hasSize(1));
        assertThat(klimov.getAffiliations().get(0).getRawAffiliationString(), is("Univ. of Edinburgh"));
    }

    // --- Sequential fallback tests ---

    @Test
    public void testSequentialFallback_noCoords() {
        // No markers, no coordinates → sequential 1:1
        List<Person> authors = authors("Smith", "Jones");
        List<Affiliation> affs = affiliations("MIT", "Stanford");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testSequentialFallback_moreAuthorsThanAffs() {
        // 3 authors, 2 affiliations, no markers/coords → distribute remaining
        List<Person> authors = authors("Smith", "Jones", "Wang");
        List<Affiliation> affs = affiliations("MIT", "Stanford");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // With fewer affs than authors, distribute all affs to all authors
        for (Person aut : authors) {
            assertThat(aut.getAffiliations(), hasSize(2));
        }
    }

    // --- Mixed tests ---

    @Test
    public void testMixed_markersAndProximity() {
        // Author 1 matched by marker string search, author 2 by proximity
        Person a1 = person("Smith");
        a1.setLayoutTokens(tokensAt(100, 100, 1));
        Person a2 = person("Jones");
        a2.setLayoutTokens(tokensAt(100, 200, 1));
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        aff1.setLayoutTokens(tokensAt(100, 120, 1));
        Affiliation aff2 = affiliation("Stanford");
        aff2.setLayoutTokens(tokensAt(100, 210, 1));
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        String originalAuthors = "Smith 1, Jones";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith gets MIT via marker
        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        // Jones gets Stanford via proximity
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    // --- Marker matching edge cases ---

    @Test
    public void testMarkerMatching_authorNameNotInOriginalString() {
        // Author name model returns a different form than what appears in the raw string.
        // The marker search should skip authors whose name isn't found rather than
        // producing a bogus match from a -1 indexOf result.
        Person a1 = person("Smith");
        Person a2 = person("Van der Berg"); // won't appear verbatim in originalAuthors
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        // originalAuthors has "Berg" but not "Van der Berg"
        String originalAuthors = "Smith 1, Berg 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith is the only author whose name appears in originalAuthors,
        // so it gets both affiliations via nearest-author string search
        assertThat(a1.getAffiliations(), hasSize(2));
    }

    // --- Direct marker matching tests (Person.getMarkers() vs Affiliation.getMarker()) ---

    @Test
    public void testDirectMarkerMatching_simple() {
        Person a1 = person("Smith");
        a1.setMarkers(Arrays.asList("1"));
        Person a2 = person("Jones");
        a2.setMarkers(Arrays.asList("2"));
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testDirectMarkerMatching_multipleMarkersPerAuthor() {
        Person a1 = person("Smith");
        a1.setMarkers(Arrays.asList("1", "2"));
        List<Person> authors = Arrays.asList(a1);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        assertThat(a1.getAffiliations(), hasSize(2));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(a1.getAffiliations().get(1).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testDirectMarkerMatching_sharedMarker() {
        Person a1 = person("Smith");
        a1.setMarkers(Arrays.asList("1"));
        Person a2 = person("Jones");
        a2.setMarkers(Arrays.asList("1"));
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // Both authors share marker "1" → both get MIT
        // Stanford (marker "2") has no matching author but gets rescued
        // to the first author with fewest affiliations (Smith, since both have 1)
        assertThat(a1.getAffiliations(), hasSize(2));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(a1.getAffiliations().get(1).getRawAffiliationString(), is("Stanford"));
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        // No affiliation should be orphaned
        assertFalse(aff1.getFailAffiliation());
        assertFalse(aff2.getFailAffiliation());
    }

    @Test
    public void testDirectMarkerMatching_fallsBackToStringSearch() {
        // Author A has person markers → matched by direct markers
        // Author B has no person markers → matched by string-search fallback
        Person a1 = person("Smith");
        a1.setMarkers(Arrays.asList("1"));
        Person a2 = person("Jones");
        // no markers set on a2
        List<Person> authors = Arrays.asList(a1, a2);

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        String originalAuthors = "Smith 1, Jones 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        // Smith gets MIT via direct markers
        assertThat(a1.getAffiliations(), hasSize(1));
        assertThat(a1.getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        // Jones gets Stanford via string-search fallback
        assertThat(a2.getAffiliations(), hasSize(1));
        assertThat(a2.getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    @Test
    public void testDirectMarkerMatching_noPersonMarkers() {
        // No person markers set → direct marker matching is a no-op,
        // string-search handles everything
        List<Person> authors = authors("Smith", "Jones");

        Affiliation aff1 = affiliation("MIT");
        aff1.setMarker("1");
        Affiliation aff2 = affiliation("Stanford");
        aff2.setMarker("2");
        List<Affiliation> affs = Arrays.asList(aff1, aff2);

        String originalAuthors = "Smith 1, Jones 2";

        AuthorAffiliationAssigner.assign(authors, affs, originalAuthors);

        assertThat(authors.get(0).getAffiliations(), hasSize(1));
        assertThat(authors.get(0).getAffiliations().get(0).getRawAffiliationString(), is("MIT"));
        assertThat(authors.get(1).getAffiliations(), hasSize(1));
        assertThat(authors.get(1).getAffiliations().get(0).getRawAffiliationString(), is("Stanford"));
    }

    // --- Orphan rescue tests ---

    @Test
    public void testOrphanRescue_proximityAllSameButOrphanExists() {
        // 3 authors on same line, 3 affiliations stacked below.
        // Without rescue, proximity assigns all authors to nearest aff0,
        // leaving aff1 and aff2 orphaned.
        Person a1 = person("Jeong");
        a1.setLayoutTokens(tokensAt(100, 50, 1));
        Person a2 = person("Chang");
        a2.setLayoutTokens(tokensAt(250, 50, 1));
        Person a3 = person("Valdez");
        a3.setLayoutTokens(tokensAt(400, 50, 1));
        List<Person> authors = Arrays.asList(a1, a2, a3);

        Affiliation aff0 = affiliation("Simon Fraser");
        aff0.setLayoutTokens(tokensAt(100, 100, 1));
        Affiliation aff1 = affiliation("Texas A&M");
        aff1.setLayoutTokens(tokensAt(100, 130, 1));
        Affiliation aff2 = affiliation("UConn");
        aff2.setLayoutTokens(tokensAt(100, 160, 1));
        List<Affiliation> affs = Arrays.asList(aff0, aff1, aff2);

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // All 3 affiliations should be assigned (no orphans)
        for (Affiliation aff : affs) {
            assertFalse("Affiliation '" + aff.getRawAffiliationString() + "' should not be orphaned",
                    aff.getFailAffiliation());
        }
        // Each author should have at least one affiliation
        for (Person aut : authors) {
            assertNotNull(aut.getAffiliations());
            assertFalse("Author '" + aut.getLastName() + "' should have affiliations",
                    aut.getAffiliations().isEmpty());
        }
    }

    @Test
    public void testOrphanRescue_noCoordinates() {
        // 2 authors, 3 affiliations, no markers, no coordinates.
        // Sequential assigns 1:1 (a1->aff0, a2->aff1), leaving aff2 orphaned.
        // Rescue should assign aff2 to the author with fewest affiliations.
        List<Person> authors = authors("Barua", "Maitra");
        List<Affiliation> affs = affiliations("ISI Kolkata", "UMinn", "Extra Lab");

        AuthorAffiliationAssigner.assign(authors, affs, null);

        // All affiliations should be assigned
        for (Affiliation aff : affs) {
            assertFalse("Affiliation '" + aff.getRawAffiliationString() + "' should not be orphaned",
                    aff.getFailAffiliation());
        }
    }

    // --- Null / empty tests ---

    @Test
    public void testNullAuthors() {
        AuthorAffiliationAssigner.assign(null, affiliations("MIT"), null);
        // no exception
    }

    @Test
    public void testNullAffiliations() {
        AuthorAffiliationAssigner.assign(authors("Smith"), null, null);
        assertNull(authors("Smith").get(0).getAffiliations());
    }

    @Test
    public void testEmptyAuthors() {
        AuthorAffiliationAssigner.assign(new ArrayList<>(), affiliations("MIT"), null);
        // no exception
    }

    // --- Helper methods ---

    private static Person person(String lastName) {
        Person p = new Person();
        p.setLastName(lastName);
        return p;
    }

    private static Affiliation affiliation(String rawString) {
        Affiliation aff = new Affiliation();
        aff.setRawAffiliationString(rawString);
        return aff;
    }

    private static List<Person> authors(String... lastNames) {
        List<Person> list = new ArrayList<>();
        for (String name : lastNames) {
            list.add(person(name));
        }
        return list;
    }

    private static List<Affiliation> affiliations(String... rawStrings) {
        List<Affiliation> list = new ArrayList<>();
        for (String raw : rawStrings) {
            list.add(affiliation(raw));
        }
        return list;
    }

    /**
     * Create a list containing a single LayoutToken with the given coordinates.
     */
    private static List<LayoutToken> tokensAt(double x, double y, int page) {
        LayoutToken token = new LayoutToken();
        token.setX(x);
        token.setY(y);
        token.setPage(page);
        List<LayoutToken> tokens = new ArrayList<>();
        tokens.add(token);
        return tokens;
    }
}
