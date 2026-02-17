package org.grobid.core.data.util;

import org.grobid.core.data.Affiliation;
import org.grobid.core.data.Person;
import org.grobid.core.layout.LayoutToken;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Assigns affiliations to authors using a priority-based strategy:
 * <ol>
 * <li>Distribution — trivial single-author or single-affiliation cases</li>
 * <li>Marker matching — searches for affiliation markers in the original author
 * string and finds the nearest author by string position (robust to name model
 * errors)</li>
 * <li>Proximity matching — coordinate distance between layout tokens (primary
 * fallback)</li>
 * <li>Sequential fallback — last resort when no coordinates available</li>
 * </ol>
 */
public class AuthorAffiliationAssigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorAffiliationAssigner.class);

    /**
     * Main entry point. Assigns affiliations to authors using the priority
     * strategy.
     *
     * @param authors         list of authors (modified in place via addAffiliation)
     * @param affiliations    list of affiliations (failAffiliation flag updated in
     *                        place)
     * @param originalAuthors the raw author string from the header (used for marker
     *                        matching)
     */
    public static void assign(List<Person> authors, List<Affiliation> affiliations, String originalAuthors) {
        if (CollectionUtils.isEmpty(authors) || CollectionUtils.isEmpty(affiliations)) {
            return;
        }

        int nbAuthors = authors.size();
        int nbAffiliations = affiliations.size();

        LOGGER.debug("Assigning affiliations: {} authors, {} affiliations", nbAuthors, nbAffiliations);

        // 1. Distribution: trivial cases
        if (nbAffiliations == 1) {
            // single affiliation → distribute to all authors
            Affiliation aff = affiliations.get(0);
            for (Person aut : authors) {
                aut.addAffiliation(aff);
            }
            aff.setFailAffiliation(false);
            LOGGER.debug("Distribution: single affiliation '{}' assigned to all {} authors",
                    aff.getRawAffiliationString(), nbAuthors);
            return;
        }

        if (nbAuthors == 1 && nbAffiliations > 1) {
            // single author → assign all affiliations
            Person auth = authors.get(0);
            for (Affiliation aff : affiliations) {
                auth.addAffiliation(aff);
                aff.setFailAffiliation(false);
            }
            LOGGER.debug("Distribution: all {} affiliations assigned to single author '{}'",
                    nbAffiliations, auth.getLastName());
            return;
        }

        // 2. Marker matching (string-search approach from master — robust to name model
        // errors)
        assignByMarkers(authors, affiliations, originalAuthors);

        // 3. Proximity matching (primary fallback for authors still without
        // affiliations)
        assignByProximity(authors, affiliations);

        // 4. Sequential fallback (last resort when no coordinates available)
        assignBySequence(authors, affiliations);
    }

    /**
     * Match authors to affiliations by searching for affiliation markers in the
     * original author string and finding the nearest author name by string
     * position.
     * <p>
     * This approach is robust to name model errors because it operates on the
     * raw concatenated author string rather than relying on structured marker
     * extraction from the name model.
     */
    static void assignByMarkers(List<Person> authors, List<Affiliation> affiliations, String originalAuthors) {
        if (StringUtils.isBlank(originalAuthors)) {
            LOGGER.debug("Marker matching: no originalAuthors string, skipping");
            return;
        }

        boolean hasMarker = false;
        for (Affiliation aff : affiliations) {
            if (aff.getMarker() != null) {
                hasMarker = true;
                break;
            }
        }

        if (!hasMarker) {
            LOGGER.debug("Marker matching: no affiliation markers found, skipping");
            return;
        }

        int indexAffiliation = 0;
        for (Affiliation aff : affiliations) {
            // circuit breaker
            if (indexAffiliation > 60)
                break;

            if (aff.getMarker() != null && aff.getMarker().length() > 0) {
                String marker = aff.getMarker();
                int from = 0;
                int ind = 0;
                ArrayList<Integer> winners = new ArrayList<>();
                while (ind != -1) {
                    ind = originalAuthors.indexOf(marker, from);

                    boolean bad = false;
                    if (ind != -1) {
                        // check for partial matches: single digit matching double digit,
                        // single special char matching double special char
                        if (marker.length() == 1) {
                            if (Character.isDigit(marker.charAt(0))) {
                                if (ind - 1 > 0) {
                                    if (Character.isDigit(originalAuthors.charAt(ind - 1))) {
                                        bad = true;
                                    }
                                }
                                if (ind + 1 < originalAuthors.length()) {
                                    if (Character.isDigit(originalAuthors.charAt(ind + 1))) {
                                        bad = true;
                                    }
                                }
                            } else if (Character.isLetter(marker.charAt(0))) {
                                if (ind - 1 > 0) {
                                    if (Character.isLetter(originalAuthors.charAt(ind - 1))) {
                                        bad = true;
                                    }
                                }
                                if (ind + 1 < originalAuthors.length()) {
                                    if (Character.isLetter(originalAuthors.charAt(ind + 1))) {
                                        bad = true;
                                    }
                                }
                            } else if (marker.charAt(0) == '*') {
                                if (ind - 1 > 0) {
                                    if (originalAuthors.charAt(ind - 1) == '*') {
                                        bad = true;
                                    }
                                }
                                if (ind + 1 < originalAuthors.length()) {
                                    if (originalAuthors.charAt(ind + 1) == '*') {
                                        bad = true;
                                    }
                                }
                            }
                        }
                        if (marker.length() == 2) {
                            // case with ** as marker
                            if ((marker.charAt(0) == '*') && (marker.charAt(1) == '*')) {
                                if (ind - 2 > 0) {
                                    if ((originalAuthors.charAt(ind - 1) == '*') &&
                                            (originalAuthors.charAt(ind - 2) == '*')) {
                                        bad = true;
                                    }
                                }
                                if (ind + 2 < originalAuthors.length()) {
                                    if ((originalAuthors.charAt(ind + 1) == '*') &&
                                            (originalAuthors.charAt(ind + 2) == '*')) {
                                        bad = true;
                                    }
                                }
                                if ((ind - 1 > 0) && (ind + 1 < originalAuthors.length())) {
                                    if ((originalAuthors.charAt(ind - 1) == '*') &&
                                            (originalAuthors.charAt(ind + 1) == '*')) {
                                        bad = true;
                                    }
                                }
                            }
                        }
                    }

                    if ((ind != -1) && !bad) {
                        // find the associated author name by proximity in string
                        String original = originalAuthors.toLowerCase();
                        int p = 0;
                        int best = -1;
                        int ind2 = -1;
                        int bestDistance = 1000;
                        for (Person aut : authors) {
                            if (!winners.contains(Integer.valueOf(p))) {
                                String lastname = aut.getLastName();

                                if (lastname != null) {
                                    lastname = lastname.toLowerCase();
                                    ind2 = original.indexOf(lastname, ind2 + 1);
                                    int dist = Math.abs(ind - (ind2 + lastname.length()));
                                    if (dist < bestDistance) {
                                        best = p;
                                        bestDistance = dist;
                                    }
                                }
                            }
                            p++;
                        }

                        // associate this affiliation to the nearest author
                        if (best != -1) {
                            authors.get(best).addAffiliation(aff);
                            aff.setFailAffiliation(false);
                            winners.add(Integer.valueOf(best));
                            LOGGER.debug("Marker matching: author '{}' matched to affiliation '{}' via marker '{}'",
                                    authors.get(best).getLastName(), aff.getRawAffiliationString(), marker);
                        }

                        from = ind + 1;
                    }
                    if ((ind != -1) && bad) {
                        from = ind + 1;
                        bad = false;
                    }

                    // circuit breaker
                    if (ind > originalAuthors.length() || ind > 1000)
                        break;
                }
            }
            indexAffiliation++;
        }
    }

    /**
     * Match remaining unmatched authors to affiliations using
     * coordinate proximity (distance between layout token centroids).
     * Multiple authors CAN share the same affiliation — each floating author
     * independently picks its nearest affiliation.
     */
    static void assignByProximity(List<Person> authors, List<Affiliation> affiliations) {
        List<Person> floatingAuthors = getFloatingAuthors(authors);
        List<Affiliation> allAffiliations = affiliations; // consider all affiliations, not just floating

        if (floatingAuthors.isEmpty()) {
            LOGGER.debug("Proximity matching: no floating authors, skipping");
            return;
        }

        // compute centroids for floating authors (only those with layout tokens)
        Map<Person, double[]> authorCentroids = new LinkedHashMap<>();
        for (Person aut : floatingAuthors) {
            double[] centroid = computeCentroid(aut.getLayoutTokens());
            if (centroid != null) {
                authorCentroids.put(aut, centroid);
            }
        }

        // compute centroids for all affiliations (only those with layout tokens)
        Map<Affiliation, double[]> affCentroids = new LinkedHashMap<>();
        for (Affiliation aff : allAffiliations) {
            double[] centroid = computeCentroid(aff.getLayoutTokens());
            if (centroid != null) {
                affCentroids.put(aff, centroid);
            }
        }

        if (authorCentroids.isEmpty() || affCentroids.isEmpty()) {
            LOGGER.debug("Proximity matching: insufficient coordinates (authors={}, affs={}), skipping",
                    authorCentroids.size(), affCentroids.size());
            return;
        }

        // Each floating author picks its nearest affiliation independently
        // (no exclusion — multiple authors can share the same affiliation)
        for (Map.Entry<Person, double[]> autEntry : authorCentroids.entrySet()) {
            Person aut = autEntry.getKey();
            double[] autCentroid = autEntry.getValue();

            Affiliation bestAff = null;
            double bestDist = Double.MAX_VALUE;

            for (Map.Entry<Affiliation, double[]> affEntry : affCentroids.entrySet()) {
                Affiliation aff = affEntry.getKey();
                double[] affCentroid = affEntry.getValue();
                double dist = distance(autCentroid, affCentroid);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestAff = aff;
                }
            }

            if (bestAff != null) {
                aut.addAffiliation(bestAff);
                bestAff.setFailAffiliation(false);
                LOGGER.debug("Proximity matching: author '{}' assigned to affiliation '{}' (distance={})",
                        aut.getLastName(), bestAff.getRawAffiliationString(), bestDist);
            }
        }
    }

    /**
     * Sequential fallback: last resort when no coordinates are available.
     * - If fewer remaining affiliations than authors → distribute all remaining
     * affiliations to all remaining authors
     * - Otherwise → 1:1 sequential assignment
     */
    static void assignBySequence(List<Person> authors, List<Affiliation> affiliations) {
        List<Person> floatingAuthors = getFloatingAuthors(authors);
        List<Affiliation> floatingAffiliations = getFloatingAffiliations(affiliations);

        if (floatingAuthors.isEmpty() || floatingAffiliations.isEmpty()) {
            return;
        }

        LOGGER.debug("Sequential fallback: {} floating authors, {} floating affiliations",
                floatingAuthors.size(), floatingAffiliations.size());

        if (floatingAffiliations.size() < floatingAuthors.size()) {
            // Fewer affiliations than authors → distribute all to each author
            for (Person aut : floatingAuthors) {
                for (Affiliation aff : floatingAffiliations) {
                    aut.addAffiliation(aff);
                    aff.setFailAffiliation(false);
                }
            }
            LOGGER.debug("Sequential fallback: distributed {} affiliations to all {} floating authors",
                    floatingAffiliations.size(), floatingAuthors.size());
        } else {
            // Equal or more affiliations than authors → 1:1 sequential
            int p = 0;
            for (Person aut : floatingAuthors) {
                if (p < floatingAffiliations.size()) {
                    aut.addAffiliation(floatingAffiliations.get(p));
                    floatingAffiliations.get(p).setFailAffiliation(false);
                    p++;
                }
            }
            LOGGER.debug("Sequential fallback: assigned {} affiliations 1:1 sequentially", p);
        }
    }

    /**
     * Compute the centroid (average X, average Y, page) of a list of layout
     * tokens.
     *
     * @return double array [x, y, page] or null if tokens are empty/null
     */
    static double[] computeCentroid(List<LayoutToken> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }

        double sumX = 0, sumY = 0;
        int count = 0;
        int page = -1;

        for (LayoutToken token : tokens) {
            if (token.getY() > 0 || token.getX() > 0) {
                sumX += token.getX();
                sumY += token.getY();
                count++;
                if (page == -1) {
                    page = token.getPage();
                }
            }
        }

        if (count == 0) {
            return null;
        }

        return new double[] { sumX / count, sumY / count, page };
    }

    /**
     * Compute distance between two centroids.
     * If on different pages, adds a large penalty to prefer same-page matches.
     */
    static double distance(double[] centroid1, double[] centroid2) {
        double dx = centroid1[0] - centroid2[0];
        double dy = centroid1[1] - centroid2[1];
        double dist = Math.sqrt(dx * dx + dy * dy);

        // page penalty: different pages are much less likely to be related
        if ((int) centroid1[2] != (int) centroid2[2]) {
            dist += 10000.0;
        }

        return dist;
    }

    /**
     * Get authors that have no affiliations assigned yet.
     */
    private static List<Person> getFloatingAuthors(List<Person> authors) {
        List<Person> floating = new ArrayList<>();
        for (Person aut : authors) {
            if (CollectionUtils.isEmpty(aut.getAffiliations())) {
                floating.add(aut);
            }
        }
        return floating;
    }

    /**
     * Get affiliations that are still unassigned (failAffiliation == true).
     */
    private static List<Affiliation> getFloatingAffiliations(List<Affiliation> affiliations) {
        List<Affiliation> floating = new ArrayList<>();
        for (Affiliation aff : affiliations) {
            if (aff.getFailAffiliation()) {
                floating.add(aff);
            }
        }
        return floating;
    }
}
