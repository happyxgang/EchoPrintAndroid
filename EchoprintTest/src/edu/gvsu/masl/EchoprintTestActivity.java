/**
 * EchoprintTestActivity.java
 * EchoprintTest
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

package edu.gvsu.masl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Hashtable;

import com.xzg.fingerprinter.AudioFingerprinter;
import com.xzg.fingerprinter.AudioFingerprinter.AudioFingerprinterListener;

import net.bluecow.spectro.Clip;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * EchoprintTestActivity<br>
 * This class demos how to use the AudioFingerprinter class
 * 
 * @author Alex Restrepo (MASL)
 *
 */
public class EchoprintTestActivity extends Activity implements AudioFingerprinterListener 
{	
	boolean recording, resolved;
	AudioFingerprinter fingerprinter;
	TextView status;
	Button btn;
	
    @Override       
    public void onCreate(Bundle savedInstanceState) 
    {    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        btn = (Button) findViewById(R.id.recordButton);
        
        status = (TextView) findViewById(R.id.status);
        btn.setOnClickListener(new View.OnClickListener() 
        {
            public void onClick(View v) 
            {
                // Perform action on click
            	if(recording)
            	{            		 
        			fingerprinter.stop();        			
            	}
            	else
            	{            		
            		if(fingerprinter == null)
            			fingerprinter = new AudioFingerprinter(EchoprintTestActivity.this);
            		
            		fingerprinter.fingerprint(15);
            	}
            }
        });
    }

	public void didFinishListening() 
	{					
		btn.setText("Start");
		
		if(!resolved)
			status.setText("Idle...");
		
		recording = false;
	}
	
	public void didFinishListeningPass()
	{
		FileOutputStream os;
		try {
//			wavIO io = new wavIO();
//			
//			io.myByteRate=fingerprinter.FREQUENCY;
//			io.myChannels = 1;
//			os = openFileOutput("xzg.wav", MODE_PRIVATE);
//			
//			io.myData = fingerprinter.short2byte(fingerprinter.audioData);
//			io.myDataSize = io.myData.length;
//			io.myChunkSize = 36 + io.myDataSize;
//			io.mySubChunk1Size = 16;
//			io.myFormat = 1;
//			io.myChannels = 1;
//			io.mySampleRate = fingerprinter.FREQUENCY;
//			io.myByteRate = 2 * io.mySampleRate;
//			io.myBlockAlign = 2;
//			io.myBitsPerSample=16;
//			io.save(os);
//			//os.write(bData, 0, fingerprinter.audioData.length * fingerprinter.ENCODING);
//			Log.d("Fingerprint","写到本地文件啦");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	public void willStartListening() 
	{
		status.setText("Listening...");
		btn.setText("Stop");
		recording = true;
		resolved = false;
	}

	public void willStartListeningPass() 
	{}

	public void didGenerateFingerprintCode(String code) 
	{
		status.setText("Will fetch info for code starting:\n" + code.substring(0, Math.min(50, code.length())));
	}

	public void didFindMatchForCode(final Hashtable<String, String> table,
			String code) 
	{
		resolved = true;
		status.setText("Status:" + code +"\n Match: \n" + table);
		
	}

	public void didNotFindMatchForCode(String code) 
	{
		resolved = true;
		status.setText("No match for code starting with: \n" + code.substring(0, Math.min(50, code.length())));
	}

	public void didFailWithException(Exception e) 
	{
		resolved = true;
		status.setText("Error: " + e);
	}
}