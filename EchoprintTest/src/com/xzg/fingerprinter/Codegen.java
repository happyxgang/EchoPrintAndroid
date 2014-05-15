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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		Frame f = clip.getFrame(0);
		for (int i = 0; i < sthresh.length; i++) {
			sthresh[i] = f.spectrum_data[i];
		}
/*		// 10 or framecount if less than 10
		int frame_count = Math.min(10, clip.getFrameCount());

		for (int i = 0; i < frame_count; i++) {
			Frame f = clip.getFrame(i);

			// assert(clip.getFrameFreqSamples() == f.getLength());
			for (int j = 0; j < sthresh.length; j++) {
				sthresh[j] = Math.max(f.spectrum_data[j], sthresh[j]);
			}
		}*/
		sthresh = util.spread(sthresh, f_sd);
	}

	public String genCode() {
		initSthresh();
		int p = 0;
		// find maxes in every frame
		for (int i = 0; i < clip.getFrameCount(); i++) {
			Frame f = clip.getFrame(i);
			double[] d = f.cloneData();
			double[] subd = util.subData(d, sthresh);
			double[] diff = util.maxData(subd, 0);
			double[] mdiff = util.locmax(diff);
			mdiff[mdiff.length - 1] = 0;
			find_maxes(mdiff, i, f.cloneData());
		}
		// find possible pairs in the clip
		ArrayList<Landmark> landmarks = findLandmarks();
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

	public ArrayList<Landmark> findLandmarks() {
		ArrayList<Landmark> landmarks = new ArrayList<Landmark>();
		for (int i = 0; i < peek_points.size(); i++) {
			Peek p1 = peek_points.get(i);
			ArrayList<Peek> match_peeks = getTargetPeeks(p1);
			for (int j = 0; j < Math
					.min(MAX_PAIRS_PER_PEEK, match_peeks.size()); j++) {
				Peek p2 = match_peeks.get(j);
				addLandmark(landmarks, p1, p2);
			}
		}
		return landmarks;
	}

	private void addLandmark(ArrayList<Landmark> landmarks, Peek p1, Peek p2) {
		Landmark lm = new Landmark();
		lm.starttime = p1.time;
		lm.f1 = p1.freq;
		lm.f2 = p2.freq;
		lm.delta_t = p2.time - p1.time;
		landmarks.add(lm);
	}

	public void find_maxes(double[] mdiff, int t, double[] data) {
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
			if (maxPointsNumInFrame < MAX_PER_FRAME) {
				if (maxPointValue > sthresh[maxPointFreq]) {
					maxPointsNumInFrame = maxPointsNumInFrame + 1;
					// 所有峰值计数加一
					nmaxes = nmaxes + 1;
					addMaxPoint(t, maxPointFreq, maxPointValue);
					update_thresh(maxPointValue, maxPointFreq);
				}
			}
		}
		decayThresh();
	}

	private void addMaxPoint(int t, int x, double s_this) {
		Peek p = new Peek();
		p.freq = x;
		p.time = t;
		p.value = s_this;
		peek_points.add(p);
	}

	public static int t = 0;

	public void update_thresh(double value, int freq) {
		double[] filter = new double[sthresh.length];
		for (int i = 0; i < filter.length; i++) {
			filter[i] = Math.exp(-0.5
					* Math.pow((((i - freq + 1.0) / (double) f_sd)), 2));
		}
		for (int i = 0; i < filter.length; i++) {
			filter[i] = filter[i] * value;
		}
		sthresh = util.maxMatrix(sthresh, filter);
	}

	public void decayThresh() {
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
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void writeCSVToFile() {
		String fn = "/home/kevin/Desktop/csv_script";
		FileWriter writer = null;
		try {
			writer = new FileWriter(fn, true);
			for (int i = 0; i < hashes.size(); i++) {
				LMHash h = hashes.get(i);
				writer.write(h.toCSVString());
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

	public static String postData(String songId) throws IOException,
			InterruptedException {
		Process p = Runtime
				.getRuntime()
				.exec(new String[] {
						"bash",
						"-c",
						"curl -X POST -d @/home/kevin/Desktop/post_data http://172.18.184.41:5000/query?songid="
								+ songId
								+ " --header \"Content-Type:text/xml\"" });
		BufferedInputStream in = new BufferedInputStream(p.getInputStream());
		BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
		String lineStr;
		StringBuilder sb = new StringBuilder();
		while ((lineStr = inBr.readLine()) != null) {
			// 获得命令执行后在控制台的输出信息
			System.out.println(lineStr);// 打印输出信息
			sb.append(lineStr);
		}
		// 检查命令是否执行失败。
		if (p.waitFor() != 0) {
			if (p.exitValue() == 1)// p.exitValue()==0表示正常结束，1：非正常结束
				System.err.println("命令执行失败!");
		}
		inBr.close();
		in.close();
		return sb.toString();
	}

	private static void runTest() throws IOException, InterruptedException {
		File dir = new File("/home/kevin/Documents/testfiles_convert");
		File[] files = dir.listFiles();
		String test_result_file = "/home/kevin/Desktop/test_result";
		String post_file = "/home/kevin/Desktop/post_data";
		Writer result_writer = new FileWriter(test_result_file);
		Writer landmark_writer = new FileWriter("/home/kevin/Desktop/landmarks");
		for (int i = 0; i < files.length; i++) {
			Writer write = new FileWriter(post_file);
			String fn = files[i].getAbsolutePath();
			Wave w = new Wave(fn);
			byte[] data = w.getBytes();
			System.out.println(w.getWaveHeader());
			Clip c = Clip.newInstance(data, w.getWaveHeader().getSampleRate());
			Codegen codegen = new Codegen(c);
			String code = codegen.genCode();

			String matlab_str = codegen.getMatlabString();

			landmark_writer.write(matlab_str);

			write.write(code);
			write.close();

			String filename = files[i].getName();

			int fileId = getIdFromFileName(filename);

			String searchResult = postData(Integer.toString(fileId));

			int resultId = getIdFromResult(searchResult);

			String resultType = "f";
			if (resultId == fileId) {
				resultType = "t";
			}
			result_writer.write(resultType + " " + fileId + " " + searchResult
					+ '\n');

			// System.out.println(code);
			// System.out.println("clip has frame : " + c.getFrameCount());
			System.out.println("find max time: " + util.frame_num);
			System.out.println("update max time: " + util.update_time);

			System.out.println("find peek points: "
					+ codegen.peek_points.size());
			System.out.println("find land marks: " + codegen.hashes.size());
			System.out.println("max to keep:" + codegen.MAX_TO_KEEP);
		}
		result_writer.close();
		landmark_writer.close();
		System.out.println("ended!");
	}

	private static void runGenHash() throws IOException {
		int id = 0;
		File dir = new File("/media/文档/AudioRelated/wav");
		File[] files = dir.listFiles();
		String id_file = "/home/kevin/Desktop/id_name";
		Writer write = new FileWriter(id_file);
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f.isDirectory()){
				continue;
			}
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
			codegen.writeCSVToFile();
			write.write(files[i].getName() + "," + id + "\n");
		}
		write.close();
		System.out.println("ended!");
	}

	public static int getIdFromFileName(String filename) {
		String idPattern = getFileNameIdPattern();
		int resultId = findIdByPattern(filename, idPattern);
		return resultId;
	}

	public static int getIdFromResult(String searchResult) {
		String filePattern = getResultIdPattern();
		int resultId = findIdByPattern(searchResult, filePattern);
		return resultId;
	}

	private static int findIdByPattern(String source, String filePattern) {
		int resultId = -1;
		Pattern pattern = Pattern.compile(filePattern);
		Matcher matcher = pattern.matcher(source);
		if (matcher.find()) {
			resultId = Integer.parseInt(matcher.group(1));
		}
		return resultId;
	}

	private static String getResultIdPattern() {
		return "\"id\":\\s\"(\\d*)\"";
	}

	private static String getFileNameIdPattern() {
		return "(\\d*)";
	}

	public static void main(String[] args) throws IOException,
			InterruptedException {
		if (args.length > 0) {
			runTest();
		} else {
			runGenHash();
		}
	}
}
