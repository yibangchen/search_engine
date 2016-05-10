package edu.upenn.cis455.SearchEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import twitter4j.Trend;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Servlet implementation class SearchInterface
 */
public class SearchInterface extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	@Override
	public void init() {
		
	}

    /**
     * Default constructor. 
     */
    public SearchInterface() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html");
		PrintWriter writer = response.getWriter();
		InputStream stream = getServletContext().getResourceAsStream("/HomePage.html");
		String data = HelperFunctions.readFile(stream);
		System.out.println(data);
		String placeHolder = "%%REPLACE TERMS HERE%%";
		data = data.replace(placeHolder, getQueryString());

		writer.write(data);
		writer.close();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
	private String getQueryString() {
		List<String> list = getTrendingWords();
		String query = "";
		for (String term : list) {
			query += "<a href=\"results?searchquery=" + term + "\">" + term + "</a>, ";
		}
		
		return query;
	}
	
	private List<String> getTrendingWords() {
		final String TWITTER_CONSUMER_KEY = "hPIXkUifHhR6GMAqddWTLrd4z";
		final String TWITTER_SECRET_KEY = "cLuVH1jsBzYTCyG23BuY6aX1jR0vqN5yst4QvDjizCGEkhyHl1";
		final String TWITTER_ACCESS_TOKEN = "728484720393244672-9WywYMHsALVKDcesvfJ8e3sGZ3245yP";
		final String TWITTER_ACCESS_TOKEN_SECRET = "T7bv8644feyxfOLv2g57LwZg4tZddnCO3DdrVKA4d0OQ1";

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(TWITTER_CONSUMER_KEY)
				.setOAuthConsumerSecret(TWITTER_SECRET_KEY)
				.setOAuthAccessToken(TWITTER_ACCESS_TOKEN)
				.setOAuthAccessTokenSecret(TWITTER_ACCESS_TOKEN_SECRET);

		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();

		try {
			Trend[] trends = twitter.trends().getPlaceTrends(2471217)
					.getTrends();
			List<String> trendList = new ArrayList<String>();
			if (trends == null) {
				return trendList;
			}

			for (Trend t : trends) {
				if (!t.getName().matches("^#.*")) {
					trendList.add(t.getName());
				}
			}
			long seed = System.nanoTime();
			Collections.shuffle(trendList, new Random(seed));

			return trendList.subList(0, 5 > trendList.size() ? trendList.size()
					: 5);
		} catch (TwitterException e) {
			e.printStackTrace();
			return new ArrayList<String>();
		}
	}



}
