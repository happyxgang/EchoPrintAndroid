package com.xzg.fingerprinter;

import java.util.ArrayList;

public class CodegenThread implements Runnable {
	RecordData recordData;
	ArrayList<CodegenClip> clips = new ArrayList<CodegenClip>();

	public CodegenThread(RecordData recordData) {
		this.recordData = recordData;
	}

	@Override
	public void run() {
		for (int i = 0; i < FPConfig.QUERY_CLIP; i++) {
			CodegenClip clip = new CodegenClip(recordData, i
					* FPConfig.QUERY_OVERLAP);
			clips.add(clip);
		}
		while (AudioFingerprinter.isRunning) {
			int hashcount = 0;
			for (int i = 0; i < clips.size(); i++) {
				CodegenClip c = clips.get(i);
				hashcount += c.getHash();
			}
			if (hashcount == 0) {
				try {
				    Thread.sleep(500);
				} catch (InterruptedException e) {
				    e.printStackTrace();
				}
			}
		}
	}

}
