package de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.simpleString;

import de.uni_mannheim.informatik.dws.Alod2vecMatcher.LabelBasedMatcher;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.UriLabelInfo;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.services.OntModelServices;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.CorrespondenceRelation;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.DefaultExtensions;
import org.apache.jena.ontology.OntModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 * Creates a homogeneous n-n alignment.
 */
public class SimpleStringMatcher extends LabelBasedMatcher {

    /**
     * Logger
     */
    private static Logger LOGGER = LoggerFactory.getLogger(SimpleStringMatcher.class);

    /**
     * Transformation to be used with Strings.
     */
    private Function<String, String> transformationFunction = new DefaultNormalizationFunction();

    /**
     * Default Constructor
     */
    public SimpleStringMatcher(){
    }

    /**
     * Constructor
     * @param transformationFunction To be used by indexer.
     */
    public SimpleStringMatcher(Function<String, String> transformationFunction){
        this.transformationFunction = transformationFunction;
    }

    @Override
    public Alignment match(OntModel sourceOntology, OntModel targetOntology, Alignment m, Properties p) {
        ontology1 = sourceOntology;
        ontology2 = targetOntology;
        alignment = m;
        loadLabels(sourceOntology, targetOntology);

        // run mapping strict string matching
        directMatch(uri2labelMapClasses_1, uri2labelMapClasses_2, "classes");
        directMatch(uri2labelMapDatatypeProperties_1, uri2labelMapDatatypeProperties_2, "datatypeProperties");
        directMatch(uri2labelMapObjectProperties_1, uri2labelMapObjectProperties_2, "objectProperties");
        directMatch(uri2labelMapRemainingProperties_1, uri2labelMapRemainingProperties_2, "remainingProperties");

        LOGGER.info("SimpleStringMatcher Completed");
        return alignment;
    }


    /**
     * A direct matching approach without stopword removal or other advanced approaches.
     * Note: If a Simple Indexer is found in the store it is automatically used as basis for a followup indexing operations.
     * This implies that SimpleStringMatchers have to be run from least to most aggressive in terms of their transformation function.
     *
     * @param uriLabelMap_1 Map 1 (source).
     * @param uriLabelMap_2 Map 2 (target).
     * @param what What is matched (identifying String for the global data store).
     */
    private void directMatch(UriLabelInfo uriLabelMap_1, UriLabelInfo uriLabelMap_2, String what) {
        // index initialization
        SimpleTransformationIndexer indexer_1, indexer_2;

        // global data store key generation
        String ont_1_key = OntModelServices.getOntId(ontology1);
        String ont_2_key = OntModelServices.getOntId(ontology2);

        // check for key validity (global store can only be used if unique ont ids exist
        if(ont_1_key == null || ont_2_key == null){
            // do not use the global store
            indexer_1 = new SimpleTransformationIndexer(uriLabelMap_1, transformationFunction);
            indexer_2 = new SimpleTransformationIndexer(uriLabelMap_2, transformationFunction);
        } else {
            String indexer_1_key = "simpleIndex_" + ont_1_key + "_" + what + "_1";
            String indexer_2_key = "simpleIndex_" + ont_2_key + "_" + what + "_2";
            if(store.containsKey(indexer_1_key)){
                indexer_1 = new SimpleTransformationIndexer(((SimpleTransformationIndexer) store.get(indexer_1_key)).index, transformationFunction);
            } else {
                indexer_1 = new SimpleTransformationIndexer(uriLabelMap_1, transformationFunction);
            }
            if(store.containsKey(indexer_2_key)){
                indexer_2 = new SimpleTransformationIndexer(((SimpleTransformationIndexer) store.get(indexer_2_key)).index, transformationFunction);
            } else {
                indexer_2 = new SimpleTransformationIndexer(uriLabelMap_2, transformationFunction);
            }
            // adding to the store
            store.put(indexer_1_key, indexer_1);
            store.put(indexer_2_key, indexer_2);
        }

        HashMap<String, ArrayList<String>> simpleIndex_1 = indexer_1.index;
        HashMap<String, ArrayList<String>> simpleIndex_2 = indexer_2.index;

        for (String s1 : simpleIndex_1.keySet()) {
            if (simpleIndex_2.containsKey(s1)) {
                for(String sourceUri : simpleIndex_1.get(s1)){
                    for(String targetUri : simpleIndex_2.get(s1)){
                        addMapping(sourceUri, targetUri, s1);
                    }
                }
            }
        }
    }


    /**
     * Adds a mapping to the alignment.
     * If the mapping is already there, the confidence is increased.
     *
     * @param uri_1 URI 1.
     * @param uri_2 URI 2.
     * @param labelThatLeadsToMatch Label that leads to a match
     */
    private void addMapping(String uri_1, String uri_2, String labelThatLeadsToMatch) {

        Correspondence existingCorrespondence = alignment.getCorrespondence(uri_1, uri_2, CorrespondenceRelation.EQUIVALENCE);
        if (existingCorrespondence != null) {
            existingCorrespondence.setConfidence(existingCorrespondence.getConfidence() + 1.0);
            String extendedExplanation = existingCorrespondence.getExtensions().get(DefaultExtensions.DublinCore.DESCRIPTION.toString()) + ", " + labelThatLeadsToMatch;
            existingCorrespondence.getExtensions().put(DefaultExtensions.DublinCore.DESCRIPTION.toString(), extendedExplanation);
            LOGGER.info("Updated correspondence: " + existingCorrespondence);
        } else {
            Map<String, Object> extensions = new HashMap<>();
            extensions.put(DefaultExtensions.DublinCore.DESCRIPTION.toString(), explanationTextDirectMatch + labelThatLeadsToMatch);
            Correspondence newCorrespondence = new Correspondence(uri_1, uri_2, 1.0, CorrespondenceRelation.EQUIVALENCE, extensions);
            alignment.add(newCorrespondence);
            LOGGER.info("New correspondence: " + newCorrespondence);
        }
    }


    /**
     * Explanation text.
     */
    private String explanationTextDirectMatch = "The two resources have the same label when normalized. This a strong indication of a match for this matcher. " +
            "A match was found on the following normalized label: ";

    public Function<String, String> getTransformationFunction() {
        return transformationFunction;
    }

    public void setTransformationFunction(Function<String, String> transformationFunction) {
        this.transformationFunction = transformationFunction;
    }

    public String getExplanationTextDirectMatch() {
        return explanationTextDirectMatch;
    }

    public void setExplanationTextDirectMatch(String explanationTextDirectMatch) {
        this.explanationTextDirectMatch = explanationTextDirectMatch;
    }
}
