package org.stevej.android.facedetection.utils;

public class Utils {

	public static String byte_to_binary(byte x) {
		char str[] = new char[8];
		int cnt, ch_cnt = 0, mask = 1 << 7;

		for (cnt = 1; cnt <= 8; ++cnt) {
			str[ch_cnt++] = ((x & mask) == 0) ? '0' : '1';
			x <<= 1;
		}
		return (new String(str));
	}
	

}
