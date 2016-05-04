package edu.scu.java.webcrawl;

import java.net.URL;
import java.util.LinkedList;

import com.trigonic.jrobotx.RobotExclusion;

import edu.scu.java.webcrawl.controller.Constants;
import edu.scu.java.webcrawl.controller.WebCrawler;
import edu.scu.java.webcrawl.models.Page;
import edu.scu.java.webcrawl.statics.Log;

public class Crawler implements Runnable {
	private boolean isRunning;
	private int CrawlerID;
	private WebCrawler Controller;
	private Thread CrawlerThread;
	private int PagesVisitedSoFar;
	private LinkedList<URL> PagesToVisit;
	RobotExclusion robotExclusion;
	public boolean isRunning() {
		return isRunning;
	}


	public int getPagesVisitedSoFar() {
		return PagesVisitedSoFar;
	}

	
	 public Crawler(WebCrawler c, int ID) {
		 this.Controller=c;
		 this.CrawlerID=ID;
		 CrawlerThread= new Thread(Controller.getCrawlersGroup(),this);
		 this.PagesToVisit= new LinkedList<URL>();
		 robotExclusion = new RobotExclusion();
		 try {
			startCrawling();
		} catch (CrawlerException e) {
			Log.writeLog(e.getMessage());
		}
		
	}

	public void startCrawling() throws CrawlerException{
	if(CrawlerThread==null)
		throw new CrawlerException("The Crawler thread '"+this.CrawlerID+"' is NULL", CrawlerException.State.Thread);
	else
		CrawlerThread.start();
	}
	
	@Override
	public synchronized void run(){
		try {
			this.crawlAPage();
		} catch (InterruptedException e) {

				try {
					throw new CrawlerException("The Crawler thread '"+this.CrawlerID+"'is Interrupted", CrawlerException.State.Thread);
				} catch (CrawlerException e1) {
					//Log.writeLog("Pages visited by '"+this.CrawlerID+"' so far : "+PagesVisitedSoFar);
					Log.writeLog(e1.getMessage());
				}
			 
		}
		
	}
	
	private void crawlAPage() throws InterruptedException{
		while(!this.CrawlerThread.isInterrupted()){
			Thread.sleep(Constants.GRACEPERIOD);
			//TODO increase Grace Period
			URL toCrawl= getNextUri();
			if(toCrawl !=null && this.Controller.isOkToProceed(toCrawl.toString(),this)){
				if(!robotExclusion.allows(toCrawl, Constants.USER_AGENT))
					return;
				Page page;
				
					page = new Page(toCrawl).executeAndReturn();
					if(page!=null){
					this.Controller.addNewVisitedPage(page);
					this.PagesVisitedSoFar++;
					}
			}
			else{
				wait();
			}
		}
	}


	public void terminate(){
		this.CrawlerThread.interrupt();
	}
	public void putNextUri(URL newLink){

		synchronized (PagesToVisit) {
			PagesToVisit.add(newLink);
			if(PagesToVisit.size()==1)
				PagesToVisit.notify();
		}
	}

	public URL  getNextUri() throws InterruptedException {	
		synchronized (PagesToVisit) {
			if(PagesToVisit.size()==0)
				PagesToVisit.wait();
			return PagesToVisit.poll();
		}
		
	}


	public Thread getCrawlerThread() {
		return CrawlerThread;
	}

}




