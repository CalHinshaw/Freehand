package com.freehand.preferences;

import com.calhounroberthinshaw.freehand.R;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.text.InputType;

public class PrefFragment extends PreferenceFragment {
	
    @Override
    public void onCreate (Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	addPreferencesFromResource(R.xml.preferences);
    	
    	EditTextPreference pref = (EditTextPreference)findPreference("pressure_sensitivity");
    	pref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    }
}