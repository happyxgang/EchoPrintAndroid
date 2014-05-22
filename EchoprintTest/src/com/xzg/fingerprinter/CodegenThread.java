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
		Log.d("CodegenThread", "start!");
		for (int i = 0; i < Config.QUERY_CLIP; i++) {
			CodegenClip clip = new CodegenClip(recordData, i
					* Config.QUERY_OVERLAP);
			clips.add(clip);
		}
		while (AudioFingerprinter.isRunning) {
			int processedFrameCount = 0;
			for (int i = 0; i < clips.size(); i++) {
				int frameCount = 0;
				CodegenClip c = clips.get(i);
				frameCount = c.getHash();
				processedFrameCount += frameCount;
				if (frameCount > 0) {
					Log.d("CodegenThread", "Clip:" + i + " process frame num:"
							+ frameCount);
				}
			}

			//TODO change use positive method to give up cpu 
			if (processedFrameCount == 0) {
				try {
					Thread.sleep(100);
					Log.d("CodegenThread", "goes to sleep");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Log.d("CodegenThread", "thread exits");
	}

}
