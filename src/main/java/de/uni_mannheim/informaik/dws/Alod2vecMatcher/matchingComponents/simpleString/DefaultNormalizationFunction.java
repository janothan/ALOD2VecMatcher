package de.uni_mannheim.informaik.dws.Alod2vecMatcher.matchingComponents.simpleString;

import java.util.function.Function;

public class DefaultNormalizationFunction implements Function<String, String> {

    /**
     * A simple label normalization function
     *
     * @param stringToBeNormalized The label to be normalized.
     * @return Normalized label.
     */
    @Override
    public String apply(String stringToBeNormalized) {
        if (stringToBeNormalized == null) return null;
        String original = stringToBeNormalized;

        // handle language annotations by deleting them
        if(stringToBeNormalized.length() > 3 && stringToBeNormalized.charAt(stringToBeNormalized.length()-3) == '@'){
            stringToBeNormalized = stringToBeNormalized.substring(0, stringToBeNormalized.length()-3);
        }

        stringToBeNormalized = stringToBeNormalized.replaceAll("\\([^\\)]*\\)", ""); // \([^\)]*\)   → removes everything in round brackets
        stringToBeNormalized = stringToBeNormalized.replaceAll("\\[[^\\]]*\\]", ""); // \[[^\]]*\]   → removes everything in squared brackets
        stringToBeNormalized = stringToBeNormalized.replaceAll("(?<!^)(?<!\\s)(?=[A-Z][a-z])", "_"); // convert camelCase to under_score_case

        stringToBeNormalized = stringToBeNormalized.replace("-", "_");
        stringToBeNormalized = stringToBeNormalized.replace(" ", "_");

        // treat line breaks as "normal" separator
        stringToBeNormalized = stringToBeNormalized.replace("\n", "_");

        // replace multi-underscores with one underscore
        stringToBeNormalized = stringToBeNormalized.replaceAll("_{2,}", "_");

        // remove trailing and leading underscores (note that spaces have also been transformed into _ by now)
        if(stringToBeNormalized.startsWith("_")){
            stringToBeNormalized = stringToBeNormalized.substring(1);
        }
        if(stringToBeNormalized.endsWith("_")){
            stringToBeNormalized = stringToBeNormalized.substring(0, stringToBeNormalized.length() - 1);
        }

        // lowercase
        stringToBeNormalized = stringToBeNormalized.toLowerCase();

        if(stringToBeNormalized.length() < 3){
            // avoid matches to labels such as 'a' or '18' or 's'.
            return original;
        }

        return stringToBeNormalized;
    }
}
