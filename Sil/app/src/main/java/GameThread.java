package com.gmail.ShaosilDev.Sil;

import android.os.Handler;
import android.os.Message;

import android.util.Log;
	
public class GameThread implements Runnable {

	public enum Request{
		StartGame
			,StopGame
			,SaveGame;

		public static Request convert(int value)
		{
			return Request.class.getEnumConstants()[value];
		}
    };

	/* game thread state */	
	private Thread thread = null;
	private boolean game_thread_running = false;
	private boolean game_fully_initialized = false;
	private boolean game_restart = false;
	private String running_profile = null;
	private NativeWrapper nativew = null;
	private StateManager state = null;
	
	public GameThread(StateManager s, NativeWrapper nw) {
		nativew = nw;
		state = s;
	}

	public synchronized void send(Request rq) {
		switch (rq) {
		case StartGame:
			start();
			break;
		case StopGame:
			stop();
			break;
		case SaveGame:
			save();
			break;
		}
	}

	private void start() {
		// sanity checks: thread must not already be running
		// and we must have a valid canvas to draw upon.
		//			already_running = game_thread_running;
		//	already_initialized = game_fully_initialized;	  


		if (state.fatalError) {

			// don't bother restarting here, we are going down.
			Log.d("Sil","start.fatalError is set");
		}
		else if (game_thread_running) {

			/* this is an onResume event */
			if (game_fully_initialized &&
				running_profile.compareTo(Preferences.getActiveProfile().getName())!=0 ) {
			
				/* plugin or profile has been changed */

				Log.d("Sil","start.profile changed");
				stop();
			}
			else {
				//Log.d("Sil","startBand.redrawing");
				state.nativew.resize();
			}			
		}
		else {
			
			/* time to start angband */

			/* notify wrapper game is about to start */
			nativew.onGameStart();
			
 			/* initialize keyboard buffer */
			state.resetKeyBuffer();

			game_thread_running = true;

			//Log.d("Sil","startBand().starting loader thread");

			thread = new Thread(this);
			thread.start();
		}
	}

	private void stop() {
		// signal keybuffer to send quit command to angband 
		// (this is when the user chooses quit or the app is pausing)

		//Log.d("Sil","GameThread.Stop()");

		if (!game_thread_running) {
			//Log.d("Sil","stop().no game running");
			return;
		}
		if (thread == null)  {
			//Log.d("Sil","stop().no thread");
			return;
		}

		state.signalSave(true);

		try {
			game_thread_running = false;
			game_fully_initialized = false;
			thread.join();
		} catch (Exception e) {
			Log.d("Sil",e.toString());
		}
	}

	private void save() {
		//Log.d("Sil","saveBand()");

		if (!game_thread_running) {
			Log.d("Sil","save().no game running");
			return;
		}
		if (thread == null) {
			Log.d("Sil","save().no thread");
			return;
		}
	 
		state.signalSave(false);
	}

	public void setFullyInitialized() {
		//if (!game_fully_initialized) 
		//	Log.d("Sil","game is fully initialized");

		game_fully_initialized = true;		
	}

	public void run() {		
		if (game_restart) {
			game_restart = false;
			/* this hackery is no longer needed after
				serializing all access to GameThread 
				through the sync'd send() method and
			 	use of handlers to initiate async actions.  */
			/*
			try {
				// if restarting, pause for effect (and to let the
				// other game thread unlock its mutex!)
				Thread.sleep(400);
			} catch (Exception ex) {}
			*/
		}

		Log.d("Sil","GameThread.run");

		running_profile = Preferences.getActiveProfile().getName();

		/* game is not running, so start it up */
		nativew.gameStart(
				  2, 
				  new String[]{
					  Preferences.getAngbandFilesDirectory(),
					  Preferences.getActiveProfile().getSaveFile()
				  }
		);
	}
}
