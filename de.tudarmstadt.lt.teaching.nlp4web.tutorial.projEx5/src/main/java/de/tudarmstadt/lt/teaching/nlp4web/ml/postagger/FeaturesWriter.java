package de.tudarmstadt.lt.teaching.nlp4web.ml.postagger;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.ml.feature.extractor.TypePathExtractor;
/*import org.cleartk.classifier.feature.extractor.simple.TypePathExtractor;*/
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This class analaysis the classification result and prints the precision and
 * recall for each pos tag and prints the average precision and recall for all
 * pos tags and the accuracy
 *
 *
 */
public class FeaturesWriter extends JCasAnnotator_ImplBase
{
	public static final String PARAM_TOKEN_VALUE_PATH = "TokenValuePath";
	public static final String PARAM_INPUT_FILE = "InputFile";
	public static final String PARAM_OUTPUT_FILE = "OnputFile";
	/**
	 * To make this class general, the path to the feature that is used
	 * for the evaluation the tokenValuePath has to be set to the feature
	 * e.g. for the pos value: pos/PosValue is used
	 * (works only for token: de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)
	 */
	@ConfigurationParameter(name = PARAM_TOKEN_VALUE_PATH, mandatory = true)
	private String tokenValuePath;
	@ConfigurationParameter(name = PARAM_INPUT_FILE, mandatory = true)
	private String inputFile;
	@ConfigurationParameter(name = PARAM_OUTPUT_FILE, mandatory = true)
	private String outputFile;
	Logger logger = UIMAFramework.getLogger(FeaturesWriter.class);

	private class Classification {
		int fp = 0;
		int tp = 0;
		int fn = 0;
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		try {
			logger.log(Level.INFO, "Start analyzing results");

			HashMap<String, Classification> map = new HashMap<String, Classification>();
			TypePathExtractor<Token> extractor;
			extractor = new TypePathExtractor<Token>(Token.class, tokenValuePath);
			String line;
			String[] splitLine = null;
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

			for (Sentence sentence : select(jCas, Sentence.class)) {
				List<Token> tokens = selectCovered(jCas, Token.class, sentence);
				for (Token token : tokens) {
					boolean searchForward = true;
					while(searchForward) {
						line = reader.readLine(); 
						splitLine = line.split("\\s");
						if(splitLine.length == 4) 
						{
							searchForward=false;
						}
						writer.write(line);
					}
					String classifiedValue = extractor.extract(jCas, token).get(0).getValue().toString();
					writer.write(" "+classifiedValue + "\n");
				}
			}
			reader.close();
			writer.close();
			logger.log(Level.INFO, "Finished writing");
		} catch (FileNotFoundException e) {
			logger.log(Level.WARNING, e.getMessage());
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage());
		}
	}
}
