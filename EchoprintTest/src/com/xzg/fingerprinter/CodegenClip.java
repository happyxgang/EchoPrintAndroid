package com.xzg.fingerprinter;

import java.util.ArrayList;
import java.util.LinkedList;

import net.bluecow.spectro.Frame;

public class CodegenClip {
	RecordData recordData;
	int startPos;
	int endPos;
	ArrayList<Peek> peekPoints;
	ArrayList<Frame> frames;
	public CodegenClip(RecordData d) {
		recordData = d;
		startPos = 0;
		endPos = 0;
	}

	public CodegenClip(RecordData d, int startPos) {
		recordData = d;
		this.startPos = startPos;
		this.endPos = startPos;
	}

	public boolean enoughData() {
		boolean enough = false;
		if (recordData.dataPos > startPos
				&& recordData.dataPos - endPos >= FPConfig.FRAME_SIZE * 2) {
			enough = true;
		}
		return enough;
	}
	public Byte[] getnextFrameData(){
		Byte[] data = new Byte[FPConfig.FRAME_SIZE*2];
		assert (recordData.dataPos - endPos > FPConfig.FRAME_SIZE*2);
		for(int i = endPos ; i < recordData.dataPos; i++){
			
		}
	}
	public LinkedList<Landmark> getHash() {
		LinkedList<Landmark> lms = new LinkedList<Landmark>();
		while (enoughData()) {
			Byte[] data = getNextFrameData();
			Frame f = new Frame()
		}
		return lms;
	}

}
