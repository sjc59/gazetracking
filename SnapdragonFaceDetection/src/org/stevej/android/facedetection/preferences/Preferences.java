package org.stevej.android.facedetection.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.stevej.android.facedetection.constants.Constants.DEFAULTS;
import org.stevej.android.facedetection.constants.Constants.IMAGE_FORMAT;
import org.stevej.android.facedetection.constants.Constants.KEYS;
import org.stevej.android.facedetection.constants.Constants.LISTS;
import org.stevej.android.facedetection.constants.Constants.SUMMARIES;
import org.stevej.android.facedetection.constants.Constants.TITLES;
import org.stevej.android.facedetection.constants.Constants.VALUES;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

/**
 * The activity that is launched when the user accesses the application preferences.
 * <p>
 * The UI etc is all constructed in code (as opposed to XML resources).
 * 
 */
public class Preferences extends PreferenceActivity implements OnPreferenceChangeListener {
	private static final String			TAG	= "Preferences";

	/*
	 * This is a hack because preferences are represented as strings and the image formats in Camera.Parameters are integer constants defined in the
	 * ImageFormat class. And there isn't a direct correspondence between the identifiers in camera parameters (eg yuv420sp) and those used in ImageFormat
	 * (eg NV21).
	 */
	/** Lookup table to convert from a string representation of a camera preview image format provided by Camera.Parameters to an ImageFormat constant. */
	private HashMap<String, Integer>	preview_format_lookup_int;

	/** Lookup table to convert from an ImageFormat constant to a string representation of a camera preview image format provided by Camera.Parameters. */
	private HashMap<Integer, String>	preview_format_lookup_string;

	private Parameters					camera_parameters;

	/**
	 * Sets the default values of the application preferences the first time the application is run.
	 * 
	 * @param context
	 *            the application context for which we a managing the preferences
	 */
	public static void setDefaults(Context context) {
		Log.d(TAG, "setDefaults");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putInt(KEYS.FRAME_PROVIDER, DEFAULTS.FRAME_PROVIDER);
		editor.putInt(KEYS.FEATURE_DETECTION_TYPE, DEFAULTS.FEATURE_DETECTION_TYPE);
		editor.putInt(KEYS.CAMERA_SELECTION, DEFAULTS.CAMERA_SELECTION);
		editor.putInt(KEYS.UI_CONFIG, DEFAULTS.UI_CONFIG);
		editor.putString(KEYS.ANTIBANDING, DEFAULTS.ANTIBANDING);
		editor.putString(KEYS.EFFECT, DEFAULTS.EFFECT);
		editor.putString(KEYS.EXPOSURE_COMPENSATION, DEFAULTS.EXPOSURE_COMPENSATION);
		editor.putString(KEYS.FOCUS_MODE, DEFAULTS.FOCUS_MODE);
		editor.putString(KEYS.PREVIEW_FORMAT, DEFAULTS.PREVIEW_FORMAT);
		editor.putString(KEYS.PREVIEW_FPS_RANGE, DEFAULTS.PREVIEW_FPS_RANGE);
		editor.putString(KEYS.PREVIEW_SIZE, DEFAULTS.PREVIEW_SIZE);
		editor.putString(KEYS.SCENE_MODE, DEFAULTS.SCENE_MODE);
		editor.putString(KEYS.WHITEBALANCE, DEFAULTS.WHITEBALANCE);
		editor.putBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, true);
		editor.commit();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		int camera_selection = sharedPreferences.getInt(KEYS.CAMERA_SELECTION, DEFAULTS.CAMERA_SELECTION);
		Log.d(TAG, "camera_selection = " + camera_selection);

		Camera camera = Camera.open(camera_selection);
		camera_parameters = camera.getParameters();
		camera.release();

		preview_format_lookup_int = new HashMap<String, Integer>();
		preview_format_lookup_int.put(IMAGE_FORMAT.NV21, ImageFormat.NV21);
		preview_format_lookup_int.put(IMAGE_FORMAT.YUY2, ImageFormat.YUY2);
		preview_format_lookup_int.put(IMAGE_FORMAT.NV16, ImageFormat.NV16);
		preview_format_lookup_int.put(IMAGE_FORMAT.RGB_565, ImageFormat.RGB_565);

		preview_format_lookup_string = new HashMap<Integer, String>();
		preview_format_lookup_string.put(ImageFormat.NV21, IMAGE_FORMAT.NV21);
		preview_format_lookup_string.put(ImageFormat.YUY2, IMAGE_FORMAT.YUY2);
		preview_format_lookup_string.put(ImageFormat.NV16, IMAGE_FORMAT.NV16);
		preview_format_lookup_string.put(ImageFormat.RGB_565, IMAGE_FORMAT.RGB_565);

		createGUI();

		IntListPreference provider_list = (IntListPreference) findPreference(KEYS.FRAME_PROVIDER);
		provider_list.setSummary(provider_list.getEntry());
		updateFrameProviderUI(provider_list.getIntValue());

		IntListPreference camera_list = (IntListPreference) findPreference(KEYS.CAMERA_SELECTION);
		camera_list.setSummary(camera_list.getEntry());

		configureCameraPreferences();
	}

	/**
	 * Intercepts a back button press so that we can ensure necessary values have been provided and otherwise warn the user.
	 * 
	 */
	public void onBackPressed() {
		int frame_provider = getIntPreferenceValue(KEYS.FRAME_PROVIDER, VALUES.LIVE_PREVIEW);
		String video_file = getPreferenceValue(KEYS.VIDEO_FILE, "");
		String frame_folder = getPreferenceValue(KEYS.FRAME_FOLDER, "");

		if (frame_provider == VALUES.VIDEO_FILE && video_file.equals("")) {
			Toast.makeText(this, "Please select a video file", Toast.LENGTH_SHORT).show();
			return;
		}
		if (frame_provider == VALUES.FRAME_FOLDER && frame_folder.equals("")) {
			Toast.makeText(this, "Please select a frame folder", Toast.LENGTH_SHORT).show();
			return;
		}
		if (frame_provider == VALUES.FRAME_FOLDER && !frame_folder.equals("")) {
			Toast.makeText(this, "Frame folder processing not implemented yet", Toast.LENGTH_SHORT).show();
			return;
		}
		super.onBackPressed();
	}

	/**
	 * Creates the GUI for the preferences. There are 3 parts: where the frame is coming from, what features are to be detected, and what the settings are for the camera.
	 */
	private void createGUI() {
		PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
		setPreferenceScreen(screen);

		createProviderCategory(screen);
		createUICategory(screen);
		createFeatureDetectionCategory(screen);
		createLivePreviewCategory(screen);
	}

	private void createUICategory(PreferenceScreen screen) {
		PreferenceCategory ui_category = new PreferenceCategory(this);
		IntListPreference ui = new IntListPreference(this);

		screen.addPreference(ui_category);
		ui_category.addPreference(ui);

		ui_category.setKey(KEYS.UI_CONFIG_CATEGORY);
		ui_category.setTitle(TITLES.UI_CONFIG_CATEGORY);

		ui.setKey(KEYS.UI_CONFIG);
		ui.setTitle(TITLES.UI_CONFIG_CATEGORY);
		ui.setEntries(LISTS.UI_CONFIG_ENTRIES);
		ui.setIntEntryValues(LISTS.UI_CONFIG_ENTRYVALUES);
		ui.setIntValue(getIntPreferenceValue(KEYS.UI_CONFIG, DEFAULTS.UI_CONFIG));
		ui.setSummary(ui.getEntry());
		ui.setOnPreferenceChangeListener(this);
	}

	/**
	 * Creates the frame provider selection UI.
	 * 
	 * @param screen
	 *            the preference screen this preference segment will be added to
	 */
	private void createProviderCategory(PreferenceScreen screen) {
		PreferenceCategory provider_category = new PreferenceCategory(this);
		IntListPreference provider = new IntListPreference(this);

		screen.addPreference(provider_category);
		provider_category.addPreference(provider);

		provider_category.setKey(KEYS.FRAME_PROVIDER_CATEGORY);
		provider_category.setTitle(TITLES.FRAME_PROVIDER_CATEGORY);

		provider.setKey(KEYS.FRAME_PROVIDER);
		provider.setTitle(TITLES.FRAME_PROVIDER);
		provider.setEntries(LISTS.FRAME_PROVIDER_ENTRIES);
		provider.setIntEntryValues(LISTS.FRAME_PROVIDER_ENTRYVALUES);
		provider.setIntValue(getIntPreferenceValue(KEYS.FRAME_PROVIDER, DEFAULTS.FRAME_PROVIDER));
		provider.setSummary(provider.getEntry());
		provider.setOnPreferenceChangeListener(this);
	}

	/**
	 * Creates the feature detector selection UI.
	 * 
	 * @param screen
	 *            the preference screen this preference segment will be added to
	 */
	private void createFeatureDetectionCategory(PreferenceScreen screen) {
		PreferenceCategory detection_category = new PreferenceCategory(this);
		IntListPreference detector = new IntListPreference(this);

		screen.addPreference(detection_category);
		detection_category.addPreference(detector);

		detection_category.setKey(KEYS.FEATURE_DETECTION_CATEGORY);
		detection_category.setTitle(TITLES.FEATURE_DETECTION_CATEGORY);

		detector.setKey(KEYS.FEATURE_DETECTION_TYPE);
		detector.setTitle(TITLES.FEATURE_DETECTION_TYPE);
		detector.setEntries(LISTS.FEATURE_DETECTION_ENTRIES);
		detector.setIntEntryValues(LISTS.FEATURE_DETECTION_ENTRYVALUES);
		detector.setIntValue(getIntPreferenceValue(KEYS.FEATURE_DETECTION_TYPE, DEFAULTS.FEATURE_DETECTION_TYPE));
		detector.setSummary(detector.getEntry());
		detector.setOnPreferenceChangeListener(this);

	}

	/**
	 * Creates the camera settings UI.
	 * 
	 * @param screen
	 *            the preference screen this preference segment will be added to
	 */
	private void createLivePreviewCategory(PreferenceScreen screen) {
		PreferenceCategory preview_category = new PreferenceCategory(this);
		IntListPreference camera_selection = new IntListPreference(this);
		PreferenceScreen camera_settings_screen = getPreferenceManager().createPreferenceScreen(this);
		PreferenceCategory camera_settings_category = new PreferenceCategory(this);

		screen.addPreference(preview_category);
		preview_category.addPreference(camera_selection);
		preview_category.addPreference(camera_settings_screen);
		camera_settings_screen.addPreference(camera_settings_category);

		preview_category.setKey(KEYS.LIVE_PREVIEW_CATEGORY);
		preview_category.setTitle(TITLES.LIVE_PREVIEW_CATEGORY);
		preview_category.setShouldDisableView(true);

		camera_settings_screen.setKey(KEYS.LIVE_PREVIEW_SETTINGS_SCREEN);
		camera_settings_screen.setTitle(TITLES.LIVE_PREVIEW_SETTINGS_SCREEN);

		camera_selection.setKey(KEYS.CAMERA_SELECTION);
		camera_selection.setTitle(TITLES.CAMERA_SELECTION);
		camera_selection.setSummary(SUMMARIES.CAMERA_SELECTION);
		camera_selection.setEntries(LISTS.CAMERA_SELECTION_ENTRIES);
		camera_selection.setIntEntryValues(LISTS.CAMERA_SELECTION_ENTRYVALUES);
		camera_selection.setIntValue(getIntPreferenceValue(KEYS.CAMERA_SELECTION, DEFAULTS.CAMERA_SELECTION));
		camera_selection.setOnPreferenceChangeListener(this);

		camera_settings_screen.setSummary(SUMMARIES.LIVE_PREVIEW_SETTINGS_SCREEN + camera_selection.getEntry());

		camera_settings_category.setKey(KEYS.CAMERA_SETTINGS_CATEGORY);
		camera_settings_category.setTitle(TITLES.CAMERA_SETTINGS_CATEGORY);

		ListPreference antibanding = new ListPreference(this);
		antibanding.setKey(KEYS.ANTIBANDING);
		antibanding.setTitle(TITLES.ANTIBANDING);
		antibanding.setEntries(LISTS.ANTIBANDING);
		antibanding.setEntryValues(LISTS.ANTIBANDING);
		antibanding.setValue(getPreferenceValue(KEYS.ANTIBANDING, DEFAULTS.ANTIBANDING));
		antibanding.setShouldDisableView(true);
		antibanding.setOnPreferenceChangeListener(this);

		ListPreference effect = new ListPreference(this);
		effect.setKey(KEYS.EFFECT);
		effect.setTitle(TITLES.EFFECT);
		effect.setEntries(LISTS.EFFECT_ENTRIES);
		effect.setEntryValues(LISTS.EFFECT_ENTRYVALUES);
		effect.setValue(getPreferenceValue(KEYS.EFFECT, DEFAULTS.EFFECT));
		effect.setShouldDisableView(true);
		effect.setOnPreferenceChangeListener(this);

		ListPreference exposure = new ListPreference(this);
		exposure.setKey(KEYS.EXPOSURE_COMPENSATION);
		exposure.setTitle(TITLES.EXPOSURE_COMPENSATION);
		exposure.setValue(getPreferenceValue(KEYS.EXPOSURE_COMPENSATION, DEFAULTS.EXPOSURE_COMPENSATION));
		exposure.setShouldDisableView(true);
		exposure.setOnPreferenceChangeListener(this);

		ListPreference focus_mode = new ListPreference(this);
		focus_mode.setKey(KEYS.FOCUS_MODE);
		focus_mode.setTitle(TITLES.FOCUS_MODE);
		focus_mode.setEntries(LISTS.FOCUS_MODE_ENTRIES);
		focus_mode.setEntryValues(LISTS.FOCUS_MODE_ENTRYVALUES);
		focus_mode.setValue(getPreferenceValue(KEYS.FOCUS_MODE, DEFAULTS.FOCUS_MODE));
		focus_mode.setShouldDisableView(true);
		focus_mode.setOnPreferenceChangeListener(this);

		ListPreference preview_format = new ListPreference(this);
		preview_format.setKey(KEYS.PREVIEW_FORMAT);
		preview_format.setTitle(TITLES.PREVIEW_FORMAT);
		preview_format.setEntries(LISTS.PREVIEW_FORMAT);
		preview_format.setEntryValues(LISTS.PREVIEW_FORMAT);
		preview_format.setValue(getPreferenceValue(KEYS.PREVIEW_FORMAT, DEFAULTS.PREVIEW_FORMAT));
		preview_format.setShouldDisableView(true);
		preview_format.setOnPreferenceChangeListener(this);
		preview_format.setEnabled(false);

		ListPreference preview_fps_range = new ListPreference(this);
		preview_fps_range.setKey(KEYS.PREVIEW_FPS_RANGE);
		preview_fps_range.setTitle(TITLES.PREVIEW_FPS_RANGE);
		preview_fps_range.setValue(getPreferenceValue(KEYS.PREVIEW_FPS_RANGE, DEFAULTS.PREVIEW_FPS_RANGE));
		preview_fps_range.setShouldDisableView(true);
		preview_fps_range.setOnPreferenceChangeListener(this);

		ListPreference preview_size = new ListPreference(this);
		preview_size.setKey(KEYS.PREVIEW_SIZE);
		preview_size.setTitle(TITLES.PREVIEW_SIZE);
		preview_format.setValue(getPreferenceValue(KEYS.PREVIEW_SIZE, DEFAULTS.PREVIEW_SIZE));
		preview_size.setShouldDisableView(true);
		preview_size.setOnPreferenceChangeListener(this);

		ListPreference scene_mode = new ListPreference(this);
		scene_mode.setKey(KEYS.SCENE_MODE);
		scene_mode.setTitle(TITLES.SCENE_MODE);
		scene_mode.setEntries(LISTS.SCENE_MODE_ENTRIES);
		scene_mode.setEntryValues(LISTS.SCENE_MODE_ENTRYVALUES);
		scene_mode.setValue(getPreferenceValue(KEYS.SCENE_MODE, DEFAULTS.SCENE_MODE));
		scene_mode.setShouldDisableView(true);
		scene_mode.setOnPreferenceChangeListener(this);

		ListPreference whitebalance = new ListPreference(this);
		whitebalance.setKey(KEYS.WHITEBALANCE);
		whitebalance.setTitle(TITLES.WHITEBALANCE);
		whitebalance.setEntries(LISTS.WHITEBALANCE_ENTRIES);
		whitebalance.setEntryValues(LISTS.WHITEBALANCE_ENTRYVALUES);
		whitebalance.setValue(getPreferenceValue(KEYS.WHITEBALANCE, DEFAULTS.WHITEBALANCE));
		whitebalance.setShouldDisableView(true);
		whitebalance.setOnPreferenceChangeListener(this);

		camera_settings_category.addPreference(antibanding);
		camera_settings_category.addPreference(effect);
		camera_settings_category.addPreference(exposure);
		camera_settings_category.addPreference(focus_mode);
		camera_settings_category.addPreference(preview_format);
		camera_settings_category.addPreference(preview_fps_range);
		camera_settings_category.addPreference(preview_size);
		camera_settings_category.addPreference(scene_mode);
		camera_settings_category.addPreference(whitebalance);
	}

	/**
	 * Creates the UI that allows the user to select a video file to the be processed.
	 */
	private void addVideoFilePreference() {
		PreferenceCategory category = (PreferenceCategory) findPreference(KEYS.FRAME_PROVIDER_CATEGORY);
		Preference preference = findPreference(KEYS.VIDEO_FILE);
		if (preference == null) {

			preference = new Preference(this);
			category.addPreference(preference);

			preference.setKey(KEYS.VIDEO_FILE);
			preference.setTitle(TITLES.VIDEO_FILE);
			preference.setSummary(getPreferenceValue(KEYS.VIDEO_FILE, ""));

			/*
			 * We don't need to explicitly provide an activity to choose a video. The system handles it because we specify an
			 * ACTION_GET_CONTENT (select something) intent, restricted to content of type video/*. This results in a video gallery being displayed.
			 * Using <em>startActivityForResult</em> means that our <em>onActivityResult</em> method is automatically invoked on return from
			 * the video chooser.
			 */
			preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				private void launchVideoChooser() {
					Intent i = new Intent(Intent.ACTION_GET_CONTENT);
					i.setType("video/*");
					startActivityForResult(i, VALUES.REQUEST_VIDEO_FILE_INTENT_CODE);
				}

				@Override
				public boolean onPreferenceClick(Preference preference) {
					launchVideoChooser();
					return true;

				}
			});
		}
	}

	/**
	 * Launch the folder selection activity.
	 */
	private void launchFolderChooser() {

	}

	/**
	 * Creates the UI to allow the user to choose a folder of frame image files to process.
	 */
	private void addFrameFolderPreference() {
		PreferenceCategory category = (PreferenceCategory) findPreference(KEYS.FRAME_PROVIDER_CATEGORY);
		Preference preference = findPreference(KEYS.FRAME_FOLDER);

		if (preference == null) {
			preference = new Preference(this);
			preference.setKey(KEYS.FRAME_FOLDER);
			preference.setTitle(TITLES.FRAME_FOLDER);
			preference.setSummary(getPreferenceValue(KEYS.FRAME_FOLDER, ""));
			preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					launchFolderChooser();
					return true;
				}
			});
		}
		category.addPreference(preference);
	}

	/**
	 * Updates the UI to contain the correct preferences when the source of frames has changed,
	 * 
	 * @param provider
	 *            the frame provider (camera, video file, frame folder)
	 */
	private void updateFrameProviderUI(int provider) {
		removePreferenceChild(KEYS.FRAME_PROVIDER_CATEGORY, KEYS.VIDEO_FILE);
		removePreferenceChild(KEYS.FRAME_PROVIDER_CATEGORY, KEYS.FRAME_FOLDER);

		if (provider == VALUES.LIVE_PREVIEW) {
			setPreferenceState(KEYS.LIVE_PREVIEW_CATEGORY, true);
		} else {
			setPreferenceState(KEYS.LIVE_PREVIEW_CATEGORY, false);
			if (provider == VALUES.VIDEO_FILE) {
				addVideoFilePreference();
			} else if (provider == VALUES.FRAME_FOLDER) {
				addFrameFolderPreference();
			}
		}
	}

	/**
	 * We have a set of options in a preference (eg a list of focus modes) and the options that the camera actually supports (retrieved from the camera parameters).
	 * 
	 * So we need to remove items from the preference options if they aren't in the supported options, and add items that are supported but not currently in the preference options.
	 * 
	 */
	private void filterValues(ListPreference list_preference, List<String> supported_values) {
		CharSequence[] preference_entries = list_preference.getEntries();
		CharSequence[] preference_entry_values = list_preference.getEntryValues();

		ArrayList<CharSequence> filtered_preference_entries = new ArrayList<CharSequence>();
		ArrayList<CharSequence> filtered_preference_entry_values = new ArrayList<CharSequence>();

		for (int i = 0; i < preference_entry_values.length; i++) {
			if (supported_values.contains(preference_entry_values[i])) {
				filtered_preference_entries.add(preference_entries[i]);
				filtered_preference_entry_values.add(preference_entry_values[i]);
				supported_values.remove(preference_entry_values[i]);
			}
		}
		for (int i = 0; i < supported_values.size(); i++) {
			filtered_preference_entries.add(supported_values.get(i).substring(0, 1).toUpperCase() + supported_values.get(i).substring(1));
			filtered_preference_entry_values.add(supported_values.get(i));
		}

		CharSequence[] new_preference_entries = new CharSequence[filtered_preference_entries.size()];
		CharSequence[] new_preference_entry_values = new CharSequence[filtered_preference_entry_values.size()];

		new_preference_entries = filtered_preference_entries.toArray(new_preference_entries);
		new_preference_entry_values = filtered_preference_entry_values.toArray(new_preference_entry_values);

		list_preference.setEntries(new_preference_entries);
		list_preference.setEntryValues(new_preference_entry_values);
	}

	/**
	 * Makes sure the preview format preference only contains supported options.
	 */
	private void filterPreviewFormatValues(ListPreference list_preference, List<Integer> supported_values) {
		CharSequence[] preference_entries = list_preference.getEntries();
		CharSequence[] preference_entry_values = list_preference.getEntryValues();

		ArrayList<CharSequence> filtered_preference_entries = new ArrayList<CharSequence>();
		ArrayList<CharSequence> filtered_preference_entry_values = new ArrayList<CharSequence>();

		for (int i = 0; i < preference_entry_values.length; i++) {
			if (supported_values.contains(preview_format_lookup_int.get(preference_entry_values[i]))) {
				filtered_preference_entries.add(preference_entries[i]);
				filtered_preference_entry_values.add(preference_entry_values[i]);
			}
		}

		CharSequence[] new_preference_entries = new CharSequence[filtered_preference_entries.size()];
		CharSequence[] new_preference_entry_values = new CharSequence[filtered_preference_entry_values.size()];

		new_preference_entries = filtered_preference_entries.toArray(new_preference_entries);
		new_preference_entry_values = filtered_preference_entry_values.toArray(new_preference_entry_values);

		list_preference.setEntries(new_preference_entries);
		list_preference.setEntryValues(new_preference_entry_values);
	}

	/**
	 * The list of exposure compensation options needs to be constructed from the min, max and step values.
	 */
	private void addExposureCompensationOptions(ListPreference list_preference, Camera.Parameters camera_parameters) {
		int min = camera_parameters.getMinExposureCompensation();
		int max = camera_parameters.getMaxExposureCompensation();
		float step = camera_parameters.getExposureCompensationStep();

		int num_steps = (int) ((max - min) / step) + 1;

		CharSequence[] new_preference_entries = new CharSequence[num_steps];
		CharSequence[] new_preference_entry_values = new CharSequence[num_steps];

		for (int i = 0; i < num_steps; i++) {
			float step_value = min + (step * i);
			String step_value_string = Float.toString(step_value);
			new_preference_entries[i] = step_value_string;
			new_preference_entry_values[i] = step_value_string;
		}

		list_preference.setEntries(new_preference_entries);
		list_preference.setEntryValues(new_preference_entry_values);
	}

	/**
	 * Makes sure the preview frame rate preference only contains supported options.
	 */
	private void addPreviewFPSRangeOptions(ListPreference list_preference, List<int[]> supported_values) {

		CharSequence[] new_preference_entries = new CharSequence[supported_values.size()];
		CharSequence[] new_preference_entry_values = new CharSequence[supported_values.size()];

		for (int i = 0; i < supported_values.size(); i++) {
			int[] fps_range = supported_values.get(i);
			new_preference_entries[i] = fps_range[Parameters.PREVIEW_FPS_MIN_INDEX] / 1000 + ".." + fps_range[Parameters.PREVIEW_FPS_MAX_INDEX] / 1000 + " fps";
			new_preference_entry_values[i] = fps_range[Parameters.PREVIEW_FPS_MIN_INDEX] + "," + fps_range[Parameters.PREVIEW_FPS_MAX_INDEX];
		}

		list_preference.setEntries(new_preference_entries);
		list_preference.setEntryValues(new_preference_entry_values);
	}

	/**
	 * Makes sure the preview size preference only contains supported options and make the entries/values have the appropriate WxH format.
	 */
	private void addPreviewSizeOptions(ListPreference list_preference, List<Size> supported_sizes) {

		CharSequence[] new_preference_entries = new CharSequence[supported_sizes.size()];
		CharSequence[] new_preference_entry_values = new CharSequence[supported_sizes.size()];

		for (int i = 0; i < supported_sizes.size(); i++) {
			Size size = supported_sizes.get(i);
			new_preference_entries[i] = size.width + "x" + size.height;
			new_preference_entry_values[i] = size.width + "x" + size.height;
		}

		list_preference.setEntries(new_preference_entries);
		list_preference.setEntryValues(new_preference_entry_values);

	}

	/**
	 * 
	 * Process the parameters of the currently active camera to construct the preferences.
	 */
	private void configureCameraPreferences() {

		ListPreference antibanding_list = (ListPreference) findPreference(KEYS.ANTIBANDING);
		ListPreference effect_list = (ListPreference) findPreference(KEYS.EFFECT);
		ListPreference exposure_compensation_list = (ListPreference) findPreference(KEYS.EXPOSURE_COMPENSATION);
		ListPreference focus_mode_list = (ListPreference) findPreference(KEYS.FOCUS_MODE);
		ListPreference preview_format_list = (ListPreference) findPreference(KEYS.PREVIEW_FORMAT);
		ListPreference preview_fps_range_list = (ListPreference) findPreference(KEYS.PREVIEW_FPS_RANGE);
		ListPreference preview_size_list = (ListPreference) findPreference(KEYS.PREVIEW_SIZE);
		ListPreference scene_mode_list = (ListPreference) findPreference(KEYS.SCENE_MODE);
		ListPreference whitebalance_list = (ListPreference) findPreference(KEYS.WHITEBALANCE);

		List<String> supported;

		/*
		 * Each parameter is dealt with by
		 * 		- getting the list of supported values (null if the feature not supported by the camera)
		 * 		- disabling the preference if the feature isn't supported
		 * 		- if the feature only has one supported value disable the preference and set its value to the single value
		 * 		- enabling the preference if there's more than one supported value
		 * 		- filtering the preference values
		 * 		- setting the summary to the current value (seems more useful than providing some explanatory text)
		 * 	
		 */
		supported = camera_parameters.getSupportedAntibanding();
		if (supported == null) {
			antibanding_list.setEnabled(false);
		} else {
			if (supported.size() == 1) {
				antibanding_list.setEnabled(false);
				antibanding_list.setValue(supported.get(0));
			} else {
				antibanding_list.setEnabled(true);
			}
			filterValues(antibanding_list, supported);
			antibanding_list.setSummary(antibanding_list.getEntry());
		}

		supported = camera_parameters.getSupportedColorEffects();
		if (supported == null) {
			effect_list.setEnabled(false);
		} else {
			if (supported.size() == 1) {
				effect_list.setEnabled(false);
				effect_list.setValue(supported.get(0));
			} else {
				effect_list.setEnabled(true);
			}
			filterValues(effect_list, supported);
			effect_list.setSummary(effect_list.getEntry());
		}

		addExposureCompensationOptions(exposure_compensation_list, camera_parameters);
		exposure_compensation_list.setSummary(exposure_compensation_list.getEntry());

		supported = camera_parameters.getSupportedFocusModes();
		if (supported == null) {
			focus_mode_list.setEnabled(false);
		} else {
			if (supported.size() == 1) {
				focus_mode_list.setEnabled(false);
				focus_mode_list.setValue(supported.get(0));
			} else {
				focus_mode_list.setEnabled(true);
			}
			filterValues(focus_mode_list, supported);
			focus_mode_list.setSummary(focus_mode_list.getEntry());
		}

		List<Integer> supported_preview_formats = camera_parameters.getSupportedPreviewFormats();
		if (supported_preview_formats == null) {
			preview_format_list.setEnabled(false);
		} else {
			if (supported_preview_formats.size() == 1) {
				preview_format_list.setEnabled(false);
				preview_format_list.setValue(preview_format_lookup_string.get(supported_preview_formats.get(0)));
			} else {
				preview_format_list.setEnabled(true);
			}
			filterPreviewFormatValues(preview_format_list, supported_preview_formats);
			preview_format_list.setSummary(preview_format_list.getEntry());
		}

		List<int[]> fps_ranges = camera_parameters.getSupportedPreviewFpsRange();
		addPreviewFPSRangeOptions(preview_fps_range_list, fps_ranges);
		preview_fps_range_list.setSummary(preview_fps_range_list.getEntry());

		List<Size> supported_preview_sizes = camera_parameters.getSupportedPreviewSizes();
		addPreviewSizeOptions(preview_size_list, supported_preview_sizes);
		preview_size_list.setSummary(preview_size_list.getEntry());

		supported = camera_parameters.getSupportedSceneModes();
		if (supported == null) {
			scene_mode_list.setEnabled(false);
		} else {
			if (supported.size() == 1) {
				scene_mode_list.setEnabled(false);
				scene_mode_list.setValue(supported.get(0));
			} else {
				scene_mode_list.setEnabled(true);
			}
			filterValues(scene_mode_list, supported);
			scene_mode_list.setSummary(scene_mode_list.getEntry());
		}

		supported = camera_parameters.getSupportedWhiteBalance();
		if (supported == null) {
			whitebalance_list.setEnabled(false);
		} else {
			if (supported.size() == 1) {
				whitebalance_list.setEnabled(false);
				whitebalance_list.setValue(supported.get(0));
			} else {
				whitebalance_list.setEnabled(true);
			}
			filterValues(whitebalance_list, supported);
			whitebalance_list.setSummary(whitebalance_list.getEntry());
		}

	}

	@Override
	/**
	 * Fires when a preference is changed by the user but before its value is stored allowing us to update 
	 * the UI as necessary.
	 * <p>
	 * If the user switched between camera preview/video file the UI needs to be updated. If a camera setting was changed the
	 * camera manager parameters are updated. If the user switched between front/back camera update the selection in the camera manager
	 * and update the preferences UI to match the new camera capabilities.
	 * <p>
	 * Returns true to have the value stored.
	 */
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		Class<? extends Preference> clazz = preference.getClass();

		if (clazz.getSimpleName().equals("IntListPreference")) {
			CharSequence[] entries = ((IntListPreference) preference).getEntries();
			int index = ((IntListPreference) preference).findIndexOfValue((Integer) newValue);

			preference.setSummary(entries[index]);

			String key = preference.getKey();
			if (key.equals(KEYS.FRAME_PROVIDER)) {
				updateFrameProviderUI((Integer) newValue);
			} else if (key.equals(KEYS.CAMERA_SELECTION)) {
				// ImageProcessingActivity.camera_manager.updateCameraSelection((Integer) newValue);
				configureCameraPreferences();

				IntListPreference camera_selection = (IntListPreference) findPreference(KEYS.CAMERA_SELECTION);
				PreferenceScreen camera_settings_screen = (PreferenceScreen) findPreference(KEYS.LIVE_PREVIEW_SETTINGS_SCREEN);
				CharSequence[] cs_entries = camera_selection.getEntries();
				int cs_index = camera_selection.findIndexOfValue((Integer) newValue);
				camera_settings_screen.setSummary(SUMMARIES.LIVE_PREVIEW_SETTINGS_SCREEN + cs_entries[cs_index]);

			}
		}

		if (clazz.getSimpleName().equals("ListPreference")) {
			CharSequence[] entries = ((ListPreference) preference).getEntries();
			int index = ((ListPreference) preference).findIndexOfValue((String) newValue);

			preference.setSummary(entries[index]);

			String key = preference.getKey();
			if (key.startsWith(KEYS.CAMERA_SETTINGS)) {

				// ImageProcessingActivity.camera_manager.updateCameraParameter(key.replace("camera-settings-", ""), (String) newValue);
			}
		}
		return true;
	}

	private String getPreferenceValue(String key, String default_val) {
		return PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString(key, default_val);
	}

	private int getIntPreferenceValue(String key, int default_val) {
		return PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getInt(key, default_val);
	}

	private void updatePreference(String key, String value) {
		Preference pref = findPreference(key);
		pref.setSummary(value);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}

	private void setPreferenceState(String key, boolean state) {
		findPreference(key).setEnabled(state);
	}

	private void removePreferenceChild(String parent_key, String child_key) {
		PreferenceGroup parent = (PreferenceGroup) findPreference(parent_key);
		Preference child = findPreference(child_key);

		if (child != null) {
			parent.removePreference(child);
		}
	}

	@Override
	/**
	 * Invoked when returning from a sub-activity. If the user was choosing a video file we work out its full path and update the corresponding
	 * preference.
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == RESULT_CANCELED) {
			return;
		}

		if (requestCode == VALUES.REQUEST_VIDEO_FILE_INTENT_CODE) {
			String video_file_path = null;

			Uri file_uri = intent.getData();

			if (file_uri.toString().startsWith("file")) {
				video_file_path = file_uri.toString().substring(6);
			} else {
				String[] proj = { MediaStore.Video.VideoColumns.DATA };
				Cursor cursor = getContentResolver().query(file_uri, proj, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATA);
				cursor.moveToFirst();

				video_file_path = cursor.getString(column_index);

			}
			updatePreference(KEYS.VIDEO_FILE, video_file_path);

		} else if (requestCode == VALUES.REQUEST_FRAME_FOLDER_INTENT_CODE) {
			String folder_path = intent.getStringExtra("selected_folder");

			updatePreference(KEYS.FRAME_FOLDER, folder_path);

		}
	}
}