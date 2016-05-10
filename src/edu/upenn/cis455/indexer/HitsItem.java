package edu.upenn.cis455.indexer;

import java.util.List;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;


@DynamoDBTable(tableName="Hits")
public class HitsItem {
	private String word;
	private String url;
	private List<Hit> hits;
	private Double tf;
        
    @DynamoDBHashKey(attributeName="WORD")  
    public String getWord() { return word;}
    public void setWord(String word) {this.word = word;}
    
    @DynamoDBRangeKey(attributeName="URL")  
    public String getUrl() {return url; }
    public void setUrl(String url) { this.url = url; }
    
    @DynamoDBAttribute(attributeName = "HITS")
    public List<Hit> getHits() { return hits; }    
    public void setHits(List<Hit> hits) { this.hits = hits; }
    
    @DynamoDBAttribute(attributeName = "TF")
	public Double getTf() { return tf; }
	public void setTf(Double tf) { this.tf = tf; }

    @Override
    public String toString(){
    	String result = "";
    	result += word + ":" + url + "\tHits: ";
    	for (Hit hit : hits) {
    		result += hit.toString() + "\t";
    	}
    	return result;
    }

}
