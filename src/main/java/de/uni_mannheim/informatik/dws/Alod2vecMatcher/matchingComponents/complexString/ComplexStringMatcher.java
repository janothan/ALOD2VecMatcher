package de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.complexString;

import de.uni_mannheim.informatik.dws.Alod2vecMatcher.LabelBasedMatcher;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.simpleString.SimpleTransformationIndexer;
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

/**
 * Creates a homogeneous n-n alignment.
 */
public class ComplexStringMatcher extends LabelBasedMatcher {

    /**
     * Logger
     */
    private static Logger LOGGER = LoggerFactory.getLogger(ComplexStringMatcher.class);


    @Override
    public Alignment match(OntModel sourceOntology, OntModel targetOntology, Alignment m, Properties p) {
        ontology1 = sourceOntology;
        ontology2 = targetOntology;
        alignment = m;
        loadLabels(sourceOntology, targetOntology);

        // run mapping strict string matching
        complexMatch(uri2labelMapClasses_1, uri2labelMapClasses_2, "classes");
        complexMatch(uri2labelMapDatatypeProperties_1, uri2labelMapDatatypeProperties_2, "datatypeProperties");
        complexMatch(uri2labelMapObjectProperties_1, uri2labelMapObjectProperties_2, "objectProperties");
        complexMatch(uri2labelMapRemainingProperties_1, uri2labelMapRemainingProperties_2, "remainingProperties");

        LOGGER.info("ComplexStringMatcher Completed");
        return alignment;
    }


    /**
     * A direct matching approach without stopword removal or other advanced approaches.
     *
     * @param uriLabelMap_1 Map 1 (source).
     * @param uriLabelMap_2 Map 2 (target).
     * @param what What is matched.
     */
    private void complexMatch(UriLabelInfo uriLabelMap_1, UriLabelInfo uriLabelMap_2, String what) {

        // global data store key generation
        String ont_1_key = OntModelServices.getOntId(ontology1);
        String ont_2_key = OntModelServices.getOntId(ontology2);

        ComplexIndexer complexIndexer_1;
        if(ont_1_key != null && store.containsKey("simpleIndex_" + ont_1_key + "_" + what + "_1")){
            complexIndexer_1 = new ComplexIndexer((SimpleTransformationIndexer) store.get("simpleIndex_" + ont_1_key + "_" + what + "_1"));
        } else {
            complexIndexer_1 = new ComplexIndexer(uriLabelMap_1);
        }

        ComplexIndexer complexIndexer_2;
        if(ont_2_key != null && store.containsKey("simpleIndex_" + ont_2_key + "_" + what + "_2")){
            complexIndexer_2 = new ComplexIndexer((SimpleTransformationIndexer) store.get("simpleIndex_" + ont_2_key + "_" + what + "_2"));
        } else {
            complexIndexer_2 = new ComplexIndexer(uriLabelMap_2);
        }

        HashMap<BagOfWords, ArrayList<String>> simpleIndex_1 = complexIndexer_1.index;
        HashMap<BagOfWords, ArrayList<String>> simpleIndex_2 = complexIndexer_2.index;

        // put to store
        if(ont_1_key != null && ont_2_key != null) {
            String complexIndexKey_1 = "complexIndexer_" + ont_1_key + "_" + what + "_1";
            store.put(complexIndexKey_1, simpleIndex_1);
            String complexIndexKey_2 = "complexIndexer_" + ont_2_key + "_" + what + "_2";
            store.put(complexIndexKey_2, simpleIndex_2);
        }

        // apply actual matching operation
        for (BagOfWords bow : simpleIndex_1.keySet()) {
            if (simpleIndex_2.containsKey(bow)) {
                for(String sourceUri : simpleIndex_1.get(bow)){
                    for(String targetUri : simpleIndex_2.get(bow)){
                        addMapping(sourceUri, targetUri, bow);
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
     * @param bowThatLeadsToMatch Label that leads to a match
     */
    private void addMapping(String uri_1, String uri_2, BagOfWords bowThatLeadsToMatch) {
        Correspondence existingCorrespondence = alignment.getCorrespondence(uri_1, uri_2, CorrespondenceRelation.EQUIVALENCE);
        if (existingCorrespondence != null) {
            existingCorrespondence.setConfidence(existingCorrespondence.getConfidence() + 1.0);
            String extendedExplanation = existingCorrespondence.getExtensions().get(DefaultExtensions.DublinCore.DESCRIPTION.toString()) + ", " + bowThatLeadsToMatch;
            existingCorrespondence.getExtensions().put(DefaultExtensions.DublinCore.DESCRIPTION.toString(), extendedExplanation);
            LOGGER.info("Updated correspondence: " + existingCorrespondence);
        } else {
            Map<String, Object> extensions = new HashMap<>();
            extensions.put(DefaultExtensions.DublinCore.DESCRIPTION.toString(), EXPLANATION_TEXT_DIRECT_MATCH + bowThatLeadsToMatch);
            Correspondence newCorrespondence = new Correspondence(uri_1, uri_2, 1.0, CorrespondenceRelation.EQUIVALENCE, extensions);
            alignment.add(newCorrespondence);
            LOGGER.info("New correspondence: " + newCorrespondence);
        }
    }


    /**
     * Explanation text.
     */
    private final static String EXPLANATION_TEXT_DIRECT_MATCH = "The two resources have the same normalized Bag of Words (BOW). " +
            "A match was found on the following (normalized) bag of words: ";

}
