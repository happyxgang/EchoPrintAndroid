package com.xzg.fingerprinter;
public class Peak {
	int freq;
	int time;
	double value;

	public String toString(){
		String str = "" + time+"," + freq+","+value+"\n";
		return str;
	}
}