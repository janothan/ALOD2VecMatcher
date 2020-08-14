package de.uni_mannheim.informaik.dws.Alod2vecMatcher.services;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

/**
 * Responsible for multiple MapDB persistences.
 */
public class PersistenceService {


    private static Logger LOGGER = LoggerFactory.getLogger(PersistenceService.class);

    /**
     * Singleton instance
     */
    private static PersistenceService service;

    /**
     * Returns the service singleton instance.
     * @return Instance of service
     */
    public static PersistenceService getService(){
        if(service == null){
            service = new PersistenceService();
        }
        return service;
    }

    /**
     * Constructor: Singleton Pattern
     */
    private PersistenceService(){
    }

    /**
     * where all database files reside
     */
    private String localStore = "./oaei-resources/persistences/";


    private BTreeMap<String, HashSet<String>> translationBuffer;
    private BTreeMap<String, HashSet<String>> translation_OF_Buffer;

    public BTreeMap<String, HashSet<String>> getTranslationBuffer(){
        if(translationBuffer == null) {
            try {
                DB db = DBMaker.fileDB(localStore + "./getTranslationBuffer.mdb").closeOnJvmShutdown().make();
                translationBuffer = db.treeMap("getTranslationBuffer").keySerializer(Serializer.STRING)
                        .valueSerializer(Serializer.JAVA).createOrOpen();
            } catch(Exception e){
                LOGGER.error("Could not initialize database 'getTranslationBuffer'.", e);
                return DBMaker.memoryDB().closeOnJvmShutdown().make().treeMap("getTranslationBuffer").keySerializer(Serializer.STRING)
                        .valueSerializer(Serializer.JAVA).createOrOpen();
            }
        }
        return translationBuffer;
    }

    public BTreeMap<String, HashSet<String>> getTranslation_OF_Buffer(){
        if(translation_OF_Buffer == null) {
            try {
                DB db = DBMaker.fileDB(localStore + "./getTranslation_OF_Buffer.mdb").closeOnJvmShutdown().make();
                translation_OF_Buffer = db.treeMap("getTranslation_OF_Buffer").keySerializer(Serializer.STRING)
                        .valueSerializer(Serializer.JAVA).createOrOpen();
            } catch(Exception e){
                LOGGER.error("Could not initialize database 'getTranslation_OF_Buffer'.", e);
                return DBMaker.memoryDB().closeOnJvmShutdown().make().treeMap("getTranslation_OF_Buffer").keySerializer(Serializer.STRING)
                        .valueSerializer(Serializer.JAVA).createOrOpen();
            }
        }
        return translation_OF_Buffer;
    }





}
