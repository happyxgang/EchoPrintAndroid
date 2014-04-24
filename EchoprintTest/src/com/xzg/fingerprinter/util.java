package com.xzg.fingerprinter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class util {
	public static double[] locmax(double[] data) {
		int len = data.length;
		int[] flag = new int[len + 1];
		flag[0] = 1;
		for (int i = 0; i < len - 1; i++) {
			if (data[i + 1] >= data[i]) {
				flag[i + 1] = 1;
			} else {
				flag[i + 1] = 0;
			}
		}
		flag[len] = 1;
		double[] tdata = new double[len];
		for (int i = 0; i < len; i++) {
			tdata[i] = data[i] * flag[i] * (1 - flag[i + 1]);
		}
		return tdata;
	}

	double[] concat(double[] A, double[] B) {
		int aLen = A.length;
		int bLen = B.length;
		double[] C = new double[aLen + bLen];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}

	public static double[] get_spread_module(int e) {
		int len = 4 * e;
		double[] module = new double[2 * len + 1];
		int index = 0;
		for (int i = -len; i < len + 1; i++) {
			module[index] = Math.exp(-0.5 * Math.pow(i / (double) e, 2.0));
			index = index + 1;
		}
		return module;
	}

	public static double[] spread(double[] x, int sp) {
		double[] y = new double[x.length];
		double[] e = get_spread_module(sp);
		double[] z = util.locmax(x);
		int lenx = x.length;
		int spos = (int) (1 + Math.round((e.length - 1) / 2.0));
		for (int i = 0; i < lenx; i++) {
			if (z[i] > 0) {
				double value = z[i];
				int start = spos - (i + 1);
				for (int j = 0; j < lenx; j++) {
					double v = 0;
					int pos = j + start;
					if (pos >= 0 && pos < e.length) {
						v = e[pos];
						y[j] = Math.max(y[j], value * v);
					}
				}
			}
		}
		
		//!!! bug here
		for(int i = lenx-6; i < lenx;i++){
			y[i] = 0;
		}
		return y;
	}

	public static void main(String args[]) {
		double[] data = { 2, 1, 2, 1, 2, 1, 2, 1, 2, 1 };
		double[] td = util.locmax(data);
		double[] tdata = util.spread(data, 4);
		for (int i = 0; i < tdata.length; i++) {
			System.out.print(tdata[i] + " ");
			// System.out.print(td[i] + " ");

		}
	}

	public static int pos_pos = -1;

	public static int[] find_positive_data_index(double[] main) {
		util.pos_pos = -1;
		int[] index = new int[main.length];
		for (int i = 0; i < main.length; i++) {
			index[i] = i;
		}
		quicksort(main, index);
		return index;
	}

	public static void quicksort(double[] main, int[] index) {
		quicksort(main, index, 0, index.length - 1);
	}

	// public static int pos_pos = Integer. ;
	// quicksort a[left] to a[right]
	public static void quicksort(double[] a, int[] index, int left, int right) {

		if (right <= left)
			return;
		int i = partition(a, index, left, right);
		if (a[i] <= 0 && (i < pos_pos || pos_pos == -1)) {
			pos_pos = i;
		}
		quicksort(a, index, left, i - 1);
		quicksort(a, index, i + 1, right);
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition(double[] a, int[] index, int left, int right) {
		int i = left - 1;
		int j = right;
		while (true) {
			while (more(a[++i], a[right]))
				// find item on left to swap
				; // a[right] acts as sentinel
			while (more(a[right], a[--j]))
				// find item on right to swap
				if (j == left)
					break; // don't go out-of-bounds
			if (i >= j)
				break; // check if pointers cross
			exch(a, index, i, j); // swap two elements into place
		}
		exch(a, index, i, right); // swap with partition element
		return i;
	}

	// is x < y ?
	private static boolean more(double x, double y) {
		return (x > y);
	}

	// exchange a[i] and a[j]
	private static void exch(double[] a, int[] index, int i, int j) {
		double swap = a[i];
		a[i] = a[j];
		a[j] = swap;
		int b = index[i];
		index[i] = index[j];
		index[j] = b;
	}

	public static interface MatrixOP {
		public void execute(double[] a, double[] b);
	}

	public static interface MatrixOP_One {
		public void execute(double[] a);
	}

	public static void matrix_op_two(double[] a, double[] b, MatrixOP op) {
		op.execute(a, b);
	}

	public static double[] matrix_op_one(double[] a, MatrixOP_One op) {
		op.execute(a);
		return a;
	}

	public static double[] absData(double[] data) {
		matrix_op_one(data, new MatrixOP_One() {
			public void execute(double[] a) {
				// TODO Auto-generated method stub
				for (int i = 0; i < a.length; i++) {
					a[i] = Math.abs(a[i]);
				}
			}
		});
		return data;
	}

	public static double[] subData(double[] a, double[] b) {
		assert (a.length == b.length);
		for (int i = 0; i < a.length; i++) {
			a[i] = a[i] - b[i];
		}
		return a;
	}

	public static double[] maxData(double[] a, double v) {
		final double value = v;
		matrix_op_one(a, new MatrixOP_One() {
			public void execute(double[] a) {
				for (int i = 0; i < a.length; i++) {
					a[i] = Math.max(a[i], value);
				}
			}
		});
		return a;
	}

	public static int update_time = 0;
	public static int frame_num = 0;

	public static double[] maxMatrix(double[] a, double[] b) {
		matrix_op_two(a, b, new MatrixOP() {
			public void execute(double[] a, double[] b) {
				util.frame_num = util.frame_num + 1;
				assert (a.length == b.length);
				for (int i = 0; i < a.length; i++) {
					a[i] = Math.max(a[i], b[i]);
				}
			}
		});
		return a;
	}

	public static double[] mutiMatrix(double[] a, double v) {
		for (int i = 0; i < a.length; i++) {
			a[i] = a[i] * v;
		}
		return a;
	}

	public static void writeArrayToFile(double[] data, String filename,
			boolean append) {
		Writer writer = null;
		try {
			writer = new FileWriter(filename, append);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < data.length; i++) {
				sb.append(data[i] + ",");
			}
			sb.append('\n');
			writer.write(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
