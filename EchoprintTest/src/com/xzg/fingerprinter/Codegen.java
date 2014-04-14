/**
 * Codegen.java
 * EchoprintLib
 * 
 * Created by Alex Restrepo on 1/22/12.
 * Copyright (C) 2012 Grand Valley State University (http://masl.cis.gvsu.edu/)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xzg.fingerprinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.xzg.fingerprinter.Landmark;

import net.bluecow.spectro.Clip;
import net.bluecow.spectro.Frame;

/**
 * Codegen class<br>
 * This class bridges the native Codegen library with the Java side...
 * 
 * @author Alex Restrepo (MASL)
 * 
 */
public class Codegen {
	public class Peek {
		int freq;
		int time;
		double value;
	}

	public Clip clip;
	public final int MAX_PER_SEC = 30;
	public int MAX_TO_KEEP;
	public final int MAX_PER_FRAME = 5;
	public final int MAX_PAIRS_PER_PEEK = 3;
	public int nmaxes = 0;
	int f_sd = 30;// spreading width
	public double[] sthresh;
	public double a_dec = 0.998;
	public ArrayList<Peek> peek_points;
	public ArrayList<LMHash> hashes;
	public int targetdt = 63;
	public int targetdf = 31;

	public final int MATCH_MATCH = 1;
	public final int MATCH_FAILED = 0;
	public final int MATCH_END = -1;
	StringBuffer hash_str;

	public Codegen(Clip c) {
		hash_str = new StringBuffer();
		clip = c;
		MAX_TO_KEEP = Math.round(MAX_PER_SEC * clip.getSeconds());
		peek_points = new ArrayList<Peek>();
		hashes = new ArrayList<LMHash>();
	}

	// init the sthresh with the greatest values in first ten frames
	public void initSthresh() {

		int len = 0;
		int length = clip.getFrameFreqSamples();
		if (length % 2 == 0) {
			len = (length / 2 + 1);
		} else {
			len = (length + 1) / 2;
		}

		sthresh = new double[len];

		// 10 or framecount if less than 10
		int frame_count = Math.min(10, clip.getFrameCount());

		for (int i = 0; i < frame_count; i++) {
			Frame f = clip.getFrame(i);

			// assert(clip.getFrameFreqSamples() == f.getLength());
			for (int j = 0; j < len; j++) {
				sthresh[j] = Math.max(Math.abs(f.spectrum_data[j]), sthresh[j]);
			}
		}

		// Writer writer = null;
		// try {
		// writer = new FileWriter("/home/kevin/Desktop/sthresh");
		//
		// StringBuilder sb = new StringBuilder();
		// for (int i = 0; i < sthresh.length; i++) {
		// sb.append(sthresh[i] + ",");
		// }
		// sb.append('\n');
		// writer.write(sb.toString());
		//
		// sb.setLength(0);
		//
		// System.out.println("Sthresh length: " + sthresh.length);
		// System.out.println("Sthresh length: " + sthresh.length);
		//
		// for (int i = 0; i < sthresh.length; i++) {
		// sb.append(sthresh[i] + ",");
		// }
		// sb.append('\n');
		//
		// writer.write(sb.toString());
		// writer.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		sthresh = util.spread(sthresh, f_sd);
	}

	public String genCode() {
		initSthresh();
		int p = 0;
		// find maxes in every frame
		for (int i = 0; i < clip.getFrameCount(); i++) {
			Frame f = clip.getFrame(i);
			double[] d = f.cloneAbsData();
			double[] subd = util.subData(d, sthresh);
			double[] diff = util.maxData(subd, 0);
			double[] mdiff = util.locmax(diff);
			mdiff[mdiff.length - 1] = 0;
			find_maxes(mdiff, i, f.cloneAbsData());
		}
		// find possible pairs in the clip
		ArrayList<Landmark> landmarks = find_landmarks();
		lm2hash(landmarks);

		return hash_str.toString();
	}

	public void lm2hash(ArrayList<Landmark> landmarks) {
		for (int i = 0; i < landmarks.size(); i++) {
			Landmark lm = landmarks.get(i);
			LMHash h = null;
			if (clip.sid > 0) {
				h = LMHash.createHash(lm, clip.sid);
			} else {
				h = LMHash.createHash(lm);
			}
			hashes.add(h);
		}
		for (int i = 0; i < hashes.size(); i++) {
			hash_str.append(hashes.get(i));
		}
	}

	public String getMatlabString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hashes.size(); i++) {
			sb.append(hashes.get(i).toMatlabString());
		}
		return sb.toString();
	}

	public ArrayList<Landmark> find_landmarks() {
		ArrayList<Landmark> landmarks = new ArrayList<Landmark>();
		for (int i = 0; i < peek_points.size(); i++) {
			Peek p1 = peek_points.get(i);
			ArrayList<Peek> match_peeks = getTargetPeeks(p1);
			for (int j = 0; j < Math
					.min(MAX_PAIRS_PER_PEEK, match_peeks.size()); j++) {
				Peek p2 = match_peeks.get(j);
				Landmark lm = new Landmark();
				lm.starttime = p1.time;
				lm.f1 = p1.freq;
				lm.f2 = p2.freq;
				lm.delta_t = p2.time - p1.time;
				landmarks.add(lm);
			}
		}
		return landmarks;
	}

	public void find_maxes(double[] mdiff, int t, double[] data) {
		int[] index = util.find_positive_data_index(mdiff);
		int nmax_this_time = 0;

		// pos_pos is the last index of local peeks in this frame
		for (int j = 0; j < util.pos_pos; j++) {
			// peek freq
			int x = index[j];
			// peek value
			double s_this = data[x];
			if (nmax_this_time < MAX_PER_FRAME) {
				if (s_this > sthresh[x]) {
					nmax_this_time = nmax_this_time + 1;
					// 所有峰值计数加一
					nmaxes = nmaxes + 1;

					Peek p = new Peek();
					p.freq = x;
					p.time = t;
					p.value = s_this;
					peek_points.add(p);
					update_thresh(s_this, x);
				}
			}
		}
		 decay_thresh();
	}

	public static int t = 0;

	public void update_thresh(double value, int freq) {
		double[] eww = new double[sthresh.length];
		for (int i = 0; i < eww.length; i++) {
			eww[i] = Math.exp(-0.5
					* Math.pow((((i - freq + 1.0) / (double) f_sd)), 2));
		}
		for (int i = 0; i < eww.length; i++) {
			eww[i] = eww[i] * value;
		}
		sthresh = util.maxMatrix(sthresh, eww);
	}

	public void decay_thresh() {
		sthresh = util.mutiMatrix(sthresh, a_dec);
	}

	public ArrayList<Peek> getTargetPeeks(Peek p) {
		ArrayList<Peek> match_points = new ArrayList<Peek>();

		for (int i = 0; i < peek_points.size(); i++) {
			Peek tmpp = peek_points.get(i);
			int ret = isMatch(p, tmpp);
			if (ret == MATCH_END) {
				break;
			} else if (ret == MATCH_MATCH) {
				match_points.add(tmpp);
			} else {
				// do nothing
			}
		}
		return match_points;
	}

	public int isMatch(Peek p1, Peek p2) {
		int minf = p1.freq - targetdf;
		int maxf = p1.freq + targetdf;
		int startt = p1.time;
		int endt = startt + targetdt;
		if (p2.time >= endt) {
			return MATCH_END;
		}
		if (p2.time <= startt || p2.freq >= maxf || p2.freq <= minf) {
			return MATCH_FAILED;
		}
		return MATCH_MATCH;
	}

	public void writeRedisScriptToFile() {
		String fn = "/home/kevin/Desktop/redis_script";
		FileWriter writer = null;
		try {
			writer = new FileWriter(fn, true);
			for (int i = 0; i < hashes.size(); i++) {
				LMHash h = hashes.get(i);
				writer.write(h.toRedisString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		int id = 0;
		File dir = new File("/home/kevin/Music/");
		File[] files = dir.listFiles();
		String id_file = "/home/kevin/Desktop/id_name";
		Writer write = new FileWriter(id_file);
		Writer write2 = new FileWriter("/home/kevin/Desktop/songfps");
		for (int i = 0; i < files.length; i++) {
			id = id + 1;
			String fn = files[i].getAbsolutePath();
			Wave w = new Wave(fn);
			byte[] data = w.getBytes();
			System.out.println(w.getWaveHeader());
			Clip c = Clip.newInstance(data, w.getWaveHeader().getSampleRate(),
					id);
			Codegen codegen = new Codegen(c);
			String code = codegen.genCode();
			codegen.writeRedisScriptToFile();
			write.write(files[i].getName() + "," + id + "\n");
			write2.write(files[i].getName() + code + "\n");
		}
		write.close();
		write2.close();
		System.out.println("ended!");
	}

}
