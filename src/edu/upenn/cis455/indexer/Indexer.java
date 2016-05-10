package edu.upenn.cis455.indexer;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;


class Debug {
    static final boolean debug = false;
    static final boolean dynamodbOpen = true;
    static final boolean printEmptyContentUrl = true;
}

public class Indexer {
	DynamodbConnector dynamodbConnector = new DynamodbConnector();
	IndexingPerformer indexingPerformer = new IndexingPerformer(dynamodbConnector);
	
	public List<String> getRelevantUrls(String word, int atMost) {
		return dynamodbConnector.getRelevantUrls(word, atMost);
	}
	
	public HitsItem getHitsItem(String word, String url) {
		return dynamodbConnector.getHitsItem(word, url);
	}
	
	public PageAttributesItem getPageAttributesItem(String url) {
		return dynamodbConnector.getPageAttributesItem(url);
	}	

	public void startIndexing() {
		System.out.println("about to start");
		indexingPerformer.start();
	}
	
	public void stopIndexing() {
		indexingPerformer.stop();
		dynamodbConnector.flush();
		System.out.println("finished");
	}
	
	public void getIndexingStatus() {
		
	}

	public void clearTable() {
		System.out.println("Are you sure you want to clear all dynamodb tables? enter yes to confirm: ");
		Scanner scanner = new Scanner(System.in);
		String input = scanner.nextLine();
		if (input.equals("yes")) {
			System.out.println("clearing...");
			dynamodbConnector.clearTable();
			System.out.println("done");
		} else {
			System.out.println("canceled");
		}
	}
}



class IndexingPerformer {
	final int THREAD_NUMBER = 20;
	List<IndexingPerformerThread> indexingPerformerThreads = new ArrayList<>();
	DynamodbConnector dynamodbConnector;
	S3Connector s3Connector = S3Connector.getInstance();
	int totalIndexingPageCount = 0;
	IdfLogger idfLogger = new IdfLogger();
	
	public IndexingPerformer(DynamodbConnector dynamodbConnector) {
		this.dynamodbConnector = dynamodbConnector;
	}
	
	static void sop(Object x) {
		System.out.println(x);
	}
	
	void start() {
		s3Connector.init();
		indexingPerformerThreads = new ArrayList<>();
		while (indexingPerformerThreads.size() < THREAD_NUMBER) {
			indexingPerformerThreads.add(new IndexingPerformerThread());
		}
		
		for (IndexingPerformerThread thread : indexingPerformerThreads) {
			thread.isRunning = true;
			thread.start();
		}
	}
	
	void stop() {
		for (IndexingPerformerThread thread : indexingPerformerThreads) {
			thread.isRunning = false;
		}
		for (IndexingPerformerThread thread : indexingPerformerThreads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}
		s3Connector.stop();
	}
		
	String getUrl() {
		return s3Connector.getUrl();
	}
	
	String getUrlContent(String url) {
		return s3Connector.getUrlContent(url);
	}
	
	class IndexingPerformerThread extends Thread {
		DynamodbConnector dynamodbConnector = new DynamodbConnector();
		public boolean isRunning;
		Map<String, Integer> idfCount = new HashMap<>();
		int idfPageCount = 0;
		static final int MAX_URL_LENGTH = 255;
		
		@Override
		public void run() {
			while(isRunning) {
				// get a url
				String url = getUrl();
				if (url == null) {
					isRunning = false;
					sop("indexing done");
					continue;
				} else if (url.length() > MAX_URL_LENGTH) {
					sop("url too long: " + url);
					continue;
				}
				// get the content
				String content;
				content = getUrlContent(url);
				if (content == null) {
					if (Debug.printEmptyContentUrl) {
						System.out.println("empty inputstream from s3. url: " + url);
					}
					continue;
				}
				// parse and save
				HitsVisitor visitor = parseContent(url, content);
				if (visitor.validResult) {
					saveToDynamodb(url, visitor, Stemmer.hash(content));					
				}
			}
			// save idf
			idfLogger.addIdf(idfCount, idfPageCount);
			// flush 
			dynamodbConnector.flush();
		}
				
		HitsVisitor parseContent(String url, String content) {
			sop("current time: " + System.currentTimeMillis());
			sop("have saved " + dynamodbConnector.writtenItemCount + " items to dynamodb");
			sop("parsing " + totalIndexingPageCount++);
			// parse
			Document doc = Jsoup.parse(content);
			HitsVisitor visitor = new HitsVisitor();
	        NodeTraversor traversor = new NodeTraversor(visitor);
	        traversor.traverse(doc);
	        return visitor;
		}	

		void saveToDynamodb(String url, HitsVisitor visitor, String sha1) {
	        Set<String> words = visitor.wordToHits.keySet();
	        Map<String, List<Hit>> wordToHits = visitor.wordToHits;
	        Map<String, Double> wordToTermFrequency = visitor.wordToTermFrequency;
	        String title = visitor.title;
	        String meta = visitor.meta;
	        String description = visitor.description;
	        int totalWordCount = visitor.totalWordCount;
	        
	        if (words.isEmpty()) {
	        	if (Debug.printEmptyContentUrl) {
		        	sop("empty file, no word to save. skip. " + url);	        		
	        	}
	        	return;
	        }
	        // iterate every word
	        for (String word : words) {
	        	// save to table RelevanceUrl
	        	RelevanceUrlItem relevanceUrlItem = new RelevanceUrlItem();
	        	Double tf = null;
	        	tf = wordToTermFrequency.get(word);
	        	relevanceUrlItem.setWord(word);
	        	relevanceUrlItem.setUrl(url, tf);
	        	dynamodbConnector.save(relevanceUrlItem);
	        	// save to table Hits
	        	HitsItem hitsItem = new HitsItem();
	        	hitsItem.setWord(word);
	        	hitsItem.setUrl(url);
	        	hitsItem.setHits(wordToHits.get(word));
	        	hitsItem.setTf(wordToTermFrequency.get(word));
	        	dynamodbConnector.save(hitsItem);
	        }
        	// save to table PageAttributes
        	PageAttributesItem pageAttributesItem = new PageAttributesItem();
        	pageAttributesItem.setUrl(url);
        	pageAttributesItem.setTitle(title);
        	pageAttributesItem.setMeta(meta);
        	pageAttributesItem.setWordCount(totalWordCount);
        	pageAttributesItem.setDescription(description);
        	pageAttributesItem.setMaxFrequency(visitor.getMaxFrequency());
        	pageAttributesItem.setHash(sha1);
        	dynamodbConnector.save(pageAttributesItem);
        	// save idf
        	for (String word : words) {
            	idfCount.put(word, idfCount.get(word));        		
        	}
        	idfPageCount++;
		}
	}
}
