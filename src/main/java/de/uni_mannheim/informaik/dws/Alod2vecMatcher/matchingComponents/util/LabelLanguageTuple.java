package de.uni_mannheim.informaik.dws.Alod2vecMatcher.matchingComponents.util;

/**
 * Data structure to hold label together with the language.
 */
public class LabelLanguageTuple {
    public String label;
    public Language language;

    /**
     * Constructor with Char2 Inference.
     * @param label Label.
     * @param language Language.
     */
    public LabelLanguageTuple(String label, String language){
        this.label = label;
        this.language = Language.inferLanguageChar2(language);
    }

    /**
     * Default Constructor.
     * @param label Label.
     * @param language Language.
     */
    public LabelLanguageTuple(String label, Language language){
        this.label = label;
        this.language = language;
    }


    @Override
    public int hashCode(){
        return this.language.hashCode() + this.label.hashCode();
    }

    @Override
    public boolean equals(Object otherObject){
        if(otherObject.getClass() != LabelLanguageTuple.class) return false;
        LabelLanguageTuple otherCasted = ((LabelLanguageTuple) otherObject);
        if(!otherCasted.label.equals(this.label)) return false;
        if(!otherCasted.language.equals(this.language)) return false;
        return true;
    }


}