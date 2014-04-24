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
	public void setup() throws IOException {
		gen = new Codegen(Clip.newInstance(new byte[] { 1, 2, 3, 4, 5, 6 }, 2));
		double[] s = { 0.5, 0.3, 0.4, 0.5, 0.4, 0.1, 0.2 };
		gen.sthresh = s;
	}

	@Test
	public void testUpdateThresh() {
		tutil.printArray(gen.sthresh);
		gen.update_thresh(0.8, 3);
		tutil.printArray(gen.sthresh);
	}

	@Test
	public void testDecThresh() {
		gen.decayThresh();
		tutil.printArray(gen.sthresh);
	}

	@Test
	public void main() {
		String result = "{\"song_name\": null, \"delta_t\": 3664, \"max_match_hash_num\": 11, \"id\": \"763\", \"second_id\": \"530\"}";
		assertEquals(Codegen.getIdFromResult(result), 763);
		String filename = "282-五月天 - 忘词.wav";
		assertEquals(Codegen.getIdFromFileName(filename), 282);

	}
}
