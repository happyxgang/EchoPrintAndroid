/*
 * Created on Jul 9, 2008
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.xzg.fingerprinter.util;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_1D;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
/**
 * A frame of audio data, represented in the frequency domain. The specific
 * frequency components of this frame are modifiable.
 */
public class Frame {
	public static interface IFunction {
		  public void execute(double[] o);
	}

    private static final Logger logger = Logger.getLogger(Frame.class.getName());

    /**
     * Array of spectral data.
     */
    public double[] data;
    private double max = Double.POSITIVE_INFINITY;
    private double mean = 0;
    public double[] spectrum_data;
    /**
     * Maps frame size to the DCT instance that handles that size.
     */
    private static Map<Integer, DoubleFFT_1D> dctInstances = new HashMap<Integer, DoubleFFT_1D>();
    
    private final WindowFunction windowFunc;
    public static int framecount = 0;
    public Frame(double[] timeData, WindowFunction windowFunc) {
        this.windowFunc = windowFunc;
        int frameSize = timeData.length;
        DoubleFFT_1D dct = getDctInstance(frameSize);

        // in place window
        windowFunc.applyWindow(timeData);

        // in place transform: timeData becomes frequency data

        dct.realForward(timeData);
        data = timeData;
        getSpectrumData(data);
        if(Frame.framecount < 200){
        	util.writeArrayToFile(spectrum_data, "/home/kevin/Desktop/spectrum_data", true);
        }
    }
    public double getMean(){
    	double m = 0;
    	for(int i = 0; i < spectrum_data.length; i++){
            m = m + spectrum_data[i];
    	}
        m = m / spectrum_data.length;
    	return m;
    }
    public double getMax(){
    	double m = 0;
    	for(int i = 0; i < spectrum_data.length; i++){
    		m = Math.max(m, spectrum_data[i]);
    	}
    	return m;
    }
    private static DoubleFFT_1D getDctInstance(int frameSize) {
        DoubleFFT_1D dct = dctInstances.get(frameSize);
        if (dct == null) {
            dct = new DoubleFFT_1D(frameSize);
            dctInstances.put(frameSize, dct);
        }
        return dct;
    }
    public void getSpectrumData(double[] data){
    	int len;
    	boolean even = true;
    	if(data.length % 2 == 0){
    		len = (data.length/2 + 1);
    	}else{
    		len =(data.length+1) / 2;
    		even = false;
    	}

    	spectrum_data = new double[len];
    	int j = 0;
    	for(int i = 0; i < data.length;i= i+2){
    		spectrum_data[j] = Math.abs(data[i]);
    		j = j + 1;
    	}
    	if(even){
    		spectrum_data[spectrum_data.length-1] = Math.abs(data[1]);
    	}
    }
    /**
     * Returns the length of this frame, in samples.
     * @return
     */
    public int getLength() {
        return spectrum_data.length;
    }
    
    /**
     * Returns the idx'th real component of this frame's spectrum.
     */
    public double getReal(int idx) {
        return data[idx];
    }

    /**
     * Returns the idx'th imaginary component of this frame's spectrum.
     */
    public double getImag(int idx) {
        return 0.0;
    }

    /**
     * Sets the real component at idx. This method sets the new actual value,
     * although it may make sense to provide another method that scales the existing
     * value.
     * 
     * @param idx The index to modify
     * @param d The new value
     */
    public void setReal(int idx, double d) {
        data[idx] = d;
    }

    /**
     * Returns the time-domain representation of this frame. Unless the spectral
     * data of this frame has been modified, the returned array will be very
     * similar to the array given in the constructor. Even if the spectral data
     * has been modified, the length of the returned array will have the same
     * length as the original array given in the constructor.
     */
    public double[] asTimeData() {
        double[] timeData = new double[data.length];
        System.arraycopy(data, 0, timeData, 0, data.length);
        DoubleFFT_1D dct = getDctInstance(data.length);
        //dct.inverse(timeData, true);
        dct.realInverse(timeData, true);
        windowFunc.applyWindow(timeData);
        return timeData;
    }
    public void doFunc(IFunction f){
    	f.execute(spectrum_data);
    }
    public double[] cloneAbsData(){
    	double[] d = new double[spectrum_data.length];
    	for(int i = 0; i < spectrum_data.length;i++){
    		d[i] = spectrum_data[i] > 0? spectrum_data[i]:-spectrum_data[i];
    	}
    	return d;
    }
    @Override
    public String toString(){
    	StringBuilder sb = new StringBuilder();
    	for(int i = 0; i < spectrum_data.length; i++){
    		sb.append(spectrum_data[i]+",");
    	}
    	return sb.toString();
    }
    /**
     * Quick demo to show original, transformed, and inverse transformed data.
     */
    public static void main(String[] args) {
        double[] orig = {980,988,1160,1080,928,1068,1156,1152,1176};
        
        Frame f = new Frame(orig, new NullWindowFunction());
        System.out.println(Arrays.toString(f.data));
        System.out.println(Arrays.toString(f.asTimeData()));
        System.out.println(Arrays.toString(f.spectrum_data));
    }

}
