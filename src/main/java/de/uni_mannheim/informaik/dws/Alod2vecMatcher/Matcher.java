package de.uni_mannheim.informaik.dws.Alod2vecMatcher;

import de.uni_mannheim.informaik.dws.Alod2vecMatcher.matchingComponents.complexString.ComplexStringMatcher;
import de.uni_mannheim.informaik.dws.Alod2vecMatcher.matchingComponents.simpleString.DefaultNormalizationFunction;
import de.uni_mannheim.informaik.dws.Alod2vecMatcher.matchingComponents.simpleString.SimpleStringMatcher;
import de.uni_mannheim.informaik.dws.Alod2vecMatcher.matchingComponents.simpleString.TrimNormalizationFunction;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Correspondence;
import org.apache.jena.ontology.OntModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Properties;

public class Matcher extends LabelBasedMatcher
{
    /**
     * Indicator whether the alignment shall be homogenous (i.e., only classes are mapped to classes, object properties
     * are mapped to object properties, and datatype properties are mapped to datatype properties).
     * If false, the alignment might be heterogeneous (e.g., classes might be mapped to datatype properties).
     */
    private boolean homogenousAlignment = true;

    /**
     * Default Logger
     */
    private static Logger LOGGER = LoggerFactory.getLogger(LabelBasedMatcher.class);

    /**
     * Constructor
     */
    public Matcher(){
        store.clear();
    }

    @Override
    public Alignment match(OntModel ontology1, OntModel ontology2, Alignment alignment, Properties properties) throws Exception {
        alignment = new Alignment();

        loadLabels(ontology1, ontology2);
        LOGGER.info("Detected Language for Source Ontology: " + mostFrequentLanguage_1);
        LOGGER.info("Detected Language for Target Ontology: " + mostFrequentLanguage_2);

        if(mostFrequentLanguage_1 != mostFrequentLanguage_2) {
            LOGGER.info("Different Languages detected - this matcher will not properly work.");
        }

            LOGGER.info("Both ontologies are in the same language - run matcher in monolingual mode.");

            // very simple matcher
            SimpleStringMatcher vsmatcher = new SimpleStringMatcher(new TrimNormalizationFunction());
            vsmatcher.setExplanationTextDirectMatch("The two resources have the same label. This the strongest indication of a match for this matcher. " +
                    "A match was found on the following label: ");
            Alignment vsAlignment = vsmatcher.match(ontology1, ontology2, alignment, properties);

            // more aggressive string matcher
            SimpleStringMatcher smatcher = new SimpleStringMatcher(new DefaultNormalizationFunction());
            Alignment sAlignment = smatcher.match(ontology1, ontology2, alignment, properties);

            // bag of word matcher
            ComplexStringMatcher cmatcher = new ComplexStringMatcher();
            Alignment cAlignment = cmatcher.match(ontology1, ontology2, alignment, properties);

            Alignment merged = mergeAlignments(vsAlignment, sAlignment);
            merged = mergeAlignments(merged, cAlignment);

            // levenshtein matcher
            LevenshteinMatcher lmatcher = new LevenshteinMatcher();
            Alignment lAlignment = lmatcher.match(ontology1, ontology2, alignment, properties);
            merged = mergeAlignments(merged, lAlignment);

            // vector matcher here

            // scale confidence values
            scaleConfidenceValues(merged);

            LOGGER.info("Ontologies aligned.");

            if(this.ontology1.listIndividuals().toSet().size() > 10 && this.ontology2.listIndividuals().toSet().size() > 10){
                LOGGER.info("Instances detected. Running Instance Matcher.");
                SimpleInstanceMatcher instanceMatcher = new SimpleInstanceMatcher();
                Alignment finalAlignment = instanceMatcher.match(ontology1, ontology2, merged, properties);
                return finalAlignment;
            }

            return merged;

    }


    /**
     * Scales the alignment confidence values using a min-max approach.
     * @param alignmentToScale The alignments that shall be scaled.
     * @return Returns the object on which the action is performed for convenience.
     */
    private Alignment scaleConfidenceValues(Alignment alignmentToScale){
        double maxConfidence = 0.0;
        for(Correspondence correspondence : alignmentToScale){
            if(correspondence.getConfidence() > maxConfidence) maxConfidence = correspondence.getConfidence();
        }
        if(maxConfidence > 0) {
            for (Correspondence correspondence : alignmentToScale) {
                correspondence.setConfidence(correspondence.getConfidence() / maxConfidence);
            }
        }
        return alignmentToScale;
    }



    /**
     * Merges the subdominant Alignment in the dominant alignment.
     * (Only the explanations from the dominant alignment will be kept.)
     * @param dominantAlignment Dominant (more important) alignment.
     * @param subdominantAlignment Subdomainant (less important) alignment.
     * @return Merged alignment.
     */
    private Alignment mergeAlignments(Alignment dominantAlignment, Alignment subdominantAlignment){
        Alignment result = new Alignment();
        result.addAll(dominantAlignment);

        for(Correspondence c : subdominantAlignment){
            if(result.contains(c)){
                continue;
            } else {
                result.add(c);
            }
        }
        return result;
    }


    /**
     * Get the size of an iterator.
     * @param iterator The iterator whose size shall be obtained.
     * @return The size of the iterator.
     */
    private static int getIteratorSize(Iterator iterator){
        int size = 0;
        while(iterator.hasNext()){
            iterator.next();
            size++;
        }
        return size;
    }

}
