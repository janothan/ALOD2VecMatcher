package de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.simpleString;


import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.LabelLanguageTuple;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.UriLabelInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

/**
 * Creates a simple index of form String -> URI.
 */
public class SimpleTransformationIndexer {

    /**
     * Data structure for which the index shall be built.
     */
    public UriLabelInfo uriLabelMap;

    /**
     * Very small strings such as 's', 'a', '10' can lead to many false positive matches.
     * Usually, it is not very meaningful to index those.
     * Therefore, this variable sets the minimal length for an index member.
     */
    public int minimalCharacterLengthForIndex = 3;


    /**
     * The transformation function to be applied.
     */
    private Function<String,String> transformationFunction;


    /**
     * Constructor.
     * @param uriLabelMap Data structure for which the index shall be built.
     */
    public SimpleTransformationIndexer(UriLabelInfo uriLabelMap){
        this(uriLabelMap, new DefaultNormalizationFunction());
    }

    /**
     * Constructor.
     * @param uriLabelMap Data structure for which the index shall be built.
     * @param transformationFunction The transformation function to be applied.
     */
    public SimpleTransformationIndexer(UriLabelInfo uriLabelMap, Function<String, String> transformationFunction){
        this.transformationFunction = transformationFunction;
        this.uriLabelMap = uriLabelMap;
        if(uriLabelMap != null) {
            runIndexing();
        }
    }

    /**
     * Constructor.
     * @param indexBasis An existing index will be used. The index will not be changed. Instead a new index is created based on this index.
     * @param transformationFunction The transformation function to be applied to the existing keys.
     */
    public SimpleTransformationIndexer(HashMap<String, ArrayList<String>> indexBasis, Function<String, String> transformationFunction){
        this.transformationFunction = transformationFunction;
        this.index = new HashMap<>();
        for(Map.Entry<String, ArrayList<String>> entry : indexBasis.entrySet()){
            String key = transformationFunction.apply(entry.getKey());

            // check for label clashes
            ArrayList<String> thisIndexEntry = this.index.get(key);
            if(thisIndexEntry != null){
                thisIndexEntry.addAll(entry.getValue());
                //this.index.put(key, thisIndexEntry);
            } else {
                this.index.put(key, entry.getValue());
            }
        }
    }

    /**
     * The index.
     * label -> URIs
     * It is intended that the value is a list rather than a set so that one URI might appear in the set multiple times.
     */
    public HashMap<String, ArrayList<String>> index = new HashMap<>();

    /**
     * Perform the indexing function.
     */
    private void runIndexing() {
        for (HashMap.Entry<String, HashSet<LabelLanguageTuple>> entry1 : uriLabelMap.getUriLabelMap().entrySet()) {
            nextTuple:
            for (LabelLanguageTuple tuple : entry1.getValue()) {
                if(tuple.label.length() < minimalCharacterLengthForIndex) continue nextTuple;
                String normalizedLabel = transformationFunction.apply(tuple.label);

                // check for label clashes
                ArrayList<String> indexEntry = index.get(normalizedLabel);
                if(indexEntry != null){
                  indexEntry.add(entry1.getKey());
                } else {
                    ArrayList<String> list = new ArrayList<>();
                    list.add(entry1.getKey());
                    index.put(normalizedLabel, list);
                }
            }
        }
    }

}
