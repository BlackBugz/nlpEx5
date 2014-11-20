package de.tudarmstadt.lt.teaching.nlp4web.ml.postagger;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
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
import de.tudarmstadt.lt.teaching.nlp4web.ml.reader.ConllAnnotator;
import de.tudarmstadt.lt.teaching.nlp4web.ml.reader.NamedEntityConverter;
import de.tudarmstadt.ukp.dkpro.core.snowball.SnowballStemmer;

public class ExecutePosTagger {

	public static void writeModel(File learnFile, String modelDirectory, String language)
			throws ResourceInitializationException, UIMAException, IOException {

		runPipeline(
				FilesCollectionReader.getCollectionReaderWithSuffixes(
						learnFile.getAbsolutePath(),
						NamedEntityConverter.NER_VIEW, learnFile.getName()),
				createEngine(NamedEntityConverter.class),
				createEngine(SnowballStemmer.class,
						SnowballStemmer.PARAM_LANGUAGE, language),
				createEngine(
						NamedEntityTaggerAnnotator.class,
						CleartkSequenceAnnotator.PARAM_IS_TRAINING, true,
						DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, modelDirectory,
						DefaultSequenceDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
						CrfSuiteStringOutcomeDataWriter.class));
		
	}

	public static void trainModel(String modelDirectory) throws Exception {
	    org.cleartk.ml.jar.Train.main(modelDirectory);
	}

	public static void classifyTestFile(String modelDirectory, File testFile, String language) throws ResourceInitializationException, UIMAException, IOException {
		runPipeline(FilesCollectionReader.getCollectionReaderWithSuffixes(
				testFile.getAbsolutePath(),
				NamedEntityConverter.NER_VIEW, testFile.getName()),
				createEngine(NamedEntityConverter.class),
				createEngine(SnowballStemmer.class,
						SnowballStemmer.PARAM_LANGUAGE, language),
						createEngine(NamedEntityTaggerAnnotator.class,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, 	modelDirectory+"model.jar"),
				createEngine(AnalyzeFeatures.class,
						AnalyzeFeatures.PARAM_INPUT_FILE, testFile.getAbsolutePath(),
						AnalyzeFeatures.PARAM_TOKEN_VALUE_PATH,"pos/PosValue")
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
		// simple short training set:
		File nerTagFile=   new File("src/main/resources/ner/ner_eng_10.train");
		File nerTestFile = new File("src/main/resources/ner/ner_eng.dev");

		// full training set:
//				File nerTagFile=   new File("src/main/resources/ner/ner_eng.train");
//		File nerTestFile = new File("src/main/resources/ner/ner_eng.dev");
		new File(modelDirectory).mkdirs();
		info("~~~~~ Starting to write model ~~~~~");
		writeModel(nerTagFile, modelDirectory,language);
		info("~~~~~ Starting to train model ~~~~~");
		trainModel(modelDirectory);
		info("~~~~~ Starting to test model ~~~~~");
		classifyTestFile(modelDirectory, nerTestFile ,language);
		long now = System.currentTimeMillis();
		info("Time: "+(now-start)+"ms");
	}
}
