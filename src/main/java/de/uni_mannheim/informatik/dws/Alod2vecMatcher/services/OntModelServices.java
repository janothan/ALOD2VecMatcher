package de.uni_mannheim.informatik.dws.Alod2vecMatcher.services;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;

/**
 * This class offers static services for jena ont models.
 */
public class OntModelServices {

    /**
     * Get the ontology ID in order to store ontology-related info.
     * @param model Model for which ID shall be retrieved.
     * @return ID as string.
     */
    public static String getOntId(OntModel model){
        try {
            QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(("SELECT ?ont WHERE { ?ont <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Ontology> . }")), model);
            ResultSet result = queryExecution.execSelect();
            if (result.hasNext()) {
                QuerySolution solution = result.next();
                Resource resource = solution.getResource("ont");
                return resource.getURI();
            } else return null;
        } catch (Exception e){
            return null;
        }
    }

}
