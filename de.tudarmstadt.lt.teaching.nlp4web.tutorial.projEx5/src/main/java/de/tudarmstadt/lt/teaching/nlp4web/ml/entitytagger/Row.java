package de.tudarmstadt.lt.teaching.nlp4web.ml.entitytagger;

import java.util.List;

public class Row {

	public Row(String key, List<String> words) {
		this.key = key;
		this.words = words;
	}

	private final String key;
	private final List<String> words;

	public String getKey() {
		return key;
	}

	public List<String> getWords() {
		return words;
	}

	public Integer indexOf(String coveredText) {
		return getWords().indexOf(coveredText); 
	}
}
