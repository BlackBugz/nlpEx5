package de.tudarmstadt.lt.teaching.nlp4web.ml.reader;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.teaching.general.type.NamedEntity;

public class NamedEntityListReader extends JCasAnnotator_ImplBase {
	public static final String NEL_VIEW = "ENLView";
	private Logger logger = null;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		logger = context.getLogger();
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas docView;
		String tbText;
		try {
			docView = jcas.getView(CAS.NAME_DEFAULT_SOFA);
			tbText = jcas.getView(NEL_VIEW).getDocumentText();
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
		
		
		// a new entity always starts with a new line
		if (tbText.charAt(0) != '\n') {
			tbText = "\n" + tbText;
		}

		String[] lines = tbText.split("(\r\n|\n)");
		Sentence sentence = null;
		int idx = 0;
		int tl = 0;
		Token token = null;
		POS posTag;
		NamedEntity neTag;

		StringBuffer docText = new StringBuffer();
		for (String line : lines) {
			// new sentence if there's a new line
			if(!line.equals(""))
			{
				String[] tokens = line.split(" ");
				String neType = "I-"+tokens[0];

				sentence = new Sentence(docView);
				sentence.setBegin(idx);
				
				for(int i = 1; i < tokens.length; i++)
				{
					tl = idx + tokens[i].length();
					docText.append(tokens[i]);
					token = new Token(docView, idx, tl);
					posTag = new POS(docView, idx, tl);
					posTag.setPosValue("NNP");
					token.setPos(posTag);
					neTag = new NamedEntity(docView, idx, tl);
					neTag.setEntityType(neType);
					token.addToIndexes();
					posTag.addToIndexes();
					neTag.addToIndexes();
					// +1 to add the space or the newline
					idx = tl+1;
					String endt = i < (tokens.length - 1) ? " " : "\n";
					docText.append(endt);
				}
				sentence.setEnd(idx-1);
				sentence.addToIndexes();
			}
		}

		docView.setSofaDataString(docText.toString(), "text/plain");
	}

}
