package de.uni_mannheim.informaik.dws.Alod2vecMatcher.matchingComponents.simpleString;

import java.util.function.Function;

/**
 * This function will only trim strings.
 */
public class TrimNormalizationFunction implements Function<String, String> {
    @Override
    public String apply(String s) {
        return s.trim();
    }
}
