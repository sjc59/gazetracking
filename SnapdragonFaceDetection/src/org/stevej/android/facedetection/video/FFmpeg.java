package org.stevej.android.facedetection.video;

import android.graphics.Bitmap;

/**
 * This class declares the JNI methods implemented in FFmpegWrapper.c so that they can be accessed in a static fashion from Java code.
 * <p>
 * A full FFmpeg library is loaded into this application as a shared (dynamic) library.
 * <p>
 * The methods in FFmpegWrapper.c call through to the appropriate native FFmpeg methods.
 * 
 */
public class FFmpeg {

	/**
	 * Open a video file in FFmpeg (includes initialising a decoder etc), returning information in the <em>dimensions</em> argument.
	 * 
	 * @param file_path
	 *            the full path of the video file to open
	 * @param dimensions
	 *            contains three ints : [0] = width of video, [1] = height of video, [2] number of bytes required to represent a video frame in YUV (ImageFormat.NV21) format
	 * @return true, if successful
	 */
	static public native boolean openFile(String file_path, int[] dimensions);

	/**
	 * Closes the FFmpeg file, freeing up memory.
	 */
	static public native void closeFile();

	/**
	 * Reset the frame grabbing position to the start of the video.
	 */
	static public native void resetToStart();

	/**
	 * Gets the next frame from the video in YUV format (the same format as provided by the camera preview).
	 * 
	 * @param yuv_frame
	 *            on return contains the pixels of the frame
	 * @return true, if successful
	 */
	static public native boolean getFrameYUV(byte[] yuv_frame, Bitmap bitmap);

	static public native boolean getFrameABGR(int[] abgr_frame);

	/**
	 * Gets an Android Bitmap of a frame from the video at the given position (in seconds)
	 * 
	 * @param bitmap
	 *            on return contains the bitmap of the frame at the given time
	 * @param secs
	 *            the position in the video (in seconds)
	 */
	static public native void getFrameBitmapAt(Bitmap bitmap, int secs);
}
