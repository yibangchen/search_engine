package edu.upenn.cis455.indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;

public class DynamodbConnector {
	public DynamoDBMapper mapper = new DynamoDBMapper(new AmazonDynamoDBClient(new ProfileCredentialsProvider("dynamodb")));
	int writtenItemCount = 0;
	
	List<String> getRelevantUrls(String stemmedWord, int atMost) {
		// create item
		RelevanceUrlItem relevanceUrlItem = new RelevanceUrlItem();
		relevanceUrlItem.setWord(stemmedWord);
		// create query expression
		DynamoDBQueryExpression<RelevanceUrlItem> queryExpression = new DynamoDBQueryExpression<>();
		queryExpression.setHashKeyValues(relevanceUrlItem);
		queryExpression.setLimit(atMost);
		List<RelevanceUrlItem> relevanceUrls = mapper.queryPage(RelevanceUrlItem.class, queryExpression).getResults();
		// convert to result
		List<String> results = new ArrayList<>();
		for (RelevanceUrlItem item : relevanceUrls) {
			results.add(item.getUrl());
		}
		return results;
	}
	
	HitsItem getHitsItem(String stemmedPhrase, String url) {
		HitsItem item = mapper.load(HitsItem.class, stemmedPhrase, url);
		return item;
	}	

	PageAttributesItem getPageAttributesItem(String url) {
		PageAttributesItem item = mapper.load(PageAttributesItem.class, url);
		return item;
	}	

	Vector<RelevanceUrlItem> relevanceUrlItems = new Vector<>();
	static final int MAX_RELEVANCE_URL_ITEM = 500;
	void save(RelevanceUrlItem item) {
		if (relevanceUrlItems.size() > MAX_RELEVANCE_URL_ITEM) {
			synchronized (this) {
				if (relevanceUrlItems.size() > MAX_RELEVANCE_URL_ITEM) {
					batchSave(relevanceUrlItems);
					relevanceUrlItems = new Vector<>();
				}
			}
		}
		relevanceUrlItems.add(item);
	}
	
	Vector<HitsItem> hitsItems = new Vector<>();
	static final int MAX_HITS_ITEM = 500;
	void save(HitsItem item) {
		if (hitsItems.size() > MAX_HITS_ITEM) {
			synchronized (this) {
				if (hitsItems.size() > MAX_HITS_ITEM) {
					batchSave(hitsItems);
					hitsItems = new Vector<>();
				}
			}
		}
		hitsItems.add(item);
	}
	
	Vector<PageAttributesItem> pageAttributesItems = new Vector<>();
	static final int MAX_PAGE_ATTRIBUTES_ITEM = 10;
	void save(PageAttributesItem item) {
		if (pageAttributesItems.size() > MAX_PAGE_ATTRIBUTES_ITEM) {
			synchronized (this) {
				if (pageAttributesItems.size() > MAX_PAGE_ATTRIBUTES_ITEM) {
					batchSave(pageAttributesItems);
					pageAttributesItems = new Vector<>();
				}
			}
		}
		pageAttributesItems.add(item);
	}
	
	synchronized void flush() {
		sop("flushing...");
		mapper.batchSave(hitsItems);
		hitsItems = new Vector<>();
		mapper.batchSave(pageAttributesItems);
		pageAttributesItems = new Vector<>();
		mapper.batchSave(relevanceUrlItems);
		relevanceUrlItems = new Vector<>();
	}

	void clearTable() {
		if (!Debug.dynamodbOpen) {
			return;
		}
		int count = 0;
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		{
			PaginatedScanList<RelevanceUrlItem> result = mapper.scan(RelevanceUrlItem.class,  scanExpression);
			for (RelevanceUrlItem data : result) {
				if (count % 100 == 0) {
					sop("deleting RelevanceUrlItem " + count + "/" + result.size());
				}
				count++;
			    mapper.delete(data);
			}
//			List<DynamoDBMapper.FailedBatch> fail = mapper.batchDelete(result);
//			sop("deleted " + fail.size() + "/" + result.size());
		}
		{
			PaginatedScanList<PageAttributesItem> result = mapper.scan(PageAttributesItem.class,  scanExpression);
			for (PageAttributesItem data : result) {
				if (count % 100 == 0) {
					sop("deleting PageAttributesItem " + count + "/" + result.size());
				}
				count++;
			    mapper.delete(data);
			}
		}
		{
			PaginatedScanList<HitsItem> result = mapper.scan(HitsItem.class,  scanExpression);
			for (HitsItem data : result) {
				if (count % 100 == 0) {
					sop("deleting HitsItem " + count + "/" + result.size());
				}
				count++;
			    mapper.delete(data);
			}
		}
	}
	
	static void sop(Object x) {
		System.out.println(x);
	}

	public List<DynamoDBMapper.FailedBatch> batchSave(List<? extends Object> objectsToSave) {
		System.out.print("about to save " + objectsToSave.size() + " items to dynamodb... ");
		writtenItemCount += objectsToSave.size();
		if (Debug.dynamodbOpen) {
			List<DynamoDBMapper.FailedBatch> results = mapper.batchSave(objectsToSave);
			if (results != null && results.size() != 0) {
				System.err.println("fail to save " + results.size() + " item.");
				return results;			
			} else {
				System.out.println("successful! already saved " + writtenItemCount + " items");
				return results;
			}
		} else {
			return null;			
		}
	}
	
	<T> void save(T object) {
		writtenItemCount += 1;
//		sop("here1");
		assert(false);
		mapper.save(object);
	}
}