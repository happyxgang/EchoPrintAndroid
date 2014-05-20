package com.xzg.fingerprinter;

import java.util.ArrayList;

import android.util.Log;

public class CodegenThread implements Runnable {
	RecordData recordData;
	ArrayList<CodegenClip> clips = new ArrayList<CodegenClip>();

	public CodegenThread(RecordData recordData) {
		this.recordData = recordData;
	}

	@Override
	public void run() {
        Log.d("CodegenThread","start!");
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
				Log.d("CodegenThread","Clip:"+i+" get hash num:" + hashcount);
			}
			if (hashcount == 0) {
				try {
				    Thread.sleep(500);
				Log.d("CodegenThread","goes to sleep");
				} catch (InterruptedException e) {
				    e.printStackTrace();
				}
			}
		}
		Log.d("CodegenThread","thread exits");
	}

}
