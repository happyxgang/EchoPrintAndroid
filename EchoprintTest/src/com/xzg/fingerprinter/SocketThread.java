package com.xzg.fingerprinter;

import android.util.Log;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;

public class SocketThread implements Runnable {
	int count = 0;
	WebSocket ws = null;

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
									ws.send("hello 123");
								} else {
									ws.close();
								}
							}
						});
						webSocket.send("hello");
						webSocket.send("xzg hello");
					}
				});
	}
}
