package edu.upenn.cis455.indexer;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName="PageAttributes")
public class PageAttributesItem {
	private String url;
	private String title;
	private String meta;
	private String description;
	private Integer wordCount;
	private Integer maxFrequency;
	private String hash;
	
	@DynamoDBHashKey(attributeName="URL")
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

	static final int MAX_TITLE_LENGTH = 200;
    @DynamoDBAttribute(attributeName="TITLE")  
    public String getTitle() {
		return title;
	}
    public void setTitle(String title) {
    	if (title == null) {
    		this.title = null;
    		return;
    	}
    	if (title.length() < MAX_TITLE_LENGTH) {
    		this.title = title;    		
    	} else {
    		this.title = title.substring(0, MAX_TITLE_LENGTH);
    	}
	}

    static final int MAX_META_LENGTH = 200;
    @DynamoDBAttribute(attributeName="META")  
    public String getMeta() {
		return meta;
	}
    public void setMeta(String meta) {
    	if (meta == null) {
    		this.meta = null;
    		return;
    	}
    	if (meta.length() < MAX_META_LENGTH) {
    		this.meta = meta;
    	} else {
    		this.meta = meta.substring(0, MAX_META_LENGTH);
    	}
	}

    static final int MAX_DESCRIPTION_LENGTH = 300;
    @DynamoDBAttribute(attributeName="DES")  
    public String getDescription() {
		return description;
	}
    public void setDescription(String description) {
    	if (description == null) {
    		this.description = null;
    		return;
    	}
    	if (description.length() < MAX_DESCRIPTION_LENGTH) {
    		this.description = description;    		
    	} else {
    		this.description = description.substring(0, MAX_DESCRIPTION_LENGTH);
    	}
	}
	
	@DynamoDBAttribute(attributeName="COUNT")
	public Integer getWordCount() {
		return wordCount;
	}
	public void setWordCount(Integer wordCount) {
		this.wordCount = wordCount;
	}

	@DynamoDBAttribute(attributeName="MAX_FREQ")
	public Integer getMaxFrequency() {
		return maxFrequency;
	}
	public void setMaxFrequency(Integer maxFrequency) {
		this.maxFrequency = maxFrequency;
	}

	@DynamoDBAttribute(attributeName="HASH")
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	
	
}