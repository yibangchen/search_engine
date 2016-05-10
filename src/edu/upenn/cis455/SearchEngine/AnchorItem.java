package edu.upenn.cis455.SearchEngine;

import java.util.List;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.JsonMarshaller;

// credential in .aws folder

@DynamoDBTable(tableName="InvertedIndex")
public class AnchorItem {
	private String Word;
	private String Url; 
	
	public AnchorItem(String word, String url) {
		this.setWord(word);
		this.setUrl(url);
	}

	@DynamoDBIndexHashKey(attributeName="Word")  
	public String getWord() {
		return Word;
	}

	public void setWord(String word) {
		this.Word = word;
	}
	
	// sort key
	@DynamoDBIndexRangeKey(attributeName="Url")  
	public String getUrl() {
		return Url;
	}
	
	public void setUrl(String url) {
		this.Url = url;
	}
	
	
}

