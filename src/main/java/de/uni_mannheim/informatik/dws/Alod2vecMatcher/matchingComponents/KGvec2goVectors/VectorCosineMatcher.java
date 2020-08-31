package de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.KGvec2goVectors;

import de.uni_mannheim.informatik.dws.Alod2vecMatcher.LabelBasedMatcher;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.complexString.BagOfWords;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.complexString.ComplexIndexer;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.UriLabelInfo;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.services.OntModelServices;
import de.uni_mannheim.informatik.dws.melt.matching_jena_matchers.filter.extraction.MaxWeightBipartiteExtractor;
import de.uni_mannheim.informatik.dws.melt.matching_ml.kgvec2go.KGvec2goClient;
import de.uni_mannheim.informatik.dws.melt.matching_ml.kgvec2go.KGvec2goDatasets;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.*;
import org.apache.jena.ontology.OntModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class VectorCosineMatcher extends LabelBasedMatcher {

    /**
     * Default Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCosineMatcher.class);

    /**
     * KGvec2go
     */
    private KGvec2goClient kGvec2goClient = KGvec2goClient.getInstance();

    /**
     * If the cosine similarity is greater than the specified threshold, a match will be added.
     */
    private double threshold = 0.75;

    /**
     * Dataset to be used.
     */
    private KGvec2goDatasets dataset = KGvec2goDatasets.ALOD;

    private static final String EXPLANATION_TEXT_COSINE_MATCH = "The following two label sets have a cosine above the given threshold: ";

    /**
     * Intermediate alignment that will be optimized.
     */
    private Alignment intermediateAlignment = new Alignment();

    @Override
    public Alignment match(OntModel sourceOntology, OntModel targetOntology, Alignment inputAlignment, Properties properties) throws Exception {

        if(kGvec2goClient.isServiceAvailable()){
            LOGGER.info("The KGvec2go server could be reached. Running vector matcher...");
        } else {
            LOGGER.error("THE KGvec2go server could NOT be reached. Please make sure that the server (kgvec2go.org) is online and that you have a working Internet connection.\n" +
                    "The vector matcher module will not be used!");
            return inputAlignment;
        }

        ontology1 = sourceOntology;
        ontology2 = targetOntology;
        alignment = inputAlignment;
        loadLabels(sourceOntology, targetOntology);

        // typical type restructuring
        loadLabels(sourceOntology, targetOntology);

        // run mapping strict string matching
        simpleVectorMatch(uri2labelMapClasses_1, uri2labelMapClasses_2, "classes");
        simpleVectorMatch(uri2labelMapDatatypeProperties_1, uri2labelMapDatatypeProperties_2, "datatypeProperties");
        simpleVectorMatch(uri2labelMapObjectProperties_1, uri2labelMapObjectProperties_2, "objectProperties");
        simpleVectorMatch(uri2labelMapRemainingProperties_1, uri2labelMapRemainingProperties_2, "remainingProperties");

        // pick best matches
        MaxWeightBipartiteExtractor mwb = new MaxWeightBipartiteExtractor();
        Alignment filteredAlignment = mwb.match(sourceOntology, targetOntology, intermediateAlignment, properties);

        // add new matches
        alignment.addAll(filteredAlignment);

        return alignment;
    }


    /**
     * A direct matching approach without stop word removal or other advanced approaches.
     *
     * @param uriLabelMap_1 Map 1 (source).
     * @param uriLabelMap_2 Map 2 (target).
     * @param what          What is matched.
     */
    private void simpleVectorMatch(UriLabelInfo uriLabelMap_1, UriLabelInfo uriLabelMap_2, String what) {

        // step 1: get BOW

        // global data store key generation
        String ont_1_key = OntModelServices.getOntId(ontology1);
        String ont_2_key = OntModelServices.getOntId(ontology2);

        // required data structures
        HashMap<BagOfWords, ArrayList<String>> bowIndex_1;
        HashMap<BagOfWords, ArrayList<String>> bowIndex_2;

        if (ont_1_key != null && ont_2_key != null) {
            String complexIndexKey_1 = "complexIndexer_" + ont_1_key + "_" + what + "_1";
            String complexIndexKey_2 = "complexIndexer_" + ont_2_key + "_" + what + "_2";

            if (store.containsKey(complexIndexKey_1) && store.containsKey(complexIndexKey_2)) {
                bowIndex_1 = store.get(complexIndexKey_1);
                bowIndex_2 = store.get(complexIndexKey_2);
            } else {
                // standard initialization
                LOGGER.warn("Could not find complex indexer in global data store. One of the following keys failed:\n" +
                        complexIndexKey_1 + "\n" + complexIndexKey_2);
                ComplexIndexer ci_1 = new ComplexIndexer(uriLabelMap_1);
                ComplexIndexer ci_2 = new ComplexIndexer(uriLabelMap_2);
                bowIndex_1 = ci_1.index;
                bowIndex_2 = ci_2.index;
            }
        } else {
            // standard initialization
            ComplexIndexer ci_1 = new ComplexIndexer(uriLabelMap_1);
            ComplexIndexer ci_2 = new ComplexIndexer(uriLabelMap_2);
            bowIndex_1 = ci_1.index;
            bowIndex_2 = ci_2.index;
        }

        // step 2: remove what has already been matched
        LOGGER.info("Index 1 size before alignment trimming: " + bowIndex_1.size());
        LOGGER.info("Index 2 size before alignment trimming: " + bowIndex_2.size());
        bowIndex_1 = trimIndexUsingAlignment(bowIndex_1);
        bowIndex_2 = trimIndexUsingAlignment(bowIndex_2);
        LOGGER.info("Index 1 size after alignment trimming: " + bowIndex_1.size());
        LOGGER.info("Index 2 size after alignment trimming: " + bowIndex_2.size());

        // step 3: remove what cannot be linked to a KGvec2go concept
        LOGGER.info("Index 1 size before BK link trimming: " + bowIndex_1.size());
        LOGGER.info("Index 2 size before BK link trimming: " + bowIndex_2.size());
        bowIndex_1 = trimIndexUsingBKsource(bowIndex_1);
        bowIndex_2 = trimIndexUsingBKsource(bowIndex_2);
        LOGGER.info("Index 1 size after BK link trimming: " + bowIndex_1.size());
        LOGGER.info("Index 2 size after BK link trimming: " + bowIndex_2.size());

        // step 4: apply actual matching
        for (Map.Entry<BagOfWords, ArrayList<String>> entry_1 : bowIndex_1.entrySet()) {
            nextEntry_2:
            for (Map.Entry<BagOfWords, ArrayList<String>> entry_2 : bowIndex_2.entrySet()) {
                BagOfWords tokens_1 = entry_1.getKey();
                BagOfWords tokens_2 = entry_2.getKey();
                if (tokens_1.size() != tokens_2.size()) {
                    // not supported
                    continue nextEntry_2;
                }
                double score = 0.0;
                int comparisons = tokens_1.size(); // tokens_2.size() would work equally well

                for (String t1 : tokens_1) {
                    double bestTokenScore = 0.0;
                    for (String t2 : tokens_2) {
                        double tokenSimilarity = kGvec2goClient.getSimilarity(t1, t2, dataset);
                        if (tokenSimilarity > bestTokenScore) {
                            bestTokenScore = tokenSimilarity;
                        }
                    }
                    score += bestTokenScore;
                } // end of token comparison

                score = score / comparisons;

                if(score > threshold){
                    // add to intermediate alignment
                    for(String uri1 : entry_1.getValue()){
                        for (String uri2 : entry_2.getValue()){
                            addMappingToIntermediateAlignment(uri1, uri2, entry_1.getKey(), entry_2.getKey(), score);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes entries from the index that cannot be linked to KGvec2go.
     *
     * @param index The index to be trimmed.
     * @return Trimmed Index.
     */
    private HashMap<BagOfWords, ArrayList<String>> trimIndexUsingBKsource(HashMap<BagOfWords, ArrayList<String>> index) {
        HashMap<BagOfWords, ArrayList<String>> result = new HashMap<>();

        nextIndexEntry:
        for (Map.Entry<BagOfWords, ArrayList<String>> entry : index.entrySet()) {

            for (String token : entry.getKey()) {
                if (kGvec2goClient.getVector(token, dataset) == null) {
                    continue nextIndexEntry;
                }
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }


    /**
     * Adds a mapping to the alignment.
     * If the mapping is already there, the confidence is increased.
     *
     * @param uri_1 URI 1.
     * @param uri_2 URI 2.
     */
    private void addMappingToIntermediateAlignment(String uri_1, String uri_2, BagOfWords bowThatLeadsToMatch1, BagOfWords bowThatLeadsToMatch2, double score) {
            Map<String, Object> extensions = new HashMap<>();
            extensions.put(DefaultExtensions.DublinCore.DESCRIPTION.toString(), EXPLANATION_TEXT_COSINE_MATCH + bowThatLeadsToMatch1 + " and " + bowThatLeadsToMatch2);
            Correspondence newCorrespondence = new Correspondence(uri_1, uri_2, score, CorrespondenceRelation.EQUIVALENCE, extensions);
            intermediateAlignment.add(newCorrespondence);
            LOGGER.info("New correspondence: " + newCorrespondence);
    }


    /**
     * Removes entries from the Index that have already been matched.
     *
     * @return Trimmed Index.
     */
    private HashMap<BagOfWords, ArrayList<String>> trimIndexUsingAlignment(HashMap<BagOfWords, ArrayList<String>> index) {
        HashMap<BagOfWords, ArrayList<String>> result = new HashMap<>();
        for (Map.Entry<BagOfWords, ArrayList<String>> entry : index.entrySet()) {
            ArrayList<String> uriList = new ArrayList<>();
            for (String uri : entry.getValue()) {
                if (alignment.isSourceContained(uri) || alignment.isTargetContained(uri)) {
                    // do nothing
                } else {
                    uriList.add(uri);
                }
            }
            if (uriList.size() > 0) {
                result.put(entry.getKey(), uriList);
            }
        }
        return result;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public KGvec2goDatasets getDataset() {
        return dataset;
    }

    public void setDataset(KGvec2goDatasets dataset) {
        this.dataset = dataset;
    }
}
