package org.stevej.android.facedetection.video;

import java.util.LinkedList;
import java.util.Queue;

import org.stevej.android.facedetection.R;
import org.stevej.android.facedetection.processing.FrameProcessor;
import org.stevej.android.facedetection.ui.PlaybackController;
import org.stevej.android.facedetection.ui.VideoPreview;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Manages grabbing and processing frames from a video file via FFmpeg, and handling playback.
 */
public class VideoFileFrameGrabber implements Callback {
	private static final String	TAG						= "VideoFileFrameGrabber";

	/** The renderer's message handler. */
	private Handler				preview_display_handler	= null;

	/** The processor for video frames. */
	private FrameProcessor		frame_processor			= null;

	private VideoPreview		preview_display			= null;

	/** Represents whether a video file is currently open. */
	private boolean				video_file_open			= false;

	/** The full path of a video file. */
	private String				video_file				= null;

	/** The width of the current video (in pixels). */
	private int					video_width;

	/** The height of the current video (in pixels). */
	private int					video_height;

	/** The number of bytes required to represent a video frame. Depends on the format specified in FFmpegWrapper.c (currently NV21 which is the same as the camera preview). */
	private int					video_frame_num_bytes;

	/**
	 * The possible playback states.
	 */
	public enum PlaybackState {
		PLAYING, PAUSED, STOPPED, FINISHED
	};

	/** The current playback state. */
	private PlaybackState		playback_state			= PlaybackState.STOPPED;

	/** The UI component that contains the playback control buttons. */
	private PlaybackController	playback_controller		= null;

	/** A bitmap of the first frame of the video. Acts as a placeholder before playback has started. */
	private Bitmap				first_frame_bitmap		= null;
	private Bitmap				current_frame_bitmap	= null;

	/** A thread for continuous retrieval of video frames. */
	private Thread				frame_grabber_thread	= null;

	/** The message for the video frame processor. */
	// private HandlerThread message_queue = null;

	/** The set of buffers that are cycled through (and recycled) for the frame images. */
	private Queue<byte[]>		frame_buffers			= new LinkedList<byte[]>();
	private Queue<byte[]>		frame_buffers_rgba		= new LinkedList<byte[]>();

	public Handler				message_handler;
	protected HandlerThread		handlerThread;
	protected Looper			looper;

	public boolean handleMessage(Message message) {
		frame_buffers.add((byte[]) message.obj);
		return true;
	}

	public Point getDimensions() {
		return new Point(video_width, video_height);
	}

	public PlaybackState getPlaybackState() {
		return playback_state;
	}

	public void setPlaybackController(PlaybackController playback_controller) {
		this.playback_controller = playback_controller;
	}

	public void setPreviewDisplay(VideoPreview preview_display) {
		this.preview_display = preview_display;
	}

	public void setPreviewCallback(FrameProcessor frame_processor) {
		this.frame_processor = frame_processor;
	}

	/**
	 * Instantiates a new video file frame grabber.
	 * 
	 * @param renderer
	 *            the UI component into which processed frames will be displayed
	 */
	public VideoFileFrameGrabber() {
		Log.d(TAG, "VideoFileFrameGrabber");
		handlerThread = new HandlerThread("ProcessorThread");
		handlerThread.start();
		looper = handlerThread.getLooper();
		message_handler = new Handler(looper, this);
	}

	/**
	 * Open the file and do the required initialisation.
	 * 
	 * @param _video_file
	 *            the the full path to the file
	 * @return true, if successful
	 */
	public boolean openVideoFile(String _video_file) {
		/*
		 * If the file is currently open just make sure the processor has the correct frame dimensions. 
		 * Otherwise close the current file.
		 */
		if (video_file != null) {
			if (video_file.equals(_video_file) && video_file_open) {
				return true;
			} else {
				closeFile();
			}
		}
		video_file = _video_file;

		int[] dimensions = new int[3];
		video_file_open = FFmpeg.openFile(video_file, dimensions);

		if (video_file_open) {
			Log.d(TAG, "width = " + dimensions[0]);
			Log.d(TAG, "height = " + dimensions[1]);
			Log.d(TAG, "nbytes = " + dimensions[2]);

			video_width = dimensions[0];
			video_height = dimensions[1];
			video_frame_num_bytes = dimensions[2];

			frame_buffers.clear();
			for (int i = 0; i < 3; i++) {
				frame_buffers.add(new byte[video_frame_num_bytes]);
			}
			if (first_frame_bitmap != null) {
				first_frame_bitmap.recycle();
			}
			if (current_frame_bitmap != null) {
				current_frame_bitmap.recycle();
			}
			first_frame_bitmap = Bitmap.createBitmap(video_width, video_height, Bitmap.Config.ARGB_8888);
			current_frame_bitmap = Bitmap.createBitmap(video_width, video_height, Bitmap.Config.ARGB_8888);
			FFmpeg.getFrameBitmapAt(first_frame_bitmap, 1);

			Log.d(TAG, "width = " + video_width);
			Log.d(TAG, "height = " + video_height);

		}
		return video_file_open;
	}

	/**
	 * Called from onDestroy() of the main activity. Clean up the currently open file, close it and stop the message queue.
	 */
	public void shutDown() {
		// if (video_file_open) {
		// cleanUp();
		// }
		// closeFile();
		stop();

	}

	/**
	 * Called in 3 situations
	 * <ul>
	 * <li>we are shutting down</li>
	 * <li>the main activity has paused for some reason (eg preferences have been launched)</li>
	 * <li>playback has been stopped</li>
	 * </ul>
	 * Stops the thread that grabs the frames, removes all pending processing and rendering messages and sets the frame grabbing position back to the start of the video file.
	 */
	public void cleanUp() {
		Log.d(TAG, "cleanUp()");
		playback_state = PlaybackState.STOPPED;
		try {
			if (frame_grabber_thread != null) {
				if (frame_grabber_thread.isAlive()) {
					frame_grabber_thread.join();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "cleanUp() : joined the thread");

		Log.d(TAG, "cleanUp() : removed processor messages");
		Log.d(TAG, "cleanUp() : removed renderer messages");
		if (video_file_open) {
			FFmpeg.resetToStart();
		}
		Log.d(TAG, "cleanUp() : reset FFmpeg");

	}

	/**
	 * Close the FFmepg file freeing the memory/resources.
	 */
	private void closeFile() {
		if (video_file_open) {
			FFmpeg.closeFile();
			video_file_open = false;
		}
		// if (first_frame_bitmap != null) {
		// first_frame_bitmap.recycle();
		// }
	}

	/**
	 * Display first frame of the video file as a placeholder.
	 */
	public void displayFirstFrame() {
		Log.d(TAG, "displayFirstFrame");
		displayFrameBitmap(first_frame_bitmap);
	}

	/**
	 * Gets the width of the current video file.
	 * 
	 * @return the video width in pixels
	 */
	public int getVideoWidth() {
		return video_width;
	}

	/**
	 * Gets the height of the current video file.
	 * 
	 * @return the video height in pixels
	 */
	public int getVideoHeight() {
		return video_height;
	}

	/**
	 * Gets the UI component into which processed video frames are drawn.
	 * 
	 * @return the renderer
	 */
	public VideoPreview getPreviewDisplay() {
		return preview_display;
	}

	/**
	 * Stop frame processing, clean up and display the first frame as a placeholder.
	 */
	public void stop() {
		cleanUp();
		displayFrameBitmap(first_frame_bitmap);
	}

	/**
	 * Pause frame processing.
	 */
	public void pause() {
		playback_state = PlaybackState.PAUSED;
	}

	/**
	 * Process the next available frame.
	 */
	public void nextFrame() {
		if (playback_state == PlaybackState.PLAYING) {
			return;
		}

		try {
			if (frame_grabber_thread != null) {
				Log.d(TAG, "nextFrame() : waiting for thread");
				if (frame_grabber_thread.isAlive()) {
					frame_grabber_thread.join();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		frame_grabber_thread = new Thread(new Runnable() {
			public void run() {
				// read a frame in NV21 format into the next frame buffer and send it to the frame processor
				if (FFmpeg.getFrameYUV(frame_buffers.peek(), current_frame_bitmap)) {
					if (frame_processor != null) {
						Message message = frame_processor.message_handler.obtainMessage();
						message.obj = frame_buffers.poll();
						message.sendToTarget();

					}

					Message message = preview_display.message_handler.obtainMessage();
					message.obj = current_frame_bitmap;
					message.sendToTarget();

				} else {
					playback_state = PlaybackState.FINISHED;
				}
				if (playback_state == PlaybackState.FINISHED) {
					playback_state = PlaybackState.STOPPED;
					displayFrameBitmap(first_frame_bitmap);
					FFmpeg.resetToStart();
					playback_controller.obtainMessage(R.id.playback_control_reset).sendToTarget();
				}
			}
		});
		frame_grabber_thread.start();

	}

	/**
	 * Start continuous processing of the view frames in sequence.
	 */
	public void start() {
		Log.d(TAG, "start()");
		if (playback_state == PlaybackState.PLAYING) {
			Log.d(TAG, "start() : already playing");
			return;
		}
		playback_state = PlaybackState.PLAYING;
		try {
			if (frame_grabber_thread != null) {
				Log.d(TAG, "start() : waiting for thread");
				if (frame_grabber_thread.isAlive()) {
					frame_grabber_thread.join();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "start() : creating new frame_grabber_thread");
		frame_grabber_thread = new Thread(new Runnable() {
			public void run() {
				while (playback_state == PlaybackState.PLAYING) {
					if (frame_buffers.peek() == null) {
						continue;
					}
					if (FFmpeg.getFrameYUV(frame_buffers.peek(), current_frame_bitmap)) {
						if (frame_processor != null) {
							Message message = frame_processor.message_handler.obtainMessage();
							message.obj = frame_buffers.poll();
							message.sendToTarget();
						}

						Message message = preview_display.message_handler.obtainMessage();
						message.obj = current_frame_bitmap;
						message.sendToTarget();

					} else {
						playback_state = PlaybackState.FINISHED;
					}
				}
				if (playback_state == PlaybackState.FINISHED) {
					playback_state = PlaybackState.STOPPED;
					displayFrameBitmap(first_frame_bitmap);
					FFmpeg.resetToStart();
					playback_controller.obtainMessage(R.id.playback_control_reset).sendToTarget();
				}
			}
		});
		frame_grabber_thread.start();
		Log.d(TAG, "start() : started new frame_grabber_thread");

	}

	/**
	 * Display the bitmap of a frame in the renderer. Called when displaying the first frame of the video as a place holder.
	 * 
	 * @param video_frame
	 *            the video_frame
	 */
	private void displayFrameBitmap(final Bitmap video_frame) {
		if (video_frame == null) {
			Log.d(TAG, "displayFrameBitmap : bitmap == null");
			return;
		}
		Message message = preview_display.message_handler.obtainMessage();
		message.obj = video_frame;
		message.sendToTarget();

	}
}
