package de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.complexString;

import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.simpleString.SimpleTransformationIndexer;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.LabelLanguageTuple;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.UriLabelInfo;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.services.StringOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Uses bag of words methodology.
 */
public class ComplexIndexer {

    /**
     * Constructor.
     * @param simpleTransformationIndexer Simple Indexer that can be used to quickly derive an updated index.
     */
    public ComplexIndexer(SimpleTransformationIndexer simpleTransformationIndexer){
        this.uriLabelMap = simpleTransformationIndexer.uriLabelMap;
        runIndexingHighPerformance(simpleTransformationIndexer.index);
    }

    /**
     * Constructor
     */
    public ComplexIndexer(UriLabelInfo uriLabelMap){
        this.uriLabelMap = uriLabelMap;
        runIndexingClassic();
    }

    /**
     * Data structure for which the index shall be built.
     */
    public UriLabelInfo uriLabelMap;


    /**
     * The index.
     */
    public HashMap<BagOfWords, ArrayList<String>> index = new HashMap<>();


    /**
     * Clashed labels while building the index.
     * Key: URI in index
     * Value: Other URI candidates not in the index.
     */
    public HashMap<String, HashSet<String>> clashes = new HashMap<>();


    /**
     * Create the index based on the old index.
     * @param oldIndex The old index (format: String -&gt; list&lt;uri&gt;) on which the new index will be built.
     */
    private void runIndexingHighPerformance(HashMap<String, ArrayList<String>> oldIndex){
        for(HashMap.Entry<String, ArrayList<String>> oldIndexEntry : oldIndex.entrySet()){
            BagOfWords bow = normalizeUsingOldIndex(oldIndexEntry.getKey());
            if(index.containsKey(bow)){
                index.get(bow).addAll(oldIndexEntry.getValue());
            } else {
                index.put(bow, oldIndexEntry.getValue());
            }
        }
    }

    /**
     * Perform the indexing function.
     */
    private void runIndexingClassic() {
        for (HashMap.Entry<String, HashSet<LabelLanguageTuple>> entry1 : uriLabelMap.getUriLabelMap().entrySet()) {
            nextLabelOfUri: for (LabelLanguageTuple tuple : entry1.getValue()) {
                BagOfWords bow = normalize(tuple.label);

                // check for label clashes
                ArrayList<String> indexEntry = index.get(bow);
                if(indexEntry != null){
                    indexEntry.add(entry1.getKey());
                } else {
                    ArrayList<String> list = new ArrayList<>();
                    list.add(entry1.getKey());
                    index.put(bow, list);
                }
            }
        }
    }


    /**
     * A label to BOW normalization function based on a previous index.
     * This approach saves computation time.
     * @param normalizedLabel The previously normalized label.
     * @return BOW instance.
     */
    public static BagOfWords normalizeUsingOldIndex(String normalizedLabel){
        // delete non alpha-numeric characters:
        normalizedLabel = normalizedLabel.replaceAll("[^a-zA-Z\\d\\s:_]", "_"); // regex: [^a-zA-Z\d\s:]

        String[] tokenized = normalizedLabel.split("_");
        tokenized = StringOperations.removeFreeFloatingGenitiveS(tokenized);
        String[] tokenizedNoStopwords = StringOperations.clearArrayFromStopwords(tokenized);

        if (tokenizedNoStopwords == null || tokenizedNoStopwords.length == 0) {
            // token is made up of stopwords
            // return stopword string rather than nothing
            return new BagOfWords(tokenized);
        }
        return new BagOfWords(tokenizedNoStopwords);
    }

    /**
     * A label to BOW normalization function.
     *
     * @param stringToBeNormalized The label to be normalized.
     * @return Normalized label.
     */
    public static BagOfWords normalize(String stringToBeNormalized) {
        if (stringToBeNormalized == null) return null;
        if(stringToBeNormalized.length() > 3 && stringToBeNormalized.charAt(stringToBeNormalized.length()-3) == '@'){
            stringToBeNormalized = stringToBeNormalized.substring(0, stringToBeNormalized.length()-3);
        }
        stringToBeNormalized = stringToBeNormalized.replaceAll("(?<!^)(?<!\\s)(?=[A-Z][a-z])", "_"); // convert camelCase to under_score_case
        stringToBeNormalized = stringToBeNormalized.replace(" ", "_");
        stringToBeNormalized = stringToBeNormalized.replace("-", "_");
        stringToBeNormalized = stringToBeNormalized.toLowerCase();

        // delete non alpha-numeric characters:
        stringToBeNormalized = stringToBeNormalized.replaceAll("[^a-zA-Z\\d\\s:_]", "_"); // regex: [^a-zA-Z\d\s:]

        String[] tokenized = stringToBeNormalized.split("_");
        tokenized = StringOperations.removeFreeFloatingGenitiveS(tokenized);
        String[] tokenizedNoStopwords = StringOperations.clearArrayFromStopwords(tokenized);

        if (tokenizedNoStopwords == null || tokenizedNoStopwords.length == 0) {
            // token is made up of stopwords
            // return stopword string rather than nothing
            return new BagOfWords(tokenized);
        }
        return new BagOfWords(tokenizedNoStopwords);
    }

}
