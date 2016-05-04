package edu.scu.java.webcrawl.models;

import java.util.List;

public class MustacheDomainTable {

	public List<Page> pages;
	public String hostName;

	public MustacheDomainTable(List<Page> pages,String host) {
		super();
		this.pages = pages;
		this.hostName=host;
	}
	
}
