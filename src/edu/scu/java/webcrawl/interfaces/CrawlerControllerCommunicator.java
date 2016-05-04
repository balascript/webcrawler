package edu.scu.java.webcrawl.interfaces;

import java.net.URI;
import java.net.URL;
import java.util.List;

import edu.scu.java.webcrawl.Crawler;
import edu.scu.java.webcrawl.models.Page;

public interface CrawlerControllerCommunicator {
	
	boolean isOkToProceed(String url,Crawler c) throws InterruptedException;
	
	void addNewVisitedPage(Page page) throws InterruptedException;
	
	ThreadGroup getCrawlersGroup();

}
