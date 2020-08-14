package de.uni_mannheim.informaik.dws.Alod2vecMatcher.matchingComponents.KGvec2goVectors;

import de.uni_mannheim.informaik.dws.Alod2vecMatcher.LabelBasedMatcher;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import org.apache.jena.ontology.OntModel;

import java.util.Properties;

public class VectorCosineMatcher extends LabelBasedMatcher {

    @Override
    public Alignment match(OntModel sourceOntology, OntModel targetOntology, Alignment inputAlignment, Properties properties) throws Exception {
        ontology1 = sourceOntology;
        ontology2 = targetOntology;
        alignment = inputAlignment;
        loadLabels(sourceOntology, targetOntology);

        // idea: get BOW on unmatched and apply best match...

        return null;
    }

}
