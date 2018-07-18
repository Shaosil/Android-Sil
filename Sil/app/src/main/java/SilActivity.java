/*
 * File: SilActivity.java
 * Purpose: Splash & installer
 *
 * Copyright (c) 2010 David Barr, Sergey Belinsky
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

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.util.Log;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;

import android.content.DialogInterface;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;

import android.os.Message;
import android.os.Handler;

public class SilActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

	protected boolean active = true;
	protected int splashTime = 500;
	protected ProgressDialog progressDialog = null;
	protected Handler handler = null;
	private Installer installer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

		String version = "unknown";
		try {
			ComponentName comp = new ComponentName(this, SilActivity.class);
			PackageInfo pinfo = this.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			version = pinfo.versionName;
		} catch (Exception e) {}

	    Preferences.init ( 
			getFilesDir(),
			getResources(), 
			getSharedPreferences(Preferences.NAME, MODE_PRIVATE),
			version
		);

		final Activity splash = this;
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
				case 0:
					//Log.d("Sil", "handler show progress");	
					progressDialog = ProgressDialog.show(splash, "Sil", "Installing files...", true);
					break;
				case 1:
					//Log.d("Sil", "handler dismiss progress");	
					if (progressDialog != null) progressDialog.dismiss();
					progressDialog = null;
					break;
				case 2:
					//Log.d("Sil", "handler fatal error");	
					new AlertDialog.Builder(splash) 
						.setTitle("Sil") 
						.setMessage((String)msg.obj) 
						.setNegativeButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								splash.finish();
							}
						}).show();
					break;
				}
			}
		};

		checkInstall();
	}

	public synchronized void checkInstall() {
		final Activity splash = this;
		installer = new Installer(splash);

		Thread splashTread = new Thread() {
		    @Override
			public void run() {
				Log.d("Sil", "splashThread.run");
				try {
					int waited = 0;
					Log.d("Sil", "splashThread.installer.needsInstall");	
					if (installer.needsInstall()) {
						handler.sendEmptyMessage(0); //show progress
						Log.d("Sil", "splashThread.startinstall");	
						installer.startInstall();
						Log.d("Sil", "splashThread.wait");	
						installer.waitForInstall();
						Log.d("Sil", "finished waiting");	
						handler.sendEmptyMessage(1); //dismiss progress

						splashTime = 200;
					}

					active = true;
					while(active && (waited < splashTime)) {
						sleep(100);
						if(active) {
							waited += 100;
						}
					}
				} catch(InterruptedException e) {
				} finally {
					if (installer.failed()) {
						handler.sendMessage(Message.obtain(handler,2,installer.errorMessage()));
						return;
					}
					else { 
						Intent intent = new Intent(splash, GameActivity.class);
						startActivity(intent);
						splash.finish();
					}
				}
			}
	    };
		splashTread.start();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			active = false;
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		// Granted pemission - proceed with installation
		boolean accepted = requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

		installer.userRespondedToPermissionRequest(accepted);
	}
}