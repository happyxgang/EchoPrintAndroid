package com.xzg.fingerprinter;
import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

import com.xzg.fingerprinter.util;


public class utilTest {
	public void printArray(String tag, double[] a){
		System.out.print(tag);
		printArray(a);
	}
	public void printArray(double[] a){
		for(int i = 0; i < a.length; i++){
			System.out.print(a[i] + " ");
		}
		System.out.println();
	}
	@Test
	public void test() {
		int len = 10;
		int [] index = new int[len];
		double[] data = new double[len];
		for(int i =0 ; i < len; i++){
			data[i] = Math.random() * 10 - 5;
			index[i] = i;
		}
		for(int i = 0; i < data.length; i++){
			System.out.print(data[i] + " ");
		}
		System.out.println();
		util.quicksort(data, index);
		for(int i = 0; i < data.length; i++){
			System.out.print("data: " + data[i] + " ");
		System.out.print("index: " + index[i] + " ");
			System.out.println();
		}
		System.out.print("pos index : " + util.pos_pos);
	}
	double[] a = {1,2,3,4,5,-5,-4,-3,-2,-1};
	double[] abs_a = {1,2,3,4,5,5,4,3,2,1};
	double[] b = {5,4,3,2,1,-1,-2,-3,-4,-5};
	double[] data = {1,4,1,2,1,2,1,2};
	double[] ans_sub = {-4,-2,0,2,4,-4,-2,0,2,4};
	double[] ans_mm = {5,4,3,4,5,-1,-2,-3,-2,-1};
	double[] ans_mutiv = {2,4,6,8,10,-10,-8,-6,-4,-2};
	@Test
	public void testSpread(){
		double[] ans = {3.876, 4.00, 3.876,3.530,3.019,2.426,1.9385,1.7650};
		double[] y = util.spread(data,4);
		assert(ans.length == y.length);
		Assert.assertArrayEquals(ans, y, 0.01);
	}
	@Test
	public void testGetModule(){
		double [] module = util.get_spread_module(4);
		
		int spos = (int) (1 + Math.round((33 - 1) / 2.0));
		System.out.print("SPOS: " + spos);
	}
	@Test
	public void testLocmax(){
		double[] ans = util.locmax(data);
		double[] m = {0,4,0,2,0,2,0,0};
		for(int i = 0; i < ans.length; i++){
			Assert.assertEquals(ans[i], m[i], 0.01);
		}
	}
	
	@Test 
	public void testAbsData(){
		double[] ans = util.absData(a);
		Assert.assertArrayEquals(a, ans,0.01);
	}
	@Test 
	public void testSubData(){
		double[] ans = util.subData(a, b);
		Assert.assertArrayEquals(ans,ans_sub,0.01);
	}	
	@Test
	public void testMaxData(){
		util.absData(a);
		for(int i = 0; i < a.length; i++){
			Assert.assertEquals(a[i], abs_a[i], 0.1);
		}
	}
	@Test
	public void testMaxMatrix(){
		double[] ans = util.maxMatrix(a, b);
		
		Assert.assertArrayEquals(ans, ans_mm,0.01);
	}
	@Test
	public void testMutiMatrix(){
		double[] ans = util.mutiMatrix(a, 2);
		Assert.assertArrayEquals(ans, ans_mutiv, 0.01);
	}
}
