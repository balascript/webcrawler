package edu.scu.java.webcrawl;

public class CrawlerException extends Exception {

	public enum State{
		Thread,Network,Other
	}
	private String Message;
	private State state;
	
	
	public CrawlerException(String message, State state) {
		super();
		Message = message;
		this.state = state;
	}


	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return Message;
	}

	
}
