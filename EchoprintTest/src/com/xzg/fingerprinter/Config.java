package com.xzg.fingerprinter;

public class Config {
	public static final String QUERYSERVER = "ws://172.18.184.41:9260/queryws";
	public static final int QUERY_OVERLAP = (int)(0.032*8000);
	public static final int QUERY_CLIP = 2;
	public static final int FRAME_SIZE = 512;
	public static final int OVER_LAP = 2;
	public static final int SCALE = 10;
	public static final int THRESH_WIDTH = 30;
	public static final int MAX_PAIRS_PER_PEEK = 3;
	public static final int FREQ_RANGE = 31;
	public static final int TIME_RANGE = 63;
	public static final int TIME_MIN_DELTA = 63;
	public static int getByteDataLength()
	{
		return FRAME_SIZE * 2;
	}
}
