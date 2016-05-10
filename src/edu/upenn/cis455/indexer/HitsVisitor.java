package edu.upenn.cis455.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import edu.upenn.cis455.indexer.PageAttributesItem;

class HitsVisitor implements NodeVisitor {
	boolean validResult = true;
	Map<String, List<Hit>> wordToHits = new HashMap<>();
	PageAttributesItem pageAttributesItem = new PageAttributesItem();
	Map<String, Double> wordToTermFrequency = new HashMap<>();
	int totalWordCount = 0;
	String title;
	String meta;
	String description;

	private Map<String, List<Hit>> oneWordToHits = new HashMap<>();
	private Map<String, List<Hit>> twoWordToHits = new HashMap<>();
	private int curPosition = 0;
	private Stemmer stemmer = new Stemmer();

	private TextClassGetter textClassGetter = new TextClassGetter();
	
    private final int MIN_WORD_LENGTH = 2;
    private final int MAX_WORD_LENGTH = 20;

	int getMaxFrequency() {
		int maxFrequency = 0;
		for (String word : wordToHits.keySet()) {
			maxFrequency = Math.max(wordToHits.get(word).size(), maxFrequency);
		}
		return maxFrequency;
	}
	
	boolean couldIndexing(String word) {
		return !stemmer.isStopWord(word.toLowerCase()) && MIN_WORD_LENGTH <= word.length() && word.length() <= MAX_WORD_LENGTH;
	}

	// return: processed word number
	private Integer processText(String text, Integer startPosition, String textClass) {
		int processedWordNumber = 0;
    	List<String> words = new ArrayList<>();
    	List<String> lowerStemmedWords = new ArrayList<>();
    	List<Integer> positions = new ArrayList<>();
    	List<Integer> capitalizations = new ArrayList<>();
    	text = text.replace('-', ' ');	// for word like: Harry Potter-Themed
    	text = text.replace(',', ' ');	// for word like: word1,word2,word3
    	text = text.replace('.', ' ');	// for word like: word1.word2
    	

    	// save to local var
    	for (String word : text.split("\\s+")) {
    		word = stemmer.removeSpecialCharacter(word);
    		if (word.isEmpty()) {
    			continue;
    		}
    		processedWordNumber++;
    		words.add(word);
    		lowerStemmedWords.add(stemmer.stem(word.toLowerCase()));
    		positions.add(startPosition++);    			
    		capitalizations.add(calculateCapitalization(word));
    	}

		// one-word
		for (int i = 0; i < words.size(); i++) {
			if (!couldIndexing(words.get(i))) {
				continue;
			}
			// add hit
			List<Hit> hits = oneWordToHits.get(lowerStemmedWords.get(i));
			Hit hit = null;
        	hit = new Hit();
        	hit.setPosition(positions.get(i));
        	hit.setCapitalization(capitalizations.get(i));
        	hit.setTextClassification(textClass);
    		hits.add(hit);
    		oneWordToHits.put(lowerStemmedWords.get(i), hits);    				
			// add word weight
		}
		// two-word
		for (int i = 0; i < words.size() - 1; i++) {
			if (!couldIndexing(words.get(i)) || !couldIndexing(words.get(i + 1))) {
				continue;
			}
			// add hit
			String key = lowerStemmedWords.get(i) + " " + lowerStemmedWords.get(i + 1);
			List<Hit> hits = twoWordToHits.get(key);
			Hit hit = new Hit();
			hit.setPosition(positions.get(i));
			hit.setCapitalization(Math.max(capitalizations.get(i), capitalizations.get(i + 1)));
			hit.setTextClassification(textClass);
			hits.add(hit);
			twoWordToHits.put(key, hits);
			// add word weight
		}
		return processedWordNumber;
	}
		
	// hit when the node is first seen
	@Override
    public void head(Node node, int depth) {
		if (depth == 0) {
			start(node);
		}
        if (node instanceof TextNode) {
        	String textClass = textClassGetter.calculateTextClassification(node);
        	curPosition += processText(((TextNode) node).text(), curPosition, textClass);
        }
	}

	@Override
	public void tail(Node node, int depth) {
		if (depth == 0) {
			finish();
		}
	}
	
	private void start(Node node) {
		// add meta and title to hits
		Document doc = (Document)node;
    	title = doc.select("head").select("title").text();
    	meta = "";
    	if (doc.select("meta[name=description]").size() > 0) {
    		meta += doc.select("meta[name=description]").get(0).attr("content");
    	}
    	if (doc.select("meta[name=keywords]").size() > 0) {
    		meta += " " + doc.select("meta[name=keywords]").get(0).attr("content");
    	}
		meta = meta.replace(',', ' ');
		if (!meta.isEmpty()) {
			processText(meta, 0, "meta");
		}
    	if (doc.select("p").size() >= 1) {
    		description = doc.select("p").get(0).text();
    	} else {
    		description = null;
    	}
	}
	
	private void finish() {
		// when the work done
		totalWordCount = curPosition;
		// process two word
		for( Iterator<Map.Entry<String, List<Hit>>> it = twoWordToHits.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, List<Hit>> entry = it.next();
			if (entry.getValue().size() < 2) {
				it.remove();
			} else {
			}
		}
		// combine one-word and two-word
		for (String word : oneWordToHits.keySet()) {
			wordToHits.put(word, oneWordToHits.get(word));
		}
		for (String word : twoWordToHits.keySet()) {
			wordToHits.put(word, twoWordToHits.get(word));
		}
		// calc term frequency
		wordToTermFrequency = calcTermFrequency(wordToHits);
		if (wordToTermFrequency == null) {
			validResult = false;
		}
	}
	
	Map<String, Double> calcTermFrequency(Map<String, List<Hit>> wordToHits) {
		Map<String, Double> map = new HashMap<>();
		double f_max = 0;
		for (String word : wordToHits.keySet()) {
			List<Hit> hits = wordToHits.get(word);
			double f_meta = 0;
			double f_highlight = 0;
			double f_body = 0;
			for (Hit hit : hits) {
				if (hit.getTextClassification() == null || hit.getTextClassification().equals("")) {
					// body
					if (hit.getCapitalization() == 0) {
						f_body += 1;
					} else if (hit.getCapitalization() == 1) {
						f_body += 1.5;
					} else if (hit.getCapitalization() == 2) {
						f_body += 2;
					} else {
						System.err.println("should no happend. hit.getCapitalization() = " + hit.getCapitalization());
						f_body += 1;
					}
				} else if (hit.getTextClassification().equals("meta")) {
					f_highlight += 3;
				} else if (hit.getTextClassification().equals("title")) {
					f_highlight += 3;
				} else if (hit.getTextClassification().equals("h1")) {
					f_highlight += 2;
				} else if (hit.getTextClassification().equals("h2")) {
					f_highlight += 2;
				} else if (hit.getTextClassification().equals("h3")) {
					f_highlight += 2;
				} else {
					System.err.println("should no happend. hit.getTextClassification() = " + hit.getTextClassification());
					f_body += 1;
				}
			}
			f_highlight = Math.min(f_highlight, f_body * 0.7);
			f_meta = Math.min(f_meta, f_body * 0.3);
			double f = f_highlight + f_meta + f_body;
			f_max = Math.max(f_max, f);
			map.put(word, f);
		}
		if (f_max < 0.1) {
			// if no word in body
			return null;
		}
		for (String word : map.keySet()) {
			double a = 0.5;
			double tf = (a + (1-a) * map.get(word)) / f_max;
			map.put(word, tf);
		}
		return map;
	}
		
	public static void sop(Object x) {
		System.out.println(x);		
	}
	
	private int calculateCapitalization(String word) {
		word = stemmer.removeSpecialCharacter(word);
		int cap = 0;
		if (word.length() > 0 && 'A' <= word.charAt(0) && word.charAt(0) <= 'Z') {
			cap = 1;
		}
		if (word.length() > 0 && word.equals(word.toUpperCase())) {
			cap = 2;
		}
		return cap;
	}
}

class TextClassGetter {
	private Map<String, Integer> textClassToWeight;
    private final int TEXT_CLASS_ITERATE_LENGTH = 2;

    TextClassGetter() {
    	textClassToWeight = new HashMap<>();
    	
    	textClassToWeight.put("title", 3);
    	textClassToWeight.put("h1", 2);
    	textClassToWeight.put("h2", 2);
    	textClassToWeight.put("h3", 2);
    	textClassToWeight.put("meta", 3);
    	
    }
    
    Integer getTextClassificationWeight(String textClassification) {
    	return textClassToWeight.get(textClassification);
    }
    
	String calculateTextClassification(Node node) {	// node should be a text node here
		for (int i = 0; i < TEXT_CLASS_ITERATE_LENGTH; i++) {
			node = node.parentNode();
			if (node == null) {
				return null;
			} else {
				if (textClassToWeight.containsKey(node.nodeName())) {
					return node.nodeName();
				}
			}
		}
		return null;
	}

}
