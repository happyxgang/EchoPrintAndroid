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

public class FPocketCallBack implements WebSocketConnectCallback {
	public FPocketCallBack(AudioFingerprinterListener linstener) {
		super();
		this.listener = linstener;
	}

	int sendNum = 0;
	AudioFingerprinterListener listener;
	private final static String[] RESULT_KEYS = { "id", "real_song_hash_match",
			"real_song_hash_match_time", "second_max_num", "second_id",
			"top25_num", "hash_num", "match_hash_num", "max_match_hash_num",
			"song_name" };

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

	private void didFindMatchForCode(final Hashtable<String, String> table) {
		// TODO Auto-generated method stub
		if(listener == null)
			return;
		if (listener instanceof Activity) {
			Activity activity = (Activity) listener;
			activity.runOnUiThread(new Runnable() {
				public void run(){
					listener.didFindMatchForCode(table,"");
				}
			});
		}else {
			listener.didFindMatchForCode(table, "");
		}
	}

	@Override
	public void onCompleted(Exception ex, WebSocket webSocket) {
		final WebSocket ws = webSocket;
		webSocket.setStringCallback(new StringCallback() {
			public void onStringAvailable(String s) {
				Log.d("websocket", "Server replys: " + s);
				try {
					JSONObject jsonResult = new JSONObject(s);
					Hashtable<String, String> match = parseResult(jsonResult);
					didFindMatchForCode(match);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				sendLMToServer(ws);
			}

		});
		sendLMToServer(ws);
	}

	void sendLMToServer(WebSocket ws) {
		LinkedList<Landmark> lm = Global.landmarks;
		StringBuilder sb = new StringBuilder();
		LMHash h = null;
		while (true) {
			if (lm.size() > sendNum) {
				for (int i = sendNum; i < lm.size(); i++) {
					h = LMHash.createHash(lm.get(i));
					sb.append(h);
				}
				ws.send(sb.toString());
				return;
			} else {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
