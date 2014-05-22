package com.xzg.fingerprinter;

import android.util.Log;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;
import com.xzg.fingerprinter.AudioFingerprinter.AudioFingerprinterListener;

public class SocketThread implements Runnable {
	private static final String TAG = "SocketThread";
	int count = 0;
	WebSocket ws = null;
	AudioFingerprinterListener listener;
	SocketThread(AudioFingerprinterListener l){
		listener = l;
	}
	@Override
	public void run() {
		Log.d(TAG,"websocket started");
		AsyncHttpClient.getDefaultInstance().websocket(Config.QUERYSERVER,
				null, new SocketCallBack(this.listener));
		Log.d(TAG,"websocket ended!!");
	}
}
