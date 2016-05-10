package edu.upenn.cis455.SearchEngine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import javax.servlet.ServletException;

public class Tester {
	
	public static void main(String args[]) throws ServletException {
		analyzeLog();
	}
	
	private static void analyzeLog() {
		String fileName = "speed_analysis_log";
		BufferedReader br;
		List<Integer> wordStagingCounts = new ArrayList<Integer>();
		List<Double> wordStagingTime = new ArrayList<Double>();
		List<Double> wordProcessingTime = new ArrayList<Double>();
		List<Integer> finalPageCount = new ArrayList<Integer>();
		List<Double> searchTime = new ArrayList<Double>();
		
		int maxResult = 0, minResult = 99999999, zeroCounts = 0, totalCount = 0;
		double maxTime = 0, minTime = 9999999;
		
		boolean isValid = false;
		try {
			br = new BufferedReader(new FileReader(fileName));
			isValid = false;
			
			String line = "";
			String query = "";
			do {
				line = br.readLine();
				String key = line.split(":")[0];
				if (key.equals("query")) {
					query = line.split(":")[1];
					if (query.split(" ").length > 3) {
						totalCount++;
						isValid = true;
					} else {
						isValid = false;
					}
				}
				if (isValid) {
					if (key.equals("stagging_doc_count")){
						wordStagingCounts.add(Integer.parseInt(line.split(":")[1]));
					} else if (key.equals("staging_time")) {
						wordStagingTime.add(Double.parseDouble(line.split(":")[1]));
					} else if (key.equals("filtering_and_scoring_time")) {
						wordProcessingTime.add(Double.parseDouble(line.split(":")[1]));
					} else if (key.equals("renderable_pages")) {
						int count = Integer.parseInt(line.split(":")[1]);
						finalPageCount.add(count);
						if (count > maxResult) maxResult = count;
						if (count < minResult) minResult = count;
						if (count == 0) {
							System.out.println(query);
							zeroCounts++;
						}
					} else if (key.equals("search_time")) {
						double t = Double.parseDouble(line.split(":")[1]);
						searchTime.add(t);
						if (maxTime < t) maxTime = t;
						if (minTime > t) minTime = t;
					} 
				}
				
			} while (line != null);
			
		} catch (Exception e) {
		}
		
		OptionalDouble avg1 = wordStagingCounts.stream().mapToDouble(a -> a).average();
		OptionalDouble avg2 = wordStagingTime.stream().mapToDouble(a -> a).average();
		OptionalDouble avg3 = wordProcessingTime.stream().mapToDouble(a -> a).average();
		OptionalDouble avg4 = finalPageCount.stream().mapToDouble(a -> a).average();
		OptionalDouble avg5 = searchTime.stream().mapToDouble(a -> a).average();
		
		System.out.println("avg_staging_count:" + avg1.getAsDouble());
		System.out.println("avg_staging_time:" + avg2.getAsDouble());
		System.out.println("avg_processing_time:" + avg3.getAsDouble());
		System.out.println("avg_result_count:" + avg4.getAsDouble());
		System.out.println("avg_search_time:" + avg5.getAsDouble());
		
		System.out.println("max_result:" + maxResult);
		System.out.println("min_result:" + minResult);
		System.out.println("max_time:" + maxTime);
		System.out.println("min_time:" + minTime);
		System.out.println("total_count:" + totalCount);
		System.out.println("zero_counts:" + zeroCounts);
		
	}
	
	private static void generateLog() throws ServletException {
		List<String> termList100 = new ArrayList<String>();
		String fileName = "googletrend2015";
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(fileName));
			
			String line = "";
			do {
				line = br.readLine();
				if (line.length() > 1) {
					termList100.add(line.substring(1));
				}
			} while (line != null);
			
		} catch (Exception e) {
		}
		System.out.println("terms_count:" + termList100.size());
		
		PageRenderer testPR = new PageRenderer();
		testPR.init();
		
		for (String term : termList100) {
			try {
				testPR.searchNewTerm(term);
			} catch (Exception e) {
				System.out.println("ErrorOnTerm:" + term);
				continue;
			}
		}
	}
	
}
