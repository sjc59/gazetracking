package org.stevej.android.facedetection;

import java.io.File;
import java.io.IOException;

import org.stevej.android.facedetection.constants.Constants.DEFAULTS;
import org.stevej.android.facedetection.constants.Constants.KEYS;
import org.stevej.android.facedetection.constants.Constants.VALUES;
import org.stevej.android.facedetection.preferences.Preferences;
import org.stevej.android.facedetection.processing.FrameProcessor;
import org.stevej.android.facedetection.ui.CalibrationOverlay;
import org.stevej.android.facedetection.ui.CameraPreview;
import org.stevej.android.facedetection.ui.FacialFeatureOverlay;
import org.stevej.android.facedetection.ui.PlaybackController;
import org.stevej.android.facedetection.ui.SurfaceReadyListener;
import org.stevej.android.facedetection.ui.VideoPreview;
import org.stevej.android.facedetection.video.VideoFileFrameGrabber;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CalibrationActivity extends Activity implements SurfaceReadyListener, OnClickListener {
	static {
		System.loadLibrary("FaceDetection");
	}
	private static final String		TAG					= "CalibrationActivity";
	private RelativeLayout			controls;
	private PlaybackController		playback_controller;
	private CameraPreview			camera_preview;
	private VideoPreview			video_preview;
	private CalibrationOverlay		calibration_overlay;
	private FacialFeatureOverlay	face_overlay;
	private Camera					camera;
	private byte[][]				preview_buffers;
	private int						preview_frame_width;
	private int						preview_frame_height;
	private int						preview_display_width;
	private int						preview_display_height;

	private boolean					calibrating			= false;
	private boolean					detecting			= false;
	private boolean					mirroring			= false;
	private boolean					analysing			= false;
	private boolean					controls_visible	= true;

	private VideoFileFrameGrabber	video_file_frame_grabber;

	private FrameProcessor			frame_processor;
	private SharedPreferences		shared_preferences;

	private GestureDetector			gesture_detector;

	private File					data_directory;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "S onCreate()");

		setContentView(R.layout.calibration_activity);

		if (savedInstanceState != null) {
			mirroring = savedInstanceState.getBoolean("mirroring");
		}
		shared_preferences = PreferenceManager.getDefaultSharedPreferences(this);

		if (!shared_preferences.getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
			Preferences.setDefaults(this);
		}

		configureUI();

		data_directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "FaceDetection/");
		data_directory.mkdirs();
		Log.d(TAG, "F onCreate()");
	}

	private void configureUI() {

		controls = (RelativeLayout) findViewById(R.id.controls);

		camera_preview = (CameraPreview) findViewById(R.id.camera_preview);
		video_preview = (VideoPreview) findViewById(R.id.video_preview);
		calibration_overlay = (CalibrationOverlay) findViewById(R.id.calibration_overlay);
		face_overlay = (FacialFeatureOverlay) findViewById(R.id.facial_feature_overlay);

		FrameLayout.LayoutParams layout_params = new FrameLayout.LayoutParams(preview_display_width, preview_display_height);
		camera_preview.setLayoutParams(layout_params);
		video_preview.setLayoutParams(layout_params);
		calibration_overlay.setLayoutParams(layout_params);
		face_overlay.setLayoutParams(layout_params);

		video_file_frame_grabber = new VideoFileFrameGrabber();
		video_file_frame_grabber.setPreviewDisplay(video_preview);

		playback_controller = new PlaybackController(this, video_file_frame_grabber);
		video_file_frame_grabber.setPlaybackController(playback_controller);

		camera_preview.setSurfaceReadyListener(this);
		video_preview.setSurfaceReadyListener(this);
		calibration_overlay.setSurfaceReadyListener(this);
		face_overlay.setSurfaceReadyListener(this);

		ImageButton mirror_button = (ImageButton) findViewById(R.id.mirror);
		mirror_button.setOnClickListener(this);

		ImageButton calibration_button = (ImageButton) findViewById(R.id.calibration);
		calibration_button.setOnClickListener(this);

		ImageButton settings_button = (ImageButton) findViewById(R.id.show_settings);
		settings_button.setOnClickListener(this);

		ImageButton detection_button = (ImageButton) findViewById(R.id.detection);
		detection_button.setOnClickListener(this);

		ImageButton analysis_button = (ImageButton) findViewById(R.id.analysis);
		analysis_button.setOnClickListener(this);

		gesture_detector = new GestureDetector(this, new MyGestureListener());
	}

	private void setImmersiveMode(boolean immersive_on) {
		int ui_options = 0;
		if (immersive_on) {
			ui_options ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			ui_options ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
			ui_options ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}
		getWindow().getDecorView().setSystemUiVisibility(ui_options);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Log.d(TAG, "onTouchEvent()");
		gesture_detector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.mirror:
				Log.d(TAG, "onClick(mirror)");
				toggleMirroring();
				break;
			case R.id.calibration:
				Log.d(TAG, "onClick(calibration)");
				toggleCalibration();
				break;
			case R.id.detection:
				Log.d(TAG, "onClick(detection)");
				toggleDetection();
				break;
			case R.id.analysis:
				Log.d(TAG, "onClick(analysis)");
				toggleAnalysis();
				break;
			case R.id.show_settings:
				Log.d(TAG, "onClick(settings)");
				Intent preferences_activity = new Intent(getApplicationContext(), Preferences.class);
				startActivity(preferences_activity);
				break;
		}
	}

	private void showView(View v, boolean to_front) {
		switch (v.getId()) {
			case R.id.controls:
				Log.d(TAG, "showView(controls, " + to_front + ")");
				break;
			case R.id.calibration_overlay:
				Log.d(TAG, "showView(calibration_overlay, " + to_front + ")");
				break;
			case R.id.facial_feature_overlay:
				Log.d(TAG, "showView(facial_feature_overlay, " + to_front + ")");
				break;
			case R.id.camera_preview:
				Log.d(TAG, "showView(camera_preview, " + to_front + ")");
				break;
			case R.id.video_preview:
				Log.d(TAG, "showView(video_preview, " + to_front + ")");
				break;
			case R.id.video_controls:
				Log.d(TAG, "showView(video_controls, " + to_front + ")");
				break;
		}
		v.setVisibility(View.VISIBLE);
		if (to_front) {
			v.bringToFront();
		}
	}

	private void hideView(View v) {
		switch (v.getId()) {
			case R.id.controls:
				Log.d(TAG, "hideView(controls)");
				break;
			case R.id.calibration_overlay:
				Log.d(TAG, "hideView(calibration_overlay)");
				break;
			case R.id.facial_feature_overlay:
				Log.d(TAG, "hideView(facial_feature_overlay)");
				break;
			case R.id.camera_preview:
				Log.d(TAG, "hideView(camera_preview)");
				break;
			case R.id.video_preview:
				Log.d(TAG, "hideView(video_preview)");
				break;
			case R.id.video_controls:
				Log.d(TAG, "hideView(video_controls)");
				break;
		}
		v.setVisibility(View.INVISIBLE);
	}

	private void toggleMirroring() {
		ImageButton mirror_button = (ImageButton) findViewById(R.id.mirror);
		if (mirroring) {
			mirror_button.setImageResource(R.drawable.flip_horizontal);
			mirroring = false;
		} else {
			mirror_button.setImageResource(R.drawable.flip_horizontal_on);
			mirroring = true;
		}

		int frame_provider = shared_preferences.getInt(KEYS.FRAME_PROVIDER, DEFAULTS.FRAME_PROVIDER);
		boolean playing = playback_controller.isPlaying();
		if (frame_provider == VALUES.VIDEO_FILE && playing) {
			playback_controller.pausePlayback();
		}
		frame_processor.setMirroring(mirroring);
		video_preview.setMirroring(mirroring);
		if (frame_provider == VALUES.VIDEO_FILE && playing) {
			try {
				Thread.sleep(250);
				playback_controller.startPlayback();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void startCalibration() {
		showView(calibration_overlay, true);
		calibration_overlay.start();
		calibrating = true;

		ImageButton calibration_button = (ImageButton) findViewById(R.id.calibration);
		calibration_button.setImageResource(R.drawable.calibrate_on);

		hideView(controls);
	}

	private void stopCalibration() {
		ImageButton calibration_button = (ImageButton) findViewById(R.id.calibration);
		calibration_overlay.stop();
		calibrating = false;
		calibration_button.setImageResource(R.drawable.calibrate);
		hideView(calibration_overlay);
	}

	private void startAnalysis() {
		// Debug.startMethodTracing("facedetection", 500000000);
		analysing = true;

		boolean playing = playback_controller.isPlaying();
		if (playing) {
			playback_controller.pausePlayback();
		}
		frame_processor.setAnalysing(analysing);

		startDetection();

		ImageButton detection_button = (ImageButton) findViewById(R.id.detection);
		detection_button.setEnabled(false);

		if (playing) {
			try {
				Thread.sleep(250);
				playback_controller.startPlayback();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		ImageButton analysis_button = (ImageButton) findViewById(R.id.analysis);
		analysis_button.setImageResource(R.drawable.analysis_on);
	}

	private void stopAnalysis() {
		// Debug.stopMethodTracing();
		stopDetection();
		analysing = false;
		frame_processor.setAnalysing(analysing);
		Thread thread = new Thread(new Runnable() {
			public void run() {
				frame_processor.dumpStats(data_directory, true);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(CalibrationActivity.this, "Calibration data saved", Toast.LENGTH_SHORT).show();
					}
				});

			}
		});
		thread.start();

		ImageButton detection_button = (ImageButton) findViewById(R.id.detection);
		detection_button.setEnabled(true);
		ImageButton analysis_button = (ImageButton) findViewById(R.id.analysis);
		analysis_button.setImageResource(R.drawable.analysis);
	}

	private void toggleCalibration() {
		if (calibrating) {
			stopCalibration();
		} else {
			startCalibration();
		}
	}

	private void toggleAnalysis() {
		if (analysing) {
			stopAnalysis();
		} else {
			startAnalysis();
		}
	}

	private void stopDetection() {
		// frame_processor.stop();

		int frame_provider = shared_preferences.getInt(KEYS.FRAME_PROVIDER, DEFAULTS.FRAME_PROVIDER);

		if (frame_provider == VALUES.VIDEO_FILE) {
			video_file_frame_grabber.setPreviewCallback(null);
			frame_processor.setVideoFileFrameGrabber(null);
		} else if (frame_provider == VALUES.LIVE_PREVIEW) {
			camera.setPreviewCallbackWithBuffer(null);
		}
		Message message = face_overlay.message_handler.obtainMessage();
		message.obj = null;
		message.sendToTarget();
		face_overlay.message_handler.removeCallbacksAndMessages(null);
		video_preview.message_handler.removeCallbacksAndMessages(null);

		message = face_overlay.message_handler.obtainMessage();
		message.obj = null;
		face_overlay.message_handler.sendMessageDelayed(message, 500);

		ImageButton detection_button = (ImageButton) findViewById(R.id.detection);
		detection_button.setImageResource(R.drawable.face_detection);
		detecting = false;

		// frame_processor.dumpStats(true);
	}

	private void startDetection() {
		int frame_provider = shared_preferences.getInt(KEYS.FRAME_PROVIDER, DEFAULTS.FRAME_PROVIDER);

		if (frame_provider == VALUES.VIDEO_FILE) {
			video_file_frame_grabber.setPreviewCallback(frame_processor);
			frame_processor.setVideoFileFrameGrabber(video_file_frame_grabber);
		} else if (frame_provider == VALUES.LIVE_PREVIEW) {
			setCameraPreviewCallback(camera, preview_frame_width, preview_frame_height);
		}

		ImageButton detection_button = (ImageButton) findViewById(R.id.detection);
		detection_button.setImageResource(R.drawable.face_detection_on);
		detecting = true;

		showView(face_overlay, true);
	}

	private void toggleDetection() {
		if (detecting) {
			stopDetection();
		} else {
			startDetection();
		}
	}

	private void setCameraPreviewCallback(Camera camera, int preview_frame_width, int preview_frame_height) {
		Parameters camera_parameters = camera.getParameters();
		int camera_preview_format = camera_parameters.getPreviewFormat();
		int buffer_size = preview_frame_width * preview_frame_height * ImageFormat.getBitsPerPixel(camera_preview_format) / 8;
		preview_buffers = new byte[3][buffer_size];
		camera.addCallbackBuffer(preview_buffers[0]);
		camera.addCallbackBuffer(preview_buffers[1]);
		camera.addCallbackBuffer(preview_buffers[2]);
		camera.setPreviewCallbackWithBuffer(frame_processor);
	}

	public void onSurfaceReady(int surface_id) {
		Log.d(TAG, "S onSurfaceReady()");

		int frame_provider = shared_preferences.getInt(KEYS.FRAME_PROVIDER, DEFAULTS.FRAME_PROVIDER);

		switch (surface_id) {
			case R.id.camera_preview:
				Log.d(TAG, "onSurfaceReady() : camera preview");
				if (frame_provider == VALUES.LIVE_PREVIEW) {
					frame_processor.setDimensions(preview_frame_width, preview_frame_height, camera_preview.getWidth(), camera_preview.getHeight());
					hideView(playback_controller.getView());
					try {
						camera.setPreviewDisplay(camera_preview.getHolder());
						camera.startPreview();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				break;
			case R.id.video_preview:
				Log.d(TAG, "onSurfaceReady() : video preview");
				if (frame_provider == VALUES.VIDEO_FILE) {
					frame_processor.setDimensions(preview_frame_width, preview_frame_height, video_preview.getWidth(), video_preview.getHeight());
					showView(playback_controller.getView(), true);
					video_file_frame_grabber.displayFirstFrame();
				}
				break;
			case R.id.calibration_overlay:
				Log.d(TAG, "onSurfaceReady() : calibration overlay");
				break;
			case R.id.facial_feature_overlay:
				Log.d(TAG, "onSurfaceReady() : facial feature overlay");
				break;
		}

		Log.d(TAG, "F onSurfaceReady()");

	}

	private void cleanUp() {
		// Log.d(TAG, "S cleanUp()");
		stopCalibration();
		stopDetection();
		stopAnalysis();
		playback_controller.stopPlayback();
		frame_processor.shutDown();
		face_overlay.shutDown();
		video_preview.shutDown();
		if (camera != null) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
		}
		camera = null;

		// Log.d(TAG, "F cleanUp()");

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, "S onSaveInstanceState()");
		outState.putBoolean("mirroring", mirroring);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "S onResume()");
		super.onResume();
		setImmersiveMode(true);

		ImageButton mirror_button = (ImageButton) findViewById(R.id.mirror);
		if (mirroring) {
			mirror_button.setImageResource(R.drawable.flip_horizontal_on);
		} else {
			mirror_button.setImageResource(R.drawable.flip_horizontal);
		}
		frame_processor = new FrameProcessor(this);
		frame_processor.setFaceOverlay(face_overlay);
		frame_processor.setMirroring(mirroring);
		video_preview.setMirroring(mirroring);

		int frame_provider = shared_preferences.getInt(KEYS.FRAME_PROVIDER, DEFAULTS.FRAME_PROVIDER);

		if (frame_provider == VALUES.LIVE_PREVIEW) {
			hideView(playback_controller.getView());
			hideView(video_preview);
			showView(camera_preview, false);
			setupCamera();
		} else if (frame_provider == VALUES.VIDEO_FILE) {
			hideView(camera_preview);
			showView(playback_controller.getView(), true);
			showView(video_preview, false);
			setupVideo();
		}

		FrameLayout.LayoutParams layout_params = new FrameLayout.LayoutParams(preview_display_width, preview_display_height);
		camera_preview.setLayoutParams(layout_params);
		video_preview.setLayoutParams(layout_params);
		calibration_overlay.setLayoutParams(layout_params);
		face_overlay.setLayoutParams(layout_params);
		Log.d(TAG, "F onResume()");

	}

	@Override
	protected void onPause() {
		Log.d(TAG, "S onPause()");
		super.onPause();
		cleanUp();
		Log.d(TAG, "F onPause()");
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "S onStop()");
		super.onStop();
		Log.d(TAG, "F onStop()");
	}

	private void setupCamera() {
		// Log.d(TAG, "S setupCamera()");

		String preview_size = shared_preferences.getString(KEYS.PREVIEW_SIZE, DEFAULTS.PREVIEW_SIZE);
		String[] width_height = preview_size.split("x");
		preview_frame_width = Integer.parseInt(width_height[0]);
		preview_frame_height = Integer.parseInt(width_height[1]);

		String preview_fps = shared_preferences.getString(KEYS.PREVIEW_FPS_RANGE, DEFAULTS.PREVIEW_FPS_RANGE);
		Log.d(TAG, "S setupCamera() : preview fps = " + preview_fps);
		String[] min_max = preview_fps.split(",");
		int min_fps = Integer.parseInt(min_max[0]);
		int max_fps = Integer.parseInt(min_max[1]);

		int selected_camera = shared_preferences.getInt(KEYS.CAMERA_SELECTION, DEFAULTS.CAMERA_SELECTION);

		camera = Camera.open(selected_camera);

		Parameters camera_parameters = camera.getParameters();
		Log.d(TAG, camera_parameters.flatten());
		camera_parameters.setPreviewFpsRange(min_fps, max_fps);
		camera_parameters.setPreviewSize(preview_frame_width, preview_frame_height);
		camera.setParameters(camera_parameters);

		camera.setDisplayOrientation(0);

		int ui_config = shared_preferences.getInt(KEYS.UI_CONFIG, DEFAULTS.UI_CONFIG);
		if (ui_config == VALUES.FILL_SCREEN) {
			preview_display_width = LayoutParams.MATCH_PARENT;
			preview_display_height = LayoutParams.MATCH_PARENT;

		} else {
			preview_display_width = preview_frame_width;
			preview_display_height = preview_frame_height;
		}
		// Log.d(TAG, "F setupCamera()");

	}

	private void setupVideo() {
		String video_file_path = shared_preferences.getString(KEYS.VIDEO_FILE, null);
		if (video_file_path == null) {
			Toast.makeText(this, "Please select a video file in Settings", Toast.LENGTH_SHORT).show();
			return;
		} else if (video_file_frame_grabber.openVideoFile(video_file_path)) {
			Point dimensions = video_file_frame_grabber.getDimensions();

			preview_frame_width = dimensions.x;
			preview_frame_height = dimensions.y;

			int ui_config = shared_preferences.getInt(KEYS.UI_CONFIG, DEFAULTS.UI_CONFIG);
			if (ui_config == VALUES.FILL_SCREEN) {
				preview_display_width = LayoutParams.MATCH_PARENT;
				preview_display_height = LayoutParams.MATCH_PARENT;

			} else {
				preview_display_width = dimensions.x;
				preview_display_height = dimensions.y;
			}

		}
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDown(MotionEvent event) {
			// Log.d(TAG, "onDown()");
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event) {
			// Log.d(TAG, "onSingleTapUp()");
			if (!controls_visible) {
				showView(controls, true);
				controls_visible = true;
			} else {
				hideView(controls);
				controls_visible = false;
			}
			return true;
		}
	}

}
