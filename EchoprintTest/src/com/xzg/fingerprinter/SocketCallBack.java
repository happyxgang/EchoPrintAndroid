package com.xzg.fingerprinter;

import java.util.Hashtable;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;

import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;
import com.xzg.fingerprinter.AudioFingerprinter.AudioFingerprinterListener;

public class SocketCallBack implements WebSocketConnectCallback {
	private final String TAG = "SocketThreadCallback";
	private final String STATUS = "status";
	private final String SUCCESS = "success";
	int sendNum = 0;
	AudioFingerprinterListener listener;
	private final static String[] RESULT_KEYS = { "id", "delta_t",
			"max_hash_num", "second_max_num", "second_id",
			"find_song_time_pair", "query_hash_num", "query_match_hash_num",
			"song_name" };

	// "real_song_hash_match", "real_song_hash_match_time",
	// "top25_num", "hash_num","match_hash_num" ,"max_match_hash_num"
	public SocketCallBack(AudioFingerprinterListener linstener) {
		super();
		this.listener = linstener;
	}

	private Hashtable<String, String> parseResult(JSONObject jsonResult) {
		Hashtable<String, String> match = new Hashtable<String, String>();
		try {
			for (int i = 0; i < RESULT_KEYS.length; i++) {
				match.put(RESULT_KEYS[i], jsonResult.getString(RESULT_KEYS[i]));
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return match;
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
		} else {
			listener.didFindMatchForCode(table, code);
		}
	}

	@Override
	public void onCompleted(Exception ex, WebSocket webSocket) {
		final WebSocket ws = webSocket;
		webSocket.setStringCallback(new StringCallback() {
			public void onStringAvailable(String s) {
				Log.d(TAG, "Server replys: " + s);
				try {
					if (AudioFingerprinter.isRunning) {
						JSONObject jsonResult = new JSONObject(s);

						String status = (String) jsonResult.get(STATUS);
						Hashtable<String, String> match = parseResult(jsonResult);

						didFindMatchForCode(match, status);
					} else {
						Log.d(TAG,"close ws");
						ws.close();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				sendLMToServer(ws);
			}
		});
		sendLMToServer(ws);
	}

	void sendLMToServer(WebSocket ws) {
		LinkedList<LMHash> hashes = Global.lmHashes;
		StringBuilder sb = new StringBuilder();
		LMHash h = null;
		while (true) {
			int hashSize = hashes.size();
			if (hashSize > sendNum) {
				for (int i = sendNum; i < hashSize; i++) {
					h = hashes.get(i);
					sb.append(h);
				}
				ws.send(sb.toString());
				sendNum = hashSize;
				return;
			} else {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
