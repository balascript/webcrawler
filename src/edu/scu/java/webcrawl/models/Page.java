package edu.scu.java.webcrawl.models;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.scu.java.webcrawl.CrawlerException;
import edu.scu.java.webcrawl.CrawlerException.State;
import edu.scu.java.webcrawl.controller.Constants;

public class Page {
	private URL Url;
	public URL getUrl() {
		return Url;
	}

	private String Title;
	private Document htmlDocument;
	private int HttpStatusCode;
	private List<String> LinksFromThePage;
	private URI HTMLUri,TextUri;
	
	public Page(URL pageUri){
		this.Url=pageUri;
		this.LinksFromThePage= new ArrayList<String>();
	}
	
	public Page executeAndReturn() {		
        try {
        	Connection connection = Jsoup.connect(this.Url.toString()).userAgent(Constants.USER_AGENT);
			this.htmlDocument = connection.get();
			this.HttpStatusCode= connection.response().statusCode();
			if(HttpStatusCode==200 && connection.response().contentType().contains("text/html")){
				 for(Element Href:htmlDocument.select("a[href]")){
					 LinksFromThePage.add(Href.absUrl("href"));
				 }
				 this.Title= htmlDocument.title();
				 this.Title = (Title == null || Title.length()<=2)? "No Title": Title;	 
			}
			else{
				this.Title="Document Fetch Failed";
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			try {
				throw new CrawlerException("Exception with the Jsoup connection : "+e.getMessage(), CrawlerException.State.Network);
			} catch (CrawlerException e1) {
				return null;
			}
		}
		
		return this;
	}

	private Document getErrorHTML() {
		String html = "<html><head><title>"+this.Url+"</title></head>"
				  + "<body><p>You are seeing this because this HTML file got this HTTP CODE: "+this.HttpStatusCode+"</p></body></html>";
		return Jsoup.parse(html);
	}

	public URL getUri() {
		return Url;
	}


	public String getTitle() {
		return Title;
	}

	public int getHttpStatusCode() {
		return HttpStatusCode;
	}

	public List<String> getLinksFromThePage() {
		return LinksFromThePage;
	}
	
	public Document getHTML(){
		return this.htmlDocument;
	}
	
	public String getSanitizedTitle(){
		String fileName= this.Url.getPath().toString().toLowerCase().replaceAll("/","_");
		fileName=fileName+"Crawled.html";
		return fileName;
	}

	public String getTextUri() {
		int hostIndex=TextUri.toString().indexOf(this.getUri().getHost());
		String relativeUri="./"+TextUri.toString().substring(hostIndex, TextUri.toString().length());
		return relativeUri;
	}

	public void setTextUri(URI textUri) {
		TextUri = textUri;
	}

	public String getHTMLUri() {
		int hostIndex=HTMLUri.toString().indexOf(this.getUri().getHost());
		String relativeUri="./"+HTMLUri.toString().substring(hostIndex, HTMLUri.toString().length());
		return relativeUri;
	}

	public void setHTMLUri(URI hTMLUri) {
		HTMLUri = hTMLUri;
	}
	
	public int noOfLinks(){
		return this.LinksFromThePage.size();
	}

	

}
