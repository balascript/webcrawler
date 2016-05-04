package edu.scu.java.webcrawl.models;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Multiset;

public class MustacheWords {
	public List<Word> words;
	
	public class Word{
		public String Value;
		public int Count;
		public Word(String value, int count) {
			super();
			Value = value;
			Count = count;
		}
		
	}
	
	public MustacheWords(List<Multiset.Entry<String>> list){
		words = new ArrayList<>();
		for(Multiset.Entry<String> e: list){
			words.add(new Word(e.getElement().toString(),e.getCount()));
			if(words.size()==100)
				break;
		}
	}

}
