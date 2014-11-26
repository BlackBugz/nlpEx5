package de.tudarmstadt.lt.teaching.nlp4web.ml.entitytagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.uimafit.util.JCasUtil;

import com.google.common.collect.HashMultimap;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class NerFeatureExtractor implements FeatureExtractor1<Token> {
	private HashMultimap<String, Row> lookupTable = HashMultimap.create();
	private Logger log = UIMAFramework.getLogger(NerFeatureExtractor.class);
	
	private int lookups = 0;
	/**
	 * Loads the file contents to be used for checking if the sourrounding
	 * content matches.
	 * 
	 * @param file
	 * @throws ResourceInitializationException
	 */
	public NerFeatureExtractor(File file)
			throws ResourceInitializationException {
		FileReader fileReader;
		BufferedReader bf;
		try {
			fileReader = new FileReader(file);
			bf = new BufferedReader(fileReader);
			String line;
			while ((line = bf.readLine()) != null) {
				if (line.equals(""))
					continue;

				String[] words = line.split(" ");

				LinkedList<String> entity = new LinkedList<>();
				entity.addAll(Arrays.asList(words));
				String key = entity.remove(0);
				Row r = new Row(key, entity);
				for (String token : entity) {
					lookupTable.put(token, r);
				}

			}
			bf.close();
			fileReader.close();
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

	}

	@Override
	public List<Feature> extract(JCas view, Token focusAnnotation)
			throws CleartkExtractorException {
		// look left and right of the focusannotation and set feature to "true"
		// if everything is working right.
		List<Feature> features = new ArrayList<Feature>();

		final String coveredText = focusAnnotation.getCoveredText();
		Set<Row> potentialMatches = lookupTable.get(coveredText);

		// for each row of the ner-list that contains the coveredText, check if
		// left and right are in the row too, at the relevant location...
		
		String label = getLabel(view, focusAnnotation, coveredText, potentialMatches);
		//
		// Collection<Token> select = JCasUtil.select(view, Token.class);
		// List<Token> predecessor = JCasUtil.selectPreceding(view, Token.class,
		// focusAnnotation, 1);

		// ist Token ein Name? (in Liste nachschauen)
		Feature isName = new Feature("IsPerson", ("PER".equalsIgnoreCase(label)));
		Feature isMisc = new Feature("IsMisc", ("MISC".equalsIgnoreCase(label)));
		Feature isLoc = new Feature("IsLoc", ("LOC".equalsIgnoreCase(label)));
		Feature isOrg = new Feature("IsOrg", ("ORG".equalsIgnoreCase(label)));

		features.add(isName);
//		features.add(isMisc);
		features.add(isLoc);
		features.add(isOrg);

		return features;
	}

	/**
	 * @param view
	 * @param focusAnnotation
	 * @param coveredText
	 * @param potentialMatches
	 * @return 
	 * @throws AssertionError
	 */
	private String getLabel(JCas view, Token focusAnnotation,
			final String coveredText, Set<Row> potentialMatches) {
		for (Row row : potentialMatches) {
			lookups++;
			if(lookups % 10000 == 0) {
				log.log(Level.INFO, String.format("%s lookups done.", lookups));				
			}
			
			final List<String> words = row.getWords();
			final Integer indexOfMatch = row.indexOf(coveredText);
			if (indexOfMatch == -1)
				throw new AssertionError();
			
			log.log(Level.FINEST,
					String.format("~~~~ Searching in '%s' for '%s'", words, view.getSofaDataString()));
			log.log(Level.FINEST,
					String.format("~~~~ Using '%s' is in Position %s of %s", coveredText, indexOfMatch, words));
			
			
			// check predecessors
			final List<Token> items = new ArrayList<>();

			/**
			 * if indexOfMatch is the first item in the row, then its index is 0
			 * => no predecessors to be selected.
			 */
			List<Token> predecessors = JCasUtil.selectPreceding(view, Token.class, focusAnnotation, indexOfMatch);
			log.log(Level.FINEST,
					String.format("%s Predecessors: '%s'", predecessors.size(), logDebugTokenList(predecessors).toString()));
			items.addAll(predecessors);

			items.add(focusAnnotation);
			log.log(Level.FINEST, String.format("FocusAnnotation: '%s'", focusAnnotation.getCoveredText()));

			/**
			 * if indexOfMatch is the last item in the row, then its index is
			 * row.getWords().size() => no successors to select.
			 */
			List<Token> successors = JCasUtil.selectFollowing(view, Token.class,
					focusAnnotation, words.size() - indexOfMatch -1);
			log.log(Level.FINEST,
					String.format("%s Successors: '%s'", successors.size(), logDebugTokenList(successors).toString()));
			items.addAll(successors);

			log.log(Level.FINEST,
					String.format("%s Total items: '%s'", items.size(), logDebugTokenList(items).toString()));

			
			boolean b = true;
			if (words.size() != items.size()) {
				// can only be successful if both counts are the same.
				b = false;
			} else {
				for (int i = 0; i < words.size(); i++) {
					final Token t = items.get(i);
					final String tokenText = t.getCoveredText();
					final String rowText = words.get(i);
					if (!tokenText.equals(rowText)) {
						b = false;
						break;
					}
				}
			}
			if (b) {
				/*
				 *  we have found a named entity by peeking in our list; Therefore, we stop and propagate the label(!) to the outside world
				 */
				
				log.log(Level.FINEST,
						String.format("Found a named entity in the list"));
				log.log(Level.FINEST, String.format(
						"key:\t%s was found in %s at position %s", coveredText,
						row.getWords(), indexOfMatch));
				log.log(Level.FINEST,
						String.format("found \n%s\n, looking for \n%s",
								logDebugTokenList(items).toString(), row.getWords()));
				return row.getKey();
//			} else {
//				return "MISC";
				// log.log(Level.FINEST, String.format("Mismatch"));
				// log.log(Level.FINEST,
				// String.format("key:\t%s was found in %s at position %s",
				// coveredText, row.getWords(), indexOfMatch));
				// log.log(Level.FINEST,
				// String.format("found \n%s\n, looking for \n%s",
				// row.getWords(), indexOfMatch));
			}

		}
		return "MISC";
	}

	private StringBuffer logDebugTokenList(final List<Token> items) {
		StringBuffer sb = new StringBuffer();
		for (Token t : items) {
			sb.append(t.getCoveredText() + " ");
		}
		return sb;
	}
}
