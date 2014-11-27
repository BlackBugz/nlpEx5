package de.tudarmstadt.lt.teaching.nlp4web.ml.entitytagger;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
/*import org.cleartk.classifier.CleartkSequenceAnnotator;
 import org.cleartk.classifier.Instance;
 import org.cleartk.classifier.feature.extractor.CleartkExtractor;
 import org.cleartk.classifier.feature.extractor.CleartkExtractor.Following;
 import org.cleartk.classifier.feature.extractor.CleartkExtractor.Preceding;
 import org.cleartk.classifier.feature.extractor.simple.CoveredTextExtractor;
 import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
 import org.cleartk.classifier.feature.extractor.simple.TypePathExtractor;
 import org.cleartk.classifier.feature.function.CapitalTypeFeatureFunction;
 import org.cleartk.classifier.feature.function.CharacterNGramFeatureFunction;
 import org.cleartk.classifier.feature.function.FeatureFunctionExtractor;
 import org.cleartk.classifier.feature.function.LowerCaseFeatureFunction;
 import org.cleartk.classifier.feature.function.NumericTypeFeatureFunction;*/
import org.cleartk.ml.CleartkSequenceAnnotator;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Focus;
import org.cleartk.ml.feature.extractor.CombinedExtractor1;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Bag;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.TypePathExtractor;
import org.cleartk.ml.feature.function.CapitalTypeFeatureFunction;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction.PatternType;
import org.cleartk.ml.feature.function.CharacterNgramFeatureFunction;
import org.cleartk.ml.feature.function.CharacterNgramFeatureFunction.Orientation;
import org.cleartk.ml.feature.function.ContainsHyphenFeatureFunction;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor;
import org.cleartk.ml.feature.function.LowerCaseFeatureFunction;
import org.cleartk.ml.feature.function.NumericTypeFeatureFunction;

import cc.mallet.grmm.learning.DefaultAcrfTrainer.FileEvaluator;

import com.thoughtworks.xstream.XStream;

import de.tudarmstadt.lt.teaching.nlp4web.ml.xml.XStreamFactory;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.teaching.general.type.NamedEntity;

public class NamedEntityTaggerAnnotator2 extends
		CleartkSequenceAnnotator<String> {

	public static final String PARAM_FEATURE_EXTRACTION_FILE = "FeatureExtractionFile";
	public static final String PARAM_ENTITY_LIST_FILE = "EntityListFile";
	

	/**
	 * if a feature extraction/context extractor filename is given the xml file
	 * is parsed and the features are used, otherwise it will not be used
	 */
	@ConfigurationParameter(name = PARAM_FEATURE_EXTRACTION_FILE, mandatory = false)
	private String featureExtractionFile = null;
	
	@ConfigurationParameter(name = PARAM_ENTITY_LIST_FILE, mandatory = false)
	private String listFile = null;

	private FeatureExtractor1<Token> tokenFeatureExtractor;

	private CleartkExtractor<Token, Token> contextFeatureExtractor;
	private TypePathExtractor<Token> stemExtractor;

	private Logger logger;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		logger = context.getLogger();
		// add feature extractors
		
		HashMap<String, Object> extractor = new HashMap<String, Object>();
		
		if (featureExtractionFile == null  || !((new File(featureExtractionFile)).isFile())) {
			CharacterNgramFeatureFunction.Orientation fromRight = Orientation.RIGHT_TO_LEFT;

			stemExtractor = new TypePathExtractor<Token>(Token.class,
					"stem/value");

			this.tokenFeatureExtractor = new FeatureFunctionExtractor<Token>(
					new CoveredTextExtractor<Token>(),
					/* new CharacterCategoryPatternFunction<Token>(PatternType.REPEATS_MERGED),
			        new CapitalTypeFeatureFunction()
//			        ,
			        //new TypePathExtractor<Annotation>(focusClass, typePath)
					*/
					new LowerCaseFeatureFunction(),
					new CapitalTypeFeatureFunction(),
					new NumericTypeFeatureFunction(),
					new CharacterNgramFeatureFunction(fromRight, 0, 2)
					//, new ContainsHyphenFeatureFunction()
					);
			

			this.contextFeatureExtractor = new CleartkExtractor<Token, Token>(
					Token.class, 
					//this.tokenFeatureExtractor,
					new CoveredTextExtractor<Token>(),
					new Preceding(3), 
					new Following(3)
					//, new Bag(new Preceding(2), new Focus(), new Following(2))
					);
			
			extractor.put("tokenFeatureExtractor", tokenFeatureExtractor);
			extractor.put("contextFeatureExtractor", contextFeatureExtractor);
			
			
			XStream xstream = XStreamFactory.createXStream();
			try {
				File out = new File(featureExtractionFile);
				out.createNewFile();
				xstream.toXML(extractor, new FileOutputStream(out));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			

		} else {// load the settings from a file
				// initialize the XStream if a xml file is given:
			System.out.println("I got the file");
			XStream xstream = XStreamFactory.createXStream();
			extractor = (HashMap<String, Object>) xstream
					.fromXML(new File(featureExtractionFile));

			tokenFeatureExtractor = (FeatureExtractor1<Token>)extractor.get("tokenFeatureExtractor");
			contextFeatureExtractor = (CleartkExtractor<Token, Token>)extractor.get("contextFeatureExtractor");
			stemExtractor = new TypePathExtractor<Token>(Token.class,
					"stem/value");
		}

	}

	private void info(String message) {
		logger.log(Level.INFO, message);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		for (Sentence sentence : select(jCas, Sentence.class)) {
			
			List<Instance<String>> instances = new ArrayList<Instance<String>>();
			List<Token> tokens = selectCovered(jCas, Token.class, sentence);
			for (Token token : tokens) {

				Instance<String> instance = new Instance<String>();
				instance.addAll(tokenFeatureExtractor.extract(jCas, token));
				instance.addAll(contextFeatureExtractor.extractWithin(jCas,
						token, sentence));
				instance.addAll(stemExtractor.extract(jCas, token));

				List<NamedEntity> entityAnnotations = selectCovered(jCas,
						NamedEntity.class, token);
				// info(String.format("covered %s annotations",
				// entityAnnotations.size() ) );
				// int i=0;
				for (NamedEntity ne : entityAnnotations) {
					// info(ne.getCoveredText());
					instance.setOutcome(ne.getEntityType());
				}

				// add the instance to the list !!!
				instances.add(instance);
			}
			// differentiate between training and classifying
			if (this.isTraining()) {
				this.dataWriter.write(instances);
			} else {
				List<String> posTags = this.classify(instances);
				List<String> neTags = this.classify(instances);
				int i = 0;
				for (Token token : tokens) {
					POS pos = new POS(jCas, token.getBegin(), token.getEnd());
					pos.setPosValue(posTags.get(i));
					token.setPos(pos);

					NamedEntity ne = new NamedEntity(jCas, token.getBegin(),
							token.getEnd());
					ne.setEntityType(neTags.get(i));

					i++;
				}
			}
		}

	}
}
