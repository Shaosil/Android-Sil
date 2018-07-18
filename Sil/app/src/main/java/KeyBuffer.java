package com.gmail.ShaosilDev.Sil;

import java.util.LinkedList;
import java.util.Queue;
import android.view.KeyEvent;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import com.gmail.ShaosilDev.Sil.KeyMapper.KeyAction;
	
public class KeyBuffer {

	/* keyboard state */
	private Queue<Integer> keybuffer = new LinkedList<Integer>();
	private Queue<Integer> keymacro = new LinkedList<Integer>();
	private boolean wait = false;
	private int quit_key_seq = 0;
	private NativeWrapper nativew = null;
	private StateManager state = null;

	private boolean ctrl_mod = false;
	private boolean shift_mod = false;
	private boolean alt_mod = false;
	private boolean shift_down = false;
	private boolean alt_down = false;
	private boolean ctrl_down = false;
	private boolean ctrl_key_pressed = false;
	private boolean ctrl_key_overload = false;
	private boolean shift_key_pressed = false;
	private boolean alt_key_pressed = false;
	private boolean eat_shift = false;

	public KeyBuffer(StateManager state) {
		this.state = state;
		nativew = state.nativew;
		clear();
		if (Preferences.getSkipWelcome()) {
			add(32); //space
		}
		quit_key_seq = 0;
	}

	public void add(int key) {
		//Log.d("Sil", "KebBuffer.add:"+key);
		synchronized (keybuffer) {
			ctrl_key_overload = false;

			if (key < 127) {
				if (key >= 'a' && key <= 'z') {
					if (ctrl_mod) {
						key = key - 'a' + 1;
						ctrl_mod = ctrl_down; // if held down, mod is still active
					}
					else if (shift_mod) {
						if (!eat_shift) key = key - 'a'  + 'A';
						shift_mod = shift_down; // if held down, mod is still active
					}
				}
			}

			eat_shift = false;

			alt_key_pressed = alt_down;
			ctrl_key_pressed = ctrl_down;
			shift_key_pressed = shift_down;

			keybuffer.offer(key);
			wakeUp();
		}
	}

	public void addDirection(int key) {
		boolean rogueLike = (nativew.gameQueryInt(1,new String[]{"rl"})==1);
		boolean alwaysRun = Preferences.getAlwaysRun();

		if (rogueLike) {
			switch(key) {
			case '1': key = 'b'; break;
			case '2': key = 'j'; break;
			case '3': key = 'n'; break;
			case '4': key = 'h'; break;
			//case '5': key = ' '; break; // now configurable below
			case '6': key = 'l'; break;
			case '7': key = 'y'; break;
			case '8': key = 'k'; break;
			case '9': key = 'u'; break;
			default: break;
			}
		}
		
		if (key == '5') { // center tap
			KeyAction act = Preferences.getKeyMapper().getCenterScreenTapAction();

			performActionKeyDown(act, 0, null);
			performActionKeyUp(act);
		}
		else { // directional tap
			if (alwaysRun && !ctrl_mod) { // let ctrl influence directionals, even with alwaysRun on
				if (shift_mod) {  // shift temporarily overrides always run
					eat_shift = true;
				}
				else if (rogueLike) {
					key = Character.toUpperCase(key);
				}
				else {
					add(46); // '.' command
				}
			}
		
			add(key);
		}
	}

	public void clear() {
		synchronized (keybuffer) {
			keybuffer.clear();
		}
	}

	public int get(int v) {
		int key = 0;
		
		synchronized (keybuffer) {

			if (keybuffer.peek() != null) {
				//peek before wait -- fix issue #3 keybuffer loss
				key = keybuffer.poll();
				//Log.w("Sil", "process key = " + key);
			}		
			else if (v == 1) {
				// running a macro?
				if (keymacro.peek() != null) {
					key = keymacro.poll();
				}
				else { // otherwise wait for key press
					try {
						//Log.d("Sil", "Wait keypress BEFORE");
						wait = true;
						//keybuffer.clear(); //not necessary
						keybuffer.wait();
						wait = false;
						//Log.d("Sil", "Wait keypress AFTER");
					} catch (Exception e) {
						Log.d("Sil", "The getch() wait exception" + e);
					}

					// return key after wait, if there is one
					if (keybuffer.peek() != null) {
						key = keybuffer.poll();
						//Log.w("Sil", "process key = " + key);
					}
				}		
			}
		}
		return key;
	}

	public void signalSave(boolean alsoQuit) {
		//Log.d("Sil", "signalSave");
		synchronized (keybuffer) {
			keybuffer.clear();
			if (alsoQuit)
				keybuffer.offer(-2);
			else
				keybuffer.offer(-1);
			wakeUp();
		}	
	}

	public void wakeUp() {
		synchronized (keybuffer) {
			if (wait) {
				keybuffer.notify();
			}
		}
	}

	private KeyMap getKeyMapFromKeyCode(int keyCode, KeyEvent event)
	{
		int meta=0, event_modifiers=0;
		if(alt_mod) {
			meta |= KeyEvent.META_ALT_ON;
			meta |= KeyEvent.META_ALT_LEFT_ON;
			if (event.getAction() == KeyEvent.ACTION_UP)
				alt_mod = alt_down; // if held down, mod is still active
		}
		int ch = 0;
		boolean char_mod = false;
		if (event != null) {
			//ch = event.getUnicodeChar(meta);
			if (android.os.Build.VERSION.SDK_INT >= 13) {
			    event_modifiers = event.getModifiers() & ~(KeyEvent.META_CTRL_ON|KeyEvent.META_CTRL_LEFT_ON|KeyEvent.META_CTRL_RIGHT_ON);
			}
			ch = event.getUnicodeChar(event_modifiers|meta);
			char_mod = (ch > 32 && ch < 127);
		}
		int key_code = char_mod ? ch : keyCode;

		String keyAssign = KeyMap.stringValue(key_code, alt_mod, char_mod);		
		//Log.d("Sil", "keyAssign="+keyAssign);
		KeyMap map = Preferences.getKeyMapper().findKeyMapByAssign(keyAssign);
		return map;
	}

	private boolean performActionKeyDown(KeyAction act, int character, KeyEvent event) {
		boolean res = true;

		if (act == KeyAction.CtrlKey) {
			if (event != null && event.getRepeatCount()>0) return true; // ignore repeat from modifiers
			ctrl_mod = !ctrl_mod;
			ctrl_key_pressed = !ctrl_mod; // double tap, turn off mod
			ctrl_down = true;
			if (ctrl_key_overload) {
				// ctrl double tap, translate into appropriate action
				act = Preferences.getKeyMapper().getCtrlDoubleTapAction();
			}
		}

   		switch(act){
		case CharacterKey:
			add(character);
			break;
		case EscKey:
			add(0xE000);
			break;
		case BackspaceKey:
			add(0x9F);
			break;
		case DeleteKey:
			add(0x9E);
			break;
		case Space:
			add(' ');
			break;
		case Wait:
			add('z');
			break;
		case Period:
			add('.');
			break;
		case EnterKey:
			add(0x0A);
			break;
		case ArrowDownKey:
			add(0x80);
			break;
		case ArrowUpKey:
			add(0x83);
			break;
		case ArrowLeftKey:
			add(0x81);
			break;
		case ArrowRightKey:
			add(0x82);
			break;
		case AltKey:
			if (event != null && event.getRepeatCount()>0) return true; // ignore repeat from modifiers
			alt_mod = !alt_mod;
			alt_key_pressed = !alt_mod; // double tap, turn off mod
			alt_down = true;
			break;
		case ShiftKey:
			if (event != null && event.getRepeatCount()>0) return true; // ignore repeat from modifiers
			shift_mod = !shift_mod;
			shift_key_pressed = !shift_mod; // double tap, turn off mod
			shift_down = true;
			break;
		case ZoomIn:
			nativew.increaseFontSize();
			break;
		case ZoomOut:
			nativew.decreaseFontSize();
			break;
		case CtrlKey:
			//handled above
			break;
		case VirtualKeyboard:
			// handled on keyup
			break;
		default:
			res = false; // let the OS handle the key
			break;
		}
		return res;
	}

	private boolean performActionKeyUp(KeyAction act) {
		boolean res = true; // handled the key

		switch(act){
		case AltKey:
			alt_down = false;		
			alt_mod = !alt_key_pressed; // turn off mod only if used at least once		
			break;
		case CtrlKey:
			ctrl_down = false;
			ctrl_mod = !ctrl_key_pressed; // turn off mod only if used at least once
			ctrl_key_overload = ctrl_mod;
			break;
		case ShiftKey:
			shift_down = false;
			shift_mod = !shift_key_pressed; // turn off mod only if used at least once
			break;
		case VirtualKeyboard:
			state.handler.sendEmptyMessage(AngbandDialog.Action.ToggleKeyboard.ordinal());
			break;

		// these are handled on keydown
		case ZoomIn:
		case ZoomOut:
		case None:
		case CharacterKey:
			break;

		default:
			res = false;  // let the OS handle the key
			break;
		}
		return res;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//Log.d("Sil", "onKeyDown("+keyCode+","+event+")");

		KeyMap map = getKeyMapFromKeyCode(keyCode, event);
		if (map == null)
			return false;
		else 
			return performActionKeyDown(map.getKeyAction(), map.getCharacter(), event);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		//Log.d("Sil", "onKeyUp("+keyCode+","+event+")");

		KeyMap map =  getKeyMapFromKeyCode(keyCode, event);
		if (map == null)
			return false;
		else 
			return performActionKeyUp(map.getKeyAction());
	}
}
