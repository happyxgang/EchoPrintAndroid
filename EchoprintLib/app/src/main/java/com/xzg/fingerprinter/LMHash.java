package com.xzg.fingerprinter;

public class LMHash {
	public int sid;
	public int starttime;
	public int hash;
	public static LMHash createHash(Landmark lm, int sid){
		LMHash h = LMHash.createHash(lm);
		h.sid = sid;
		return h;
	}
	public static LMHash createHash(Landmark lm){
		LMHash h = new LMHash();
		h.starttime = lm.starttime;
		assert h.starttime < (int)Math.pow(2,8);
		int df = h.getDF(lm.f1, lm.f2);
		//this.hash = lm.starttime * (int)Math.pow(2,14) + lm.f1 * (int)Math.pow(2,14)  
		//		+ df*(int)Math.pow(2,6) + Math.abs(lm.delta_t) % (int)Math.pow(2,6);
		System.out.println("DF:"+df);
		h.hash = lm.f1 * (int)Math.pow(2,12)  
				+ df*(int)Math.pow(2,6) + Math.abs(lm.delta_t) % (int)Math.pow(2,6);
		return h;
	}
	public int getDF(int a,int b){
		int result = 0;
		result = b - a;
		if (result < 0){
			result = result + (int)Math.pow(2,6);
		}
		return result;
	}
	public static void main(String[] args){
		System.out.println(2^4);
	}
	public String toString(){
		String str = "sid:" + sid + " starttime: " +starttime + " hash: " + hash;
		return str;
	}
	
}
