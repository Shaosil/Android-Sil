package com.gmail.ShaosilDev.Sil;

import android.content.Context;
import android.util.AttributeSet;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.view.MotionEvent;
import android.widget.PopupWindow;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.Region.Op;
import java.lang.reflect.Field;

public class AngbandKeyboardView extends KeyboardView
{
	// A few proxy fields from our super to avoid copy/pasting the entire class
	private PopupWindow mSuperPopupPreview;
	private Rect mSuperDirtyRect;
	private Paint mSuperPaint;
	private Drawable mSuperKeyBackground;
	private Bitmap mSuperBuffer;
	private boolean mSuperDrawPending;
	private boolean mSuperKeyboardChanged;
	private Keyboard mSuperKeyboard;
	private Key[] mSuperKeys;
	private int mSuperKeyTextColor;
	private Key mSuperInvalidatedKey;
	private int mSuperLabelTextSize;
	private int mSuperKeyTextSize;

	public AngbandKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Get the underlying mPreviewPopup for later hackarounds
		mSuperPopupPreview = getSuperObject("mPreviewPopup", PopupWindow.class);

		// And a couple others which are only set once in KeyboardView so performance takes less of a hit
		mSuperDirtyRect = getSuperObject("mDirtyRect", Rect.class);
		mSuperPaint = getSuperObject("mPaint", Paint.class);
		mSuperKeyBackground = getSuperObject("mKeyBackground", Drawable.class);
		mSuperKeyTextColor = getSuperObject("mKeyTextColor", Integer.class);
		mSuperLabelTextSize = getSuperObject("mLabelTextSize", Integer.class);
		mSuperKeyTextSize = getSuperObject("mKeyTextSize", Integer.class);
	}

	private Field getSuperField(String name) {
		try {
			Field f = getClass().getSuperclass().getDeclaredField(name);
			f.setAccessible(true);
			return f;
		}
		catch (NoSuchFieldException e) {
			return null;
		}
	}

	private void setSuperField(String name, Object value) {
		try {
			getSuperField(name).set(this, value);
		}
		catch (IllegalAccessException e) {
			Exception e2 = e;
		}
	}

	private <T> T getSuperObject(String name, Class<T> c) {
		// Access a private variable from our super, KeyboardView, and return its hard typed object
		try {
			return c.cast(getSuperField(name).get(this));
		}
		catch(IllegalAccessException e) { return null; }
	}

    @Override
    public boolean onTouchEvent(MotionEvent me) {
		// Hack - dismiss the popup preview window manually on key down events so the animation doesn't play
		if (me.getAction() == 0) mSuperPopupPreview.dismiss();
		return super.onTouchEvent(me);
    }

	@Override
    public void onDraw(Canvas canvas) {
		// Copied onDraw from KeyboardView and made minor changes to support transparency. First we get the private fields which may have changed
		mSuperDrawPending = getSuperObject("mDrawPending", Boolean.class);
		mSuperBuffer = getSuperObject("mBuffer", Bitmap.class);
		mSuperKeyboardChanged = getSuperObject("mKeyboardChanged", Boolean.class);
		mSuperKeyboard = getSuperObject("mKeyboard", Keyboard.class);
		mSuperKeys = getSuperObject("mKeys", Key[].class);
		mSuperInvalidatedKey = getSuperObject("mInvalidatedKey", Key.class);

        if (mSuperDrawPending || mSuperBuffer == null || mSuperKeyboardChanged) {
            onBufferDraw();
        }
        canvas.drawBitmap(mSuperBuffer, 0, 0, null);
    }

    private void onBufferDraw() {
		// Force 100% opacity if overlap is disabled, otherwise take opacity from preferences
		int alpha = Preferences.getKeyboardOverlap() ? (int)(255 * (Preferences.getKeyboardOpacity() / 100f)) : 255;

		// Hack alpha
		getBackground().setAlpha(alpha);

        if (mSuperBuffer == null || mSuperKeyboardChanged) {
            if (mSuperBuffer == null || mSuperKeyboardChanged &&
                    (mSuperBuffer.getWidth() != getWidth() || mSuperBuffer.getHeight() != getHeight())) {
                // Make sure our bitmap is at least 1x1
                final int width = Math.max(1, getWidth());
                final int height = Math.max(1, getHeight());
				mSuperBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                setSuperField("mBuffer", mSuperBuffer);
                setSuperField("mCanvas", new Canvas(mSuperBuffer));
            }
            invalidateAllKeys();
			mSuperKeyboardChanged = false;
            setSuperField("mKeyboardChanged", mSuperKeyboardChanged);
        }
        final Canvas canvas = getSuperObject("mCanvas", Canvas.class);
        canvas.clipRect(mSuperDirtyRect, Op.REPLACE);
        if (mSuperKeyboard == null) return;

        final Rect clipRegion = new Rect(0, 0, 0, 0);
        final Rect padding = new Rect(0, 0, 0, 0);
        mSuperPaint.setColor(mSuperKeyTextColor);

		// Hack alpha
		mSuperPaint.setAlpha(alpha);

        boolean drawSingleKey = false;
        if (mSuperInvalidatedKey != null && canvas.getClipBounds(clipRegion)) {
          // Is clipRegion completely contained within the invalidated key?
          if (mSuperInvalidatedKey.x - 1 <= clipRegion.left &&
                  mSuperInvalidatedKey.y - 1 <= clipRegion.top &&
                  mSuperInvalidatedKey.x + mSuperInvalidatedKey.width + 1 >= clipRegion.right &&
                  mSuperInvalidatedKey.y + mSuperInvalidatedKey.height + 1 >= clipRegion.bottom) {
              drawSingleKey = true;
          }
        }
        canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);

        final int keyCount = mSuperKeys.length;
        for (int i = 0; i < keyCount; i++) {
            final Key key = mSuperKeys[i];
            if (drawSingleKey && mSuperInvalidatedKey != key) {
                continue;
            }
            int[] drawableState = key.getCurrentDrawableState();
            mSuperKeyBackground.setState(drawableState);
            // Switch the character to uppercase if shift is pressed
            String label = key.label == null? null : adjustCase(key.label).toString();
            final Rect bounds = mSuperKeyBackground.getBounds();
            if (key.width != bounds.right ||
                    key.height != bounds.bottom) {
                mSuperKeyBackground.setBounds(0, 0, key.width, key.height);
            }
            canvas.translate(key.x, key.y);

			// Hack alpha 
			mSuperKeyBackground.setAlpha(alpha);

            mSuperKeyBackground.draw(canvas);
            if (label != null) {
                // For characters, use large font. For labels like "Done", use small font.
                if (label.length() > 1 && key.codes.length < 2) {
                    mSuperPaint.setTextSize(mSuperLabelTextSize);
                    mSuperPaint.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    mSuperPaint.setTextSize(mSuperKeyTextSize);
                    mSuperPaint.setTypeface(Typeface.DEFAULT);
                }

                // Draw the text
                canvas.drawText(label,
                    (key.width - padding.left - padding.right) / 2
                            + padding.left,
                    (key.height - padding.top - padding.bottom) / 2
                            + (mSuperPaint.getTextSize() - mSuperPaint.descent()) / 2 + padding.top,
                    mSuperPaint);
            } else if (key.icon != null) {
                final int drawableX = (key.width - padding.left - padding.right
                                - key.icon.getIntrinsicWidth()) / 2 + padding.left;
                final int drawableY = (key.height - padding.top - padding.bottom
                        - key.icon.getIntrinsicHeight()) / 2 + padding.top;
                canvas.translate(drawableX, drawableY);
                key.icon.setBounds(0, 0,
                        key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());

				// Hack alpha
				key.icon.setAlpha(alpha);

                key.icon.draw(canvas);
                canvas.translate(-drawableX, -drawableY);
            }
            canvas.translate(-key.x, -key.y);
        }
        mSuperInvalidatedKey = null;
		mSuperDrawPending = false;
        setSuperField("mDrawPending", mSuperDrawPending);
        mSuperDirtyRect.setEmpty();
    }
	
    private CharSequence adjustCase(CharSequence label) {
        if (mSuperKeyboard.isShifted() && label != null && label.length() < 3
                && Character.isLowerCase(label.charAt(0))) {
            label = label.toString().toUpperCase();
        }
        return label;
    }

	@Override
	public void invalidateKey(int keyIndex) {
		mSuperKeys = getSuperObject("mKeys", Key[].class);

        if (mSuperKeys == null) return;
        if (keyIndex < 0 || keyIndex >= mSuperKeys.length) {
            return;
        }
        final Key key = mSuperKeys[keyIndex];
		mSuperInvalidatedKey = key;
        setSuperField("mInvalidatedKey", mSuperInvalidatedKey);
        mSuperDirtyRect.union(key.x, key.y,
                key.x + key.width, key.y + key.height );
        onBufferDraw();
        invalidate(key.x, key.y,
                key.x + key.width, key.y + key.height);
	}
}
