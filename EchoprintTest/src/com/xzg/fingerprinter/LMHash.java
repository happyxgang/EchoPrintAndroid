package com.xzg.fingerprinter;

public class LMHash {
	public long sid;
	public int starttime;
	public int hash;
	public int f1;
	public int f2;
	public int delta_t;
	public static LMHash createHash(Landmark lm, long sid){
		LMHash h = LMHash.createHash(lm);
		h.sid = sid;
		return h;
	}
	public static LMHash createHash(Landmark lm){
		LMHash h = new LMHash();
		h.starttime = lm.starttime;
		h.f1 = lm.f1;
		h.f2 = lm.f2;
		h.delta_t = lm.delta_t;
		assert h.starttime < (int)Math.pow(2,8);
		int df = h.getDF(lm.f1, lm.f2);
		//this.hash = lm.starttime * (int)Math.pow(2,14) + lm.f1 * (int)Math.pow(2,14)  
		//		+ df*(int)Math.pow(2,6) + Math.abs(lm.delta_t) % (int)Math.pow(2,6);
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

	public String toString(){
		String str =  starttime + ":" + hash + ",";
		if (sid > 0){
			str =  sid + ":" + str;
		}
		return str;
	}
	public String toMatlabString(){
		String str = starttime + " " + f1 + " " + f2 + " " + delta_t + " \n";
		return str;
	}
	public String toRedisString(){
		StringBuilder sb = new StringBuilder();
		sb.append("*3\r\n");
		sb.append("$4\r\n");
		sb.append("sadd\r\n");
		sb.append("$" + (Integer.toString(hash).length() + 2) + "\r\n");
		sb.append( "h:" + Integer.toString(hash) + "\r\n");
		sb.append("$" + (Long.toString(sid).length() + Integer.toString(starttime).length() + 1) + "\r\n");
		sb.append(sid + "," + starttime + "\r\n");
		return sb.toString();
	}
	public static void main(String[] args){
		LMHash h = new LMHash();
		h.sid = 222;
		h.hash = 1111;
		h.starttime = 3333;
		System.out.println(h.toRedisString());
	}
}
