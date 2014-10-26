package org.stevej.android.facedetection.stats;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.stevej.android.facedetection.processing.CalibrationFrameData;

import android.graphics.Rect;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import com.qualcomm.snapdragon.sdk.face.FaceData;

public class CompleteFrameStats implements Callback {
	private static final String		TAG		= "CompleteFrameStats";
	private static DecimalFormat	df		= new DecimalFormat("#0.######");
	private final double 			ppgaX 	= 82; //94.8;
	private final double 			ppgaY 	= 101.4;

	//Captured data
	private DescriptiveStatistics	left_eye_x;
	private DescriptiveStatistics	left_eye_y;
	private DescriptiveStatistics	right_eye_x;
	private DescriptiveStatistics	right_eye_y;
	private DescriptiveStatistics	mouth_x;
	private DescriptiveStatistics	mouth_y;
	private DescriptiveStatistics	gaze_point_x;
	private DescriptiveStatistics	gaze_point_y;
	private DescriptiveStatistics	gaze_angle_h;
	private DescriptiveStatistics	gaze_angle_v;
	private DescriptiveStatistics	roll;
	private DescriptiveStatistics	pitch;
	private DescriptiveStatistics	yaw;
	
	//Processed data
	private DescriptiveStatistics 	naive_gaze_pixel_x;
	private DescriptiveStatistics 	naive_gaze_pixel_y;
	private DescriptiveStatistics 	smoothed_gaze_pixel_x;
	private DescriptiveStatistics 	smoothed_gaze_pixel_y;
	private DescriptiveStatistics	transformed_gaze_pixel_x;
	private DescriptiveStatistics	transformed_gaze_pixel_y;
		
	//For accuracy measurements as the DescriptiveStatistics only keeps the last 8 values
	private ArrayList<Pair<Double, Double>> smoothed_gaze_pixel_history;
	
	private Matrix					trans_mat;
	//private Matrix 					trans_mat_top;
	//private Matrix 					trans_mat_bottom;
	private static float			trans_mat_boundary = 9.06f;
		
	private ArrayList<Rect>			marker_rects	= new ArrayList<Rect>();

	public Handler					message_handler;

	@Override
	public boolean handleMessage(Message message) {
		CalibrationFrameData frame_data = (CalibrationFrameData) message.obj;
		addFrame(frame_data.marker, frame_data.face_data);
		return true;
	}

	public CompleteFrameStats() {

		left_eye_x = new DescriptiveStatistics();
		left_eye_y = new DescriptiveStatistics();
		right_eye_x = new DescriptiveStatistics();
		right_eye_y = new DescriptiveStatistics();
		mouth_x = new DescriptiveStatistics();
		mouth_y = new DescriptiveStatistics();
		gaze_point_x = new DescriptiveStatistics();
		gaze_point_y = new DescriptiveStatistics();
		gaze_angle_h = new DescriptiveStatistics();
		gaze_angle_v = new DescriptiveStatistics();
		roll = new DescriptiveStatistics();
		pitch = new DescriptiveStatistics();
		yaw = new DescriptiveStatistics();
		naive_gaze_pixel_x = new DescriptiveStatistics();
		naive_gaze_pixel_y = new DescriptiveStatistics();
		
		transformed_gaze_pixel_x = new DescriptiveStatistics();
		transformed_gaze_pixel_y = new DescriptiveStatistics();
		
		//last 8 values are included in mean for a rolling average
		smoothed_gaze_pixel_x = new DescriptiveStatistics();
		smoothed_gaze_pixel_y = new DescriptiveStatistics();
		
		smoothed_gaze_pixel_x.setWindowSize(8); 
		smoothed_gaze_pixel_y.setWindowSize(8);
		smoothed_gaze_pixel_history = new ArrayList<Pair<Double, Double>>();

		HandlerThread handlerThread = new HandlerThread("StatsThread");
		handlerThread.start();
		Looper looper = handlerThread.getLooper();
		message_handler = new Handler(looper, this);
		
		//set up transformation matrices
		//trans_mat_top = new Matrix();
		//trans_mat_bottom = new Matrix();
		trans_mat = new Matrix();
		
		//float[] srcTop = new float[] {-19.37f, 1.58f, 12.16f, 0.94f, -21.27f, -9.06f, 9.82f, -9.06f};
		//float[] dstTop = new float[] {0, 0, 2560, 0, 0, 720, 2560, 720};

		//float[] srcBot = new float[] {-21.3f, -9.06f, 9.82f, -9.06f, -23.19f, -12.59f, 7.47f, -12.17f,};
		//float[] dstBot = new float[] {0, 720, 2560, 720, 0, 1440, 2560, 1440};

		//TL, TR, BR, BL, C
		float[] src = new float[] {-19.4f, 1.6f, 12.2f, 0.9f, -23.2f, -12.6f, 7.6f, -12.2f, -6.9f, -9.1f};
		float[] dst = new float[] {0, 0, 2560, 0, 0, 1440, 2560, 1440, 1280, 720};
		
		trans_mat.setPolyToPoly(src, 0, dst, 0, 4);
		
		//boolean success = trans_mat_top.setPolyToPoly(srcTop, 0, dstTop, 0, 4);
		//Log.d(TAG, "Top transformation matrix creation succeeded: " + success);
		
		//success = trans_mat_bottom.setPolyToPoly(srcBot, 0, dstBot, 0, 4);
		//Log.d(TAG, "Bottom transformation matrix creation succeeded: " + success);
	}

	public long getFrameCount() {
		return left_eye_x.getN();
	}

	public void addFrame(int[] marker, FaceData[] faces) {
		if (faces == null || faces.length != 1) {
			return;
		}
		
		if (marker != null)
			marker_rects.add(new Rect(marker[0], marker[1], marker[2], marker[3]));

		FaceData face_data = faces[0];

		left_eye_x.addValue(face_data.leftEye.x);
		left_eye_y.addValue(face_data.leftEye.y);
		right_eye_x.addValue(face_data.rightEye.x);
		right_eye_y.addValue(face_data.rightEye.y);
		mouth_x.addValue(face_data.mouth.x);
		mouth_y.addValue(face_data.mouth.y);

		gaze_point_x.addValue(face_data.getEyeGazePoint().x);
		gaze_point_y.addValue(face_data.getEyeGazePoint().y);
		gaze_angle_h.addValue(face_data.getEyeHorizontalGazeAngle());
		gaze_angle_v.addValue(face_data.getEyeVerticalGazeAngle());

		roll.addValue(face_data.getRoll());
		pitch.addValue(face_data.getPitch());
		yaw.addValue(face_data.getYaw());
		
		double gazeAngleH = face_data.getEyeHorizontalGazeAngle();
		double gazeAngleV = face_data.getEyeVerticalGazeAngle();
		
		//transform angles to pixels
		//geometrical transform in 2d space
		float[] toMap = new float[] {(float) gazeAngleH, (float) gazeAngleV};
		
		trans_mat.mapPoints(toMap);
		//if (gazeAngleV < trans_mat_boundary)
		//	trans_mat_top.mapPoints(toMap);
		//else
		//	trans_mat_bottom.mapPoints(toMap);

		double gazePixelX = toMap[0];
		double gazePixelY = toMap[1];
		
		//Log.e(TAG, "X: " + gazePixelX + " Y: " + gazePixelY);
		
		transformed_gaze_pixel_x.addValue(gazePixelX);
		transformed_gaze_pixel_y.addValue(gazePixelY);
		
		//smoothing through rolling average over 8 frames (windowSize = 8)
		smoothed_gaze_pixel_x.addValue(gazePixelX);
		smoothed_gaze_pixel_y.addValue(gazePixelY);
		smoothed_gaze_pixel_history.add(Pair.create(smoothed_gaze_pixel_x.getMean(), smoothed_gaze_pixel_y.getMean()));
		
		//for comparison keep naive transform data calculations:
		//---------------------------------------------
		//normalize to min = ~0 for naive transform
		gazeAngleH = Math.max(gazeAngleH + 21.3, 0);
		gazeAngleV = Math.max(gazeAngleV - 1.2, 0);
		
		//naive transform
		gazePixelX = gazeAngleH * ppgaX;
		gazePixelY = gazeAngleV * ppgaY;
		
		naive_gaze_pixel_x.addValue(gazePixelX);
		naive_gaze_pixel_y.addValue(gazePixelY);
		//---------------------------------------------
	}
	
	/**
	 * Gets the average of the last 8 gaze pixel x,y values
	 */
	public Pair<Double, Double> getSmoothedGazePixels() {
		return smoothed_gaze_pixel_history.get(smoothed_gaze_pixel_history.size() - 1);
	}
	
	public void saveResultsCSV(final File result_dir) {
		try {
			FileWriter fw = new FileWriter(result_dir.getAbsolutePath() + "/" + System.currentTimeMillis() + ".csv");
			fw.write(toString());
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveResultsARFF(final File result_dir) {
		try {
			FileWriter fw = new FileWriter(result_dir.getAbsolutePath() + "/" + System.currentTimeMillis() + ".arff");
			fw.write(toARFFString());
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String toARFFString() {
		StringBuilder sb = new StringBuilder(10000);
		sb.append(ARFFData());
//		Log.d("FOO", ARFFData());
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(10000);
		sb.append(descriptiveStats());
		sb.append(dataTable());
		sb.append(frequencyTable());
		return sb.toString();
	}

	public String frequencyTable() {
		StringBuilder sb = new StringBuilder(1000);
		sb.append("\n");

		HashMap<String, Integer> counts = new HashMap<String, Integer>();

		int N = (int) left_eye_x.getN();
		for (int i = 0; i < N; i++) {
			String key = gaze_angle_h.getElement(i) + "," + gaze_angle_v.getElement(i);
			if (counts.containsKey(key)) {
				int val = counts.get(key).intValue();
				val = val + 1;
				counts.put(key, Integer.valueOf(val));
			} else {
				counts.put(key, Integer.valueOf(1));
			}
		}
		sb.append("AngleH,AngleV,Frequency\n");
		Set<Entry<String, Integer>> items = counts.entrySet();
		Iterator<Entry<String, Integer>> it = items.iterator();
		while (it.hasNext()) {
			Entry<String, Integer> entry = it.next();
			String key = entry.getKey();
			int frequency = entry.getValue();
			String[] parts = key.split(",");
			double angle_h = Double.parseDouble(parts[0]);
			double angle_v = Double.parseDouble(parts[1]);
			sb.append(angle_h);
			sb.append(",");
			sb.append(angle_v);
			sb.append(",");
			sb.append(frequency);
			sb.append("\n");

		}

		return sb.toString();
	}

	public String descriptiveStats() {
		StringBuilder sb = new StringBuilder(1000);

		sb.append("NumFrames,");
		sb.append(left_eye_x.getN());
		sb.append("\n");

		sb.append(",LeftEyeX,LeftEyeY,RightEyeX,RightEyeY,MouthX,MouthY,GazePointX,GazePointY,GazeAngleH,GazeAngleV,Roll,Pitch,Yaw\n");

		sb.append("mean,");
		sb.append(Math.round(left_eye_x.getMean()));
		sb.append(",");
		sb.append(Math.round(left_eye_y.getMean()));
		sb.append(",");
		sb.append(Math.round(right_eye_x.getMean()));
		sb.append(",");
		sb.append(Math.round(right_eye_y.getMean()));
		sb.append(",");
		sb.append(Math.round(mouth_x.getMean()));
		sb.append(",");
		sb.append(Math.round(mouth_y.getMean()));
		sb.append(",");
		sb.append(df.format(gaze_point_x.getMean()));
		sb.append(",");
		sb.append(df.format(gaze_point_y.getMean()));
		sb.append(",");
		sb.append(Math.round(gaze_angle_h.getMean()));
		sb.append(",");
		sb.append(Math.round(gaze_angle_v.getMean()));
		sb.append(",");
		sb.append(Math.round(roll.getMean()));
		sb.append(",");
		sb.append(Math.round(pitch.getMean()));
		sb.append(",");
		sb.append(Math.round(yaw.getMean()));
		sb.append("\n");
		sb.append("variance,");
		sb.append(Math.round(left_eye_x.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(left_eye_y.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(right_eye_x.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(right_eye_y.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(mouth_x.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(mouth_y.getPopulationVariance()));
		sb.append(",");
		sb.append(df.format(gaze_point_x.getPopulationVariance()));
		sb.append(",");
		sb.append(df.format(gaze_point_y.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(gaze_angle_h.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(gaze_angle_v.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(roll.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(pitch.getPopulationVariance()));
		sb.append(",");
		sb.append(Math.round(yaw.getPopulationVariance()));
		sb.append("\n");
		sb.append("sd,");
		sb.append(Math.round(Math.sqrt(left_eye_x.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(left_eye_y.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(right_eye_x.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(right_eye_y.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(mouth_x.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(mouth_y.getPopulationVariance())));
		sb.append(",");
		sb.append(df.format(Math.sqrt(gaze_point_x.getPopulationVariance())));
		sb.append(",");
		sb.append(df.format(Math.sqrt(gaze_point_y.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(gaze_angle_h.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(gaze_angle_v.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(roll.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(pitch.getPopulationVariance())));
		sb.append(",");
		sb.append(Math.round(Math.sqrt(yaw.getPopulationVariance())));
		sb.append("\n\n");

		return sb.toString();
	}

	public String ARFFData() {

		StringBuilder sb = new StringBuilder(10000);
		sb.append("@relation eyegaze");
		sb.append("\n\n");
		sb.append("@attribute MarkerX1 numeric\n@attribute MarkerY1 numeric\n@attribute MarkerX2 numeric\n@attribute MarkerY2 numeric\n@attribute LeftEyeX numeric\n"
				+ "@attribute LeftEyeY numeric\n@attribute RightEyeX numeric\n@attribute RightEyeY numeric\n" + "@attribute MouthX numeric\n@attribute MouthY numeric\n@attribute GazePointX numeric\n"
				+ "@attribute GazePointY numeric\n@attribute GazeAngleH numeric\n@attribute GazeAngleV numeric\n" + "@attribute Roll numeric\n@attribute Pitch numeric\n@attribute Yaw numeric\n"
				+ "@attribute GazePixelX numeric\n@attribute GazePixelY numeric\n@attribute TransformedGazePixelX\n@attribute TransformedGazePixelY\n@attribute SmoothedGazePixelX\n@attribute SmoothedGazePixelY");
		sb.append("\n\n@data\n");
		
		int N = (int) left_eye_x.getN();

		for (int i = 0; i < N; i++) {
			sb.append(marker_rects.get(i).left);
			sb.append(",");
			sb.append(marker_rects.get(i).top);
			sb.append(",");
			sb.append(marker_rects.get(i).right);
			sb.append(",");
			sb.append(marker_rects.get(i).bottom);
			sb.append(",");
			sb.append(left_eye_x.getElement(i));
			sb.append(",");
			sb.append(left_eye_y.getElement(i));
			sb.append(",");
			sb.append(right_eye_x.getElement(i));
			sb.append(",");
			sb.append(right_eye_y.getElement(i));
			sb.append(",");
			sb.append(mouth_x.getElement(i));
			sb.append(",");
			sb.append(mouth_y.getElement(i));
			sb.append(",");
			sb.append(gaze_point_x.getElement(i));
			sb.append(",");
			sb.append(gaze_point_y.getElement(i));
			sb.append(",");
			sb.append(gaze_angle_h.getElement(i));
			sb.append(",");
			sb.append(gaze_angle_v.getElement(i));
			sb.append(",");
			sb.append(roll.getElement(i));
			sb.append(",");
			sb.append(pitch.getElement(i));
			sb.append(",");
			sb.append(yaw.getElement(i));
			sb.append(",");
			sb.append(naive_gaze_pixel_x.getElement(i));
			sb.append(",");
			sb.append(naive_gaze_pixel_y.getElement(i));
			sb.append(",");
			sb.append(transformed_gaze_pixel_x.getElement(i));
			sb.append(",");
			sb.append(transformed_gaze_pixel_y.getElement(i));
			sb.append(",");
			sb.append(smoothed_gaze_pixel_history.get(i).first);
			sb.append(",");
			sb.append(smoothed_gaze_pixel_history.get(i).second);
			sb.append("\n");
		}

		return sb.toString();
	}

	public String dataTable() {

		StringBuilder sb = new StringBuilder(10000);

		int N = (int) left_eye_x.getN();
		sb.append("Frame,MarkerX1,MarkerY1,MarkerX2,MarkerY2,LeftEyeX,LeftEyeY,RightEyeX,RightEyeY,MouthX,MouthY,GazePointX,GazePointY,GazeAngleH,GazeAngleV,Roll,Pitch,Yaw,GazePixelX,GazePixelY,TransformedGazePixelX,TransformedGazePixelY,SmoothedGazePixelX,SmoothedGazePixelY\n");
		
		for (int i = 0; i < N; i++) {
			sb.append(i + 1);
			sb.append(",");
			sb.append(marker_rects.get(i).left);
			sb.append(",");
			sb.append(marker_rects.get(i).top);
			sb.append(",");
			sb.append(marker_rects.get(i).right);
			sb.append(",");
			sb.append(marker_rects.get(i).bottom);
			sb.append(",");
			sb.append(left_eye_x.getElement(i));
			sb.append(",");
			sb.append(left_eye_y.getElement(i));
			sb.append(",");
			sb.append(right_eye_x.getElement(i));
			sb.append(",");
			sb.append(right_eye_y.getElement(i));
			sb.append(",");
			sb.append(mouth_x.getElement(i));
			sb.append(",");
			sb.append(mouth_y.getElement(i));
			sb.append(",");
			sb.append(gaze_point_x.getElement(i));
			sb.append(",");
			sb.append(gaze_point_y.getElement(i));
			sb.append(",");
			sb.append(gaze_angle_h.getElement(i));
			sb.append(",");
			sb.append(gaze_angle_v.getElement(i));
			sb.append(",");
			sb.append(roll.getElement(i));
			sb.append(",");
			sb.append(pitch.getElement(i));
			sb.append(",");
			sb.append(yaw.getElement(i));
			sb.append(",");
			sb.append(naive_gaze_pixel_x.getElement(i));
			sb.append(",");
			sb.append(naive_gaze_pixel_y.getElement(i));
			sb.append(",");
			sb.append(transformed_gaze_pixel_x.getElement(i));
			sb.append(",");
			sb.append(transformed_gaze_pixel_y.getElement(i));
			sb.append(",");
			sb.append(smoothed_gaze_pixel_history.get(i).first);
			sb.append(",");
			sb.append(smoothed_gaze_pixel_history.get(i).second);
			sb.append("\n");
		}
		
		return sb.toString();
	}
}