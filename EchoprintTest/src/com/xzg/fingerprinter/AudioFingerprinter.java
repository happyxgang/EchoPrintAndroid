/**
 * AudioFingerprinter.java
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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;

import net.bluecow.spectro.Clip;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Main fingerprinting class<br>
 * This class will record audio from the microphone, generate the fingerprint
 * code using a native library and query the data server for a match
 * 
 * @author Alex Restrepo (MASL)
 * 
 */
public class AudioFingerprinter implements Runnable {
	public final static String META_SCORE_KEY = "meta_score";
	public final static String SCORE_KEY = "score";
	public final static String DELTAT_KEY = "delta_t";
	public final static String NAME_KEY = "song_name";
	public final static String ALBUM_KEY = "release";
	public final static String TITLE_KEY = "track";
	public final static String TRACK_ID_KEY = "track_id";
	public final static String ARTIST_KEY = "artist";

	public final static String[] RESULT_KEYS = {"id","song_name",
            "delta_t", "match_hash_num", "hash_num",
			"top25_num","second_max_num", "second_id","query_time",
			"real_song_hash_match_time", "real_song_hash_match"};

	public final int FREQUENCY = 8000;
	public final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
	public final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	public final int FRAME_SIZE = 512;

	private Thread recordThread;
	public static volatile boolean isRunning = false;
	AudioRecord mRecordInstance = null;

	public byte audioData[];
	private int bufferSize;
	private volatile boolean continuous;

	private int maxRecordTime = 60;

	private AudioFingerprinterListener listener;
	private long recordStartTime;
	private Thread codegenThread;
	private int samplesIn;
	private RecordData recordData;
	private Thread socketThread;

	/**
	 * Constructor for the class
	 * 
	 * @param listener
	 *            is the AudioFingerprinterListener that will receive the
	 *            callbacks
	 */
	public AudioFingerprinter(AudioFingerprinterListener listener) {
		this.listener = listener;
	}

	/**
	 * Starts the listening / fingerprinting process using the default
	 * parameters:<br>
	 * A single listening pass of 20 seconds
	 */
	public void fingerprint() {
		// set dafault listening time to 20 seconds
		this.fingerprint(20);
	}

	/**
	 * Starts a single listening / fingerprinting pass
	 * 
	 * @param seconds
	 *            the seconds of audio to record.
	 */
	public void fingerprint(int seconds) {
		// no continuous listening
		this.fingerprint(seconds, false);
	}

	/**
	 * Starts the listening / fingerprinting process
	 * 
	 * @param seconds
	 *            the number of seconds to record per pass
	 * @param continuous
	 *            if true, the class will start a new fingerprinting pass after
	 *            each pass
	 */
	public void fingerprint(int seconds, boolean continuous) {
		if (AudioFingerprinter.isRunning)
			return;

		this.continuous = continuous;
		int minBufferSize = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL,
				ENCODING);

		// cap to 30 seconds max, 10 seconds min.
		this.recordStartTime = System.currentTimeMillis();

		System.out.println("minBufferSize: " + minBufferSize);

		// and the actual buffer size for the audio to record
		// frequency * seconds to record.
		bufferSize = getByteBufferSize();

		Log.d("Fingerprintter", "BufferSize: " + bufferSize);
		RecordData.init(bufferSize);

		// TODO: use max buffer replace the record buffer and say what happends
		// start recorder
		mRecordInstance = new AudioRecord(MediaRecorder.AudioSource.MIC,
				FREQUENCY, CHANNEL, ENCODING, minBufferSize);

		// start the recording thread
		AudioFingerprinter.isRunning = true;

		recordThread = new Thread(this);
		recordThread.start();

		if (Config.isMultiThread) {
			this.codegenThread = new Thread(new CodegenThread(this.recordData));
			codegenThread.start();

			SocketThread st = new SocketThread(listener);
			this.socketThread = new Thread(st);
			socketThread.setPriority(Thread.MAX_PRIORITY);
			socketThread.start();
		}

	}

	/**
	 * stops the listening / fingerprinting process if there's one in process
	 */
	public void stop() {
		this.continuous = false;
		if (mRecordInstance != null)
			mRecordInstance.stop();
	}

	public String fetchServerResult(String code) throws IllegalStateException,
			IOException {

		StringEntity requestEntity = new StringEntity(code);

		HttpPost post = new HttpPost(Config.Server_One_Thread);
		post.setEntity(requestEntity);
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(post);
		// Examine the response status
		// Log.d("Fingerprinter", response.getStatusLine().toString());

		// Get hold of the response entity
		HttpEntity entity = response.getEntity();
		// If the response does not enclose an entity, there is no
		// need
		// to worry about connection release

		String result = "";
		if (entity != null) {
			// A Simple JSON Response Read
			InputStream instream = entity.getContent();
			result = convertStreamToString(instream);
			// now you have the string representation of the HTML
			// request
			instream.close();
		}
		return result;
	}

	public int getByteBufferSize() {

		// get the minimum buffer size
		int minBufferSize = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL,
				ENCODING);
		Log.d("Fingerprinter", "SecondsToRecord:" + Config.SECONDS_TO_RECORD);
		return Math
				.max(minBufferSize, Config.SECONDS_TO_RECORD * FREQUENCY * 2);
	}

	/**
	 * The main thread<br>
	 * Records audio and generates the audio fingerprint, then it queries the
	 * server for a match and forwards the results to the listener.
	 */
	public void run() {
		Log.d("FingerPrinter", "Thread Fingerprinter started!");
		AudioFingerprinter.isRunning = true;
		try {
			// create the audio buffer
			// get the minimum buffer size

			willStartListening();

			mRecordInstance.startRecording();
			do {
				try {
					willStartListeningPass();

					long time = System.currentTimeMillis();

					RecordData.dataPos = 0;
					this.samplesIn = 0;
					do {
						samplesIn += mRecordInstance.read(RecordData.data,
								samplesIn, bufferSize - samplesIn);
						RecordData.dataPos = samplesIn;
						if (mRecordInstance.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
							break;
					} while (samplesIn < bufferSize && AudioFingerprinter.isRunning);
					Wave w = new Wave();
					w.data = RecordData.data;
					WaveFileManager wfm = new WaveFileManager(w);
					wfm.saveWaveAsFile("/sdcard/record.wav");
					if (!Config.isMultiThread) {
						Clip clip = Clip.newInstance(RecordData.data);
						Codegen codegen = new Codegen(clip);
						String code = codegen.genCode();
						String result = fetchServerResult(code);
						System.out.println("result: " + result);
						JSONObject jsonResult = new JSONObject(result);

						if (jsonResult.has("id"))
							Log.d("Fingerprinter", "Response code:"
									+ jsonResult.getInt("id"));

						if (jsonResult.has("id")) {
							if (jsonResult.getInt("id") >= 0) {
								Hashtable<String, String> match = parseResult(jsonResult);
								didFindMatchForCode(match, "match");
							} else{
								didNotFindMatchForCode(result);
							}
						}
					}
					didFinishListeningPass();
					writeWaveToFile();
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("Fingerprinter", e.getLocalizedMessage());

					didFailWithException(e);
				}
			} while (this.continuous);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Fingerprinter", e.getLocalizedMessage());

			didFailWithException(e);
		}

		if (mRecordInstance != null) {
			mRecordInstance.stop();
			mRecordInstance.release();
			mRecordInstance = null;
		}
		AudioFingerprinter.isRunning = false;

		didFinishListening();
		Log.d("FingerPrinter", "Thread AudioFingerPrinter exits");
	}

	private void writeWaveToFile() {
		Wave w = new Wave();
		w.data = RecordData.data;
		WaveFileManager wm = new WaveFileManager();
		wm.setWave(w);
		wm.saveWaveAsFile("/sdcard/record.wav");

	}

	private boolean recordTimeExceed() {
		long nowTime = System.currentTimeMillis();
		long recordLastTime = nowTime - this.recordStartTime;
		if (AudioFingerprinter.isRunning && recordLastTime > Config.SECONDS_TO_RECORD * 1000) {
			return true;
		}
		return false;
	}

	static Hashtable<String, String> parseResult(JSONObject jsonResult) {
		Hashtable<String, String> match = new Hashtable<String, String>();
		try {
			for (int i = 0; i < RESULT_KEYS.length; i++) {
				match.put(RESULT_KEYS[i], jsonResult.getString(RESULT_KEYS[i]));
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return match;
	}

	public byte[] short2byte(short[] sData) {
		int shortArrsize = sData.length;
		byte[] bytes = new byte[shortArrsize * 2];
		for (int i = 0; i < shortArrsize; i++) {
			bytes[i * 2] = (byte) (sData[i] & 0x00FF);
			bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
			sData[i] = 0;
		}
		return bytes;

	}

	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	private void didFinishListening() {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didFinishListening();
				}
			});
		} else
			listener.didFinishListening();
	}

	private void didFinishListeningPass() {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didFinishListeningPass();
				}
			});
		} else
			listener.didFinishListeningPass();
	}

	private void willStartListening() {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.willStartListening();
				}
			});
		} else
			listener.willStartListening();
	}

	private void willStartListeningPass() {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.willStartListeningPass();
				}
			});
		} else
			listener.willStartListeningPass();
	}

	private void didGenerateFingerprintCode(final String code) {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didGenerateFingerprintCode(code);
				}
			});
		} else
			listener.didGenerateFingerprintCode(code);
	}

	private void didFindMatchForCode(final Hashtable<String, String> table,
			final String code) {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didFindMatchForCode(table, code);
				}
			});
		} else
			listener.didFindMatchForCode(table, code);
	}

	private void didNotFindMatchForCode(final String code) {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didNotFindMatchForCode(code);
				}
			});
		} else
			listener.didNotFindMatchForCode(code);
	}

	private void didFailWithException(final Exception e) {
		if (listener == null)
			return;

		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					listener.didFailWithException(e);
				}
			});
		} else
			listener.didFailWithException(e);
	}

	/**
	 * Interface for the fingerprinter listener<br>
	 * Contains the different delegate methods for the fingerprinting process
	 * 
	 * @author Alex Restrepo
	 * 
	 */
	public interface AudioFingerprinterListener {
		/**
		 * Called when the fingerprinter process loop has finished
		 */
		public void didFinishListening();

		/**
		 * Called when a single fingerprinter pass has finished
		 */
		public void didFinishListeningPass();

		/**
		 * Called when the fingerprinter is about to start
		 */
		public void willStartListening();

		/**
		 * Called when a single listening pass is about to start
		 */
		public void willStartListeningPass();

		/**
		 * Called when the codegen libary generates a fingerprint code
		 * 
		 * @param code
		 *            the generated fingerprint as a zcompressed, base64 string
		 */
		public void didGenerateFingerprintCode(String code);

		/**
		 * Called if the server finds a match for the submitted fingerprint code
		 * 
		 * @param table
		 *            a hashtable with the metadata returned from the server
		 * @param code
		 *            the submited fingerprint code
		 */
		public void didFindMatchForCode(Hashtable<String, String> table,
				String code);

		/**
		 * Called if the server DOES NOT find a match for the submitted
		 * fingerprint code
		 * 
		 * @param code
		 *            the submited fingerprint code
		 */
		public void didNotFindMatchForCode(String code);

		/**
		 * Called if there is an error / exception in the fingerprinting process
		 * 
		 * @param e
		 *            an exception with the error
		 */
		public void didFailWithException(Exception e);
	}

	public static void main(String[] args) throws IOException {
		String fn = "/home/kevin/Documents/amazing.wav";
		Wave w = new Wave(fn);
		byte[] data = w.getBytes();
		System.out.println(w.getWaveHeader());
		Clip c = Clip.newInstance(data, w.getWaveHeader().getSampleRate());
		Codegen codegen = new Codegen(c);
		String code = codegen.genCode();
		codegen.writeCSVToFile();
		System.out.println(code);
		System.out.println("ended!");
	}
}
