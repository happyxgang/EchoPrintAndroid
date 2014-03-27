package com.xzg.fingerprinter;

import static org.junit.Assert.*;

import java.io.IOException;

import net.bluecow.spectro.Clip;
import net.bluecow.spectro.Frame;

import org.junit.Before;
import org.junit.Test;

public class CodegenTest {
	public Codegen gen;
	
	@Before
	public void setup() throws IOException{
		gen = new Codegen( Clip.newInstance(new byte[]{1,2,3,4,5,6},2,3));
		double[] s = {0.5,0.3,0.4,0.5,0.4,0.1,0.2};
		gen.sthresh = s;
	}
	@Test
	public void testUpdateThresh() {
		tutil.printArray(gen.sthresh);
		gen.update_thresh(0.8, 3);
		tutil.printArray(gen.sthresh);
	}
	@Test
	public void testDecThresh(){
		gen.decay_thresh();
		tutil.printArray(gen.sthresh);
	}
}
