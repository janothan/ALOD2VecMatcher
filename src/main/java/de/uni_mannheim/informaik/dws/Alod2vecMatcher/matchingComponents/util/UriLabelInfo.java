package de.uni_mannheim.informaik.dws.Alod2vecMatcher.matchingComponents.util;


import java.util.HashMap;
import java.util.HashSet;


/**
 * Structure to hold URI->label info as well as language distribution.
 */
public class UriLabelInfo {

    /**
     * The distribution of languages (map: language -> total frequency).
     */
    private HashMap<Language, Integer> languageDistribution;

    /**
     * A map from URI -> set of tuples
     */
    private HashMap<String, HashSet<LabelLanguageTuple>> uriLabelMap;


    /**
     * Constructor
     */
    public UriLabelInfo(){
        languageDistribution = new HashMap<>();
        uriLabelMap = new HashMap<>();
    }

    /**
     * Add {@Code LabelLanguageTuples}. The language distribution is calculated on the fly.
     * @param uri The URI to which the labels belong.
     * @param labels A set of labels together with the language.
     */
    public void add(String uri, HashSet<LabelLanguageTuple> labels){
        uriLabelMap.put(uri, labels);

        // language distribution
        for(LabelLanguageTuple tuple : labels){
            if(languageDistribution.containsKey(tuple.language)){
                languageDistribution.put(tuple.language, languageDistribution.get(tuple.language) + 1);
            } else {
                languageDistribution.put(tuple.language, 1);
            }
        }
    }

    public HashMap<Language, Integer> getLanguageDistribution() {
        return languageDistribution;
    }

    public HashMap<String, HashSet<LabelLanguageTuple>> getUriLabelMap() {
        return uriLabelMap;
    }

    /**
     * This method produces a string representation of a language distribution.
     * @param distribution The distribution for which a String representation shall be obtained.
     * @return The string representation.
     */
    public static String transformLanguageDistributionToString(HashMap<Language, Integer> distribution){
        String result = "";
        for(HashMap.Entry<Language, Integer> entry : distribution.entrySet()){
            result = result + entry.getKey().toWiktionaryChar3() + ": " + entry.getValue() + "  \n";
        }
        return result.substring(0, result.length() -2);
    }

    /**
     * Merge multiple distributions, i.e., sum their frequencies for corresponding languages.
     * @param distributions A set of distributions.
     * @return A new distribution. The original distributions stay untouched.
     */
    public static HashMap<Language, Integer> mergeDistributions(HashMap<Language, Integer>... distributions){
        if(distributions == null) return null;
        HashMap<Language, Integer> result = new HashMap<>();
        boolean firstPass = true;
        for(HashMap<Language, Integer> individualDistribution : distributions){
            if(individualDistribution == null){
                continue;
            }
            if(firstPass) {
                result.putAll(individualDistribution);
                firstPass = false;
                continue;
            }
            for(HashMap.Entry<Language, Integer> entry : individualDistribution.entrySet()){
                if(result.containsKey(entry.getKey())){
                    result.put(entry.getKey(), result.get(entry.getKey()) + entry.getValue());
                } else {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }


    /**
     * Given a language distribution, this method determines the most used language.
     * Note that UNKNOWN languages are ignored. If the distribution consists only of UNKNOWN, null will be returned.
     * @param distribution Distribution of which the most used language shall be obtained.
     * @return The most frequent language
     */
    public static Language getMostUsedLanguage(HashMap<Language, Integer> distribution){
        if(distribution == null) return null;
        int highestValue = 0;
        Language mostFrequentLanguage = null;
        for(HashMap.Entry<Language, Integer> entry : distribution.entrySet()){
            if(entry.getKey() == Language.UNKNOWN) continue;
            if(entry.getValue() > highestValue){
                highestValue = entry.getValue();
                mostFrequentLanguage = entry.getKey();
            }
        }
        return mostFrequentLanguage;
    }

}
