/*
 * File: PreferencesActivity.java
 * Purpose: Preferences activity for Android application
 *
 * Copyright (c) 2009 David Barr, Sergey Belinsky
 * 
 * This work is free software; you can redistribute it and/or modify it
 * under the terms of either:
 *
 * a) the GNU General Public License as published by the Free Software
 *    Foundation, version 2, or
 *
 * b) the "Angband licence":
 *    This software may be copied and distributed for educational, research,
 *    and not for profit purposes provided that this copyright and statement
 *    are included in all such copies.  Other copyrights may also apply.
 */

package com.gmail.ShaosilDev.Sil;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.WindowManager;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceChangeListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(Preferences.NAME);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
		
		// Enable or disable keyboard transparency based on overlap setting
		findPreference(Preferences.KEY_KEYBOARDOVERLAP).setOnPreferenceChangeListener(this);
		findPreference(Preferences.KEY_KEYBOARDOPACITY).setEnabled(Preferences.getKeyboardOverlap());
	}

	@Override
	protected void onResume() {
		super.onResume();

		setSummaryAll(getPreferenceScreen());
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		SharedPreferences pref = getSharedPreferences(Preferences.NAME,
				MODE_PRIVATE);

		if (pref.getBoolean(Preferences.KEY_FULLSCREEN, true)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	@Override protected void onPause() {
		super.onPause(); 
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	} 

	private void setSummaryAll(PreferenceScreen pScreen) {        
		for (int i = 0; i < pScreen.getPreferenceCount(); i++) {
            Preference pref = pScreen.getPreference(i);            
			setSummaryPref(pref);
		}
	} 

	public void setSummaryPref(Preference pref) {
		if (pref == null) return;

		String key = pref.getKey();
		if (key == null) key = "";

		if (pref instanceof KeyMapPreference) {                
			KeyMapPreference kbPref = (KeyMapPreference) pref;     
			String desc = kbPref.getDescription();
			pref.setSummary(desc); 
		}
		else if (pref instanceof PreferenceCategory) {
			PreferenceCategory prefCat = (PreferenceCategory)pref;
			int count = prefCat.getPreferenceCount();
			for (int i=0; i < count; i++) {
				setSummaryPref(prefCat.getPreference(i));
			}
		}
		else if (pref instanceof ListPreference) {
			ListPreference lPref = (ListPreference) pref;
			pref.setSummary(lPref.getEntry()); 
		} 
		else if (pref instanceof ProfileCheckBoxPreference ) {
			ProfileCheckBoxPreference pcPref = (ProfileCheckBoxPreference) pref;     
			if (key.compareTo(Preferences.KEY_SKIPWELCOME)==0) {
				pcPref.setChecked(Preferences.getActiveProfile().getSkipWelcome());
			}
		}
		else if (pref instanceof SeekBarPreference) {
			SeekBarPreference sbPref = (SeekBarPreference)pref;
			sbPref.setSummary(String.format("(%d)", sbPref.getProgress()));
		}
		else if (pref instanceof PreferenceScreen) {
			setSummaryAll((PreferenceScreen) pref); 
		} 
		else if (key.compareTo(Preferences.KEY_GAMEPROFILE)==0) {
			pref.setSummary(Preferences.getActiveProfile().getName());
		} 
	}

	public void	onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key == Preferences.KEY_ACTIVEPROFILE || key == Preferences.KEY_PROFILES) {
			setSummaryAll(getPreferenceScreen());			
		}
		else {
			Preference pref = findPreference(key); 
			setSummaryPref(pref);
		}
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean shouldBeEnabled = (Boolean)newValue;
		findPreference(Preferences.KEY_KEYBOARDOPACITY).setEnabled(shouldBeEnabled);

		return true;
	}
}
