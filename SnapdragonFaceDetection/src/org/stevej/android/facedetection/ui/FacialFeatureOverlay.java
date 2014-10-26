package org.stevej.android.facedetection.ui;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.stevej.android.facedetection.R;
import org.stevej.android.facedetection.processing.CalibrationFrameData;
import org.stevej.android.facedetection.stats.FPS;
import org.stevej.android.facedetection.stats.CompleteFrameStats;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;

import com.qualcomm.snapdragon.sdk.face.FaceData;

public class FacialFeatureOverlay extends DisplaySurface implements Callback {
	private static final String	TAG					= "FacialFeatureOverlay";
	private Paint				marker_paint;
	private Paint				marker_centre_paint;
	private Paint				gazepoint_paint;
	private Paint				rect_paint;
	private Paint				text_paint;
	private Rect				marker_rect			= new Rect();
	private Rect				gazepoint_rect 		= new Rect();
	private int 				gazeAngleH;
	private int 				gazeAngleV;
	
	private DescriptiveStatistics accuracyX			= new DescriptiveStatistics();
	private DescriptiveStatistics accuracyY			= new DescriptiveStatistics();

	@Override
	public boolean handleMessage(Message message) {
//		Log.d(TAG, "S handleMessage()");

		canvas = surface_holder.lockCanvas();
		if (canvas == null) {
			surface_holder.unlockCanvasAndPost(canvas);
			return true;
		}
		CalibrationFrameData data = (CalibrationFrameData) message.obj;
		if (data == null) {
			Log.d(TAG, "F handleMessage() : null data");
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
			surface_holder.unlockCanvasAndPost(canvas);
			return true;
		}
		FaceData[] faces = data.face_data;
		int[] marker = data.marker;

		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

		if (faces != null) {
			if (faces.length == 1) {
				FaceData face = faces[0];
				canvas.drawCircle(face.leftEye.x, face.leftEye.y, 10, marker_paint);
				canvas.drawCircle(face.rightEye.x, face.rightEye.y, 10, marker_paint);
				canvas.drawCircle(face.mouth.x, face.mouth.y, 10, marker_paint);
				canvas.drawRect(face.rect, rect_paint);
				
				Pair<Double, Double> smoothedPixels = data.global_stats.getSmoothedGazePixels();
				float pixelsX = smoothedPixels.first.floatValue();
				float pixelsY = smoothedPixels.second.floatValue();
				
				canvas.drawCircle(pixelsX, pixelsY, 100, gazepoint_paint);
			}
		}
		if (marker != null) {
			marker_rect.set(marker[0], marker[1], marker[2], marker[3]);
			canvas.drawRect(marker_rect, rect_paint);
		}
		canvas.drawText(FPS.getFpsString(), 100, 100, text_paint);
		canvas.drawText(Long.toString(FPS.getFrameCount()), 100, 300, text_paint);
		surface_holder.unlockCanvasAndPost(canvas);

//		Log.d(TAG, "F handleMessage()");
		return true;
	}

	public FacialFeatureOverlay(Context context, AttributeSet attributes) {
		super(context, attributes, R.id.facial_feature_overlay);
		marker_paint = new Paint();
		marker_paint.setColor(Color.YELLOW);
		marker_paint.setStyle(Style.FILL_AND_STROKE);

		marker_centre_paint = new Paint(marker_paint);
		marker_centre_paint.setColor(Color.RED);

		rect_paint = new Paint(marker_centre_paint);
		rect_paint.setStrokeWidth(3f);
		rect_paint.setStyle(Style.STROKE);

		text_paint = new Paint(marker_paint);
		text_paint.setTextSize(48f);
		
		gazepoint_paint = new Paint();
		gazepoint_paint.setColor(Color.GREEN);
		gazepoint_paint.setStyle(Style.FILL_AND_STROKE);

		message_handler = new Handler(looper, this);

	}

}