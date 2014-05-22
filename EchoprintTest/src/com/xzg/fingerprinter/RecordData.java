package com.xzg.fingerprinter;

public class RecordData {
	static byte[] data;
	static int dataPos;
	static void init(int datalen){
		if (data == null){
			data = new byte[datalen];
		}
		dataPos = 0;
	}
}
