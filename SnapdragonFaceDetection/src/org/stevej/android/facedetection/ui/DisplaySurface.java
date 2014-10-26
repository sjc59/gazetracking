package org.stevej.android.facedetection.ui;

import java.io.IOException;

import org.stevej.android.facedetection.R;
import org.stevej.android.facedetection.constants.Constants.VALUES;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DisplaySurface extends SurfaceView implements SurfaceHolder.Callback {
	private static final String		TAG				= "DisplaySurface";
	protected SurfaceHolder			surface_holder	= null;
	protected SurfaceReadyListener	listener;
	public Handler					message_handler	= null;
	protected Canvas				canvas;
	protected HandlerThread			handlerThread;
	protected Looper				looper;
	protected int					surface_id;
	protected int					width;
	protected int					height;

	public DisplaySurface(Context context, AttributeSet attributes, int id) {
		super(context, attributes);

		surface_id = id;
		surface_holder = getHolder();
		surface_holder.addCallback(this);
		surface_holder.setFormat(PixelFormat.RGBA_8888);
		setZOrderMediaOverlay(true);

		handlerThread = new HandlerThread("HandlerThread");
		handlerThread.start();
		looper = handlerThread.getLooper();
	}

	public void shutDown() {
		if (message_handler != null) {
			message_handler.removeCallbacksAndMessages(null);
		}
	}

	public void setSurfaceReadyListener(SurfaceReadyListener _listener) {
		listener = _listener;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		switch (surface_id) {
			case R.id.video_preview:
				Log.d(TAG, "surfaceChanged() : video preview");
				break;
			case R.id.calibration_overlay:
				Log.d(TAG, "surfaceChanged() : calibration overlay");
				break;
			case R.id.facial_feature_overlay:
				Log.d(TAG, "surfaceChanged() : facial feature overlay");
				break;
		}
		listener.onSurfaceReady(surface_id);
		this.width = width;
		this.height = height;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		switch (surface_id) {
			case R.id.video_preview:
				Log.d(TAG, "surfaceCreated() : video preview");
				break;
			case R.id.calibration_overlay:
				Log.d(TAG, "surfaceCreated() : calibration overlay");
				break;
			case R.id.facial_feature_overlay:
				Log.d(TAG, "surfaceCreated() : facial feature overlay");
				break;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		switch (surface_id) {
			case R.id.camera_preview:
				Log.d(TAG, "surfaceDestroyed() : camera preview");
				break;
			case R.id.video_preview:
				Log.d(TAG, "surfaceDestroyed() : video preview");
				break;
			case R.id.calibration_overlay:
				Log.d(TAG, "surfaceDestroyed() : calibration overlay");
				break;
			case R.id.facial_feature_overlay:
				Log.d(TAG, "surfaceDestroyed() : facial feature overlay");
				break;
		}
	}
}