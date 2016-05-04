package edu.scu.java.webcrawl.controller;

public class Constants {
	public static final String USER_AGENT =
            "Chrome/13.0.782.112";
	public static final <T> int getLength(T[] arr){
	    int count = 0;
	    for(T el : arr)
	        if (el != null)
	            ++count;
	    return count;
	}
	public static final int MAX_CRAWLERS=10;
	public static final long GRACEPERIOD = 10;
}
