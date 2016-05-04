package edu.scu.java.webcrawl.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.validator.UrlValidator;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.opencsv.CSVReader;

import edu.scu.java.webcrawl.ContentExtractor;
import edu.scu.java.webcrawl.Crawler;
import edu.scu.java.webcrawl.interfaces.CrawlerControllerCommunicator;
import edu.scu.java.webcrawl.models.MustacheCharts;
import edu.scu.java.webcrawl.models.MustacheDomainTable;
import edu.scu.java.webcrawl.models.MustacheWords;
import edu.scu.java.webcrawl.models.Page;
import edu.scu.java.webcrawl.statics.Log;

public class WebCrawler implements CrawlerControllerCommunicator {
	private int LimitNumberOfPages;
	private boolean isDomainRestricted;
	private Integer VisitedCount=0;
	private Crawler[] CrawlerPool;
	public int getVisitedCount() {
		return VisitedCount;
	}
	public synchronized void incVisitedCount() {
		 VisitedCount++;
	}
	private URL SeedUrl;
	private List<Page> visitedPages;
	private HashSet<String> MapPagesVisited;
	private HashMap<String,Crawler> DomainCrawlerMap;
	private final String[] schemes = {"http","https"}; //supporting only http and https
	private ThreadGroup CrawlersGroup;
	private final UrlValidator urlValidator = new UrlValidator(schemes);
	public Crawler RootCrawler;
	private StandardAnalyzer analyzer;
	private Multiset<String> multiset;
	private HashMap<Integer,Integer> HttpCodesMap;
	private HashMap<String,List<Page>> DomainPagesMap;
 	public int getNumberOfPages() {
		return LimitNumberOfPages;
	}

	public void setNumberOfPages(int numberOfPages) {
		LimitNumberOfPages = numberOfPages;
	}

	public boolean isDomainRestricted() {
		return isDomainRestricted;
	}

	public void setDomainRestricted(boolean isDomainRestricted) {
		this.isDomainRestricted = isDomainRestricted;
	}

	public URL getSeedUrl() {
		return SeedUrl;
	}

	public void setSeedUrl(URL uri) {
		SeedUrl = uri;
	}

	public WebCrawler(String url,int numberofPages, boolean domainrestricted) throws MalformedURLException{
		this.SeedUrl=new URL(url);
		this.LimitNumberOfPages=numberofPages;
		this.isDomainRestricted=domainrestricted;
		this.visitedPages= new ArrayList<Page>();
		this.MapPagesVisited= new HashSet<String>();
		this.DomainCrawlerMap= new HashMap<String,Crawler>();
		this.CrawlersGroup= new ThreadGroup("crawlers");
		this.CrawlerPool= new Crawler[Constants.MAX_CRAWLERS];
		//Multiset for storing word occurrences
		multiset = HashMultiset.create();
		analyzer = new StandardAnalyzer(Version.LUCENE_40);
		HttpCodesMap= new HashMap<>();
		DomainPagesMap= new HashMap<>();
	}
	
	public List<Page> getPagesListForDomain(String domain){
		if(DomainPagesMap.get(domain)==null){
			DomainPagesMap.put(domain, new ArrayList<Page>());
		}
		return DomainPagesMap.get(domain);
	}
	
	public static WebCrawler webCrawlerFactory(String FileName){
		 try {
			CSVReader reader = new CSVReader(new FileReader(FileName));
			String[] input= reader.readNext();
			if((input.length==3)){
				return new WebCrawler(input[0], Integer.parseInt(input[1]), Boolean.parseBoolean(input[2]));
			}
			else if((input.length==2)){
				return new WebCrawler(input[0], Integer.parseInt(input[1]),false); // by default, we should allow cross domain crawling
			}
			else{
				Log.writeLog("Something wrong with the input File");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 return null;
	}

	@Override
	public synchronized boolean isOkToProceed(String url,Crawler c) throws InterruptedException {
		// To stop crawler threads to exceed the limit
		synchronized (VisitedCount) {
			if(VisitedCount>=LimitNumberOfPages)
				{
				return false;
				}
			else{
				return true;
			}
			
		}

	}

	public void updateLinks(List<String> newUrls) {
		synchronized (MapPagesVisited) {
			for(String uri:newUrls){
				try {
					uri=uri.replaceAll(" ","");
					if(!isvalidUrl(uri))
						continue;
					else if(isDomainRestricted && !(new URL(uri).getHost().equals(SeedUrl.getHost()))){
						continue;
					}
					URL newLink=new URL(uri);
					String link=newLink.getHost()+((newLink.getPath()==null || newLink.getPath().equals("/"))?"":newLink.getPath())+((newLink.getQuery()==null)?"":newLink.getQuery());
					if(link.endsWith("/"))
						link = link.substring(0, link.length()-1);
					if(MapPagesVisited.add(link)){
							putNextUriForCrawlers(new URL(newLink.getProtocol()+"://"+link));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	
		}
			
	}
	
	private void putNextUriForCrawlers(URL newLink) {
	
		Crawler crawler= getCrawlerForDomain(newLink.getHost());
		crawler.putNextUri(newLink);
	}
	private boolean isvalidUrl(String uri) {
		
		if (urlValidator.isValid(uri)) {
		   return true;
		} 
		return false;
	}

	public void start(){
		RootCrawler= getCrawlerForDomain(SeedUrl.getHost());
			RootCrawler.putNextUri(SeedUrl);
		Thread StatusListener= new Thread(new Runnable() {
			
			@Override
			public void run() {
				ThreadMXBean bean = ManagementFactory.getThreadMXBean();
				
					while(getVisitedCount()<LimitNumberOfPages){
						try {
							Thread.sleep(200);
							System.out.print("\rDomains : "+DomainCrawlerMap.size()+"\tPages Crawled : "+visitedPages.size()+"\t Crawlers : "+Constants.getLength(CrawlerPool)+"");
							long[] threadIds = bean.findDeadlockedThreads(); // Returns null if no threads are deadlocked.
							if (threadIds != null) {
								Log.writeLog("Problem detected with threads.. Please restart the process ");
								break;
							}
							
						} catch (InterruptedException e) {
							Log.writeLog(" Main Status Listener Interrupted "+e.getMessage());
						}
						
					}
					stopAllCrawlersAbruptly();
					
				}
				
			
		});
		StatusListener.start();
		
	}

	@Override
	public void addNewVisitedPage(Page page) throws InterruptedException {
		synchronized (VisitedCount) {
			if(VisitedCount>=LimitNumberOfPages){
				return;
			}
			else{
				VisitedCount++;
				this.visitedPages.add(page);
				synchronized (MapPagesVisited) {
					MapPagesVisited.add(page.getUri().getHost()+((page.getUri().getPath()==null || page.getUri().getPath().equals("/"))?"":page.getUri().getPath())+((page.getUri().getQuery()==null)?"":page.getUri().getQuery()));
				}
				this.updateLinks(page.getLinksFromThePage());
			}
			
		}
		
	}
	private void processContent() {
		Log.writeLog("Processing Content Starts.....");
		Log.writeLog("Total Number of Pages Crawled and stored :"+ VisitedCount );
		Log.writeLog("Total Number of Crawlers that have been created so far : " + Constants.getLength(CrawlerPool));
		
		//We will create a separate folder for each host/domain and have it's files inside it.
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_hh_mm_ss");
		String ParentFolder = System.getProperty("user.dir")+"/WebCrawler"+dateFormat.format(now);
		ContentExtractor extractor= new ContentExtractor();
		
		for(Page p: visitedPages){
			 try {
			 Path path= Paths.get(ParentFolder+"/"+p.getUri().getHost());
			 
			 if(!Files.exists(path)){
					Files.createDirectories(path);
				//	File pageHtml= new File(path.toUri()+"/"+p.getUri()+".html");		
			 	}
			 String filePath=path+"/"+p.getSanitizedTitle();
			 System.out.println(Paths.get(filePath));
			 if(p.getHTML()!=null){
				 if(!Files.exists(Paths.get(filePath)))
					 Files.createFile(Paths.get(filePath));
				 FileWriter fileWriter = new FileWriter(filePath);
				    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
				        bufferedWriter.write(p.getHTML().html().toString());
				        bufferedWriter.close();	
				        p.setHTMLUri(Paths.get(filePath).toUri());
		    try {
				String text= extractor.extractFromDocument(p.getHTML());//CommonExtractors.DEFAULT_EXTRACTOR.getText(p.getHTML().html().toString());
				if(!Files.exists(Paths.get(filePath+"_less.txt")))
					 Files.createFile(Paths.get(filePath+"_less.txt"));
				    FileWriter fileWriterText = new FileWriter(filePath+"_less.txt");
				    BufferedWriter bufferedWriterText = new BufferedWriter(fileWriterText);
				    bufferedWriterText.write(text);
				    bufferedWriterText.close();
				    p.setTextUri(Paths.get(filePath+"_less.txt").toUri());
				    TokenStream stream = analyzer.tokenStream(null, new StringReader(text));
					while (stream.incrementToken()){
						multiset.add(stream.getAttribute(CharTermAttribute.class).toString());
					}
					// Creating Lists for report html
					
					getPagesListForDomain(p.getUri().getHost()).add(p);
					Integer count=HttpCodesMap.get(p.getHttpStatusCode());
					HttpCodesMap.put(p.getHttpStatusCode(),(count==null)?1:++count);
			} catch (Exception e) { //BoilerpipeProcessing
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				 	
			 }
			 } catch (IOException e) {
					Log.writeLog("Problem in File IO for page :"+p.getUri());
				}
		}
		
		List<Multiset.Entry<String>> words= new ArrayList<>(multiset.entrySet());
		Comparator<Entry<String>> occurence_comparator = new Comparator<Multiset.Entry<String>>() {

			@Override
			public int compare(Entry<String> e1, Entry<String> e2) {
				return e2.getCount()-e1.getCount();
			}
		};
		Collections.sort(words,occurence_comparator);
		
		Log.writeLog("All the Pages are saved at "+ParentFolder +"\n"
				+ "Generating Report Html");
		
		try {
			createReport(ParentFolder,DomainPagesMap,HttpCodesMap,VisitedCount,Constants.getLength(CrawlerPool),words);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void createReport(String parentFolder, final HashMap<String, List<Page>> domainPagesMap, HashMap<Integer, Integer> httpCodesMap,
			final Integer visitedCount, int length, final List<Multiset.Entry<String>> words) throws IOException {
		MustacheFactory mf = new DefaultMustacheFactory();
		Mustache mustache = mf.compile("table.mustache");
		FileWriter reportHtml= new FileWriter(parentFolder+"/report.html");
		mf.compile("reportHead.mustache").execute(reportHtml, null).flush();
		for(Map.Entry<String, List<Page>> entry:domainPagesMap.entrySet()){
			try {
				mustache.execute(reportHtml,new MustacheDomainTable(entry.getValue(),entry.getKey())).flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mf.compile("reportChart.mustache").execute(reportHtml, new MustacheCharts(domainPagesMap, httpCodesMap,words)).flush();
		mf.compile("reportStat.mustache").execute(reportHtml, new Object()
		{
			public int numberOfHosts= domainPagesMap.size();
			public int numberOfPages= visitedCount;
			public int numberOfWords=words.size();
		}).flush();
		mf.compile("reportWords.mustache").execute(reportHtml,new MustacheWords(words)).flush();
		mf.compile("reportFooter.mustache").execute(reportHtml, null).flush();
		Log.writeLog("The Content is processed and a report is available at "+parentFolder+"/report.html");		
	}
	private void stopAllCrawlersAbruptly() {
		Log.writeLog(" Stopping signal has been issued !!! ");
	
		CrawlersGroup.interrupt();

	Log.writeLog(" All the Sub-Crawlers stopped ");
	processContent();
	}

	private Crawler getCrawlerForDomain(String domain){
		if(DomainCrawlerMap==null)
			return null;
		else if(!DomainCrawlerMap.containsKey(domain)){
			Crawler newCrawler = createNewCrawler();// new Crawler(this, domain);
			DomainCrawlerMap.put(domain, newCrawler);
			return newCrawler;
		}
		return DomainCrawlerMap.get(domain);
	}

	
	private synchronized Crawler createNewCrawler() {
	    int randomNum = ThreadLocalRandom.current().nextInt(0,Constants.MAX_CRAWLERS);
	    if(CrawlerPool[randomNum]==null){
	    	CrawlerPool[randomNum]= new Crawler(this,randomNum );
	    }
		return CrawlerPool[randomNum];
	}
	@Override
	public ThreadGroup getCrawlersGroup(){
		return this.CrawlersGroup;
	}


	
	
}
