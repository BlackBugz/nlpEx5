package de.tudarmstadt.lt.teaching.nlp4web.ml.postagger;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
/*import org.cleartk.classifier.CleartkSequenceAnnotator;
 import org.cleartk.classifier.jar.DefaultSequenceDataWriterFactory;
 import org.cleartk.classifier.jar.DirectoryDataWriterFactory;
 import org.cleartk.classifier.jar.GenericJarClassifierFactory;*/
import org.cleartk.ml.CleartkSequenceAnnotator;
import org.cleartk.ml.crfsuite.CrfSuiteStringOutcomeDataWriter;
import org.cleartk.ml.jar.DefaultSequenceDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.mallet.MalletCrfStringOutcomeDataWriter;
import org.cleartk.util.cr.FilesCollectionReader;

import de.tudarmstadt.lt.teaching.nlp4web.ml.entitytagger.NamedEntityTaggerAnnotator;
import de.tudarmstadt.lt.teaching.nlp4web.ml.reader.NamedEntityConverter;
import de.tudarmstadt.lt.teaching.nlp4web.ml.reader.NamedEntityListReader;
import de.tudarmstadt.ukp.dkpro.core.snowball.SnowballStemmer;

public class ExecutePosTaggerOnList {

	static String featureFile = "src/main/resources/ner/features.xml";

	public static void writeModel(File learnFile, File listFileList[],
			String modelDirectory, String language)
			throws ResourceInitializationException, UIMAException, IOException {

		AnalysisEngine model = createEngine(NamedEntityTaggerAnnotator.class,
				NamedEntityTaggerAnnotator.PARAM_FEATURE_EXTRACTION_FILE,
				featureFile, 
				CleartkSequenceAnnotator.PARAM_IS_TRAINING, true,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				modelDirectory,
				DefaultSequenceDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				CrfSuiteStringOutcomeDataWriter.class);
		
		runPipeline(
				FilesCollectionReader.getCollectionReaderWithSuffixes(
						learnFile.getAbsolutePath(),
						NamedEntityConverter.NER_VIEW, learnFile.getName()),
				createEngine(NamedEntityConverter.class),
				createEngine(SnowballStemmer.class,
						SnowballStemmer.PARAM_LANGUAGE, language), model);
		for(File listFile : listFileList){
			runPipeline(
					FilesCollectionReader.getCollectionReaderWithSuffixes(
							listFile.getAbsolutePath(),
							NamedEntityListReader.NEL_VIEW, listFile.getName()),
					createEngine(NamedEntityListReader.class),
					createEngine(SnowballStemmer.class,
							SnowballStemmer.PARAM_LANGUAGE, language), model);
		}
	}

	public static void trainModel(String modelDirectory) throws Exception {
		org.cleartk.ml.jar.Train.main(modelDirectory);
	}

	public static void classifyTestFile(String modelDirectory, File testFile,
			String language) throws ResourceInitializationException,
			UIMAException, IOException {
		System.out.println("Classifing "+testFile.getAbsolutePath());
		runPipeline(
				FilesCollectionReader.getCollectionReaderWithSuffixes(
						testFile.getAbsolutePath(),
						NamedEntityConverter.NER_VIEW, testFile.getName()),
				createEngine(NamedEntityConverter.class),
				createEngine(SnowballStemmer.class,
						SnowballStemmer.PARAM_LANGUAGE, language),
				createEngine(
						NamedEntityTaggerAnnotator.class,
						NamedEntityTaggerAnnotator.PARAM_FEATURE_EXTRACTION_FILE,
						featureFile,
						GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
						modelDirectory + "model.jar"),
				createEngine(FeaturesWriter.class,
						FeaturesWriter.PARAM_INPUT_FILE,
						testFile.getAbsolutePath(),
						FeaturesWriter.PARAM_TOKEN_VALUE_PATH, "pos/PosValue",
						FeaturesWriter.PARAM_OUTPUT_FILE,
						testFile.getAbsolutePath() + ".out")
		);
	}

	private static void info(String message) {
		Logger logger = UIMAFramework.getLogger();
		logger.log(Level.INFO, message);
	}

	public static void main(String[] args) throws Exception {

		long start = System.currentTimeMillis();
		info("Started");

		String modelDirectory = "src/test/resources/model/";
		String language = "en";

		// full training set:
		File nerTagFile = new File("src/main/resources/ner/ner_eng.train");
		File nelTagFile[] = {
				new File("src/main/resources/ner/eng.list"),
				new File("src/main/resources/ner/cities.list"),
				new File("src/main/resources/ner/entities.list")
		};
		
		File nelTestFile = new File("src/main/resources/ner/ner_eng.test");
		new File(modelDirectory).mkdirs();
		info("Starting executing ExecutePosTaggerOnList");
		info("~~~~~ Starting to write model ~~~~~");
		writeModel(nerTagFile, nelTagFile, modelDirectory, language);
		info("~~~~~ Starting to train model ~~~~~");
		trainModel(modelDirectory);
		info("~~~~~ Starting to test model ~~~~~");
		classifyTestFile(modelDirectory, nelTestFile, language);
		long now = System.currentTimeMillis();
		info("Time: " + (now - start) + "ms");
	}
}
