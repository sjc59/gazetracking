package org.stevej.android.facedetection.preferences;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Lists in application preferences have to be backed by string values. This allows lists backed by integer values.
 * 
 * This is an improved/extended version of code published at http://kvance.livejournal.com/1039349.html
 */
public class IntListPreference extends ListPreference {
	private int	mClickedDialogEntryIndex;

	public IntListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public IntListPreference(Context context) {
		super(context);
	}

	public int[] getIntEntryValues() {
		CharSequence[] entry_values = super.getEntryValues();
		int[] int_entry_values = new int[entry_values.length];
		for (int i = 0; i < entry_values.length; i++) {
			int_entry_values[i] = Integer.parseInt((String) entry_values[i]);
		}
		return int_entry_values;
	}

	@Override
	public CharSequence getEntry() {

		CharSequence[] entries = getEntries();
		return entries[findIndexOfValue(getIntValue())];
	}

	public int getIntValue() {
		String value = super.getValue();
		return Integer.parseInt(value);
	}

	public void setIntEntryValues(int[] entry_values) {
		CharSequence[] cs_entry_values = new CharSequence[entry_values.length];
		for (int i = 0; i < entry_values.length; i++) {
			cs_entry_values[i] = String.valueOf(entry_values[i]);
		}
		super.setEntryValues(cs_entry_values);
	}

	public void setIntValue(int value) {
		super.setValue(String.valueOf(value));
	}

	public void setIntDefaultValue(int value) {
		super.setValue(String.valueOf(value));
	}

	public int findIndexOfValue(int value) {
		int[] mEntryValues = getIntEntryValues();
		if (mEntryValues != null) {
			for (int i = mEntryValues.length - 1; i >= 0; i--) {
				if (mEntryValues[i] == value) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {

		if (super.getEntries() == null || getIntEntryValues() == null) {
			throw new IllegalStateException("ListPreference requires an entries array and an entryValues array.");
		}

		mClickedDialogEntryIndex = findIndexOfValue(getIntValue());
		builder.setSingleChoiceItems(getEntries(), mClickedDialogEntryIndex, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mClickedDialogEntryIndex = which;

				IntListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
				dialog.dismiss();
			}
		});

		builder.setPositiveButton(null, null);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {

		int[] mEntryValues = getIntEntryValues();

		if (positiveResult && mClickedDialogEntryIndex >= 0 && mEntryValues != null) {
			int value = mEntryValues[mClickedDialogEntryIndex];
			if (callChangeListener(value)) {
				setIntValue(value);
			}
		}
	}

	@Override
	protected boolean persistString(String value) {
		if (value == null) {
			return false;
		} else {
			return persistInt(Integer.valueOf(value));
		}
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		if (getSharedPreferences().contains(getKey())) {
			int intValue = getPersistedInt(0);
			return String.valueOf(intValue);
		} else {
			return defaultReturnValue;
		}
	}
}