package edu.upenn.cis455.SearchEngine;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class SearchWorker {
	
	public static String search (String query, String urlSearch) {
		
		String url = null;
		String result = "";
		
		try {
			url = urlSearch + URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	

}
