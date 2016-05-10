package edu.upenn.cis455.indexer;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBDocument
public class Hit {
	private Integer position;
	private String font;
	private Integer capitalization;
	private String textClassification;

	@DynamoDBAttribute(attributeName = "POS")
	public Integer getPosition() {
		return position;
	}
	public void setPosition(Integer position) {
		this.position = position;
	}

	@DynamoDBAttribute(attributeName = "FONT")
	public String getFont() {
		return font;
	}
	public void setFont(String font) {
		this.font = font;
	}

	@DynamoDBAttribute(attributeName = "CAPTL")
	public Integer getCapitalization() {
		return capitalization;
	}
	public void setCapitalization(Integer capitalization) {
		this.capitalization = capitalization;
	}	
	
	@DynamoDBAttribute(attributeName = "CLASS")
	public String getTextClassification() {
		return textClassification;
	}
	public void setTextClassification(String textClassification) {
		this.textClassification = textClassification;
	}	

	@Override
	public String toString() {
		return "position: " + position + ", font: " + font + ", capitalization: " + capitalization;
	}
}
