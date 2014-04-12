/*
 * Created on Jul 8, 2008
 *
 * Spectro-Edit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Spectro-Edit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package net.bluecow.spectro;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.musicg.wave.Wave;
import com.xzg.fingerprinter.Codegen;
import com.xzg.fingerprinter.LMHash;
import com.xzg.fingerprinter.util;

import net.bluecow.spectro.Frame.IFunction;

/**
 * A Clip represents an audio clip of some length. The clip is split up into a
 * series of equal-size frames of spectral information. The frames of spectral
 * information can be accessed in random order, and the clip can also provide an
 * AudioInputStream of the current spectral information for playback or saving
 * to a traditional PCM (WAV or AIFF) audio file.
 */
public class Clip {

	private static final Logger logger = Logger.getLogger(Clip.class.getName());

	/**
	 * The audio format this class works with. Input audio will be converted to
	 * this format automatically, and output data will always be created in this
	 * format.
	 */
	public long sid = -1;
	public static final int DEFAULT_FRAME_SIZE = 512;
	public static final int DEFAULT_OVERLAP = 2;

	private final List<Frame> frames = new ArrayList<Frame>();

	/**
	 * Number of samples per frame. Currently must be a power of 2 (this is a
	 * requirement of many DFT routines).
	 */
	private final int frameSize;

	/**
	 * The amount of overlap: this is the number of frames that will carry
	 * information about the same sample. A value of 1 means no overlap; 2 means
	 * frames will overlap to cover every sample twice, and so on. More overlap
	 * means better time resolution.
	 */
	private final int overlap;

	public int getSeconds() {
		seconds = frames.size() * (DEFAULT_FRAME_SIZE - DEFAULT_OVERLAP) / 8000;
		return seconds;
	}

	public void setSeconds(int seconds) {

		this.seconds = seconds;
	}

	public int getFreq() {
		return freq;
	}

	public void setFreq(int freq) {
		this.freq = freq;
	}

	private int seconds;
	private int freq;
	/**
	 * The amount that the time samples are divided by before sending to the
	 * transformation, and the amount they're multiplied after being transformed
	 * back.
	 */
	private double spectralScale = 10000.0;

	/**
	 * Stores the current edit in progress, or null if there is no edit in
	 * progress.
	 */

	/**
	 * Creates a new Clip based on the acoustical information in the given audio
	 * file.
	 * <p>
	 * TODO: this could be time-consuming, so spectral conversion should be done
	 * in a background thread.
	 * 
	 * @param file
	 *            The audio file to read. Currently, single-channel WAV and AIFF
	 *            are supported.
	 * @throws UnsupportedAudioFileException
	 *             If the given file can't be read because it's not of a
	 *             supported type.
	 * @throws IOException
	 *             If the file can't be read for more basic reasons, such as
	 *             nonexistence.
	 */
	public static Clip newInstance(byte[] data, int freq, long sid)
			throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		return new Clip(in, DEFAULT_FRAME_SIZE, DEFAULT_OVERLAP, sid);
	}

	public static Clip newInstance(byte[] data, int freq) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		return new Clip(in, DEFAULT_FRAME_SIZE, DEFAULT_OVERLAP, -1);
	}

	/**
	 * Creates a new clip from the audio data in the given input stream.
	 * 
	 * @param name
	 *            The name of this clip. Could be the file name it was read
	 *            from, or something supplied by the user.
	 * @param in
	 *            The audio data to read. Must have the following
	 *            characteristics:
	 *            <ul>
	 *            <li>Contains bytes in the format that an AudioInputStream in
	 *            the format specified by {@link #AUDIO_FORMAT}
	 *            <li>Supports mark() and reset(). (This can be accomplished by
	 *            wrapping your stream in a BufferedInputStream)
	 *            </ul>
	 * @throws IOException
	 *             If reading the input stream fails for any reason.
	 */
	public Clip(InputStream in, int frameSize, int overlap, long sid)
			throws IOException {
		this.sid = sid;
		this.frameSize = frameSize;
		this.overlap = overlap;
		WindowFunction windowFunc = new HammingWindowFunction(frameSize);
		byte[] buf = new byte[frameSize * 2]; // 16-bit mono samples
		int n;
		in.mark(buf.length * 2);
		while ((n = readFully(in, buf)) != -1) {
			logger.finest("Read " + n + " bytes");
			if (n != buf.length) {
				// this should only happen at the end of the input file (last
				// frame)
				logger.warning("Only read " + n + " of " + buf.length
						+ " bytes at frame " + frames.size());
				break;
			}
			double[] samples = new double[frameSize];
			for (int i = 0; i < frameSize; i++) {
				int low = buf[2 * i] & 0xff;
				int hi = buf[2 * i + 1];// & 0xff; //need sign extension
				int sampVal = ((hi << 8) | low);
				samples[i] = (sampVal);
			}

			frames.add(new Frame(samples, windowFunc));
			in.reset();
			long bytesToSkip = (frameSize * 2) / overlap;
			long bytesSkipped;
			if ((bytesSkipped = in.skip(bytesToSkip)) != bytesToSkip) {
				logger.info("Skipped " + bytesSkipped + " bytes, but wanted "
						+ bytesToSkip + " at frame " + frames.size());
			}
			in.mark(buf.length * 2);
		}
		turnToLog();
		subMean();
		for (int i = 0; i < 50; i++) {
			util.writeArrayToFile(frames.get(i).cloneAbsData(),
					"/home/kevin/Desktop/spectrum_data", true);
		}
		logger.info(String.format(
				"Read %d frames  (%d bytes). frameSize=%d overlap=%d\n",
				frames.size(), frames.size() * buf.length, frameSize, overlap));
	}

	/**
	 * Fills the given buffer by reading the given input stream repeatedly until
	 * the buffer is full. The only conditions that will prevent buf from being
	 * filled by the time this method returns are if the input stream indicates
	 * an EOF condition or an IO error occurs.
	 * 
	 * @param in
	 *            The input stream to read
	 * @param buf
	 *            The buffer to fill with bytes from the input stream
	 * @return The number of bytes actually read into buf
	 * @throws IOException
	 *             If an IO error occurs
	 */
	private int readFully(InputStream in, byte[] buf) throws IOException {
		int offset = 0;
		int length = buf.length;
		int bytesRead = 0;
		while ((offset < buf.length)
				&& ((bytesRead = in.read(buf, offset, length)) != -1)) {
			logger.finest("read " + bytesRead + " bytes at offset " + offset);
			length -= bytesRead;
			offset += bytesRead;
		}
		if (offset > 0) {
			logger.fine("Returning " + offset + " bytes read into buf");
			return offset;
		} else {
			logger.fine("Returning EOF");
			return -1;
		}
	}

	/**
	 * Returns the number of time samples per frame.
	 */
	public int getFrameTimeSamples() {
		return frameSize;
	}

	/**
	 * Returns the number of frequency samples per frame.
	 */
	public int getFrameFreqSamples() {
		return frameSize;
	}

	/**
	 * Returns the number of frames.
	 * 
	 * @return
	 */
	public int getFrameCount() {
		return frames.size();
	}

	/**
	 * Returns the <i>i</i>th frame.
	 * 
	 * @param i
	 *            The frame number--frame numbering starts with 0.
	 * @return The <i>i</i>th frame. The returned frame is mutable; modifying
	 *         its data permanently alters the acoustic qualities of this clip.
	 */
	public Frame getFrame(int i) {
		return frames.get(i);
	}

	/**
	 * Returns the number of frames that overlap to produce any given time
	 * sample. An overlap of at least 2 is required in order to produce a
	 * click-free result after modifying the specral information. Larger values
	 * give better time resolution at the cost of a linear increase in memory
	 * and CPU consumption.
	 */
	public int getOverlap() {
		return overlap;
	}

	public double getMean() {
		double m = 0;
		for (int i = 0; i < getFrameCount(); i++) {
			Frame f = getFrame(i);
			m = m + f.getMean();
		}
		m = m / getFrameCount();
		return m;
	}

	public double getMax() {
		double m = 0;
		for (int i = 0; i < getFrameCount(); i++) {
			Frame f = getFrame(i);
			m = Math.max(m, f.getMax());
		}
		return m;
	}

	public boolean turnToLog() {
		boolean t = true;
		final double max = getMax();
		for (int i = 0; i < getFrameCount(); i++) {
			Frame f = getFrame(i);
			f.doFunc(new IFunction() {
				public void execute(double[] data) {
					for (int i = 0; i < data.length; i++) {
						data[i] = Math.log(Math.max((max / 1e6), data[i]));
					}
				}
			});
		}
		return t;
	}

	public void subMean() {
		final double mean = getMean();
		for (int i = 0; i < getFrameCount(); i++) {
			Frame f = getFrame(i);
			f.doFunc(new IFunction() {
				public void execute(double[] data) {
					for (int i = 0; i < data.length; i++) {
						data[i] = data[i] - mean;
					}
				}
			});
		}
	}

	public static void main(String[] args) throws IOException,
			InterruptedException {
		String fn = "/home/kevin/Documents/yyyy_test.wav";
		String id_file = "/home/kevin/Desktop/post_data";
		String matlab_file = "/home/kevin/Desktop/music_hash";
		Writer write = new FileWriter(id_file);
		Writer matlab_write = new FileWriter(matlab_file);

		Wave w = new Wave(fn);
		byte[] data = w.getBytes();
		System.out.println(w.getWaveHeader());
		Clip c = Clip.newInstance(data, w.getWaveHeader().getSampleRate());
		Codegen codegen = new Codegen(c);
		String code = codegen.genCode();
		String matlab_str = codegen.getMatlabString();
		System.out.println(code);
		System.out.println("find max time: " + util.frame_num);
		System.out.println("update max time: " + util.update_time);
		matlab_write.write(matlab_str);
		write.write(code);
		write.close();
		matlab_write.close();

		Process p = Runtime
				.getRuntime()
				.exec(new String[] {
						"bash",
						"-c",
						"curl -X POST -d @/home/kevin/Desktop/post_data http://172.18.184.41:5000/query --header \"Content-Type:text/xml\"" });
		BufferedInputStream in = new BufferedInputStream(p.getInputStream());
		BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
		String lineStr;
		while ((lineStr = inBr.readLine()) != null)
			// 获得命令执行后在控制台的输出信息
			System.out.println(lineStr);// 打印输出信息
		// 检查命令是否执行失败。
		if (p.waitFor() != 0) {
			if (p.exitValue() == 1)// p.exitValue()==0表示正常结束，1：非正常结束
				System.err.println("命令执行失败!");
		}
		inBr.close();
		in.close();
		System.out.println("find peek points: " + codegen.peek_points.size());
		System.out.println("find land marks: " + codegen.hashes.size());
		System.out.println("max to keep:" + codegen.MAX_TO_KEEP);
		System.out.println("ended!");
	}
}
