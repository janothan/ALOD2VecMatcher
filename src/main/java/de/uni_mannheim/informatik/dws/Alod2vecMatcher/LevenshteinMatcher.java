package de.uni_mannheim.informatik.dws.Alod2vecMatcher;

import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.LabelLanguageTuple;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.UriLabelInfo;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.CorrespondenceRelation;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.DefaultExtensions;
import org.apache.commons.collections.map.HashedMap;
import org.apache.jena.ontology.OntModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A simple levenshtein matcher.
 */
public class LevenshteinMatcher extends LabelBasedMatcher {

    /**
     * Allowed levenshtein distance to add a correspondence to the final alignment.
     */
    private int allowedDifference = 1;

    /**
     * On very short words and abbreviations, Levenshtein leads to false-positives, e.g. for 'ADP' (adenosine diphosphate)
     * and for 'ATP' (adenosine triphosphate). Therefore, a minimal character length can be set in order to exclude
     * such words.
     */
    private int minimalCharacterLength = 4;

    /**
     * Default logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LevenshteinMatcher.class);

    @Override
    public Alignment match(OntModel sourceOntology, OntModel targetOntology, Alignment alignment, Properties properties) throws Exception {
        ontology1 = sourceOntology;
        ontology2 = targetOntology;
        this.alignment = alignment;
        loadLabels(sourceOntology, targetOntology);

        LOGGER.info("Running Levenshtein Matcher");
        matchWithLevenshtein(this.uri2labelMapClasses_1, this.uri2labelMapClasses_2);
        matchWithLevenshtein(this.uri2labelMapDatatypeProperties_1, this.uri2labelMapDatatypeProperties_2);
        matchWithLevenshtein(this.uri2labelMapObjectProperties_1, this.uri2labelMapObjectProperties_2);
        matchWithLevenshtein(this.uri2labelMapRemainingProperties_1, this.uri2labelMapRemainingProperties_2);
        LOGGER.info("Completed Running Levenshtein Matcher");

        return this.alignment;
    }


    private void matchWithLevenshtein(UriLabelInfo uriLabelMap_1, UriLabelInfo uriLabelMap_2) {
        HashMap<String, MinMax> extrama1 = getLabelExtrema(uriLabelMap_1.getUriLabelMap());
        HashMap<String, MinMax> extrema2 = getLabelExtrema(uriLabelMap_2.getUriLabelMap());

        loop1:
        for (HashedMap.Entry<String, HashSet<LabelLanguageTuple>> entry1 : uriLabelMap_1.getUriLabelMap().entrySet()) {
            if (alignment.isSourceContained(entry1.getKey())) continue loop1;
            MinMax minMax1 = extrama1.get(entry1.getKey());

            loop2:
            for (HashedMap.Entry<String, HashSet<LabelLanguageTuple>> entry2 : uriLabelMap_2.getUriLabelMap().entrySet()) {
                if (alignment.isTargetContained(entry2.getKey())) continue loop2;

                MinMax minMax2 = extrema2.get(entry2.getKey());
                if(minMax1.max + allowedDifference < minMax2.min ||
                        minMax1.min > minMax2.max + allowedDifference ||
                        (minMax1.max < minimalCharacterLength && minMax2.max < minimalCharacterLength)){
                    continue loop1;
                }

                loop11:
                for (LabelLanguageTuple s1 : entry1.getValue()) {
                    if(s1.label.length() < minimalCharacterLength) continue loop11;

                    loop22:
                    for (LabelLanguageTuple s2 : entry2.getValue()) {
                        if(s2.label.length() < minimalCharacterLength) continue loop22;
                        if (Math.abs(s1.label.length() - s2.label.length()) > allowedDifference) continue loop22;
                        if (isDamerauLevenshteinEqualOrLess(s1.label.toLowerCase(), s2.label.toLowerCase(), allowedDifference)) {
                            Map<String, Object> extensions = new HashMap<>();
                            extensions.put(DefaultExtensions.DublinCore.DESCRIPTION.toString(), "Label '" + s1.label + "' of ontology 1 and label '" + s2.label + " of ontology 2 have a very similar writing.");
                            Correspondence newCorrespondence = new Correspondence(entry1.getKey(), entry2.getKey(), 1.0, CorrespondenceRelation.EQUIVALENCE, extensions);
                            alignment.add(newCorrespondence);
                            LOGGER.info("New correspondence: " + newCorrespondence);
                        }
                    } // end of loop 22
                } // end of loop11
            } // end of loop2
        } // end of loop1
    }


    /**
     * Get the minimum and maximum lengths of the labels.
     * @param uriLabelMap
     * @return
     */
    private HashMap<String, MinMax> getLabelExtrema(HashMap<String, HashSet<LabelLanguageTuple>> uriLabelMap){
        HashMap<String, MinMax> result = new HashMap<>();
        for (HashedMap.Entry<String, HashSet<LabelLanguageTuple>> entry : uriLabelMap.entrySet()) {
            int min = 0;
            int max = 0;
            for(LabelLanguageTuple llt : entry.getValue()){
                if(llt.label.length() > max) max = llt.label.length();
                if(llt.label.length() < min) min = llt.label.length();
            }
            result.put(entry.getKey(), new MinMax(min, max));
        }
        return result;
    }

    private class MinMax{
        int min;
        int max;
        MinMax(int min, int max){
            this.min = min;
            this.max = max;
        }
    }


    public static boolean isDamerauLevenshteinEqualOrLess(String s1, String s2, int allowedDifference){
        return damerauLevenshteinDistanceWithLimit(s1.toCharArray(), s2.toCharArray(), allowedDifference) <= allowedDifference;
    }


    /**
     * Implementation by https://stackoverflow.com/questions/9453731/how-to-calculate-distance-similarity-measure-of-given-2-strings/9454016#9454016
     * (ported to Java)
     * @param source
     * @param target
     * @return
     */
    public static int damerauLevenshteinDistanceWithLimit(char[] source, char[] target, int threshold) {
        int length1 = source.length;
        int length2 = target.length;

        // Return trivial case - difference in string lengths exceeds threshhold
        if (Math.abs(length1 - length2) > threshold) { return 1000; }

        // Ensure arrays [i] / length1 use shorter length
        if (length1 > length2) {
            char[] temporary = source;
            source = target;
            target = temporary;
            length1 = source.length;
            length2 = target.length;
        }

        int maxi = length1;
        int maxj = length2;

        int[] dCurrent = new int[maxi + 1];
        int[] dMinus1 = new int[maxi + 1];
        int[] dMinus2 = new int[maxi + 1];
        int[] dSwap;

        for (int i = 0; i <= maxi; i++) { dCurrent[i] = i; }

        int jm1 = 0, im1 = 0, im2 = -1;

        for (int j = 1; j <= maxj; j++) {

            // Rotate
            dSwap = dMinus2;
            dMinus2 = dMinus1;
            dMinus1 = dCurrent;
            dCurrent = dSwap;

            // Initialize
            int minDistance = 1000;
            dCurrent[0] = j;
            im1 = 0;
            im2 = -1;

            for (int i = 1; i <= maxi; i++) {

                int cost = source[im1] == target[jm1] ? 0 : 1;

                int del = dCurrent[im1] + 1;
                int ins = dMinus1[i] + 1;
                int sub = dMinus1[im1] + cost;

                //Fastest execution for min value of 3 integers
                int min = (del > ins) ? (ins > sub ? sub : ins) : (del > sub ? sub : del);

                if (i > 1 && j > 1 && source[im2] == target[jm1] && source[im1] == target[j - 2])
                    min = Math.min(min, dMinus2[im2] + cost);

                dCurrent[i] = min;
                if (min < minDistance) { minDistance = min; }
                im1++;
                im2++;
            }
            jm1++;
            if (minDistance > threshold) { return 1000; }
        }

        int result = dCurrent[maxi];
        return (result > threshold) ? 1000 : result;
    }


    /**
     * Iplementation: https://www.baeldung.com/java-levenshtein-distance
     * @param x
     * @param y
     * @return
     */
    public static int levenshteinDynamic(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = min(dp[i - 1][j - 1]
                                    + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }
        return dp[x.length()][y.length()];
    }

    public static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    public static int min(int... numbers) {
        return Arrays.stream(numbers)
                .min().orElse(Integer.MAX_VALUE);
    }



    /**
     * Implementation by <a href ="https://rosettacode.org/wiki/Levenshtein_distance#Java">https://rosettacode.org/wiki/Levenshtein_distance#Java</a>
     * @param s1 String 1.
     * @param s2 String 2.
     * @return The distance.
     */
    public static int levenshteinDistanceRecursive(String s1, String s2){
        /* if either string is empty, difference is inserting all chars
         * from the other
         */
        if(s1.length() == 0) return s2.length();
        if(s2.length() == 0) return s1.length();

        /* if first letters are the same, the difference is whatever is
         * required to edit the rest of the strings
         */
        if(s1.charAt(0) == s2.charAt(0))
            return levenshteinDistanceRecursive(s1.substring(1), s2.substring(1));

        /* else try:
         *      changing first letter of s to that of t,
         *      remove first letter of s, or
         *      remove first letter of t
         */
        int a = levenshteinDistanceRecursive(s1.substring(1), s2.substring(1));
        int b = levenshteinDistanceRecursive(s1, s2.substring(1));
        int c = levenshteinDistanceRecursive(s1.substring(1), s2);

        if(a > b) a = b;
        if(a > c) a = c;

        //any of which is 1 edit plus editing the rest of the strings
        return a + 1;
    }

}
