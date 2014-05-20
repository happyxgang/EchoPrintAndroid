package com.xzg.fingerprinter;

import java.util.ArrayList;
import java.util.LinkedList;

import net.bluecow.spectro.Frame;

public class CodegenClip {
	private static final int MATCH_END = -1;
	private static final int MATCH_FAILED = 0;
	private static final int MATCH_SUCCESS = 1;
	RecordData recordData;
	int startPos;
	int endPos;
	ArrayList<ArrayList<Peak>> peakPoints = new ArrayList<ArrayList<Peak>>();
	ArrayList<Frame> frames = new ArrayList<Frame>();
	boolean first = true;
	double[] thresh;

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
				&& ((isFirstFrame() && recordData.dataPos - endPos >= FPConfig
						.getByteDataLength()) || (!isFirstFrame() && recordData.dataPos
						- endPos >= ((FPConfig.getByteDataLength()) / FPConfig.OVER_LAP)))) {
			enough = true;
		}
		return enough;
	}

	public boolean isFirstFrame() {
		return endPos == startPos;
	}

	public double[] getNextFrameData() {
		int dataLen = FPConfig.getByteDataLength();
		double[] data = new double[FPConfig.FRAME_SIZE];

		// TODO: change datalen to datalen/2
		// data diff only needs halfe of datalen becauseof overlap
		int dataStart = 0;

		if (isFirstFrame()) {
			dataStart = endPos;
		} else {
			dataStart = endPos - (dataLen) / FPConfig.OVER_LAP;
		}

		for (int i = 0; i < data.length; i++) {
			int low = recordData.data[endPos + 2 * i] & 0xff;
			int hi = recordData.data[endPos + 2 * i + 1];// & 0xff; //need sign
															// extension
			int sampVal = ((hi << 8) | low);
			data[i] = (sampVal / FPConfig.SCALE);
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
		LinkedList<Landmark> lms = Global.landmarks;
		int findLandmarkNum = 0;
		while (AudioFingerprinter.isRunning) {
			if (enoughData()) {
				double[] data = getNextFrameData();
				Frame f = new Frame(data);
				frames.add(f);

				if (first) {
					first = false;
					thresh = initSthresh(data);
				}
				ArrayList<Peak> peaks = find_peakpoints(f);

				LinkedList<Landmark> landmarks = findLandmarks(peaks);
				findLandmarkNum += landmarks.size();
				lms.addAll(landmarks);
			}else{
				break;
			}
		}
		return findLandmarkNum;
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
		int maxPointsNumInFrame = 0;

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
				maxPointsNumInFrame = maxPointsNumInFrame + 1;
				// 所有峰值计数加一
				addMaxPoint(time, maxPointFreq, maxPointValue, peaks);
				update_thresh(maxPointValue, maxPointFreq);
			}
		}
		peakPoints.add(peaks);
		return peaks;
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
					.exp(-0.5 * Math.pow( (((i - freq + 1.0) / (double) FPConfig.THRESH_WIDTH)), 2));
		}
		for (int i = 0; i < filter.length; i++) {
			filter[i] = filter[i] * value;
		}
		thresh = util.maxMatrix(thresh, filter);
	}

	public int isMatch(Peak p1, Peak p2) {
		int minf = p1.freq - FPConfig.FREQ_RANGE;
		int maxf = p1.freq + FPConfig.FREQ_RANGE;

		// TODO add delta t to start time
		int endt = p1.time;
		int startt = endt - FPConfig.TIME_RANGE;

		if (p2.time < startt) {
			return MATCH_END;
		}
		if (p2.time > endt || p2.time <= startt || p2.freq >= maxf
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
			Peak p1 = peaks.get(i);
			ArrayList<Peak> match_peaks = getTargetPeaks(p1);
			for (int j = 0; j < Math.min(FPConfig.MAX_PAIRS_PER_PEEK,
					match_peaks.size()); j++) {
				Peak p2 = match_peaks.get(j);
				addLandmark(landmarks, p1, p2);
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

}
