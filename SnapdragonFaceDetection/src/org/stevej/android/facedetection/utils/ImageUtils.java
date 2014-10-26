package org.stevej.android.facedetection.utils;

public class ImageUtils {

	public static native void init(int width, int height);

	public static native void deinit();

	public static native void findMarker(byte[] yuv, int width, int height, int[] marker, boolean mirroring);

	public static native void GtoAGGG(byte[] in, int width, int height, int[] out);

	public static native void RGBAToYUV(int[] rgba, byte[] yuv, int width, int height);

}
