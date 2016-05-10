package edu.upenn.cis455.indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import edu.upenn.cis455.indexer.HitsItem;
import edu.upenn.cis455.indexer.PageAttributesItem;
import edu.upenn.cis455.indexer.RelevanceUrlItem;

class S3Connector {
	String urlQueueFile = "data/to_be_indexed_urls";
	BufferedReader urlQueueFileReader;
	int urlQueueIndex = 0;
	String urlQueueIndexFile = "data/url_queue_index";
	String urlContentS3BucketName = "newcrawler";
	String toBeIndexedUrlS3BucketName = "urlqueue";
	String toBeIndexedUrlS3Key = "";
	AmazonS3 s3client;
	static S3Connector instance = null;
	
	static S3Connector getInstance() {
		if (instance == null) {
			instance = new S3Connector();
		}
		return instance;
	}
	
	private S3Connector() {
		s3client = new AmazonS3Client(new ProfileCredentialsProvider());
	}

	void init() {
		// read url queue index from disk
		readUrlQueueIndex();
	    // if toBeIndexedUrlFile not exist, download some
	    File file = new File(urlQueueFile);
	    if (!file.exists()) {
		    System.out.println("no to_be_indexed_urls on local disk. prepare to download some on S3");
		    System.exit(1);
	    }
	    // seek to line number
		try {
			urlQueueFileReader = new BufferedReader(new FileReader(urlQueueFile));
			String line;
			for (int i = 0; i < urlQueueIndex; i++) {
				line = urlQueueFileReader.readLine();
				if (line == null) {
					// no more line. retrive more.
					urlQueueFileReader.close();
					System.out.println("no more urls on local disk. quit");
					System.exit(0);
//					retrieveUrlQueueFromS3();
					urlQueueFileReader = new BufferedReader(new FileReader(urlQueueFile));
					break;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("fail to seek to line number: " + urlQueueIndex + " in file: " + urlQueueFile);
			e.printStackTrace();
		}
	}
	
	void stop() {
		// save current line number
        writeUrlQueueIndex(urlQueueIndex);		
	}
	
	private void writeUrlQueueIndex(int urlQueueIndex) {
		this.urlQueueIndex = urlQueueIndex;
		FileWriter writer;
		try {
			writer = new FileWriter(urlQueueIndexFile);
			writer.write(String.valueOf(urlQueueIndex));
			writer.close();
		} catch (IOException e) {
			System.err.println("fail to write url queue index to disk!");
			e.printStackTrace();
			return;
		}	
	}

	private void readUrlQueueIndex() {
	    try {
			File file = new File(urlQueueIndexFile);
			if (!file.exists()) {
				writeUrlQueueIndex(0);
			}  else {
				Scanner sc = new Scanner(file);
				urlQueueIndex = sc.nextInt();
				sc.close();			
			}
		} catch (IOException e) {
			System.err.println("fail to open/create url queue index file");
			e.printStackTrace();
			return;
		}
	}

	synchronized String getUrl() {
		String line;
	    try {
			line = urlQueueFileReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		urlQueueIndex++;
		return line;
	}
	
	String getUrlContent(String url) {
		String bucketName = urlContentS3BucketName;
		String key = url;
		S3Object s3object = s3client.getObject(new GetObjectRequest(bucketName, key));
		try {
			String result = new Scanner(s3object.getObjectContent()).useDelimiter("\\A").next();
			if (result.length() > 5 * 1024 * 1024) {
				System.err.println("file too big! url: " + url);
				return null;
			}
			return result;			
		} catch (NoSuchElementException e) {
			return null;
		}
	}
}

class IdfLogger {
	Map<String, Integer> wordToPageCount = new HashMap<>();
	int pageCount = 0;
	String idfFile = "data/idf";
	
	private synchronized void readFromFile() {
	    try {
			File file = new File(idfFile);
			if (!file.exists()) {
				System.out.println("idf file no exist. won't change anything. ");
				return;
			}  else {
				Scanner sc = new Scanner(file);
				pageCount = Integer.valueOf(sc.nextLine());
				wordToPageCount = new HashMap<>();
				while (sc.hasNext()) {
					String word = sc.nextLine();
					Integer count = Integer.valueOf(sc.nextLine());
					wordToPageCount.put(word, count);
				}
				sc.close();			
			}
		} catch (IOException e) {
			System.err.println("fail to read from idf file");
			e.printStackTrace();
			return;
		}
	}
	
	private synchronized void writeToFile() {
		try {
			FileWriter writer = new FileWriter(idfFile);
			writer.write("" + pageCount + "\n");
			for (String word : wordToPageCount.keySet()) {
				writer.write(word + "\n");
				writer.write(wordToPageCount.get(word) + "\n");
			}
			writer.close();
		} catch (IOException e) {
			System.err.println("can not write to disk: " + idfFile);
			e.printStackTrace();
		}
	}
	
	synchronized void addIdf(Map<String, Integer> newWordToPageCount, int newPageCount) {
		readFromFile();
		for (String word : newWordToPageCount.keySet()) {
			Integer count = wordToPageCount.get(word);
			if (count == null) {
				count = 0;
			}
			count += newWordToPageCount.get(word);
			wordToPageCount.put(word, count);
		}
		pageCount += newPageCount;
		writeToFile();
	}
}
