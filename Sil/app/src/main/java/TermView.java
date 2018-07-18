/*
 * File: TermView.java
 * Purpose: Terminal-base view for Android application
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import java.util.LinkedList;
import java.util.Queue;
import android.util.Log;

public class TermView extends View implements OnGestureListener {
	Typeface tfModern;
	Typeface tfClassic;
	Bitmap bitmap;
	Canvas canvas;
	Paint fore;
	Paint back;
	Paint cursor;

	public class DirtyPoint {
		public int row;
		public int col;
		public TermWindow.TermPoint point;

		public DirtyPoint(int r, int c, TermWindow.TermPoint p) {
			row = r;
			col = c;
			point = p;
		}
	}

	public Queue<DirtyPoint> dirtyPoints = new LinkedList<DirtyPoint>();

	public int canvas_width = 0;
	public int canvas_height = 0;

	private int screen_width = 0;
	private int screen_height = 0;
	private int char_height = 0;
	private int char_width = 0;
	private int min_font_size = 8;
	private int font_text_size = 0;

	private Vibrator vibrator;
	private boolean vibrate;
	private Handler handler = null;
	private StateManager state = null;
	private GameActivity _context;

	private GestureDetector gesture;

	public TermView(Context context) {
		super(context);
		initTermView(context);
	}

	public TermView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTermView(context);
	}

	protected void initTermView(Context context) {
		_context = (GameActivity)context;
		handler = _context.getHandler();
		state = _context.getStateManager();

		fore = new Paint();
		fore.setTextAlign(Paint.Align.LEFT);
		setForeColor(Color.WHITE);
		if ( isHighRes() ) fore.setAntiAlias(true);

		back = new Paint();
		setBackColor(Color.BLACK);

		cursor = new Paint();
		cursor.setColor(Color.GREEN);
		cursor.setStyle(Paint.Style.STROKE);
		cursor.setStrokeWidth(0);

		vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);

		setFocusableInTouchMode(true);
		gesture = new GestureDetector(context, this);

		tfModern = Typeface.createFromAsset(getResources().getAssets(), "VeraMoBd.ttf");
		tfClassic = Typeface.createFromAsset(getResources().getAssets(), "8x13.ttf");
	}
	
	@Override
	protected void onDraw(Canvas c) {
		if (bitmap != null) {
			// Update canvas based on queue of DirtyPoints waiting to be drawn
			while (dirtyPoints.peek() != null) {
				drawPoint(dirtyPoints.poll(), false);
			}

			c.drawBitmap(bitmap, 0, 0, null);

			// due to font "scrunch", cursor is sometimes a bit too big
			if (state.stdscr.cursor_visible) {
				int x = state.stdscr.col * (char_width);
				int y = (state.stdscr.row + 1) * char_height;

				int cl = Math.max(x,0);
				int cr = Math.min(x+char_width,canvas_width-1);
				int ct = Math.max(y-char_height,0);
				int cb = Math.min(y,canvas_height-1);

				c.drawRect(cl, ct, cr, cb, cursor);
			}
		}
	}

	public void computeCanvasSize() {
		canvas_width = Preferences.cols*char_width;
	    canvas_height = Preferences.rows*char_height;
	}

	protected void setForeColor(int a) {
		fore.setColor(a);			
	}
	protected void setBackColor(int a) {
		back.setColor(a);			
	}

	public boolean isHighRes() {
		Display display = ((WindowManager) _context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int maxWidth = display.getWidth();
		int maxHeight = display.getHeight();

		//Log.d("Angband","isHighRes "+maxHeight+","+maxWidth +","+ (Math.max(maxWidth,maxHeight)>480));
		return Math.max(maxWidth,maxHeight)>480;
	}

	public void autoSizeFont(boolean byWidth) {
		int maxVal = byWidth ? screen_width : screen_height;
		int indexes = byWidth ? Preferences.cols : Preferences.rows;

		//Log.d("TermView.autoSizeFont()", "Autosizing font by " + (byWidth ? "width" : "height") + ". Max measurement: " + maxVal + ". Indexes: " + indexes);

		// HACK -- keep 480x320 fullscreen as-is
		if (!isHighRes()) {
			setFontSizeLegacy();
		}
		else {
			int charSize;
			font_text_size = min_font_size;
			do {
				increaseFontSize(false);
				charSize = byWidth ? char_width : char_height;
			} while (charSize * indexes < maxVal);
		}

		saveFontSize();
	}

	private void setFontSizeLegacy() {
		font_text_size = 13;
		char_height = 13;
		char_width = 8;
		setFontSize(font_text_size, true);
	}

	private void setFontFace() {
		Typeface curTypeface = fore.getTypeface();
		int oldFontId = curTypeface == tfModern ? 0 : 1;
		int newFontId = Preferences.getFont();

		if (oldFontId != newFontId) {
			//Log.d("TermView.setFontFace()", "Setting font face.");
			fore.setTypeface(newFontId == 0 ? tfModern : tfClassic);

			// Autosize if this is a font "change", or we've never set the font size
			//Log.d("TermView.setFontFace()", "CurTypeface is" + (curTypeface != null ? " not" : "") + " null, font_text_size: " + font_text_size);
			if (curTypeface != null || font_text_size == 0) {
				//Log.d("TermView.setFontFace()", "Autosizing...");
				autoSizeFont(true);
			}
		}
	}

	public void increaseFontSize(boolean persist) {
		setFontSize(font_text_size + 1, persist);
	}

	public void decreaseFontSize(boolean persist) {
		if (font_text_size > min_font_size)
			setFontSize(font_text_size-1, persist);
	}

	private void setFontSize(int size, boolean persist) {
		//Log.d("TermView.setFontSize()", "Setting font size to " + size + "." + (persist ? "Persisting" : "") );

		font_text_size = size;

		setFontFace();

		fore.setTextSize(font_text_size);
		
		if (persist)
			saveFontSize();
 
		char_height = (int)Math.ceil(fore.getFontSpacing()); 
		char_width = (int)fore.measureText("X");	
	}

	@Override
	protected void onMeasure(int widthmeasurespec, int heightmeasurespec) {
		screen_width = MeasureSpec.getSize(widthmeasurespec);
		screen_height = MeasureSpec.getSize(heightmeasurespec);

		int fs = Preferences.isScreenPortraitOrientation() ? Preferences.getPortraitFontSize() : Preferences.getLandscapeFontSize();
		
		//Log.d("TermView.onMeasure()", "w/h/fs " + screen_width + "x" + screen_height + ", " + fs);

		setFontSize(fs, false);  

		fore.setTextAlign(Paint.Align.LEFT);

		setMeasuredDimension(screen_width, screen_height);
	}

	@Override 
	public boolean onTouchEvent(MotionEvent me) {
		return gesture.onTouchEvent(me);
	}
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {	   
		int newscrollx = this.getScrollX() + (int)distanceX;
		int newscrolly = this.getScrollY() + (int)distanceY;
	
		if(newscrollx < 0) 
			newscrollx = 0;
		if(newscrolly < 0) 
			newscrolly = 0;
		if(newscrollx >= canvas_width - getWidth())
			newscrollx = canvas_width - getWidth() + 1;
		if(newscrolly >= canvas_height - getHeight())
		 	newscrolly = canvas_height - getHeight() + 1;

		if (canvas_width <= getWidth()) newscrollx = 0; //this.getScrollX();
		if (canvas_height <= getHeight()) newscrolly = 0; //this.getScrollY();		

		scrollTo(newscrollx, newscrolly);

		return true;
	}
	public boolean onDown(MotionEvent e) {
		return true;
	}
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return true;
	}
	public void onLongPress(MotionEvent e) {
		handler.sendEmptyMessage(AngbandDialog.Action.OpenContextMenu.ordinal());
	}
	public void onShowPress(MotionEvent e) {
	}
	public boolean onSingleTapUp(MotionEvent event) {
		if (!Preferences.getEnableTouch()) return false;

		int x = (int) event.getX();
		int y = (int) event.getY();

		int w = getWidth();
		int h = getHeight();
		int r, c;
		c = (x * 3) / w;
		r = (y * 3) / (h - _context.getKeyboardOverlapHeight());

		int key = (2 - r) * 3 + c + '1';

		//Log.d("TermView.onSingleTapUp()", String.format("X/Y: %d/%d - W/H: %d/%d - KBHeight: %d - Row/Col: %d/%d - Key: %d",
				//x, y, w, h, _context.getKeyboardOverlapHeight(), r, c, key));

		state.addDirectionKey(key);
			
		return true;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		//Log.d("Sil", "onSizeChanged");
		super.onSizeChanged(w, h, oldw, oldh);
		handler.sendEmptyMessage(AngbandDialog.Action.StartGame.ordinal());
  	}

	public boolean onGameStart() {
		computeCanvasSize();

		//Log.d("TermView.onGameStart()", "Canvas size: " + canvas_width + "x" + canvas_height);

		// sanity 
		if (canvas_width == 0 || canvas_height == 0) return false;

		bitmap = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.RGB_565);
		canvas = new Canvas(bitmap);		
		/*
		  canvas.setDrawFilter(new PaintFlagsDrawFilter(
		  Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG,0
		  )); // this seems to have no effect, why?
		*/		

   		return true;
	}

	public void drawPoint(DirtyPoint point, boolean extendedErase) {
		float x = point.col * char_width;
		float y = point.row * char_height;

		if (canvas == null) {
			// OnSizeChanged has not been called yet
			//Log.d("Sil","null canvas in drawPoint");
			return;
		}
		
		switch (point.point.Flag){
			case 1: // SAME AS FOREGROUND
				setBackColor(point.point.Color);
				break;
			case 2: // HIGHLIGHT
				setBackColor(0xFF303030);
				break;
			default: // BLACK
				setBackColor(0xFF000000);
				break;
		}

		canvas.drawRect(
						x, 
						y, 
						x + char_width + (extendedErase ? 1 : 0), 
						y + char_height + (extendedErase ? 1 : 0), 
						back
						);					

		if (point.point.Char != ' ') {
			String str = point.point.Char + "";

			setForeColor(point.point.Color);

			canvas.drawText (
								str,
								x, 
								y + char_height - fore.descent(), 
								fore
								);
		}
	}

	public void clear() {
		if (canvas != null) {
			canvas.drawPaint(back);
		}
	}

	public void noise() {
		if (vibrate) {
			vibrator.vibrate(50);
		}
	}

	public void onResume() {
		//Log.d("Sil","Termview.onResume()");
		vibrate = Preferences.getVibrate();
	}

	public void onStop() {
		//Log.d("Sil","Termview.onStop()");
		// this is the only guaranteed safe place to save state 
		// according to SDK docs
		state.gameThread.send(GameThread.Request.SaveGame);
	}

	private void saveFontSize() {
		//Log.d("TermView.saveFontSize()", "Saving font size");

		if(Preferences.isScreenPortraitOrientation())
			Preferences.setPortraitFontSize(font_text_size);
		else
			Preferences.setLandscapeFontSize(font_text_size);
	}
}