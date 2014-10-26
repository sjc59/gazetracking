package org.stevej.android.facedetection.processing;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;

import org.stevej.android.facedetection.stats.CompleteFrameStats;
import org.stevej.android.facedetection.stats.FPS;
import org.stevej.android.facedetection.ui.FacialFeatureOverlay;
import org.stevej.android.facedetection.utils.ImageUtils;
import org.stevej.android.facedetection.video.VideoFileFrameGrabber;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.qualcomm.snapdragon.sdk.face.FaceData;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.PREVIEW_ROTATION_ANGLE;

public class FrameProcessor implements PreviewCallback, Callback {
	private static final String					TAG						= "FrameProcessor";

	private FacialProcessing					face_detector;
	private EnumSet<FacialProcessing.FP_DATA>	features				= EnumSet.of(FacialProcessing.FP_DATA.FACE_COORDINATES, FacialProcessing.FP_DATA.FACE_GAZE,
																				FacialProcessing.FP_DATA.FACE_ORIENTATION);

	private FacialFeatureOverlay				face_overlay;
	private int									preview_frame_width		= 640;
	private int									preview_frame_height	= 480;
	private int									preview_display_width	= 1920;
	private int									preview_display_height	= 1080;

	private boolean								normalizing				= true;
	private boolean								mirroring				= false;
	private boolean								analysing				= false;

	private Object								lock					= new Object();

	private CompleteFrameStats					calibration_data;

	public Handler								message_handler;
	private HandlerThread						handlerThread;
	private Looper								looper;

	private VideoFileFrameGrabber				vffg					= null;
	private int[]								marker_pos				= new int[4];

	@Override
	public boolean handleMessage(Message message) {
		onPreviewFrame((byte[]) message.obj);
		return true;
	}

	public FrameProcessor(Context context) {
		face_detector = FacialProcessing.getInstance();

		handlerThread = new HandlerThread("ProcessorThread");
		handlerThread.start();
		looper = handlerThread.getLooper();
		message_handler = new Handler(looper, this);
		face_detector.setProcessingMode(FacialProcessing.FP_MODES.FP_MODE_VIDEO); // PHOTO???

		calibration_data = new CompleteFrameStats();
	}

	public void shutDown() {
		synchronized (lock) {
			Log.d(TAG, "shutDown()");
			message_handler.removeCallbacksAndMessages(null);
			if (face_detector != null) {
				face_detector.release();
			}
			Log.d(TAG, "done shutDown()");
		}
	}

	public void setDimensions(int preview_width, int preview_height, int display_width, int display_height) {
		synchronized (lock) {
			preview_frame_width = preview_width;
			preview_frame_height = preview_height;
			preview_display_width = display_width;
			preview_display_height = display_height;
			normalizing = (preview_frame_width != preview_display_width) || (preview_frame_height != preview_display_height);
			ImageUtils.deinit();
			ImageUtils.init(preview_frame_width, preview_frame_height);
			Log.d(TAG, "preview dimensions : " + preview_frame_width + " x " + preview_frame_height);
			Log.d(TAG, "display dimensions : " + preview_display_width + " x " + preview_display_height);
			Log.d(TAG, "normalizing : " + normalizing);
		}
	}

	public void setMirroring(boolean mirror) {
		synchronized (lock) {
			mirroring = mirror;
		}
	}

	public void setAnalysing(boolean analyse) {
		synchronized (lock) {
			analysing = analyse;
		}
	}

	public void dumpStats(final File result_dir, final boolean clear) {
		calibration_data.saveResultsCSV(result_dir);
		if (clear) {
			calibration_data = new CompleteFrameStats();
		}
	}

	public void setFaceOverlay(FacialFeatureOverlay face_overlay) {
		synchronized (lock) {
			this.face_overlay = face_overlay;
		}
	}

	public void setVideoFileFrameGrabber(VideoFileFrameGrabber grabber) {
		synchronized (lock) {
			this.vffg = grabber;
		}
	}

	@Override
	public void onPreviewFrame(byte[] preview_buffer, Camera camera) {
		onPreviewFrame(preview_buffer);

		camera.addCallbackBuffer(preview_buffer);

		// Log.d(TAG, "F onPreviewFrame()");
	}

	public void onPreviewFrame(byte[] preview_buffer) {
		// Log.d(TAG, "S onPreviewFrame()");
		FPS.startFrame();
		synchronized (lock) {
			if (analysing) {
				ImageUtils.findMarker(preview_buffer, preview_frame_width, preview_frame_height, marker_pos, mirroring);
			}

			if (normalizing) {
				face_detector.normalizeCoordinates(preview_display_width, preview_display_height);
			}
			face_detector.setFrame(preview_buffer, preview_frame_width, preview_frame_height, mirroring, PREVIEW_ROTATION_ANGLE.ROT_0);
			FaceData[] face_data = face_detector.getFaceData(features);

			CalibrationFrameData data;
			if (analysing) {
				data = new CalibrationFrameData(face_data, Arrays.copyOf(marker_pos, 4), calibration_data);
			} else {
				data = new CalibrationFrameData(face_data, null, calibration_data);
			}
			
			Message message = calibration_data.message_handler.obtainMessage();
			message.obj = data;
			message.sendToTarget();

			message = face_overlay.message_handler.obtainMessage();
			message.obj = data;
			message.sendToTarget();

			if (vffg != null) {
				message = vffg.message_handler.obtainMessage();
				message.obj = preview_buffer;
				message.sendToTarget();
			}
			// Log.d(TAG, "F onPreviewFrame()");
		}
		FPS.endFrame();
	}
}
