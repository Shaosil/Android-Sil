package com.gmail.ShaosilDev.Sil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.util.Log;
import android.os.Environment;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;

public class Installer {

	/* installer state */
	public enum InstallState {
		Unknown
			,MediaNotReady
			,InProgress
			,Success
			,Failure;
		public static InstallState convert(int value)
		{
			return InstallState.class.getEnumConstants()[value];
		}
		public static boolean isError(InstallState s) {
			return (s == MediaNotReady || s == Failure);
		}
    };

	private static Thread installWorker = null;
	public InstallState state;
	public String message = "";
	private Activity activity = null;
	private boolean userResponded = false;

	public Installer(Activity act) {
		state = InstallState.Unknown;
		activity = act;
	}

	public synchronized void startInstall() {
		if (state == InstallState.Unknown) {
			state = InstallState.InProgress;
			
			installWorker = new Thread() { 
				@Override
				public void run() {
					message = "";
		
					// Request permissions in case someone is on M+
					if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
						ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);

						// Wait until the user responds
						while (!userResponded) {
							try { sleep(100); }
							catch (InterruptedException e) { }
						}

						// Check state to see if permissions were granted
						if (state != InstallState.Success)
							message = "Error: Cannot create necessary Sil library files without WRITE permissions.";
					}
					else {
						// basic install of required files
						extractPluginResources();
					}
				}
			};
			installWorker.start();
		}
		else {
			return; // install is in error or in progress, cancel
		}
	}

	public boolean failed() {
		return state != InstallState.Success;
	}


	public String errorMessage() {
		String errorMsg = "Error: failed to write and verify files to external storage, cannot continue.";
		switch(state) {
		case MediaNotReady:
			errorMsg = "Error: external storage card not found, cannot continue.";
			break;
		case Failure:
			if (message.length() > 0) 
				errorMsg = message;
			break;
		}
		return errorMsg;
	}

	public boolean needsInstall() {
		// validate sdcard here
		String check = Environment.getExternalStorageState();
		//Log.v("Sil", "media check:" + check);
		if (check.compareTo(Environment.MEDIA_MOUNTED) != 0) {
			state = InstallState.MediaNotReady;
			return true;
		}

		// Check if sil has been extracted
		File silLib = new File(Preferences.getAngbandFilesDirectory());
		if (!silLib.exists() || !silLib.isDirectory() || silLib.list() == null || silLib.list().length <= 0)
			return true;

		state = InstallState.Success;
		return false;
	}

	public void install() {
	}

	private void extractPluginResources() {
		//Log.d("Sil","extractPluginResources "+plugin);
		boolean success = true;
		try {
			File f = new File(Preferences.getAngbandFilesDirectory());
			f.mkdirs();
			String abs_path = f.getAbsolutePath();
			//Log.v("Sil", "installing to " + abs_path);

			// copy game files
			ZipInputStream zis = new ZipInputStream(Preferences.getResources().openRawResource(R.raw.zipsil));
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				String ze_name = ze.getName();
				//Log.v("Sil", "extracting " + ze_name);

				String filename = abs_path + "/" + ze_name;
				File myfile = new File(filename);

				if (ze.isDirectory()) {
					myfile.mkdirs();
					continue;
				}
				else
					myfile.createNewFile();

				byte contents[] = new byte[(int) ze.getSize()];

				FileOutputStream fos = new FileOutputStream(myfile);
				int remaining = (int) ze.getSize();

				int totalRead = 0;

				while (remaining > 0) {
					int readlen = zis.read(contents, 0, remaining);
					fos.write(contents, 0, readlen);
					totalRead += readlen;
					remaining -= readlen;
				}

				fos.close();
				
				// perform a basic length validation
				myfile = new File(filename);
				if (myfile.length() != totalRead) {
					//Log.v("Sil", "Installer.length mismatch: " + filename);
					message = "Error: failed to verify installed file on sdcard: "+filename;
					throw new IllegalStateException();					
				}
				
				zis.closeEntry();
			}
			zis.close();

		} catch (Exception e) {
			success = false;
			if (message.length() == 0)
				message = "Error: failed to install files to sdcard. "+e.getMessage();
			//Log.v("Sil", "error extracting files: " + e);
		}

		if (success) {
			// replaced with crc logic
			//Preferences.setInstalledVersion(Preferences.getVersion());
			state = InstallState.Success;
		}
		else
			state = InstallState.Failure;
	}

	public void waitForInstall() {
		if (installWorker != null) {
			try {
				installWorker.join();
				installWorker = null;
			} catch (Exception e) {
				Log.d("Sil","installWorker "+e.toString());
			}
		}
	}

	public void userRespondedToPermissionRequest(boolean accepted) {
		if (accepted) extractPluginResources();
		else state = InstallState.Failure;
		userResponded = true;
	}
}