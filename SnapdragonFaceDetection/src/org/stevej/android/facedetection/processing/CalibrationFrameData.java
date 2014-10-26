package org.stevej.android.facedetection.processing;

import org.stevej.android.facedetection.stats.CompleteFrameStats;

import com.qualcomm.snapdragon.sdk.face.FaceData;

public class CalibrationFrameData {
	public FaceData[]			face_data		= null;
	public int[]				marker			= null;
	public CompleteFrameStats	global_stats;
	
	public CalibrationFrameData(FaceData[] face_data, int[] marker, CompleteFrameStats stats) {
		this.face_data = face_data;
		this.marker = marker;
		this.global_stats = stats;
	}

}
