package com.xzg.fingerprinter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;

import android.util.Log;
import net.bluecow.spectro.Frame;

public class CodegenClip {
	private static final int MATCH_END = -1;
	private static final int MATCH_FAILED = 0;
	private static final int MATCH_SUCCESS = 1;
	private static final String TAG = "CodegenClip";
	RecordData recordData;
	int startPos;
	int endPos;
	ArrayList<ArrayList<Peak>> peakPoints = new ArrayList<ArrayList<Peak>>();
	ArrayList<Frame> frames = new ArrayList<Frame>();
	boolean first = true;
	double[] thresh;
	String post_file = "/home/kevin/Desktop/mt_lm";
	Writer lmWriter;
	Writer locallmwriter;

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
				&& ((isFirstFrame() && recordData.dataPos - endPos >= Config
						.getByteDataLength()) || (!isFirstFrame() && recordData.dataPos
						- endPos >= ((Config.getByteDataLength()) / Config.OVER_LAP)))) {
			enough = true;
		}
		return enough;
	}

	public boolean isFirstFrame() {
		return endPos == startPos;
	}

	
	// frame data turned into log domain
	public double[] getNextFrameData() {
		int dataLen = Config.getByteDataLength();
		double[] data = new double[Config.FRAME_SIZE];

		// TODO: change datalen to datalen/2
		// data diff only needs halfe of datalen becauseof overlap
		int dataStart = 0;

		if (isFirstFrame()) {
			dataStart = endPos;
		} else {
			dataStart = endPos - (dataLen) / Config.OVER_LAP;
		}

		for (int i = 0; i < data.length; i++) {
			int lowpos = endPos + 2 * i;
			int hipos = endPos + 2 * i + 1;
			if (lowpos >= recordData.data.length) {
				break;
			}
			int low = recordData.data[lowpos] & 0xff;
			int hi = recordData.data[hipos];// & 0xff; //need sign
											// extension
			int sampVal = ((hi << 8) | low);
			data[i] = ((double)sampVal / Config.SCALE);
		}

		// update endPos
		int dataEnd = dataStart + dataLen;
		endPos = dataEnd;

		return data;
	}

	private double[] initSthresh(double[] data) {
		double[] s = new double[data.length];
		for (int i = 0; i < s.length; i++) {
			s[i] = data[i];
		}
		return s;
	}

	public int getHash() {
		LinkedList<LMHash> hashes = Global.lmHashes;
		int findLandmarkNum = 0;
		int frameCount = 0;
		while (AudioFingerprinter.isRunning ) {
			if(this.endPos >= recordData.data.length -  ((Config.getByteDataLength()) / Config.OVER_LAP)){
				return -1;
			}
			if (enoughData()) {
				double[] data = getNextFrameData();
				Frame f = new Frame(data);
				frames.add(f);

				if (first) {
					first = false;
					thresh = initSthresh(data);
				}

				ArrayList<Peak> peaks = find_peakpoints(f);
				peakPoints.add(peaks);
				decayThresh();
				LinkedList<Landmark> landmarks = findLandmarks(peaks);
				findLandmarkNum += landmarks.size();

				LinkedList<LMHash> lmhash = landmarksToLMHashes(landmarks);
				hashes.addAll(lmhash);

				writeHashToFile(lmhash);
				frameCount++;
				if (frameCount > 20) {
					break;
				}
			} else {
				break;
			}
		}
		if (frameCount > 0) {
			Log.d(TAG, "get hash process frame num:" + frameCount);
		}
		if (findLandmarkNum > 0) {
			Log.d(TAG, "get landmark num:" + findLandmarkNum);
		}
		return frameCount;
	}

	private LinkedList<LMHash> landmarksToLMHashes(
			LinkedList<Landmark> landmarks) {
		// TODO Auto-generated method stub
		LinkedList<LMHash> lmHashes = new LinkedList<LMHash>();
		for (int i = 0; i < landmarks.size(); i++) {
			Landmark lm = landmarks.get(i);
			LMHash lmhash = LMHash.createHash(lm);
			lmHashes.add(lmhash);
		}
		return lmHashes;
	}

	private void writeHashToFile(LinkedList<LMHash> lmhash) {
		Writer lmWriter = null;
		Writer locallmwriter = null;
		try {
			lmWriter = new FileWriter(post_file, true);
			locallmwriter = new FileWriter(post_file + "_" + startPos, true);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		for (int i = 0; i < lmhash.size(); i++) {
			LMHash hash = lmhash.get(i);
			try {
				lmWriter.write(hash.toMatlabString());
				locallmwriter.write(hash.toMatlabString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			lmWriter.close();
			locallmwriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ArrayList<Peak> find_peakpoints(Frame f) {
		double[] d = f.cloneData();
		double[] subd = util.subData(d, thresh);
		double[] diff = util.maxData(subd, 0);
		double[] mdiff = util.locmax(diff);
		// TODO what's the use of this
		mdiff[mdiff.length - 1] = 0;
		int time = frames.size() + 1;
		ArrayList<Peak> peaks = find_maxes(mdiff, time, f.cloneData());
		return peaks;
	}

	public ArrayList<Peak> find_maxes(double[] mdiff, int time, double[] data) {
		ArrayList<Peak> peaks = new ArrayList<Peak>();
		int[] index = util.find_positive_data_index(mdiff);

		// pos_pos is the last index of local peeks in this frame
		for (int j = 0; j < util.pos_pos; j++) {
			if (mdiff[j] == 0) {
				break;
			}
			// peek freq
			int maxPointFreq = index[j];
			// peek value
			double maxPointValue = data[maxPointFreq];

			if (maxPointValue > thresh[maxPointFreq]) {
				// 所有峰值计数加一
				addMaxPoint(time, maxPointFreq, maxPointValue, peaks);
				update_thresh(maxPointValue, maxPointFreq);
			}
		}
		return peaks;
	}

	public void decayThresh() {
		thresh = util.mutiMatrix(thresh, Config.DECAY_RATE);
	}

	private void addMaxPoint(int t, int x, double s_this, ArrayList<Peak> peaks) {
		Peak p = new Peak();
		p.freq = x;
		p.time = t;
		p.value = s_this;
		peaks.add(p);
	}

	public void update_thresh(double value, int freq) {
		double[] filter = new double[thresh.length];
		for (int i = 0; i < filter.length; i++) {
			filter[i] = Math
					.exp(-0.5
							* Math.pow(
									(((i - freq + 1.0) / (double) Config.THRESH_WIDTH)),
									2));
		}
		for (int i = 0; i < filter.length; i++) {
			filter[i] = filter[i] * value;
		}
		thresh = util.maxMatrix(thresh, filter);
	}

	public int isMatch(Peak p1, Peak p2) {
		int minf = p1.freq - Config.FREQ_RANGE;
		int maxf = p1.freq + Config.FREQ_RANGE;

		// TODO add delta t to start time
		int endt = p1.time;
		int startt = endt - Config.TIME_RANGE;

		if (p2.time < startt) {
			return MATCH_END;
		}
		if (p2.time >= endt || p2.time <= startt || p2.freq >= maxf
				|| p2.freq <= minf) {
			return MATCH_FAILED;
		}
		return MATCH_SUCCESS;
	}

	private ArrayList<Peak> getTargetPeaks(Peak sourcePeak) {
		ArrayList<Peak> targetPeaks = new ArrayList<Peak>();
		boolean exit = false;
		for (int i = peakPoints.size() - 1; i >= 0 && !exit; i--) {
			ArrayList<Peak> peaks = peakPoints.get(i);
			for (int j = 0; j < peaks.size(); j++) {
				Peak targetPeak = peaks.get(j);
				int ret = isMatch(sourcePeak, targetPeak);
				if (ret == MATCH_END) {
					exit = true;
					break;
				} else if (ret == MATCH_SUCCESS) {
					targetPeaks.add(targetPeak);
				} else {
					// do nothing when no match
				}
			}
		}
		return targetPeaks;
	}

	public LinkedList<Landmark> findLandmarks(ArrayList<Peak> peaks) {
		LinkedList<Landmark> landmarks = new LinkedList<Landmark>();
		for (int i = 0; i < peaks.size(); i++) {
			Peak endPeak = peaks.get(i);
			ArrayList<Peak> match_peaks = getTargetPeaks(endPeak);

			for (int j = 0; j < match_peaks.size(); j++) {
				Peak startPeak = match_peaks.get(j);
				addLandmark(landmarks, startPeak, endPeak);
			}
		}
		return landmarks;
	}

	private void addLandmark(LinkedList<Landmark> landmarks, Peak p1, Peak p2) {
		Landmark lm = new Landmark();
		lm.starttime = p1.time;
		lm.f1 = p1.freq;
		lm.f2 = p2.freq;
		lm.delta_t = p2.time - p1.time;
		landmarks.add(lm);
	}

	public void writePeakPoints() {
//        String fn = "/sdcard/fp/mt_peakpoints";
		String fn = "/home/kevin/Desktop/mt_peakpoints";
		FileWriter writer = null;
		try {
			writer = new FileWriter(fn, true);
			for (int i = 0; i < peakPoints.size(); i++) {
				ArrayList<Peak> peakArray = peakPoints.get(i);

				for(int j = 0; j < peakArray.size();j++){
                        writer.write(peakArray.get(j).toString());
				}
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

}
