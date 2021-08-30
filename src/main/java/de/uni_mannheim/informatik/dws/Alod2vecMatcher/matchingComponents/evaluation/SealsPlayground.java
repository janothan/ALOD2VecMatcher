package de.uni_mannheim.informatik.dws.Alod2vecMatcher.matchingComponents.evaluation;

import de.uni_mannheim.informatik.dws.melt.matching_data.TrackRepository;
import de.uni_mannheim.informatik.dws.melt.matching_eval.ExecutorSeals;
import de.uni_mannheim.informatik.dws.melt.matching_eval.evaluator.EvaluatorCSV;

public class SealsPlayground {

    public static void main(String[] args) {
        String sealsClientJar = "/Users/janportisch/SEALS/JAR/seals-omt-client.jar";
        String sealsHome = "/Users/janportisch/SEALS/HOME";
        String java8command = "/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/bin/java";
        String pathToUnzippedSealsPackage = "/Users/janportisch/TMP/ALOD2VecMatcher-1.0-SNAPSHOT-seals_external.zip";
        ExecutorSeals es = new ExecutorSeals(java8command, sealsClientJar, sealsHome);
        EvaluatorCSV evaluatorCSV = new EvaluatorCSV(es.run(TrackRepository.Anatomy.Default, pathToUnzippedSealsPackage));
        evaluatorCSV.writeToDirectory();
    }

}
