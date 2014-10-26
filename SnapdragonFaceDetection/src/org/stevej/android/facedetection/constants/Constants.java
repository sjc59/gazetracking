package org.stevej.android.facedetection.constants;

public class Constants {

	/**
	 * Keys for the application preferences.
	 */
	public static class KEYS {
		public static final String	FRAME_PROVIDER_CATEGORY			= "frame-provider-category";
		public static final String	FRAME_PROVIDER					= "frame-provider";
		public static final String	UI_CONFIG_CATEGORY				= "ui-config-category";
		public static final String	UI_CONFIG						= "ui-config";
		public static final String	VIDEO_FILE						= "video-file";
		public static final String	FRAME_FOLDER					= "frame-folder";
		public static final String	FEATURE_DETECTION_CATEGORY		= "feature-detection-category";
		public static final String	FEATURE_DETECTION_TYPE			= "feature-detection-type";
		public static final String	LIVE_PREVIEW_CATEGORY			= "live-preview-category";
		public static final String	LIVE_PREVIEW_SETTINGS_SCREEN	= "live-preview-settings-screen";
		public static final String	CAMERA_SELECTION_CATEGORY		= "camera-selection-category";
		public static final String	CAMERA_SELECTION				= "camera-selection";
		public static final String	CAMERA_SETTINGS_CATEGORY		= "camera-settings-category";
		public static final String	CAMERA_SETTINGS					= "camera-settings";
		public static final String	ANTIBANDING						= "camera-settings-antibanding";
		public static final String	EFFECT							= "camera-settings-effect";
		public static final String	EXPOSURE_COMPENSATION			= "camera-settings-exposure-compensation";
		public static final String	FOCUS_MODE						= "camera-settings-focus-mode";
		public static final String	PREVIEW_FORMAT					= "camera-settings-preview-format";
		public static final String	PREVIEW_FPS_RANGE				= "camera-settings-preview-fps-range";
		public static final String	PREVIEW_SIZE					= "camera-settings-preview-size";
		public static final String	SCENE_MODE						= "camera-settings-scene-mode";
		public static final String	WHITEBALANCE					= "camera-settings-whitebalance";
	}

	public static class VALUES {
		public static final int	LIVE_PREVIEW						= 0;
		public static final int	VIDEO_FILE							= 1;
		public static final int	FRAME_FOLDER						= 2;

		public static final int	FILL_SCREEN							= 0;
		public static final int	MATCH_SOURCE						= 1;

		public static final int	ANDROID_FACE_DETECTION				= 0;
		public static final int	SIGN_DETECTION						= 1;
		public static final int	GAZE_DETECTION						= 2;

		public static final int	HIGHLIGHT_RED_PIXELS				= 3;
		public static final int	FASTCV_SOBEL_3x3					= 4;
		public static final int	FASTCV_CANNY_3x3					= 5;
		public static final int	OPENCV_CANNY_3x3					= 6;
		public static final int	OPENCV_DETECT_RED_BLOBS				= 7;
		public static final int	OPENCV_DETECT_RECTANGLES			= 8;
		public static final int	DETECT_QR_CODES						= 9;
		public static final int	FASTCV_FAST_CORNERS_9_SCORE			= 10;
		public static final int	FASTCV_FAST_CORNERS_10_SCORE		= 11;

		public static final int	CAMERA_SELECTION_REAR				= 0;
		public static final int	CAMERA_SELECTION_FRONT				= 1;

		public static final int	REQUEST_VIDEO_FILE_INTENT_CODE		= 0;
		public static final int	REQUEST_FRAME_FOLDER_INTENT_CODE	= 1;
	}

	public static class IMAGE_FORMAT {
		public static final String	NV21				= "yuv420sp";
		public static final String	YUY2				= "yuv422i-yuyv";
		public static final String	NV16				= "yuv422sp";
		public static final String	RGB_565				= "rgb565";
		public static final int		ARGB_8888			= 1;
		public static final int		ABGR_8888			= 2;
		public static final int		GREYSCALE_AGGG_8888	= 3;
		public static final int		BGRA_8888			= 4;
		public static final int		RGBA_8888			= 5;
		public static final int		RED_CHANNEL			= 6;
		public static final int		GREEN_CHANNEL		= 7;
		public static final int		BLUE_CHANNEL		= 8;
	}

	public static class DEFAULTS {
		public static final int		FRAME_PROVIDER			= VALUES.VIDEO_FILE;
		public static final int		CAMERA_SELECTION		= VALUES.CAMERA_SELECTION_REAR;
		public static final int		FEATURE_DETECTION_TYPE	= VALUES.SIGN_DETECTION;
		public static final int		UI_CONFIG				= VALUES.FILL_SCREEN;
		public static final String	ANTIBANDING				= LISTS.ANTIBANDING[0];
		public static final String	EFFECT					= LISTS.EFFECT_ENTRYVALUES[0];
		public static final String	FOCUS_MODE				= LISTS.FOCUS_MODE_ENTRYVALUES[0];
		public static final String	PREVIEW_FORMAT			= LISTS.PREVIEW_FORMAT[0];
		public static final String	SCENE_MODE				= LISTS.SCENE_MODE_ENTRYVALUES[0];
		public static final String	WHITEBALANCE			= LISTS.WHITEBALANCE_ENTRYVALUES[0];
		public static final String	PREVIEW_FPS_RANGE		= "30000,30000";
		public static final String	PREVIEW_SIZE			= "640x480";
		public static final String	EXPOSURE_COMPENSATION	= "0.0";

	}

	public static class LABELS {
		public static final String	VIDEO_FILE						= "Video file";
		public static final String	FRAME_FOLDER					= "Folder of video frames";
		public static final String	LIVE_PREVIEW					= "Live camera preview";
		public static final String	FILL_SCREEN						= "Fill screen";
		public static final String	MATCH_SOURCE					= "Match source";
		public static final String	CAMERA_SELECTION_REAR			= "Rear camera";
		public static final String	CAMERA_SELECTION_FRONT			= "Front camera";
		public static final String	ANDROID_FACE_DETECTION			= "Android face detection";
		public static final String	SIGN_DETECTION					= "Detect traffic signs";
		public static final String	GAZE_DETECTION					= "Detect eye gaze";
		public static final String	HIGHLIGHT_RED_PIXELS			= "Highlight red pixels";
		public static final String	DETECT_QR_CODES					= "Detect QR codes";
		public static final String	FASTCV_SOBEL_3x3				= "Sobel 3x3 filter (FastCV)";
		public static final String	FASTCV_CANNY_3x3				= "Canny 3x3 filter (FastCV)";
		public static final String	OPENCV_CANNY_3x3				= "Canny 3x3 filter (OpenCV)";
		public static final String	OPENCV_DETECT_RED_BLOBS			= "Red blob detection (OpenCV)";
		public static final String	OPENCV_DETECT_RECTANGLES		= "Rectangle detection (OpenCV)";
		public static final String	FASTCV_FAST_CORNERS_9_SCORE		= "9 score corner detection (FastCV)";
		public static final String	FASTCV_FAST_CORNERS_10_SCORE	= "10 score corner detection (FastCV)";
	}

	public static class LISTS {
		public static final int[]		FRAME_PROVIDER_ENTRYVALUES		= { VALUES.LIVE_PREVIEW, VALUES.VIDEO_FILE, VALUES.FRAME_FOLDER };
		public static final String[]	FRAME_PROVIDER_ENTRIES			= { LABELS.LIVE_PREVIEW, LABELS.VIDEO_FILE, LABELS.FRAME_FOLDER };
		public static final int[]		UI_CONFIG_ENTRYVALUES			= { VALUES.FILL_SCREEN, VALUES.MATCH_SOURCE };
		public static final String[]	UI_CONFIG_ENTRIES				= { LABELS.FILL_SCREEN, LABELS.MATCH_SOURCE };
		public static final int[]		CAMERA_SELECTION_ENTRYVALUES	= { VALUES.CAMERA_SELECTION_REAR, VALUES.CAMERA_SELECTION_FRONT };
		public static final String[]	CAMERA_SELECTION_ENTRIES		= { LABELS.CAMERA_SELECTION_REAR, LABELS.CAMERA_SELECTION_FRONT };
		public static final int[]		FEATURE_DETECTION_ENTRYVALUES	= { VALUES.ANDROID_FACE_DETECTION, VALUES.SIGN_DETECTION, VALUES.GAZE_DETECTION, VALUES.FASTCV_FAST_CORNERS_9_SCORE,
																				VALUES.FASTCV_FAST_CORNERS_10_SCORE, VALUES.DETECT_QR_CODES, VALUES.OPENCV_DETECT_RECTANGLES,
																				VALUES.OPENCV_DETECT_RED_BLOBS, VALUES.HIGHLIGHT_RED_PIXELS, VALUES.FASTCV_SOBEL_3x3, VALUES.FASTCV_CANNY_3x3,
																				VALUES.OPENCV_CANNY_3x3 };
		public static final String[]	FEATURE_DETECTION_ENTRIES		= { LABELS.ANDROID_FACE_DETECTION, LABELS.SIGN_DETECTION, LABELS.GAZE_DETECTION, LABELS.FASTCV_FAST_CORNERS_9_SCORE,
																				LABELS.FASTCV_FAST_CORNERS_10_SCORE, LABELS.DETECT_QR_CODES, LABELS.OPENCV_DETECT_RECTANGLES,
																				LABELS.OPENCV_DETECT_RED_BLOBS, LABELS.HIGHLIGHT_RED_PIXELS, LABELS.FASTCV_SOBEL_3x3, LABELS.FASTCV_CANNY_3x3,
																				LABELS.OPENCV_CANNY_3x3 };
		public static final String[]	ANTIBANDING						= { "auto", "50Hz", "60Hz", "off" };
		public static final String[]	EFFECT_ENTRIES					= { "None", "Aqua", "Blackboard", "Mono", "Negative", "Posterize", "Sepia", "Solarize" };
		public static final String[]	EFFECT_ENTRYVALUES				= { "none", "aqua", "blackboard", "mono", "negative", "posterize", "sepia", "solarize" };
		public static final String[]	FOCUS_MODE_ENTRIES				= { "Auto", "Infinity", "Macro", "Extended depth of field", "Continuous (for video)", "Fixed" };
		public static final String[]	FOCUS_MODE_ENTRYVALUES			= { "auto", "infinity", "macro", "edof", "continuous-video", "fixed" };
		public static final String[]	PREVIEW_FORMAT					= { IMAGE_FORMAT.NV21, IMAGE_FORMAT.YUY2, IMAGE_FORMAT.NV16, IMAGE_FORMAT.RGB_565 };

		public static final String[]	SCENE_MODE_ENTRIES				= { "Auto", "Action", "Barcode", "Beach", "Candlelight", "Fireworks", "Landscape", "Night", "Night portrait", "Party",
																				"Portrait", "Snow", "Sports", "Steady photo", "Sunset", "Theatre" };

		public static final String[]	SCENE_MODE_ENTRYVALUES			= { "auto", "action", "barcode", "beach", "candlelight", "fireworks", "landscape", "night", "night_portrait", "party",
																				"portrait", "snow", "sports", "steadyphoto", "sunset", "theatre" };

		public static final String[]	WHITEBALANCE_ENTRIES			= { "Auto", "Cloudy", "Daylight", "Fluorescent", "Incandescent", "Shade", "Twilight", "Warm fluorescent" };

		public static final String[]	WHITEBALANCE_ENTRYVALUES		= { "auto", "cloudy_daylight", "daylight", "fluorescent", "incandescent", "shade", "twilight", "warm-fluorescent" };
	}

	public static class TITLES {
		public static final String	FRAME_PROVIDER_CATEGORY			= "Input source";
		public static final String	FRAME_PROVIDER					= "Use frames from...";
		public static final String	VIDEO_FILE						= "Video file selection";
		public static final String	FRAME_FOLDER					= "Video frame folder selection";
		public static final String	UI_CONFIG_CATEGORY				= "Frame display";
		public static final String	FEATURE_DETECTION_CATEGORY		= "Feature detection";
		public static final String	FEATURE_DETECTION_TYPE			= "Feature detector";
		public static final String	LIVE_PREVIEW_CATEGORY			= "Live preview";
		public static final String	LIVE_PREVIEW_SETTINGS_SCREEN	= "Camera settings";
		public static final String	CAMERA_SELECTION_CATEGORY		= "Camera selection";
		public static final String	CAMERA_SELECTION				= "Camera selection";
		public static final String	CAMERA_SETTINGS_CATEGORY		= "Camera settings";
		public static final String	ANTIBANDING						= "Anti-banding";
		public static final String	EFFECT							= "Image effect";
		public static final String	EXPOSURE_COMPENSATION			= "Exposure compensation";
		public static final String	FOCUS_MODE						= "Focus mode";
		public static final String	PREVIEW_FORMAT					= "Preview pixel format";
		public static final String	PREVIEW_FPS_RANGE				= "Preview frame rate";
		public static final String	PREVIEW_SIZE					= "Preview size";
		public static final String	SCENE_MODE						= "Scene mode";
		public static final String	WHITEBALANCE					= "White balance";
	}

	public static class SUMMARIES {
		public static final String	FRAME_PROVIDER					= "How video frames are provided to the application";
		public static final String	UI_CONFIG						= "How preview frames are displayed";
		public static final String	FEATURE_DETECTION_TYPE			= "Set feature detector applied to preview";
		public static final String	LIVE_PREVIEW_SETTINGS_SCREEN	= "Settings for the ";
		public static final String	CAMERA_SELECTION				= "Select camera to use for the live preview";
	}
}
