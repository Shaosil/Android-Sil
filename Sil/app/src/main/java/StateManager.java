package com.gmail.ShaosilDev.Sil;

import android.os.Handler;

import android.view.KeyEvent;
import android.util.Log;

import java.util.Map;
import java.util.HashMap;

public class StateManager {
	/* screen state */
	public Map<Integer,TermWindow> termwins = null;
	public TermWindow virtscr = null;
	public TermWindow stdscr = null;
	public int termWinNext = 0;

	/* alert dialog state */
	public boolean fatalError = false;
	public boolean warnError = false;
	public String fatalMessage = "";
	public String warnMessage = "";

	/* progress dialog state */
	public static String progress_lock = "lock";

	/* keybuffer */
	private KeyBuffer keyBuffer = null;

	/* native angband library interface */
	public NativeWrapper nativew = null;

	/* game thread */
	public GameThread gameThread = null;

	/* game thread */
	public Handler handler = null;

	StateManager() {
		endWin();

		nativew = new NativeWrapper(this);
		gameThread = new GameThread(this, nativew);

		keyBuffer = new KeyBuffer(this);
	}

	public void link(TermView t, Handler h) {
		handler = h;
		nativew.link(t);
	}

	public void endWin() {
		termWinNext = -1;
		termwins = new HashMap<Integer,TermWindow>();

		// initialize virtual screen (virtscr) and curses stdscr 
		int h = newWin(0,0,0,0);
		virtscr = getWin(h);
		h = newWin(0,0,0,0);
		stdscr = getWin(h);
	}
	public TermWindow getWin(int handle) {
		TermWindow w = termwins.get(handle);
		return w;
	}
	public void delWin(int handle) {
		termwins.remove(handle);
	}
	public int newWin(int nlines, int ncols, int begin_y, int begin_x) {
		int h = termWinNext;
		termwins.put(h,new TermWindow(nlines,ncols,begin_y,begin_x));
		termWinNext++;
		return h;
	}

	public String getFatalError() {
		return "Angband quit with the following error: "+fatalMessage;
	}

	public String getWarnError() {
		return "Angband sent the following warning: "+warnMessage;
	}

	public void clearKeys() {
		if (this.keyBuffer != null)
			this.keyBuffer.clear();
	}
	public void resetKeyBuffer() {
		this.keyBuffer = new KeyBuffer(this);
	}

	public void addKey(int k) {
		if (this.keyBuffer == null)
			return;
		this.keyBuffer.add(k);
	}

	public int getKey(int v) {
		if (this.keyBuffer != null)
			return keyBuffer.get(v);
		else
			return 0;
	}
	public void addDirectionKey(int k) {
		if (this.keyBuffer != null)
			this.keyBuffer.addDirection(k);
	}
	public void signalSave(boolean alsoQuit) {
		if (this.keyBuffer != null)
			this.keyBuffer.signalSave(alsoQuit);
	}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (this.keyBuffer != null)
			return this.keyBuffer.onKeyDown(keyCode, event);
		else
			return false;
	}
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (this.keyBuffer != null)
			return this.keyBuffer.onKeyUp(keyCode, event);
		else
			return false;
	}
}
