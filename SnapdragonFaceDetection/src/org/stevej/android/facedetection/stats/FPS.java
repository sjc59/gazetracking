package org.stevej.android.facedetection.stats;

import java.text.DecimalFormat;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import android.util.Log;

public class FPS {
	private static final String			TAG					= "FPS";

	public static final boolean			ENABLED				= true;

	private static long					frame_start;
	private static SummaryStatistics	fps_stats			= new SummaryStatistics();
	private static DecimalFormat		df					= new DecimalFormat("#0.####");

	public synchronized static void startFrame() {
		frame_start = System.nanoTime();
	}

	public synchronized static double getFps() {
		return 1000.0 / fps_stats.getMean();
	}

	public synchronized static String getFpsString() {
		return df.format(1000.0 / fps_stats.getMean());
	}

	public synchronized static long getFrameCount() {
		return fps_stats.getN();
	}

	public synchronized static void reset() {
		fps_stats.clear();
	}

	public synchronized static void endFrame() {
		long duration_ms = (System.nanoTime() - frame_start) / 1000000;

		fps_stats.addValue(duration_ms);
	}

	public static void logResults() {
		Log.d(TAG, "                 num frames = " + fps_stats.getN());
		Log.d(TAG, "         mean frame time ms = " + fps_stats.getMean());
		Log.d(TAG, "                   mean fps = " + 1000.0 / fps_stats.getMean());
	}
}
