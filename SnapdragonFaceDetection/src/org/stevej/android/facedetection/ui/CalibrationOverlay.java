package org.stevej.android.facedetection.ui;

import org.stevej.android.facedetection.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

public class CalibrationOverlay extends DisplaySurface {
	private static final String	TAG				= "CalibrationOverlay";
	private Paint				marker_paint;
	private Paint				marker_centre_paint;
	private Paint				rect_paint;
	private Paint				text_paint;

	private DrawingThread		drawing_thread	= null;

	public CalibrationOverlay(Context context, AttributeSet attributes) {
		super(context, attributes, R.id.calibration_overlay);
		marker_paint = new Paint();
		marker_paint.setColor(Color.YELLOW);
		marker_paint.setStyle(Style.FILL_AND_STROKE);

		marker_centre_paint = new Paint(marker_paint);
		marker_centre_paint.setColor(Color.RED);

		rect_paint = new Paint(marker_paint);
		rect_paint.setStyle(Style.STROKE);

		text_paint = new Paint(marker_paint);
		text_paint.setTextSize(32f);

	}

	@Override
	public void shutDown() {
		stop();
		super.shutDown();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		super.surfaceDestroyed(holder);
		stop();
	}

	public void start() {
		drawing_thread = new DrawingThread(surface_holder);
		drawing_thread.setRunning(true);
		drawing_thread.start();
		Log.d(TAG, "started drawing thread");
	}

	public void stop() {
		if (drawing_thread != null) {
			drawing_thread.setRunning(false);
			boolean retry = true;
			while (retry) {
				try {
					drawing_thread.join();
					retry = false;
					Log.d(TAG, "stopped drawing thread");
				} catch (InterruptedException e) {
				}
			}
		}
		drawing_thread = null;

		canvas = surface_holder.lockCanvas();
		if (canvas != null) {
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
			surface_holder.unlockCanvasAndPost(canvas);
		}

	}

	class DrawingThread extends Thread {
		static final long		TARGET_FPS			= 30;
		private boolean			running				= false;
		private SurfaceHolder	surface_holder;
		private int				marker_radius;
		private int				marker_x;
		private int				marker_y;
		private int				speed				= 4;
		private Rect[]			markers;
		private int				marker_index		= 0;
		private int				frame_count			= 0;
		private final int		frames_per_marker	= 300;

		private final Object	run_lock			= new Object();

		public DrawingThread(SurfaceHolder holder) {
			surface_holder = holder;
		}

		public void setRunning(boolean run) {
			synchronized (run_lock) {
				if (run) {
					marker_radius = 60;
					marker_x = 0;
					marker_y = marker_radius;
					markers = new Rect[5];
					markers[0] = new Rect(0, 0, marker_radius, marker_radius);
					markers[1] = new Rect(width - marker_radius, 0, width, marker_radius);
					markers[2] = new Rect(width - marker_radius, height - marker_radius, width, height);
					markers[3] = new Rect(0, height - marker_radius, marker_radius, height);
					markers[4] = new Rect(width / 2 - marker_radius / 2, height / 2 - marker_radius / 2, width / 2 + marker_radius / 2, height / 2 + marker_radius / 2);
				}
				running = run;
			}
		}

		private void updateMarker() {
			marker_index = (int) (frame_count / frames_per_marker) % 5;
			frame_count++;
			// Log.d(TAG, "mi = " + marker_index + ", fc = " + frame_count);
			// marker_x = marker_x + speed;
			// if (marker_x > getWidth()) {
			// marker_x = getWidth();
			// speed = -speed;
			// } else if (marker_x < 0) {
			// marker_x = 0;
			// speed = -speed;
			// }

		}

		private void doDraw(Canvas canvas) {

			// canvas.drawColor(Color.BLACK);
			// canvas.drawRect(20, 20, 40, 40, marker_paint);

			// // // canvas.drawColor(Color.BLACK);
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

			// canvas.drawRect(marker_x - marker_radius, marker_y - marker_radius, marker_x + marker_radius, marker_y + marker_radius, marker_paint);
			canvas.drawRect(markers[marker_index], marker_paint);

		}

		@Override
		public void run() {
			long ticksPS = 1000 / TARGET_FPS;
			long startTime;
			long sleepTime;
			while (running) {
				startTime = System.currentTimeMillis();
				try {
					canvas = surface_holder.lockCanvas();
					synchronized (surface_holder) {
						updateMarker();
						synchronized (run_lock) {
							if (running) {
								doDraw(canvas);
							}
						}
					}
				} finally {
					if (canvas != null) {
						surface_holder.unlockCanvasAndPost(canvas);
					}
				}
				sleepTime = ticksPS - (System.currentTimeMillis() - startTime);
				try {
					if (sleepTime > 0)
						sleep(sleepTime);
					else
						sleep(10);
				} catch (Exception e) {
				}
			}
		}
	}

}