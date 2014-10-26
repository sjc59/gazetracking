package org.stevej.android.facedetection.ui;

import org.stevej.android.facedetection.R;
import org.stevej.android.facedetection.video.VideoFileFrameGrabber;
import org.stevej.android.facedetection.video.VideoFileFrameGrabber.PlaybackState;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/**
 * Manages interaction with the playback buttons (start/pause, stop, next frame), and resulting playback mode when grabbing frames from a video file.
 */
public class PlaybackController extends Handler implements OnClickListener {
	private static final String		TAG							= "PlaybackController";
	private LinearLayout			video_controls;
	private ImageButton				start_pause_button;
	private ImageButton				stop_button;
	private ImageButton				next_frame_button;
	private VideoFileFrameGrabber	video_file_frame_grabber	= null;
	private boolean					playing						= false;

	public PlaybackController(Activity activity, VideoFileFrameGrabber video_file_frame_grabber) {
		this.video_file_frame_grabber = video_file_frame_grabber;
		video_controls = (LinearLayout) activity.findViewById(R.id.video_controls);
		start_pause_button = (ImageButton) activity.findViewById(R.id.video_start_pause_processing);
		start_pause_button.setOnClickListener(this);
		stop_button = (ImageButton) activity.findViewById(R.id.video_stop_processing);
		stop_button.setOnClickListener(this);
		next_frame_button = (ImageButton) activity.findViewById(R.id.video_next_frame_processing);
		next_frame_button.setOnClickListener(this);
	}

	public View getView() {
		return video_controls;
	}

	public void reset() {
		start_pause_button.setImageResource(android.R.drawable.ic_media_play);
		next_frame_button.setEnabled(true);
		stop_button.setEnabled(false);
	}

	public void handleMessage(Message message) {
		switch (message.what) {
			case R.id.playback_control_reset:
				reset();
				break;
		}
	}

	public boolean isPlaying() {
		return playing;
	}

	public void pausePlayback() {
		Log.d(TAG, "pausePlayback()");
		video_file_frame_grabber.pause();
		playing = false;
		start_pause_button.setImageResource(android.R.drawable.ic_media_play);
		next_frame_button.setEnabled(true);
		stop_button.setEnabled(true);
	}

	public void startPlayback() {
		Log.d(TAG, "startPlayback()");
		video_file_frame_grabber.start();
		playing = true;
		start_pause_button.setImageResource(android.R.drawable.ic_media_pause);
		next_frame_button.setEnabled(false);
		stop_button.setEnabled(true);
	}

	public void stopPlayback() {
		Log.d(TAG, "stopPlayback()");
		video_file_frame_grabber.stop();
		start_pause_button.setImageResource(android.R.drawable.ic_media_play);
		next_frame_button.setEnabled(true);
		stop_button.setEnabled(false);
		playing = false;
	}

	/**
	 * Receives user clicks on the video file control buttons, and changes playback state depending upon which button was clicked.
	 */
	public void onClick(View v) {
		PlaybackState current_state = video_file_frame_grabber.getPlaybackState();
		switch (v.getId()) {
			case R.id.video_start_pause_processing:
				if (current_state == PlaybackState.STOPPED || current_state == PlaybackState.PAUSED) {
					startPlayback();
				} else if (current_state == PlaybackState.PLAYING) {
					pausePlayback();
				}

				break;
			case R.id.video_stop_processing:
				stopPlayback();
				break;
			case R.id.video_next_frame_processing:
				video_file_frame_grabber.nextFrame();
				stop_button.setEnabled(true);
				break;
		}

	}

}
