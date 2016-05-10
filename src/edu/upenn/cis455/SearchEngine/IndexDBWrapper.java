package edu.upenn.cis455.SearchEngine;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

public class IndexDBWrapper {
	
	public static void main(String[] args) {
		DynamoDBMapper mapper = new DynamoDBMapper(new AmazonDynamoDBClient(new ProfileCredentialsProvider()));
		// write to DB
		
		String word = "United States";
		String url = "https://www.usa.gov/about-the-us";
		AnchorItem item = new AnchorItem(word, url);
		mapper.save(item);
		// read DB
    	item = mapper.load(AnchorItem.class, "abc.com", "def.com"); // cloud to local - first partition, then sort key
    	mapper.save(item);
		// TODO Auto-generated method stub

	}

}
