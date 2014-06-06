package com.xzg.fingerprinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.bluecow.spectro.Clip;
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
//					Log.d("CodegenThread", "Clip:" + i + " process frame num:"
//							+ frameCount);
				}
			}

			//TODO change use positive method to give up cpu 
			if (processedFrameCount == 0) {
				try {
					Thread.sleep(100);
//					Log.d("CodegenThread", "goes to sleep");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		if(clips.size() > 0 ){
                clips.get(0).writePeakPoints();
		}
		Clip clip = null;
		try {
			clip = Clip.newInstance(RecordData.data,8000,0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Codegen codegen = new Codegen(clip); 
		codegen.genCode();
		writeCodes(codegen);
//		Log.d("CodegenThread", "thread exits");
	}
	public void writeCodes(Codegen codegen) {
		// TODO Auto-generated method stub
        String fn = "/sdcard/fp/mt_origin_code";
        String peakfn = "/sdcard/fp/mt_origin_peaks";
		FileWriter writer = null;
		FileWriter peakwriter = null;
		try {
			writer = new FileWriter(fn, true);
			for(int i = 0; i < codegen.hashes.size(); i++){
                LMHash h = codegen.hashes.get(i);
				writer.write(h.toMatlabString());
			}
			peakwriter = new FileWriter(peakfn,true);
			for(int i =0; i < codegen.peak_points.size();i++){
				Peak p = codegen.peak_points.get(i);
				peakwriter.write(p.toString());
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args){
		String fn = "/home/kevin/Documents/34_origin.wav";
		Wave w = new Wave(fn);
		byte[] data = w.getBytes();
		System.out.println(w.getWaveHeader());
		RecordData.data=data;
		RecordData rd = new RecordData();
		rd.dataPos = rd.data.length-1;
		CodegenClip clip = new CodegenClip(rd,0);
		int count = 0;
		AudioFingerprinter.isRunning = true;
		while(count >= 0){
			count = clip.getHash();
		}
		clip.writePeakPoints();
	}
}
