package de.uni_mannheim.informatik.dws.Alod2vecMatcher;

import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.LabelLanguageTuple;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.Language;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.util.UriLabelInfo;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.services.OntModelServices;
import de.uni_mannheim.informatik.dws.Alod2vecMatcher.services.StringOperations;
import de.uni_mannheim.informatik.dws.melt.matching_base.DataStore;
import de.uni_mannheim.informatik.dws.melt.matching_jena.MatcherYAAAJena;
import de.uni_mannheim.informatik.dws.melt.yet_another_alignment_api.Alignment;
import eu.sealsproject.platform.res.domain.omt.IOntologyMatchingToolBridge;
import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class LabelBasedMatcher extends MatcherYAAAJena implements IOntologyMatchingToolBridge {

    /**
     * Logger
     */
    private static Logger LOGGER = LoggerFactory.getLogger(LabelBasedMatcher.class);

    /**
     * Ontologies
     */
    protected OntModel ontology1;
    protected OntModel ontology2;

    /**
     * Alignment
     */
    protected Alignment alignment = new Alignment();

    /**
     * Source class URIs mapped to multiple labels.
     */
    protected UriLabelInfo uri2labelMapClasses_1;

    /**
     * Target class URIs mapped to multiple labels.
     */
    protected UriLabelInfo uri2labelMapClasses_2;

    /**
     * Source datatype property URIs mapped to multiple labels.
     */
    protected UriLabelInfo uri2labelMapDatatypeProperties_1;

    /**
     * Target datatype property URIs mapped to multiple labels.
     */
    protected UriLabelInfo uri2labelMapDatatypeProperties_2;

    /**
     * Source object property URIs mapped to multiple labels.
     */
    protected UriLabelInfo uri2labelMapObjectProperties_1;

    /**
     * Target object property URIs mapped to multiple labels.
     */
    protected UriLabelInfo uri2labelMapObjectProperties_2;

    /**
     * Source object property URIs mapped to multiple labels.
     */
    protected UriLabelInfo uri2labelMapRemainingProperties_1;

    /**
     * Target object property URIs mapped to multiple labels.
     */
    protected UriLabelInfo uri2labelMapRemainingProperties_2;

    /**
     * Distribution of label languages of ontology 1.
     */
    protected HashMap<Language, Integer> languageDistribution_1;

    /**
     * Distribution of label languages of ontology 2.
     */
    protected HashMap<Language, Integer> languageDistribution_2;

    /**
     * Most frequent language in ontology 1.
     */
    protected Language mostFrequentLanguage_1;

    /**
     * Most frequent language in ontology 2.
     */
    protected Language mostFrequentLanguage_2;

    /**
     * Global data store.
     */
    protected static DataStore store = DataStore.getGlobal();

    /**
     * Indicates whether labels with 50% of numbers in their label shall be excluded.
     * Default: true
     */
    private boolean filterNonMeaningfulLabels = false;

    /**
     * This is required for the KG track.
     */
    private static final Property ANCHOR_TEXT_PROPERTY = ModelFactory.createDefaultModel().createProperty("http://dbkwik.webdatacommons.org/ontology/wikiPageWikiLinkText");

    /**
     * The usage of the extended fragment in {@link LabelBasedMatcher#getAnnotationProperties(OntResource, OntModel)}
     * can be dangerous, if all URIs end with the same fragment. This map controls whether this is the case and
     * stops those fragments from creating extensive matches.
     */
    private HashMap<String, Integer> extendedFragmentCounter = new HashMap<>();

    /**
     * The usage of the fragment in {@link LabelBasedMatcher#getAnnotationProperties(OntResource, OntModel)}
     * can be dangerous, if all URIs end with the same fragment. This map controls whether this is the case and
     * stops those fragments from creating extensive matches.
     */
    private HashMap<String, Integer> fragmentCounter = new HashMap<>();

    /**
     * Load the label structures.
     */
    public void loadLabels(OntModel sourceOntology, OntModel targetOntology) {
        ontology1 = sourceOntology;
        ontology2 = targetOntology;

        String ont_1_key = OntModelServices.getOntId(ontology1);
        String ont_2_key = OntModelServices.getOntId(ontology2);

        // retrieve all labels either from store or through parsing.
        if (ont_1_key != null) {
            String key = "uri2labelMapClasses_1_" + ont_1_key;
            if (store.containsKey(key)) {
                uri2labelMapClasses_1 = (UriLabelInfo) store.get(key);
            } else {
                uri2labelMapClasses_1 = getURIlabelMapAndCalculateLanguageDistribution(ontology1.listClasses(), ontology1);
                store.put(key, uri2labelMapClasses_1);
            }
        } else {
            uri2labelMapClasses_1 = getURIlabelMapAndCalculateLanguageDistribution(ontology1.listClasses(), ontology1);
        }


        if (ont_2_key != null) {
            String key = "uri2labelMapClasses_2_" + ont_2_key;
            if (store.containsKey(key)) {
                uri2labelMapClasses_2 = (UriLabelInfo) store.get(key);
            } else {
                uri2labelMapClasses_2 = getURIlabelMapAndCalculateLanguageDistribution(ontology2.listClasses(), ontology2);
                store.put(key, uri2labelMapClasses_2);
            }
        } else {
            uri2labelMapClasses_2 = getURIlabelMapAndCalculateLanguageDistribution(ontology2.listClasses(), ontology2);
        }


        if (ont_1_key != null) {
            String key = "uri2labelMapDatatypeProperties_1_" + ont_1_key;
            if (store.containsKey(key)) {
                uri2labelMapDatatypeProperties_1 = (UriLabelInfo) store.get(key);
            } else {
                uri2labelMapDatatypeProperties_1 = getURIlabelMapAndCalculateLanguageDistribution(ontology1.listDatatypeProperties(), ontology1);
                store.put(key, uri2labelMapDatatypeProperties_1);
            }
        } else {
            uri2labelMapDatatypeProperties_1 = getURIlabelMapAndCalculateLanguageDistribution(ontology1.listDatatypeProperties(), ontology1);
        }


        if (ont_2_key != null) {
            String key = "uri2labelMapDatatypeProperties_2_" + ont_2_key;
            if (store.containsKey(key)) {
                uri2labelMapDatatypeProperties_2 = (UriLabelInfo) store.get(key);
            } else {
                uri2labelMapDatatypeProperties_2 = getURIlabelMapAndCalculateLanguageDistribution(ontology2.listDatatypeProperties(), ontology2);
                store.put(key, uri2labelMapDatatypeProperties_2);
            }
        } else {
            uri2labelMapDatatypeProperties_2 = getURIlabelMapAndCalculateLanguageDistribution(ontology2.listDatatypeProperties(), ontology2);
        }

        if (ont_1_key != null) {
            String key = "uri2labelMapObjectProperties_1_" + ont_1_key;
            if (store.containsKey(key)) {
                uri2labelMapObjectProperties_1 = (UriLabelInfo) store.get(key);
            } else {
                uri2labelMapObjectProperties_1 = getURIlabelMapAndCalculateLanguageDistribution(ontology1.listObjectProperties(), ontology1);
                store.put(key, uri2labelMapObjectProperties_1);
            }
        } else {
            uri2labelMapObjectProperties_1 = getURIlabelMapAndCalculateLanguageDistribution(ontology1.listObjectProperties(), ontology1);
        }

        if (ont_2_key != null) {
            String key = "uri2labelMapObjectProperties_2_" + ont_2_key;
            if (store.containsKey(key)) {
                uri2labelMapObjectProperties_2 = (UriLabelInfo) store.get(key);
            } else {
                uri2labelMapObjectProperties_2 = getURIlabelMapAndCalculateLanguageDistribution(ontology2.listObjectProperties(), ontology2);
                store.put(key, uri2labelMapObjectProperties_2);
            }
        } else {
            uri2labelMapObjectProperties_2 = getURIlabelMapAndCalculateLanguageDistribution(ontology2.listObjectProperties(), ontology2);
        }

        if (ont_1_key != null) {
            String key = "uri2labelMapRemainingProperties_1_" + ont_1_key;
            if (store.containsKey(key)) {
                uri2labelMapRemainingProperties_1 = (UriLabelInfo) store.get(key);
            } else {
                uri2labelMapRemainingProperties_1 = getURIlabelMapAndCalculateLanguageDistributionForRemainingProperties(ontology1.listAllOntProperties(), ontology2);
                store.put(key, uri2labelMapRemainingProperties_1);
            }
        } else {
            uri2labelMapRemainingProperties_1 = getURIlabelMapAndCalculateLanguageDistributionForRemainingProperties(ontology1.listObjectProperties(), ontology1);
        }

        if (ont_2_key != null) {
            String key = "uri2labelMapRemainingProperties_2_" + ont_2_key;
            if (store.containsKey(key)) {
                uri2labelMapRemainingProperties_2 = (UriLabelInfo) store.get(key);
            } else {
                uri2labelMapRemainingProperties_2 = getURIlabelMapAndCalculateLanguageDistributionForRemainingProperties(ontology2.listAllOntProperties(), ontology2);
                store.put(key, uri2labelMapRemainingProperties_2);
            }
        } else {
            uri2labelMapRemainingProperties_2 = getURIlabelMapAndCalculateLanguageDistribution(ontology2.listObjectProperties(), ontology2);
        }

        languageDistribution_1 = UriLabelInfo.mergeDistributions(uri2labelMapClasses_1.getLanguageDistribution(), uri2labelMapDatatypeProperties_1.getLanguageDistribution(), uri2labelMapObjectProperties_1.getLanguageDistribution(), uri2labelMapRemainingProperties_1.getLanguageDistribution());
        languageDistribution_2 = UriLabelInfo.mergeDistributions(uri2labelMapClasses_2.getLanguageDistribution(), uri2labelMapDatatypeProperties_2.getLanguageDistribution(), uri2labelMapObjectProperties_2.getLanguageDistribution(), uri2labelMapRemainingProperties_1.getLanguageDistribution());

        // obtain the most frequent language of the ontologies
        mostFrequentLanguage_1 = UriLabelInfo.getMostUsedLanguage(languageDistribution_1);
        mostFrequentLanguage_2 = UriLabelInfo.getMostUsedLanguage(languageDistribution_2);
        // assume default language: English
        if (mostFrequentLanguage_1 == null) mostFrequentLanguage_1 = Language.ENGLISH;
        if (mostFrequentLanguage_2 == null) mostFrequentLanguage_2 = Language.ENGLISH;

        //LOGGER.info("Language Distribution of Ontology 1:\n" + UriLabelInfo.transformLanguageDistributionToString(languageDistribution_1));
        //LOGGER.info("Language Distribution of Ontology 2:\n" + UriLabelInfo.transformLanguageDistributionToString(languageDistribution_2));
    }


    /**
     * Returns all annotation properties of a resource.
     *
     * @param resource Resource for which annotation properties shall be retrieved.
     * @param ontModel The ontology model.
     * @return Set of annotation properties.
     */
    public HashSet<LabelLanguageTuple> getAnnotationProperties(OntResource resource, OntModel ontModel) {
        if (resource.isAnon()) {
            // anonymous node →
            return null;
        }

        // System.out.println(resource.getLocalName());
        HashSet<LabelLanguageTuple> result = getAnnotationPropertiesRecursionDeadLockSafe(resource, ontModel, 0);
        String localName = resource.getLocalName();

        // the getLocalName() method cuts URIs with certain characters, such as '_'. This is inaccurate in many cases.
        // therefore, the string after the last '/' is added in addition to the resulting HashSet.
        String fragmentExtended = "";
        Pattern pattern = Pattern.compile("[^\\/]*$"); // [^\/]*$
        Matcher matcher = pattern.matcher(resource.getURI());
        if(matcher.find()){
            fragmentExtended = matcher.group();
        }

        boolean isExcessiveFragment = false;
        boolean isExcessiveExtendedFragment = false;

        // control for excessive fragment
        if(extendedFragmentCounter.get(fragmentExtended) == null) extendedFragmentCounter.put(fragmentExtended, 1);
        else extendedFragmentCounter.put(fragmentExtended, extendedFragmentCounter.get(fragmentExtended) + 1);
        if(extendedFragmentCounter.get(fragmentExtended) > 15) isExcessiveExtendedFragment = true;

        if(fragmentCounter.get(localName) == null) fragmentCounter.put(localName, 1);
        else fragmentCounter.put(localName, fragmentCounter.get(localName) + 1);
        if(fragmentCounter.get(localName) > 15) isExcessiveFragment = true;

        if (filterNonMeaningfulLabels) {
            if(!isExcessiveExtendedFragment && !fragmentExtended.equals("") && fragmentExtended.contains("%")){
                // the fragmentExtended is the correct one to pick due to errors in getLocalName()
                if(StringOperations.isMeaningfulFragment(fragmentExtended)){
                    result.add(new LabelLanguageTuple(fragmentExtended, Language.UNKNOWN));
                }
            } else {
                if(!isExcessiveFragment && StringOperations.isMeaningfulFragment(localName)){
                    result.add(new LabelLanguageTuple(localName, Language.UNKNOWN));
                }
            }
        } else {
            if(!isExcessiveExtendedFragment && !fragmentExtended.equals("") && fragmentExtended.contains("%")){
                // the fragmentExtended is the correct one to pick due to errors in getLocalName()
                result.add(new LabelLanguageTuple(fragmentExtended, Language.UNKNOWN));
            } else if(!isExcessiveFragment){
                result.add(new LabelLanguageTuple(localName, Language.UNKNOWN));
            }
        }
        return result;
    }


    /**
     * Infinity loop save way to get annotation properties. Do not call this method directly but rather
     * its wrapper.
     *
     * @param resource       The resource for which the annotation properties shall be retrieved.
     * @param ontModel       The ontology model.
     * @param recursionDepth The depth of the recursion
     * @return A set of Strings that was retrieved.
     */
    private HashSet<LabelLanguageTuple> getAnnotationPropertiesRecursionDeadLockSafe(Resource resource, OntModel ontModel,
                                                                                     int recursionDepth) {
        recursionDepth++;
        if (resource.isAnon()) {
            // anonymous node →
            return null;
        }
        HashSet<LabelLanguageTuple> result = new HashSet<>();
        ExtendedIterator<AnnotationProperty> propertyIterator = ontModel.listAnnotationProperties();

        HashSet<Property> properties = new HashSet<>();
        while(propertyIterator.hasNext()){
            properties.add(propertyIterator.next());
        }
        //properties.add(ANCHOR_TEXT_PROPERTY);
        properties.add(RDFS.label);
        properties.add(SKOS.altLabel);

        for(Property property : properties) {

            StmtIterator stmtIterator = resource.listProperties(property);
            while (stmtIterator.hasNext()) {
                RDFNode object = stmtIterator.next().getObject();

                // case of resource
                if (object.isURIResource()) {
                    if (recursionDepth < 10) {
                        try {
                            result.addAll(getAnnotationPropertiesRecursionDeadLockSafe(object.asResource(),
                                    ontModel, recursionDepth));
                        } catch (Exception e){
                            System.out.println();
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Potential Infinity Loop Detected - aborting annotation property retrieval.");
                        return result;
                    }
                } else {
                    // case of value

                    Literal literal = object.asLiteral();
                    String label = literal.getLexicalForm();

                    if (filterNonMeaningfulLabels) {
                        if (StringOperations.isMeaningfulFragment(label)) {
                            result.add(new LabelLanguageTuple(label, literal.getLanguage()));
                        }
                    } else result.add(new LabelLanguageTuple(label, literal.getLanguage()));
                }

            }
        }
        return result;
    }

    /**
     * Creates a map of the form {@code URI -> set<labels>}.
     * In addition, the language distribution is calculated.
     *
     * @param iterator Iterator over OntResources.
     * @return A map that links from URIs to a set of labels.
     */
    UriLabelInfo getURIlabelMapAndCalculateLanguageDistribution(ExtendedIterator<? extends OntResource> iterator,
                                                                OntModel ontModel) {
        UriLabelInfo result = new UriLabelInfo();
        while (iterator.hasNext()) {
            OntResource r1 = iterator.next();
            HashSet<LabelLanguageTuple> labels = getAnnotationProperties(r1, ontModel);
            if (labels != null && labels.size() > 0) {
                result.add(r1.getURI(), labels);
            }
        }
        return result;
    }


    /**
     * Creates a map of the form {@code URI -> set<labels>}.
     * In addition, the language distribution is calculated.
     *
     * @param iterator Iterator over OntResources.
     * @return A map that links from URIs to a set of labels.
     */
    UriLabelInfo getURIlabelMapAndCalculateLanguageDistributionForRemainingProperties(ExtendedIterator<? extends OntResource> iterator,
                                                                                      OntModel ontModel) {
        UriLabelInfo result = new UriLabelInfo();
        nextResource:
        while (iterator.hasNext()) {
            OntResource r1 = iterator.next();
            if (uri2labelMapObjectProperties_1.getUriLabelMap().get(r1.getURI()) != null ||
                    uri2labelMapObjectProperties_2.getUriLabelMap().get(r1.getURI()) != null ||
                    uri2labelMapDatatypeProperties_1.getUriLabelMap().get(r1.getURI()) != null ||
                    uri2labelMapDatatypeProperties_2.getUriLabelMap().get(r1.getURI()) != null)
            {
                // do not add properties that were added before
                continue nextResource;
            }

            HashSet<LabelLanguageTuple> labels = getAnnotationProperties(r1, ontModel);
            if (labels != null && labels.size() > 0) {
                result.add(r1.getURI(), labels);
            }
        }
        return result;
    }




    /**
     *
     * @param iterator Iterator
     * @param ontModel Ont Model
     * @param ignoreImagesAndSkosConcept The KG track explicitly requires to remove those resources for a fair evaluation.
     * @return URI Label Info instance
     */
    UriLabelInfo calculateUriLabelInfoGivenIterator(ResIterator iterator, OntModel ontModel, boolean ignoreImagesAndSkosConcept){
        UriLabelInfo result = new UriLabelInfo();

        nextResource:
        while (iterator.hasNext()) {
            Resource resource = iterator.nextResource();
            OntResource r1 = ontModel.getOntResource(resource.getURI()) ;
            if(r1 == null){
                //LOGGER.debug(resource.getURI() + " is not an OntResource.");
                continue nextResource;
            }

            if(r1.isClass() || r1.isProperty()){
                // ignore classes and properties
                continue nextResource;
            }

            if(ignoreImagesAndSkosConcept) {
                Iterator<Resource> typeIterator = r1.listRDFTypes(true);
                while(typeIterator.hasNext()){
                    String typeUri = typeIterator.next().getURI();
                    if(typeUri.equals("http://dbkwik.webdatacommons.org/ontology/Image") ||
                            typeUri.equals("http://www.w3.org/2004/02/skos/core#Concept")){
                        //LOGGER.info("Skipping URI " + r1.getURI() + " due to type 'Image'.");
                        continue nextResource;
                    }
                }
            }
            HashSet<LabelLanguageTuple> labels = getAnnotationProperties(r1, ontModel);
            if (labels != null && labels.size() > 0) {
                result.add(resource.getURI(), labels);
            }
        }
        return result;
    }

    /**
     *
     * @param iterator Iterator
     * @param ontModel Ont Model
     * @param ignoreImagesAndSkosConcept The KG track explicitly requires to remove those resources for a fair evaluation.
     * @return URI Label Info instance
     */
    UriLabelInfo calculateUriLabelInfoGivenIterator(ExtendedIterator<Individual> iterator, OntModel ontModel, boolean ignoreImagesAndSkosConcept){
        UriLabelInfo result = new UriLabelInfo();

        nextResource:
        while (iterator.hasNext()) {
            Individual individual = iterator.next();


            OntResource r1 = ontModel.getOntResource(individual.getURI()) ;
            if(r1 == null){
                //LOGGER.debug(resource.getURI() + " is not an OntResource.");
                continue nextResource;
            }

            if(r1.isClass() || r1.isProperty()){
                // ignore classes and properties
                continue nextResource;
            }

            if(ignoreImagesAndSkosConcept) {
                Iterator<Resource> typeIterator = individual.listRDFTypes(true);
                while(typeIterator.hasNext()){
                    String typeUri = typeIterator.next().getURI();
                    if(typeUri.equals("http://dbkwik.webdatacommons.org/ontology/Image") ||
                            typeUri.equals("http://www.w3.org/2004/02/skos/core#Concept")){
                        //LOGGER.info("Skipping URI " + r1.getURI() + " due to type 'Image'.");
                        continue nextResource;
                    }
                }
            }
            HashSet<LabelLanguageTuple> labels = getAnnotationProperties(individual, ontModel);
            if (labels != null && labels.size() > 0) {
                result.add(individual.getURI(), labels);
            }
        }
        return result;
    }

    public boolean isFilterNonMeaningfulLabels() {
        return filterNonMeaningfulLabels;
    }

    public void setFilterNonMeaningfulLabels(boolean filterNonMeaningfulLabels) {
        this.filterNonMeaningfulLabels = filterNonMeaningfulLabels;
    }
}