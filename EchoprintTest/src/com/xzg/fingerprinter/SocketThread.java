package com.xzg.fingerprinter;

import android.util.Log;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;
import com.xzg.fingerprinter.AudioFingerprinter.AudioFingerprinterListener;

public class SocketThread implements Runnable {
	int count = 0;
	WebSocket ws = null;
	AudioFingerprinterListener listener;
	SocketThread(AudioFingerprinterListener l){
		listener = l;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub

		AsyncHttpClient.getDefaultInstance().websocket(FPConfig.QUERYSERVER,
				null, new WebSocketConnectCallback() {
					@Override
					public void onCompleted(Exception ex, WebSocket webSocket) {
						ws = webSocket;
						webSocket.setStringCallback(new StringCallback() {
							public void onStringAvailable(String s) {
								count = count + 1;
								Log.d("websocket", "Server replys: " + s);
								if (count < 5) {
									try {
										Thread.sleep(5000 * count);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									ws.send("hello 123");
								} else {
									ws.close();
								}
							}
						});
						webSocket.send("hello");
					}
					
				});
		// while (AudioFingerprinter.isRunning) {
		// Log.d("SocketThread",
		// "Size of landmarks:" + Global.landmarks.size());
		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
	}
}
