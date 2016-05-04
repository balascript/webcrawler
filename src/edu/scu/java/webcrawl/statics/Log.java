package edu.scu.java.webcrawl.statics;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Log {

	private static  Logger CrawlLogger;
	public static void init() throws SecurityException, IOException{
		if(CrawlLogger==null){
			CrawlLogger = Logger.getLogger("Web Crawler");
		}
	}
	
	public synchronized static void writeLog(String log){
		if(CrawlLogger==null)
			try {
				init();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		CrawlLogger.log(Level.INFO, log);
	}
}
