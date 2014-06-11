package com.xzg.fingerprinter;

public class Config {
	public static final String Server_WS = "ws://172.18.184.41:9260/queryws";
	public static final String Server_One_Thread = "http://172.18.184.41:5000/query";
	public static final int QUERY_OVERLAP = (int)(0.032*8000);
	public static final int QUERY_CLIP = 2;
	public static final int FRAME_SIZE = 512;
	public static final int OVER_LAP = 2;
	public static final double SCALE =32768.0;
	public static final int THRESH_WIDTH = 30;
	public static final int MAX_PAIRS_PER_PEEK = 3;
	public static final int FREQ_RANGE = 31;
	public static final int TIME_RANGE = 63;
	public static final int TIME_MIN_DELTA = 2;
	public static final double DECAY_RATE = 0.99;
	public static final int SECONDS_TO_RECORD = 15;
	public static final boolean isMultiThread = false;
	public static boolean useBackwarPrue = true;
	public static int getByteDataLength()
	{
		return FRAME_SIZE * 2;
	}
}
