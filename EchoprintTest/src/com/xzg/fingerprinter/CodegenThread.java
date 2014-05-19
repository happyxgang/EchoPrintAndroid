package com.xzg.fingerprinter;

public class CodegenThread implements Runnable {
	RecordData recordData;

	public CodegenThread(RecordData recordData) {
		this.recordData = recordData;
	}

	@Override
	public void run() {
		for (int i = 0; i < FPConfig.QUERY_CLIP; i++) {
			
		}
	}

}
