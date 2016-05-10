package edu.upenn.cis455.SearchEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.upenn.cis455.indexer.DynamodbConnector;
import edu.upenn.cis455.indexer.Hit;
import edu.upenn.cis455.indexer.HitsItem;

/**
 * Servlet implementation class PageRenderer
 */
public class PageRenderer extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final int NUM_PER_PAGE = 10;
	private static final int NUM_NAV_PAGES = 10;
	private static final int MAX_TERMED_SITES = 1000;
	private static final double IDF_THRESHOLD = 10;

	private static List<UrlObject> rankedUrlList = new ArrayList<UrlObject>();
	private static Map<String, Double> termIdf = new HashMap<String, Double>();
	private static Map<String, Double> pageRankScores = new HashMap<String, Double>();

	private static int totalResults = 0;
	private String userQuery;
	private double totalTime = 0;

	private DynamodbConnector dynamodbConnector = new DynamodbConnector();
	private boolean isReady = true;

	/**
	 * Reversely ranked object
	 * 
	 * @author user
	 * 
	 */
	class UrlObject implements Comparable<UrlObject> {
		String urlString;
		double relevanceScore = 0;

		public UrlObject() {
		}

		public UrlObject(String urlString, double indexScore, double pageRank, double factor) {
			this.urlString = urlString;
			relevanceScore = factor * indexScore * pageRank/ Math.sqrt(urlString.length());
		}

		@Override
		public int compareTo(UrlObject o) {
			if (this.relevanceScore < o.relevanceScore)
				return 1;
			else if (this.relevanceScore > o.relevanceScore)
				return -1;
			else
				return 0;
		}
	}

	/**
	 * Reversely ranked object
	 * 
	 * @author user
	 * 
	 */
	class queryTermIdf implements Comparable<queryTermIdf> {
		String term;
		double idf;

		queryTermIdf(String term, double idf) {
			this.term = term;
			this.idf = idf;
		}

		@Override
		public int compareTo(queryTermIdf o) {
			if (this.idf < o.idf)
				return 1;
			else if (this.idf > o.idf)
				return -1;
			return 0;
		}
	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public PageRenderer() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void init() throws ServletException {
		intiJob();
	}
	
	private void intiJob() {
		termIdf = HelperFunctions.readIdf();		
		pageRankScores = HelperFunctions.readPageRank();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		int pageNum = 1;
		String currQuery = request.getParameter("searchquery");

		if (currQuery == null)
			response.sendRedirect("/interface");
		else if (currQuery.equals(""))
			response.sendRedirect("/interface");
		else if (!currQuery.equals(this.userQuery)) {
			
			searchNewTerm(currQuery);
			
		} else {
		
			String currentPage = request.getParameter("currentpage");
			try {
				pageNum = Integer.parseInt(currentPage);
			} catch (NumberFormatException e) {
			}
		}

		response.setContentType("text/html");
		PrintWriter writer = response.getWriter();
		InputStream stream = getServletContext().getResourceAsStream(
				"/ResultPage.html");
		String data = HelperFunctions.readFile(stream);

		String placeHolder = "%%REPLACE QUERY HERE%%";
		data = data.replace(placeHolder, userQuery);

		placeHolder = "%%%RESULT COUNT%%%";
		data = data.replace(placeHolder, Integer.toString(totalResults));

		placeHolder = "%%%SEARCH TIME%%%";
		data = data.replace(placeHolder, Double.toString(totalTime));

		placeHolder = "%%REPLACE PAGE DIR%%";
		data = data.replace(placeHolder, generatePageDir(pageNum));

		placeHolder = "%%REPLACE RESULTS HERE%%";
		data = data.replace(placeHolder, generateResults(pageNum));

		writer.write(data);
		writer.close();

		for (int i = 0; i < 10; i++) {
			System.out.println(this.rankedUrlList.get(i).relevanceScore + " : "
					+ this.rankedUrlList.get(i).urlString);
		}
		isReady = true;
	}

	public int searchNewTerm(String newQuery) {
		
		userQuery = newQuery;

		System.out.println("query:" + userQuery);
		long startTime = System.currentTimeMillis();

		rankedUrlList = new ArrayList<UrlObject>();
		findMatchedDocs(userQuery);

		System.out.println("renderable_pages:" + rankedUrlList.size());

		long endTime = System.currentTimeMillis();
		totalTime = (endTime - startTime) / 1000.0;
		System.out.println("search_time:" + totalTime);
		
		return rankedUrlList.size();
	}

	/**
	 * METHOD TESTED
	 * 
	 * @param currentPage
	 * @return
	 */
	private String generatePageDir(int currentPage) {

		String linkHtml = "";
		int startIndex = Math.max(1, currentPage - NUM_NAV_PAGES / 2 + 1);

		for (int i = startIndex; i < startIndex + NUM_PER_PAGE; i++) {
			if ((i - 1) * NUM_PER_PAGE < totalResults)
				linkHtml += "<a href=\"results?currentpage=" + i
						+ "&searchquery=" + userQuery + "\">" + i + "</a>";
		}

		if ((startIndex + NUM_NAV_PAGES - 1) * NUM_PER_PAGE < totalResults)
			linkHtml += "<a href=\"results?currentpage=" + startIndex
					+ NUM_NAV_PAGES + "&searchquery=" + userQuery
					+ "\">......</a>";

		return linkHtml;
	}

	/**
	 * METHOD TESTED
	 * 
	 * @param currentPage
	 * @return
	 */
	private String generateResults(int currentPage) {
		String content = "";

		int numResults;
		if (totalResults > currentPage * NUM_PER_PAGE) {
			numResults = NUM_PER_PAGE;
		} else
			numResults = totalResults - (currentPage - 1) * NUM_PER_PAGE;

		for (int i = 0; i < numResults; i++) {
			int index = (currentPage - 1) * NUM_PER_PAGE + i;
			String urlString = rankedUrlList.get(index).urlString;
			String titleDesc = HelperFunctions.getTitleDescription(urlString);
			String title = titleDesc.split("\n")[0].trim();
			if (title.equals(""))
				title = urlString;
			String description = titleDesc.split("\n")[1].trim();

			content += "<div class=\"row\" id=\"result5\">"
					+ "<div class=\"col-md-1 space\"></div>"
					+ "<div class=\"col-md-6 content\">" + "<a href=\""
					+ urlString + "\"><p><b>" + title + "</b></p></a>" + "<p>"
					+ description + "</p>" + "<a href=\"" + urlString + "\">"
					+ rankedUrlList.get(index).urlString + "</a>"
					+ "</div></div>";
		}

		return content;
	}

	/**
	 * NO duplicate term here!!
	 * 
	 * @param query
	 * @return
	 */
	public void findMatchedDocs(String query) {
		List<String> termList = HelperFunctions.getStemmedTerms(query); // With
																		// duplicates!!
		Set<String> singleTermList = HelperFunctions.getSingleTerms(query); // NO
																			// duplicates!!
		if (singleTermList.size() == 0)
			return;
		Map<String, Integer> queryTermFreq = new HashMap<String, Integer>();
		Map<String, Double> queryTermWeight = new HashMap<String, Double>();
		Map<String, Double> idfSubset = new HashMap<String, Double>();
		double cons = 0.5;
		int maxFreq = 0;

		long startTime = System.currentTimeMillis();
		for (String term : termList) {
			Integer freq = queryTermFreq.get(term);
			if (freq == null)
				freq = 0;
			if (++freq > maxFreq)
				maxFreq = freq;
			queryTermFreq.put(term, freq);
		}

		for (String term : queryTermFreq.keySet()) {
			int freq = queryTermFreq.get(term);

			double idf = 0;
			try {
				idf = termIdf.get(term); // DOUBLE CHECK
			} catch (Exception e) {
			}

			double weight = cons + (1 - cons) * freq / maxFreq * idf;
			queryTermWeight.put(term, weight);
			idfSubset.put(term, idf);
		}

		long endTime = System.currentTimeMillis();
		System.out.println("*Time for QUERY term frequency: "
				+ (endTime - startTime) / 1000.0);
		
		List<String> rankedTerms = getEssentialTerms(termList);
		if (rankedTerms.size() == 0) return;

		rankDocuments(rankedTerms, singleTermList, idfSubset,
				queryTermWeight);
	}

	/**
	 * 
	 * @param rankedTerms
	 *            : list of terms ranked by idf, descending
	 * @param idfSubset
	 *            : ALL distinct terms and their idf scores
	 */
	private void rankDocuments(List<String> rankedTerms,
			Set<String> singleTerms, Map<String, Double> idfSubset,
			Map<String, Double> queryTermWeight) {
		int numTerms = HelperFunctions.findTermCount(rankedTerms.size());
		Set<String> stagedDocs = new HashSet<String>();

		long startTime = System.currentTimeMillis();
		for (int i = 0; i < numTerms; i++) {
			List<String> docUrls = HelperFunctions.getTermDocs(
					rankedTerms.get(i), 6 / numTerms * MAX_TERMED_SITES);
			for (String url : docUrls) {
				stagedDocs.add(HelperFunctions.getRootUrl(url));
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("stagging_doc_count:" + stagedDocs.size() + "\nstaging_time:"
				+ (endTime - startTime) / 1000.0);

		startTime = System.currentTimeMillis();

		List<String> allTerms = new ArrayList<>();
		allTerms.addAll(queryTermWeight.keySet());

		Map<String, Double> urlScore = new HashMap<>();

		Map<String, Map<String, HitsItem>> wordToUrlToHits = getHitsItem(
				allTerms, stagedDocs);
		for (String url : stagedDocs) {
			// check if all single terms exist
			boolean flag = true;
			for (String term : singleTerms) {
				if (!wordToUrlToHits.get(term).containsKey(url)) {
					flag = false;
					break;
				}
			}
			// calculate score
			if (flag == true) {
				Double score = calcIndexingScore(url, queryTermWeight,
						wordToUrlToHits);
				urlScore.put(url, score);
			}

		}

		Map<String, Double> boosters = getBoosterScores(wordToUrlToHits, rankedTerms);
		
		for (String docUrl : urlScore.keySet()) {
			double pageRank = this.getPageRank(docUrl);
			UrlObject newMatch = new UrlObject(docUrl, urlScore.get(docUrl), pageRank, boosters.get(docUrl));
			rankedUrlList.add(newMatch);
		}

		endTime = System.currentTimeMillis();
		System.out.println("filtering_and_scoring_time:"
				+ (endTime - startTime) / 1000.0);

		startTime = System.currentTimeMillis();
		Collections.sort(rankedUrlList);
		totalResults = rankedUrlList.size();

		endTime = System.currentTimeMillis();
		System.out.println("sorting_score_time:"
				+ (endTime - startTime) / 1000.0);
	}
	
	private Map<String, Double> getBoosterScores (Map<String, Map<String, HitsItem>> wordUrlHits, List<String> rankedTerms) {
		Map<String, Double> boosterFactor = new HashMap<String, Double>();
		
		for (int i = 0; i < rankedTerms.size(); i++) {
			double factor = Math.sqrt(Math.sqrt(rankedTerms.size() - i)) * 1.2;
			String term = rankedTerms.get(i);
			Map<String, HitsItem> urlHits = wordUrlHits.get(term);
			for (String url : urlHits.keySet()) {
				Double booster = boosterFactor.get(url);
				if (booster == null) booster =1.0;
				List<Hit> hits = urlHits.get(url).getHits();
				for (Hit h : hits) {
					String tag = h.getTextClassification();
					if (tag != null && tag.equals("meta")) booster *= factor;
				}
				handle url containing the term
				String newUrl = url.replace("-", "");
				if (newUrl.contains(term.replace(" ", ""))) booster *= factor;
				boosterFactor.put(url, booster);
			}
		}
		return boosterFactor;
	}

	private Map<String, Map<String, HitsItem>> getHitsItem(List<String> terms,
			Set<String> urls) {
		Hashtable<String, Map<String, HitsItem>> wordToUrlToHits = new Hashtable<>();
		List<Thread> threadList = new ArrayList<Thread>();
		for (int i = 0; i < terms.size(); i++) {
			Thread thread = new QueryThread(wordToUrlToHits, terms.get(i), urls);
			threadList.add(thread);
			thread.start();
		}

		try {
			for (int i = 0; i < terms.size(); i++) {
				threadList.get(i).join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return wordToUrlToHits;
	}

	private double calcIndexingScore(String url,
			Map<String, Double> queryTermWeight,
			Map<String, Map<String, HitsItem>> wordToUrlToHits) {
		double score = 0.0;
		for (String term : queryTermWeight.keySet()) {
			if (wordToUrlToHits.get(term).containsKey(url)) {
				Double idf = this.termIdf.get(term);
				if (idf == null) {
					continue;
				}
				score += idf * wordToUrlToHits.get(term).get(url).getTf()
						* queryTermWeight.get(term);
			}
		}
		return score;
	}

	/**
	 * This method extract query terms and rank them by their idf - invalid idf
	 * terms are ignored - NO DUPLICATE RESULTS
	 * 
	 * @param rawQuery
	 * @return
	 */
	private List<String> getEssentialTerms(List<String> termList) {
		Set<String> duplicateCheck = new HashSet<String>();
		List<queryTermIdf> idfTerms = new ArrayList<queryTermIdf>();
		List<String> rankedTerms = new ArrayList<String>();

		for (String term : termList) {
			if (duplicateCheck.contains(term))
				continue;
			else
				duplicateCheck.add(term);

			double idf = 0;
			try {
				idf = termIdf.get(term); // DOUBLE CHECK
			} catch (Exception e) {
			}

			if (idf < this.IDF_THRESHOLD) {
				if (termIdf.get(term) != null) {
					queryTermIdf idfObj = new queryTermIdf(term,
							termIdf.get(term));
					idfTerms.add(idfObj);
				}
			}
		}

		Collections.sort(idfTerms);
		for (queryTermIdf obj : idfTerms) {
			rankedTerms.add(obj.term);
		}
		System.out.println("essential_terms:" + rankedTerms.size());

		return rankedTerms;
	}

	private boolean isUrlMatched(String url, Set<String> matchTerms) {
		for (String term : matchTerms) {
			if (HelperFunctions.getTF(term, url) == 0)
				return false; // IF term is not found in the url's document,
								// FALSE
		}
		return true;
	}

	/**
	 * This method queries the page rank database for an url
	 * 
	 * @param url
	 * @return
	 */
	private double getPageRank(String docUrl) {
		docUrl = HelperFunctions.convertPRKey(docUrl);
		Double score = pageRankScores.get(docUrl);
		if (score == null || score == 0.0)
			score = 0.75;
		return Math.log10(score) + 0.15 + 10;
	}

	class QueryThread extends Thread {

		Hashtable<String, Map<String, HitsItem>> wordToUrlToHits;
		String term;
		Set<String> urls;

		public QueryThread(
				Hashtable<String, Map<String, HitsItem>> wordToUrlToHits,
				String term, Set<String> urls) {
			this.wordToUrlToHits = wordToUrlToHits;
			this.term = term;
			this.urls = urls;
		}

		public void run() {
//			System.out.println("QueryThread starts running.");
			
			List<HitsItem> items = new ArrayList<>();
			for (String url : urls) {
				HitsItem item = new HitsItem();
				item.setWord(term);
				item.setUrl(url);
				items.add(item);
			}
			
			long startTime = System.currentTimeMillis();
			
			// send query
			Map<String, List<Object>> map = dynamodbConnector.mapper
					.batchLoad(items);
			List<Object> results = map.get("Hits");
			
			long midTime = System.currentTimeMillis();
			
			// url -> HitsItem
			Map<String, HitsItem> urlToHits = new HashMap<>();
			for (Object object : results) {
				HitsItem item = (HitsItem) object;
				urlToHits.put(item.getUrl(), item);
			}
			wordToUrlToHits.put(term, urlToHits);
			
			long endTime = System.currentTimeMillis();
			
			double time1 = (midTime - startTime)/1000.0;
			double time2 = (endTime - midTime)/1000.0;
			System.out.println(this.currentThread().getName() + " " + term + " with batch time " +  time1 + " with url -> HitsItem " + time2);
		}
	}

}
