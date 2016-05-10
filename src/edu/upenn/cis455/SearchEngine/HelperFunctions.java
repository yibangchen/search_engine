package edu.upenn.cis455.SearchEngine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.util.VersionInfoUtils;

import edu.upenn.cis455.indexer.HitsItem;
import edu.upenn.cis455.indexer.Indexer;
import edu.upenn.cis455.indexer.PageAttributesItem;

public class HelperFunctions {
	
	private static Indexer indexer = new Indexer();
	
	/**
	 * Reads the gives stream file from disk and returns content as String
	 * @param stream
	 * @return
	 * @throws FileNotFoundException
	 */
	public static String readFile(InputStream stream)
			throws FileNotFoundException{
		StringBuffer sb = new StringBuffer();
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		
		try{
			String read="";
			while((read = br.readLine())!=null){
				sb.append(read);
			}
			br.close();
		}catch(IOException ioe){
		}finally{
			if(br != null){
				try{
					br.close();
				}catch(IOException ioe){
				}
			}
		}
		
		return sb.toString();
	}
	
	public static Map<String, Double> readIdf(){
		long startTime = System.currentTimeMillis();
		Map<String, Double> terms = new HashMap<String, Double>();
		String fileName = "termidf";
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(fileName));
			
			String term = "";
			String line = br.readLine();
			boolean isIdf = true;
			try {
				double idf = Double.parseDouble(line);
			} catch (NumberFormatException e) {
				term = line.trim();
				isIdf = false;
			}
			
			while (line != null) {
				if (isIdf) {
					double idf = 0;
					try {
						idf = Double.parseDouble(line);
					} catch (NumberFormatException e) {
						idf = 0;
					}
					terms.put(term, idf);
					isIdf = false;
				} else {
					term = line.trim();
					isIdf = true;
				}
				
				line = br.readLine();
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) { // for readLine()
			e.printStackTrace();
			return terms;
		}
		
		System.out.print("idf_term_count:" + terms.size());
		long endTime = System.currentTimeMillis();
		System.out.println((endTime - startTime) / 1000.0);
		
		return terms;
	}
	
	public static Map<String, Double> readPageRank(){
		long startTime = System.currentTimeMillis();
		Map<String, Double> rankScores = new HashMap<String, Double>();
		String fileName = "pagerank";
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(fileName));
			
			String url = "";
			String line = br.readLine();
			boolean isRank = true;
			try {
				double pr = Double.parseDouble(line);
			} catch (NumberFormatException e) {
				url = line.trim();
				isRank = false;
			}
			
			while (line != null) {
				if (isRank) {
					double score = 0;
					try {
						score = Double.parseDouble(line);
					} catch (NumberFormatException e) {
						score = 0;
					}
					rankScores.put(url, score);
					isRank = false;
				} else {
					url = line.trim();
					isRank = true;
				}
				
				line = br.readLine();
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) { // for readLine()
			e.printStackTrace();
			return rankScores;
		}
		
		System.out.print("pagerank_url_count:" + rankScores.size());
		long endTime = System.currentTimeMillis();
		System.out.println((endTime - startTime) / 1000.0);
		
		return rankScores;
	}
	
	/**
	 * This function calculates the dot product of 2 vectors
	 * @param a
	 * @param b
	 * @return
	 */
	public static double dotProd(double[] a, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("The dimensions have to be equal!");
		}
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i] * b[i];
		}
		
		return sum;
	}
	
	/**
	 * This method cleans up the query
	 * @param query
	 * @return
	 */
	public static String filter(String query) {
		if (query == null) return null;
		query = query.replaceAll("\\W+", " ");
		query = query.toLowerCase();
		query = query.trim();
		if(query.length() == 0){
			return null;
		} else {
			return query;
		}
	}
	
	public static List<String> getStemmedTerms(String query) {
		Stemmer stem = new Stemmer();
		query = stem.removeSpecialCharacter(query);
		List<String> termList = new ArrayList<String>();
		List<String> resultList = new ArrayList<String>();
		String[] ls = query.toLowerCase().split("\\s+");
		
		for (String term: ls) {
			term = stem.removeSpecialCharacter(term);
			termList.add(stem.stem(term));
		}
		
		for (String term : termList) {
			if (! stem.isStopWord(term)) {
				resultList.add(term);
			}
		}

		for (int i = 1; i < termList.size(); i++) {
			String comboTerm = termList.get(i-1) + " " + termList.get(i);
			resultList.add(comboTerm);
		}
		
		return resultList;
	}
	
	public static Set<String> getSingleTerms(String query) {
		Stemmer stem = new Stemmer();
		query = stem.removeSpecialCharacter(query);
		Set<String> termList = new HashSet<String>();
		String[] ls = query.toLowerCase().split("\\s+");
		
		for (String term: ls) {
			term = stem.removeSpecialCharacter(term);
//			System.out.println("term " + term);
			if (term.equals("")) {
				continue;
			}
			if (! stem.isStopWord(term))
				termList.add(stem.stem(term));
		}
		
		return termList;
	}
	
	/**
	 * This method limits the number of terms to search by 6 under several conditions
	 * @param totalTerms
	 * @return
	 */
	public static int findTermCount (int totalTerms) {
		int numTerms = totalTerms *6/10;
		numTerms = Math.max(3, numTerms);
		if (totalTerms < 4) numTerms = totalTerms;
		if (numTerms > 6) return 6;
		
		return numTerms; 
	}
	
	public static String getRootUrl (String rawUrl) {
		String delim = "#";
		return rawUrl.split(delim)[0];
	}
	
	
	public static List<String> getTermDocs(String term, int numOfSites) {
		return indexer.getRelevantUrls(term, numOfSites);
	}
	
	public static double getTF (String term, String docUrl) {
		HitsItem item = indexer.getHitsItem(term, docUrl);
		if (item == null) return 0;
		return item.getTf();
	}

	/**
	 * This method gets the title of a document's URL from database
	 * @param docUrl
	 * @return
	 */
	public static String getTitleDescription (String docUrl) {
		PageAttributesItem item = indexer.getPageAttributesItem(docUrl);
		String titleDesc = "";
		try {
			titleDesc += item.getTitle().trim();
		} catch (Exception e) {}
		titleDesc += " \n";
		try {
			titleDesc += item.getDescription().trim();
		} catch (Exception e) {}
		titleDesc += " ";
		return titleDesc;
	}
	
	/**
	 * This method converts a raw url to a search key in the page rank database
	 * @param url
	 * @return
	 */
	public static String convertPRKey(String url) {
		try {
			String[] tokens = url.toLowerCase().split("/");
			String site = tokens[2].replaceAll("www\\.", "");
			site = site.replaceAll("[\\?#:].*", "");
			tokens = site.split("\\.");
			int len = tokens.length;
			if (len < 2) {
				return "";
			} else if (len < 3) {
				return site;
			} else {
				if (tokens[len - 2].matches("^(com)|(edu)|(org)|(gov)|(net)|(mil)|(int)|(info)|(biz)$")) {
					/* the second last is normal */
					return tokens[len - 3] + "." + tokens[len - 2];
				} else {
					/* the second last is not normal */
					if (tokens[len - 2].length() < 3 && tokens[len - 1].length() < 3) {
						return tokens[len - 3] + "." + tokens[len - 2] + "." + tokens[len - 1];
					} else {
						return tokens[len - 2] + "." + tokens[len - 1];
					}
				}
			}
		} catch (Exception e) {
			return "";
		}
	}
}
