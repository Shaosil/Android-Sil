package com.gmail.ShaosilDev.Sil;

import java.io.File;

import android.os.Environment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.app.AlertDialog;

final public class Preferences {

	public static final int rows = 24;
	public static final int cols = 80;

	static final String NAME = "angband";
	
	static final String KEY_FONT = "angband.font";
	static final String KEY_VIBRATE = "angband.vibrate";
	static final String KEY_FULLSCREEN = "angband.fullscreen";
	static final String KEY_ORIENTATION = "angband.orientation";

	static final String KEY_KEYBOARDOVERLAP = "angband.allowKeyboardOverlap";
	static final String KEY_KEYBOARDOPACITY = "angband.keyboardOpacity";
	static final String KEY_ENABLETOUCH = "angband.enabletouch";
	static final String KEY_PORTRAITKB = "angband.portraitkb";
	static final String KEY_LANDSCAPEKB = "angband.landscapekb";
	static final String KEY_PORTRAITFONTSIZE = "angband.portraitfontsize";
	static final String KEY_LANDSCAPEFONTSIZE = "angband.landscapefontsize";
	static final String KEY_ALWAYSRUN = "angband.alwaysrun";

	static final String KEY_GAMEPROFILE = "angband.gameprofile";
	static final String KEY_SKIPWELCOME = "angband.skipwelcome";

	static final String KEY_PROFILES = "angband.profiles";
	static final String KEY_ACTIVEPROFILE = "angband.activeprofile";

	static final String KEY_INSTALLEDVERSION = "angband.installedversion";

	private static String activityFilesPath;

	private static SharedPreferences pref;
	private static ProfileList profiles;
	private static String version;
	private static int fontSize = 17;
	private static Resources resources;

	private static KeyMapper keymapper;
	
	Preferences() {}

	public static void init(File filesDir, Resources res, SharedPreferences sharedPrefs, String pversion) {
		activityFilesPath = filesDir.getAbsolutePath();
		pref = sharedPrefs;
		resources = res;
		version = pversion;

		keymapper = new KeyMapper(pref);
	}

	public static String getVersion() {
		return version;
	}

	public static Resources getResources() {
		return resources;
	}

	public static String getString(String key) {
		return pref.getString(key, "");
	}

	public static boolean getFullScreen() {
		return pref.getBoolean(Preferences.KEY_FULLSCREEN, true);
	}
	public static void setFullScreen(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_FULLSCREEN, value);
		ed.commit();			
	}
	public static boolean isScreenPortraitOrientation() {
		Configuration config = resources.getConfiguration();		
		return (config.orientation == Configuration.ORIENTATION_PORTRAIT);
	}

	public static int getOrientation() {
		return Integer.parseInt(pref.getString(Preferences.KEY_ORIENTATION, "0"));
	}

	public static boolean getKeyboardOverlap() {
		return pref.getBoolean(Preferences.KEY_KEYBOARDOVERLAP, true);
	}

	public static int getKeyboardOpacity() {
		return pref.getInt(Preferences.KEY_KEYBOARDOPACITY, 50);
	}

	public static int getFont() {
		return Integer.parseInt(pref.getString(Preferences.KEY_FONT, "0"));
	}

	public static int getDefaultFontSize() {
		return fontSize;
	}

	public static boolean getVibrate() {
		return pref.getBoolean(Preferences.KEY_VIBRATE, false);
	}

	public static boolean getPortraitKeyboard() {
		return pref.getBoolean(Preferences.KEY_PORTRAITKB, true);
	}
	public static void setPortraitKeyboard(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_PORTRAITKB, value);
		ed.commit();			
	}
	public static boolean getLandscapeKeyboard() {
		return pref.getBoolean(Preferences.KEY_LANDSCAPEKB, false);
	}
	public static void setLandscapeKeyboard(boolean value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putBoolean(Preferences.KEY_LANDSCAPEKB, value);
		ed.commit();			
	}

	public static int getPortraitFontSize() {
		return pref.getInt(Preferences.KEY_PORTRAITFONTSIZE, 0);
	}
	public static void setPortraitFontSize(int value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_PORTRAITFONTSIZE, value);
		ed.commit();			
	}
	public static int getLandscapeFontSize() {
		return pref.getInt(Preferences.KEY_LANDSCAPEFONTSIZE, 0);
	}
	public static void setLandscapeFontSize(int value) {
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_LANDSCAPEFONTSIZE, value);
		ed.commit();			
	}

	public static boolean getEnableTouch() {
		return pref.getBoolean(Preferences.KEY_ENABLETOUCH, true);
	}

	public static boolean getAlwaysRun() {
		return pref.getBoolean(Preferences.KEY_ALWAYSRUN, false);
	}

	public static boolean getSkipWelcome() {
		return getActiveProfile().getSkipWelcome();
	}

	public static String getAngbandFilesDirectory() {
		return 
			Environment.getExternalStorageDirectory()
			+ "/"
			+ "Android/data/com.gmail.ShaosilDev.Sil/files/lib";
	}

	public static String getActivityFilesDirectory() {
		return activityFilesPath;
	}

	public static ProfileList getProfiles() {
		if (profiles == null) {
			//Log.d("Sil", "loading profiles");
			String s = pref.getString(Preferences.KEY_PROFILES, "");
			if (s.length() == 0) {
				profiles = ProfileList.deserialize("0~Default~PLAYER~0");
				saveProfiles();

				// for some reason ProfileListPreference needs a persisted value to display
				// the very first time.
				// ...there is probably an override to get around this in ListPreference.
				SharedPreferences.Editor ed = pref.edit();
				ed.putBoolean("initialized", true);
				ed.commit();
			}
			else {
				profiles = ProfileList.deserialize(s);
			}
		}
		return profiles;
	}

	public static void saveProfiles() {
		// low-level save
		// assumes validation has already occurred in activity

		//Log.d("Sil", "saving profiles");

		// generate Ids if necessary
		ProfileList pl = getProfiles();
		for(int ix = 0; ix < pl.size(); ix++) {
			if (pl.get(ix).id == 0) {
				pl.get(ix).id = pl.getNextId();
			}
		}

		SharedPreferences.Editor ed = pref.edit();
		ed.putString(Preferences.KEY_PROFILES, profiles.serialize());
		ed.commit();
	}

	public static Profile getActiveProfile() {
		ProfileList pl = getProfiles();
		int id = pref.getInt(Preferences.KEY_ACTIVEPROFILE, 0);
		Profile p = pl.findById(id);
		if (p == null) {
			p = pl.get(0);
			setActiveProfile(p);
		}
		return p;
	}

	public static void setActiveProfile(Profile p) {
		ProfileList pl = getProfiles();
		SharedPreferences.Editor ed = pref.edit();
		ed.putInt(Preferences.KEY_ACTIVEPROFILE, p.id);
		ed.commit();
	}

	public static boolean saveFileExists(String filename) {
		File f = new File(
			getAngbandFilesDirectory() 
			+ "/save/" 
			+ filename
		);
		if (f.exists()) return true;

		return false;
	}

	public static String generateSaveFilename() {
		ProfileList pl = getProfiles();
		String saveFile = null;
		for(int i = 2; i < 100; i++) {
			saveFile = "PLAYER"+i;
			if (pl.findBySaveFile(saveFile,0) == null
				&& !saveFileExists(saveFile)) 
				break;
		}
		return saveFile;
	}		

	public static int alert(Context ctx, String title, String msg) {
		new AlertDialog.Builder(ctx) 
			.setTitle(title) 
			.setMessage(msg) 
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {}
			}
		).show();
		return 0;
	}

	public static KeyMapper getKeyMapper() {
		return keymapper;
	}
}

