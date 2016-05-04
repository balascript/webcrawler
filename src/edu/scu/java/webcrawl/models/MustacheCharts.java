package edu.scu.java.webcrawl.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multiset.Entry;

public class MustacheCharts {
public class DomainPages{
	public String DomainName;
	public int numberOfPages;
	public DomainPages(String domainName, int numberOfPages) {
		super();
		DomainName = domainName;
		this.numberOfPages = numberOfPages;
	}
	
}

public class HttpCodes{
	public int HttpCode;
	public int count;
	public HttpCodes(int httpCode, int count) {
		super();
		HttpCode = httpCode;
		this.count = count;
	}
	
}
public class WordCount{
	public int frequency;
	public String word;
	public WordCount(int frequency, String word) {
		super();
		this.frequency = frequency;
		this.word = word;
	}
	
}
public List<DomainPages> domainPages;
public List<HttpCodes> httpCodes;
public List<WordCount> wordCounts;
public MustacheCharts(HashMap<String, List<Page>> domainPagesMap, HashMap<Integer, Integer> httpCodesMap, List<Entry<String>> words){
	domainPages= new ArrayList<>();
	httpCodes= new ArrayList<>();
	wordCounts= new ArrayList<>();
	for(Map.Entry<String, List<Page>> e:domainPagesMap.entrySet() ){
		domainPages.add(new DomainPages(e.getKey(), e.getValue().size()));
	}
	for(Map.Entry<Integer, Integer> e:httpCodesMap.entrySet()){
		httpCodes.add(new HttpCodes(e.getKey(), e.getValue()));
	}
	for(Entry<String> e:words){
		wordCounts.add(new WordCount(e.getCount(), e.getElement()));
	}
}
}
