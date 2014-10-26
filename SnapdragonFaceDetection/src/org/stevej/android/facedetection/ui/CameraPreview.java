package org.stevej.android.facedetection.ui;

import org.stevej.android.facedetection.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	private static final String		TAG				= "CameraPreview";
	private SurfaceHolder			surface_holder	= null;
	private SurfaceReadyListener	listener;

	public CameraPreview(Context context, AttributeSet attributes) {
		super(context, attributes);

		// Log.d(TAG, "S CameraPreview()");

		surface_holder = getHolder();
		surface_holder.addCallback(this);
		// Log.d(TAG, "F CameraPreview()");
	}

	public void setSurfaceReadyListener(SurfaceReadyListener _listener) {
		listener = _listener;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		// Log.d(TAG, "S surfaceChanged() : " + width + " x " + height);

		listener.onSurfaceReady(R.id.camera_preview);

		// Log.d(TAG, "F surfaceChanged()");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		// Log.d(TAG, "S surfaceCreated()");
		// Log.d(TAG, "F surfaceCreated()");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		// Log.d(TAG, "S surfaceDestroyed()");
		// Log.d(TAG, "F surfaceDestroyed()");

	}
}