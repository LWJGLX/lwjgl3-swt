/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others. All rights reserved.
 * The contents of this file are made available under the terms
 * of the GNU Lesser General Public License (LGPL) Version 2.1 that
 * accompanies this distribution (lgpl-v21.txt).  The LGPL is also
 * available at http://www.gnu.org/licenses/lgpl.html.  If the version
 * of the LGPL at http://www.gnu.org is different to the version of
 * the LGPL accompanying this distribution and there is any conflict
 * between the two license versions, the terms of the LGPL accompanying
 * this distribution shall govern.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.gtk;


import org.eclipse.swt.internal.*;

// Common type translation table:
// C   ->  Java
// --------------------
// Primitives:
// int   -> int
// guint -> long   #Reason:
//					c : unsigned int range: 4294967295
//                  java : int range      : 2147483647 (less than c unsigned int)
//                  Java : long range: 9,223,372,036,854,775,807
//				    // Note: Not to be used for pointers.
//
// gint* -> int[]
// boolean   -> int  ex setenv
// gboolean  -> boolean
//
// Pointers: (the /*int*/ tells 32bit linux to use int instead of long.
// gpointer -> int /*long*/
// void *   -> int /*long*/    # C pointers (*) are normally int /*long*/
//
// Strings:
// gchar *      -> int /*long*/
// const char * -> byte[]  ex setenv
// const gchar* -> byte[]  ex g_log_remove_handler
//
// Special types:
// GQuark -> int
// GError ** -> int /*long*/[]  ex g_filename_to_uri


/**
 * This class contains native functions for various libraries.
 *
 * Any dynamic functions must be manually linked to their corresponding library. See os_cutom.h  #define FUNC_LIB_* LIB_*
 */
public class OS extends C {
	/** OS Constants */
	public static final boolean IsAIX, IsLinux, IsWin32, BIG_ENDIAN;
	static {

		/* Initialize the OS flags and locale constants */
		String osName = System.getProperty ("os.name");
		boolean isAIX = false, isLinux = false, isWin32 = false;
		if (osName.equals ("Linux")) isLinux = true;
		if (osName.equals ("AIX")) isAIX = true;
		if (osName.startsWith("Windows")) isWin32 = true;
		IsAIX = isAIX;  IsLinux = isLinux;  IsWin32 = isWin32;

		byte[] buffer = new byte[4];
		int /*long*/ ptr = C.malloc(4);
		C.memmove(ptr, new int[]{1}, 4);
		C.memmove(buffer, ptr, 1);
		C.free(ptr);
		BIG_ENDIAN = buffer[0] == 0;
	}

	/** Initialization; load native libraries */
	static {
		String propertyName = "SWT_GTK3";
		String gtk3 = getEnvironmentalVariable (propertyName);
		if (gtk3 != null && gtk3.equals("0")) {
			try {
				Library.loadLibrary("swt-pi");
			} catch (Throwable e) {
				Library.loadLibrary("swt-pi3");
			}
		} else {
			try {
				Library.loadLibrary("swt-pi3");
			} catch (Throwable e) {
				Library.loadLibrary("swt-pi");
			}
		}
		cachejvmptr();
	}

	//Add ability to debug gtk warnings for SWT snippets via SWT_FATAL_WARNINGS=1
	// env variable. Please see Eclipse bug 471477
	static {
		String propertyName = "SWT_FATAL_WARNINGS";
		String swt_fatal_warnings = getEnvironmentalVariable (propertyName);

		if (swt_fatal_warnings != null && swt_fatal_warnings.equals("1")) {
			OS.swt_debug_on_fatal_warnings ();
		}
	}

	// Bug 519124
	static {
		String swt_lib_versions = getEnvironmentalVariable (OS.SWT_LIB_VERSIONS); // Note, this is read in multiple places.
		if (swt_lib_versions != null && swt_lib_versions.equals("1")) {
			System.out.print("SWT_LIB_Gtk:"+GTK.gtk_major_version()+"."+GTK.gtk_minor_version()+"."+GTK.gtk_micro_version());
			System.out.print(" (Dynamic gdbus)");
			System.out.println("");
		}
	}

	public static final String SWT_LIB_VERSIONS = "SWT_LIB_VERSIONS";

	public static String getEnvironmentalVariable (String envVarName) {
		String envVarValue = null;
		int /*long*/ ptr = C.getenv(ascii(envVarName));
		if (ptr != 0) {
			int length = C.strlen(ptr);
			byte[] buffer = new byte[length];
			C.memmove(buffer, ptr, length);
			char[] convertedChar = new char[buffer.length];
			for (int i = 0; i < buffer.length; i++) {
				convertedChar[i]=(char)buffer[i];
			}
			envVarValue = new String(convertedChar);
		}
		return envVarValue;
	}

	/** Constants */
	public static final int G_FILE_TEST_IS_DIR = 1 << 2;
	public static final int G_FILE_TEST_IS_EXECUTABLE = 1 << 3;
	public static final int G_SIGNAL_MATCH_FUNC = 1 << 3;
	public static final int G_SIGNAL_MATCH_DATA = 1 << 4;
	public static final int G_SIGNAL_MATCH_ID = 1 << 0;
	public static final int G_LOG_FLAG_FATAL = 0x2;
	public static final int G_LOG_FLAG_RECURSION = 0x1;
	public static final int G_LOG_LEVEL_MASK = 0xfffffffc;
	public static final int G_APP_INFO_CREATE_NONE = 0;
	public static final int G_APP_INFO_CREATE_NEEDS_TERMINAL = (1 << 0);
	public static final int G_APP_INFO_CREATE_SUPPORTS_URIS  = (1 << 1);
	public static final int None = 0;
	public static final int PANGO_ALIGN_LEFT = 0;
	public static final int PANGO_ALIGN_CENTER = 1;
	public static final int PANGO_ALIGN_RIGHT = 2;
	public static final int PANGO_ATTR_FOREGROUND = 9;
	public static final int PANGO_ATTR_BACKGROUND = 10;
	public static final int PANGO_ATTR_UNDERLINE = 11;
	public static final int PANGO_ATTR_UNDERLINE_COLOR = 18;
	public static final int PANGO_DIRECTION_LTR = 0;
	public static final int PANGO_DIRECTION_RTL = 1;
	public static final int PANGO_SCALE = 1024;
	public static final int PANGO_STRETCH_ULTRA_CONDENSED = 0x0;
	public static final int PANGO_STRETCH_EXTRA_CONDENSED = 0x1;
	public static final int PANGO_STRETCH_CONDENSED = 0x2;
	public static final int PANGO_STRETCH_SEMI_CONDENSED = 0x3;
	public static final int PANGO_STRETCH_NORMAL = 0x4;
	public static final int PANGO_STRETCH_SEMI_EXPANDED = 0x5;
	public static final int PANGO_STRETCH_EXPANDED = 0x6;
	public static final int PANGO_STRETCH_EXTRA_EXPANDED = 0x7;
	public static final int PANGO_STRETCH_ULTRA_EXPANDED = 0x8;
	public static final int PANGO_STYLE_ITALIC = 0x2;
	public static final int PANGO_STYLE_NORMAL = 0x0;
	public static final int PANGO_STYLE_OBLIQUE = 0x1;
	public static final int PANGO_TAB_LEFT = 0;
	public static final int PANGO_UNDERLINE_NONE = 0;
	public static final int PANGO_UNDERLINE_SINGLE = 1;
	public static final int PANGO_UNDERLINE_DOUBLE = 2;
	public static final int PANGO_UNDERLINE_LOW = 3;
	public static final int PANGO_UNDERLINE_ERROR = 4;
	public static final int PANGO_VARIANT_NORMAL = 0;
	public static final int PANGO_VARIANT_SMALL_CAPS = 1;
	public static final int PANGO_WEIGHT_BOLD = 0x2bc;
	public static final int PANGO_WEIGHT_NORMAL = 0x190;
	public static final int PANGO_WRAP_WORD = 0;
	public static final int PANGO_WRAP_WORD_CHAR = 2;
	public static final int RTLD_GLOBAL = OS.IsWin32 ? 0 : OS.RTLD_GLOBAL();
	public static final int RTLD_LAZY = OS.IsWin32 ? 0 : OS.RTLD_LAZY();
	public static final int RTLD_MEMBER = 0x00040000;
	public static final int RTLD_NOW = OS.IsWin32 ? 0 : OS.RTLD_NOW();


	/**
	 * GDBus Session types.
	 * @category gdbus */
	public static final int G_BUS_TYPE_STARTER = -1; //An alias for the message bus that activated the process, if any.
	/** @category gdbus */
	public static final int G_BUS_TYPE_NONE = 0;    // Not a message bus.
	/** @category gdbus */
	public static final int G_BUS_TYPE_SYSTEM  = 1; // The system-wide message bus.
	/** @category gdbus */
	public static final int G_BUS_TYPE_SESSION = 2; //The login session message bus.
	/** @category gdbus */
	public static final int G_BUS_NAME_OWNER_FLAGS_NONE = 0; //No flags set.
	/** @category gdbus */
	public static final int G_BUS_NAME_OWNER_FLAGS_ALLOW_REPLACEMENT = (1<<0); //Allow another message bus connection to claim the name.
	/**
	 * If another message bus connection owns the name and have
	 * specified #G_BUS_NAME_OWNER_FLAGS_ALLOW_REPLACEMENT, then take the name from the other connection.
	 * @category gdbus */
	public static final int G_BUS_NAME_OWNER_FLAGS_REPLACE = (1<<1);

	// Proxy flags found here: https://developer.gnome.org/gio/stable/GDBusProxy.html#GDBusProxyFlags
	public static final int G_DBUS_PROXY_FLAGS_NONE = 0;
	public static final int G_DBUS_PROXY_FLAGS_DO_NOT_LOAD_PROPERTIES = 1;
	public static final int G_DBUS_PROXY_FLAGS_DO_NOT_CONNECT_SIGNALS = 2;
	public static final int G_DBUS_PROXY_FLAGS_DO_NOT_AUTO_START = 3;
	public static final int G_DBUS_PROXY_FLAGS_GET_INVALIDATED_PROPERTIES = 4;
	public static final int G_DBUS_PROXY_FLAGS_DO_NOT_AUTO_START_AT_CONSTRUCTION = 5;

	public static final int G_DBUS_CALL_FLAGS_NONE = 0;
	public static final int G_DBUS_CALL_FLAGS_NO_AUTO_START = (1<<0);

	/**
	 * DBus Data types as defined by:
	 * https://dbus.freedesktop.org/doc/dbus-specification.html#idm423
	 * If using these, make sure they're properly handled in all GDBus code. Only some of these are supported by some GDBus classes.
	 * @category gdbus */
	public static final String DBUS_TYPE_BYTE = "y"; // 8 bit, unsigned int.
	/** @category gdbus */
	public static final String DBUS_TYPE_BOOLEAN = "b";
	/** @category gdbus */
	public static final String DBUS_TYPE_ARRAY = "a";
	/** @category gdbus */
	public static final String DBUS_TYPE_STRING = "s";
	/** @category gdbus */
	public static final String DBUS_TYPE_STRING_ARRAY = "as";
	/** @category gdbus */
	public static final String DBUS_TYPE_STRUCT_ARRAY_BROWSER_FUNCS = "a(tss)";
	/** @category gdbus */
	public static final String DBUS_TYPE_INT32 = "i";
	/** @category gdbus */
	public static final String DBUS_TYPE_UINT64 = "t";
	/** @category gdbus */
	public static final String DBUS_TYPE_DOUBLE = "d";
	/** @category gdbus */
	public static final String DBUS_TYPE_STRUCT = "r"; // Not used by Dbus, but implemented by GDBus.
	/** @category gdbus */
	public static final String DBUS_TYPE_SINGLE_COMPLETE = "*";

	/**
	 * GVariant Types
	 * These are for the most part quite similar to DBus types with a few differences. Read:
	 * https://developer.gnome.org/glib/stable/glib-GVariantType.html
	 *
	 * @category gdbus
	 */
	public static final byte[] G_VARIANT_TYPE_BYTE = ascii(DBUS_TYPE_BYTE);
	/** @category gdbus */
	public static final byte[] G_VARIANT_TYPE_BOOLEAN = ascii(DBUS_TYPE_BOOLEAN);
	/** @category gdbus */
	public static final byte[] G_VARIANT_TYPE_STRING_ARRAY = ascii(DBUS_TYPE_STRING_ARRAY);
	/** @category gdbus */
	public static final byte[] G_VARIANT_TYPE_STRING = ascii(DBUS_TYPE_STRING);
	/** @category gdbus */
	public static final byte[] G_VARIANT_TYPE_IN32 = ascii(DBUS_TYPE_INT32);
	/** @category gdbus */
	public static final byte[] G_VARIANT_TYPE_UINT64 = ascii(DBUS_TYPE_UINT64);
	/** @category gdbus */
	public static final byte[] G_VARIANT_TYPE_DOUBLE = ascii(DBUS_TYPE_DOUBLE);
	/** @category gdbus */
	public static final byte[] G_VARIANT_TYPE_TUPLE = ascii(DBUS_TYPE_STRUCT);
	/** @category gdbus */
	public static final byte[] G_VARIANT_TYPE_ARRAY_BROWSER_FUNCS = ascii(DBUS_TYPE_STRUCT_ARRAY_BROWSER_FUNCS);


	/** Signals */
	public static final byte[] accel_closures_changed = ascii("accel-closures-changed");		// Gtk2,3,4
	public static final byte[] activate = ascii("activate");	// ?
	public static final byte[] angle_changed = ascii("angle_changed");	// Gtk3/4, Guesture related.
	public static final byte[] backspace = ascii("backspace");
	public static final byte[] begin = ascii("begin");
	public static final byte[] button_press_event = ascii("button-press-event");
	public static final byte[] button_release_event = ascii("button-release-event");
	public static final byte[] changed = ascii("changed");
	public static final byte[] change_current_page = ascii("change-current-page");
	public static final byte[] change_value = ascii("change-value");
	public static final byte[] clicked = ascii("clicked");
	public static final byte[] commit = ascii("commit");
	public static final byte[] configure_event = ascii("configure-event");
	public static final byte[] copy_clipboard = ascii("copy-clipboard");
	public static final byte[] cut_clipboard = ascii("cut-clipboard");
	public static final byte[] create_menu_proxy = ascii("create-menu-proxy");
	public static final byte[] delete_event = ascii("delete-event");
	public static final byte[] delete_from_cursor = ascii("delete-from-cursor");
	public static final byte[] day_selected = ascii("day-selected");
	public static final byte[] day_selected_double_click = ascii("day-selected-double-click");
	public static final byte[] delete_range = ascii("delete-range");
	public static final byte[] delete_text = ascii("delete-text");
	public static final byte[] direction_changed = ascii("direction-changed");
	public static final byte[] drag_begin = ascii("drag_begin");
	public static final byte[] drag_data_delete = ascii("drag_data_delete");
	public static final byte[] drag_data_get = ascii("drag_data_get");
	public static final byte[] drag_data_received = ascii("drag_data_received");
	public static final byte[] drag_drop = ascii("drag_drop");
	public static final byte[] drag_end = ascii("drag_end");
	public static final byte[] drag_leave = ascii("drag_leave");
	public static final byte[] drag_motion = ascii("drag_motion");
	public static final byte[] draw = ascii("draw");
	public static final byte[] end = ascii("end");
	public static final byte[] enter_notify_event = ascii("enter-notify-event");
	public static final byte[] event = ascii("event");
	public static final byte[] event_after = ascii("event-after");
	public static final byte[] expand_collapse_cursor_row = ascii("expand-collapse-cursor-row");
	public static final byte[] expose_event = ascii("expose-event");
	public static final byte[] focus = ascii("focus");
	public static final byte[] focus_in_event = ascii("focus-in-event");
	public static final byte[] focus_out_event = ascii("focus-out-event");
	public static final byte[] grab_focus = ascii("grab-focus");
	public static final byte[] hide = ascii("hide");
	public static final byte[] icon_release = ascii("icon-release");
	public static final byte[] input = ascii("input");
	public static final byte[] insert_text = ascii("insert-text");
	public static final byte[] key_press_event = ascii("key-press-event");
	public static final byte[] key_release_event = ascii("key-release-event");
	public static final byte[] keys_changed = ascii("keys-changed");
	public static final byte[] leave_notify_event = ascii("leave-notify-event");
	public static final byte[] link_color = ascii("link-color");
	public static final byte[] map = ascii("map");
	public static final byte[] map_event = ascii("map-event");
	public static final byte[] mnemonic_activate = ascii("mnemonic-activate");
	public static final byte[] month_changed = ascii("month-changed");
	public static final byte[] motion_notify_event = ascii("motion-notify-event");
	public static final byte[] move_cursor = ascii("move-cursor");
	public static final byte[] move_focus = ascii("move-focus");
	public static final byte[] output = ascii("output");
	public static final byte[] paste_clipboard = ascii("paste-clipboard");
	public static final byte[] popped_up = ascii("popped-up");
	public static final byte[] popup_menu = ascii("popup-menu");
	public static final byte[] populate_popup = ascii("populate-popup");
	public static final byte[] preedit_changed = ascii("preedit-changed");
	public static final byte[] property_notify_event = ascii("property-notify-event");
	public static final byte[] realize = ascii("realize");
	public static final byte[] row_activated = ascii("row-activated");
	public static final byte[] row_changed = ascii("row-changed");
	public static final byte[] row_has_child_toggled = ascii("row-has-child-toggled");
	public static final byte[] row_inserted = ascii("row-inserted");
	public static final byte[] row_deleted = ascii("row-deleted");
	public static final byte[] scale_changed = ascii("scale-changed");
	public static final byte[] scroll_child = ascii("scroll-child");
	public static final byte[] scroll_event = ascii("scroll-event");
	public static final byte[] select = ascii("select");
	public static final byte[] selection_done = ascii("selection-done");
	public static final byte[] show = ascii("show");
	public static final byte[] show_help = ascii("show-help");
	public static final byte[] size_allocate = ascii("size-allocate");
	public static final byte[] size_request = ascii("size-request");
	public static final byte[] start_interactive_search = ascii("start-interactive-search");
	public static final byte[] style_set = ascii("style-set");
	public static final byte[] swipe = ascii("swipe");
	public static final byte[] switch_page = ascii("switch-page");
	public static final byte[] test_collapse_row = ascii("test-collapse-row");
	public static final byte[] test_expand_row = ascii("test-expand-row");
	public static final byte[] toggled = ascii("toggled");
	public static final byte[] unmap = ascii("unmap");
	public static final byte[] unmap_event = ascii("unmap-event");
	public static final byte[] unrealize = ascii("unrealize");
	public static final byte[] value_changed = ascii("value-changed");
	public static final byte[] window_state_event = ascii("window-state-event");

	/** Properties */
	public static final byte[] active = ascii("active");
	public static final byte[] background_gdk = ascii("background-gdk");
	public static final byte[] background_rgba = ascii("background-rgba");
	public static final byte[] button_relief = ascii("button-relief");
	public static final byte[] cell_background_gdk = ascii("cell-background-gdk");
	public static final byte[] cell_background_rgba = ascii("cell-background-rgba");
	public static final byte[] default_border = ascii("default-border");
	public static final byte[] expander_size = ascii("expander-size");
	public static final byte[] fixed_height_mode = ascii("fixed-height-mode");
	public static final byte[] focus_line_width = ascii("focus-line-width");
	public static final byte[] focus_padding = ascii("focus-padding");
	public static final byte[] font_desc = ascii("font-desc");
	public static final byte[] foreground_gdk = ascii("foreground-gdk");
	public static final byte[] foreground_rgba = ascii("foreground-rgba");
	public static final byte[] grid_line_width = ascii("grid-line-width");
	public static final byte[] inner_border = ascii("inner-border");
	public static final byte[] has_backward_stepper = ascii("has-backward-stepper");
	public static final byte[] has_secondary_backward_stepper = ascii("has-secondary-backward-stepper");
	public static final byte[] has_forward_stepper = ascii("has-forward-stepper");
	public static final byte[] has_secondary_forward_stepper = ascii("has-secondary-forward-stepper");
	public static final byte[] horizontal_separator = ascii("horizontal-separator");
	public static final byte[] inconsistent = ascii("inconsistent");
	public static final byte[] indicator_size = ascii("indicator-size");
	public static final byte[] indicator_spacing = ascii("indicator-spacing");
	public static final byte[] initial_gap = ascii("initial-gap");
	public static final byte[] interior_focus = ascii("interior-focus");
	public static final byte[] mode = ascii("mode");
	public static final byte[] model = ascii("model");
	public static final byte[] spacing = ascii("spacing");
	public static final byte[] pixbuf = ascii("pixbuf");
	public static final byte[] gicon = ascii("gicon");
	public static final byte[] text = ascii("text");
	public static final byte[] xalign = ascii("xalign");
	public static final byte[] ypad = ascii("ypad");
	public static final byte[] margin_bottom = ascii("margin-bottom");
	public static final byte[] margin_top = ascii("margin-top");
	public static final byte[] scrollbar_spacing = ascii("scrollbar-spacing");


	/** CUSTOM_CODE START
	 *
	 * Functions for which code is not generated automatically.
	 * Don't move to different class or update these unless you also manually update the custom code part as well.
	 * These functions are usually hand-coded in os_custom.c.
	 *
	 * Typical method to generate them is as following:
	 * 1) Move native call and don't auto-generate bindings.
	 * - define function as regular function. SWT Tools should generate wrappers in os.c
	 * - move wrappers from os.c into os_custom.c and make your adaptations/changes.
	 * - add the 'flags=no_gen' to the method in OS.java
	 *  (e.g, 'flags=no_gen' functions)
	 *
	 * 2) Make native call invoke a custom function.
	 * - create a function in os_custom.c
	 * - create a function in OS.java that will call your function.
	 * (e.g, see the 'swt_*' functions).
	 *
	 * Approach 2 is more portable than approach 1.
	 * (e.g '2' functions can be moved around, where as with '1', the c counter-parts have to be updated manually.)
	 *
	 * '@category custom' is for annotation/visibility in outline.
	 * '@flags=no_gen' is an instruction for SWT tools not to generate code.
	 */
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native boolean GDK_WINDOWING_X11();
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native boolean GDK_WINDOWING_WAYLAND();
	/**
	 * Can't be auto-generated because of mapping guint to long for keyval
	 * @method flags=no_gen
	 * @category custom
	 */
	public static final native boolean _gdk_keymap_translate_keyboard_state (int /*long*/ keymap, int hardware_keycode, int state, int group, long[] keyval, int[] effective_group, int[] level,  int[] consumed_modifiers);
	public static final boolean gdk_keymap_translate_keyboard_state (int /*long*/ keymap, int hardware_keycode, int state, int group, long[] keyval, int[] effective_group, int[] level,  int[] consumed_modifiers) {
		lock.lock();
		try {
			return _gdk_keymap_translate_keyboard_state(keymap, hardware_keycode, state, group, keyval, effective_group, level, consumed_modifiers);
		} finally {
			lock.unlock();
		}
	}
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native void _call_get_size (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ arg4, int /*long*/ arg5, int /*long*/ arg6);
	public static final void call_get_size (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ arg4, int /*long*/ arg5, int /*long*/ arg6) {
		// See also related call_* functions.
		lock.lock();
		try {
			 _call_get_size(function, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
		} finally {
			lock.unlock();
		}
	}

	/** Custom callbacks */
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native int /*long*/ pangoLayoutNewProc_CALLBACK(int /*long*/ func);
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native int /*long*/ pangoFontFamilyNewProc_CALLBACK(int /*long*/ func);
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native int /*long*/ pangoFontFaceNewProc_CALLBACK(int /*long*/ func);
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native int /*long*/ printerOptionWidgetNewProc_CALLBACK(int /*long*/ func);
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native int /*long*/ imContextNewProc_CALLBACK(int /*long*/ func);
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native int /*long*/ imContextLast();
	/** @method flags=no_gen
	 * @category custom
	 */
	public static final native void _cachejvmptr ();
	/** Cache the JVM pointer so that it's usable in other implementations. */
	public static final void cachejvmptr() {
		// See bug 521487.
		lock.lock();
		try {
			_cachejvmptr();
		} finally {
			lock.unlock();
		}
	}

	/** @category custom */
	public static final native void _swt_debug_on_fatal_warnings();
	/** Add ability to debug gtk warnings for SWT snippets via SWT_FATAL_WARNINGS=1
	 * env variable. Please see Eclipse bug 471477 */
	public static final void swt_debug_on_fatal_warnings() {
		lock.lock();
		try {
			_swt_debug_on_fatal_warnings ();
		} finally {
			lock.unlock();
		}
	}

	/** @category custom */
	public static final native int /*long*/ _swt_fixed_get_type();
	public static final int /*long*/ swt_fixed_get_type() {
		lock.lock();
		try {
			return _swt_fixed_get_type();
		} finally {
			lock.unlock();
		}
	}

	/** @category custom */
	public static final native int /*long*/ _swt_fixed_accessible_get_type();
	public static final int /*long*/ swt_fixed_accessible_get_type() {
		lock.lock();
		try {
			return _swt_fixed_accessible_get_type();
		} finally {
			lock.unlock();
		}
	}
	/**
	 * @param obj cast=(AtkObject*)
	 * @param is_native cast=(gboolean)
	 * @param to_map cast=(GtkWidget *)
	 * @category custom
	 */
	public static final native void _swt_fixed_accessible_register_accessible(int /*long*/ obj, boolean is_native, int /*long*/ to_map);
	public static final void swt_fixed_accessible_register_accessible(int /*long*/ obj, boolean is_native, int /*long*/ to_map) {
		lock.lock();
		try {
			_swt_fixed_accessible_register_accessible(obj, is_native, to_map);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * @param fixed cast=(SwtFixed*)
	 * @param widget cast=(GtkWidget*)
	 * @param sibling cast=(GtkWidget*)
	 * @category custom
	 */
	public static final native void _swt_fixed_restack(int /*long*/ fixed, int /*long*/ widget, int /*long*/ sibling, boolean above);
	public static final void swt_fixed_restack(int /*long*/ fixed, int /*long*/ widget, int /*long*/ sibling, boolean above) {
		lock.lock();
		try {
			_swt_fixed_restack(fixed, widget, sibling, above);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * @param fixed cast=(SwtFixed*)
	 * @param widget cast=(GtkWidget*)
	 * @category custom
	 */
	public static final native void _swt_fixed_move(int /*long*/ fixed, int /*long*/ widget, int x, int y);
	public static final void swt_fixed_move(int /*long*/ fixed, int /*long*/ widget, int x, int y) {
		lock.lock();
		try {
			_swt_fixed_move(fixed, widget, x, y);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * @param fixed cast=(SwtFixed*)
	 * @param widget cast=(GtkWidget*)
	 * @category custom
	 */
	public static final native void _swt_fixed_resize(int /*long*/ fixed, int /*long*/ widget, int width, int height);
	public static final void swt_fixed_resize(int /*long*/ fixed, int /*long*/ widget, int width, int height) {
		lock.lock();
		try {
			_swt_fixed_resize(fixed, widget, width, height);
		} finally {
			lock.unlock();
		}
	}

	/** @param str cast=(const gchar *)
	 * @category custom
	 */
	public static final native int /*long*/ _g_utf16_offset_to_pointer(int /*long*/ str, int /*long*/ offset);
	/** Custom version of g_utf8_pointer_to_offset */
	public static final int /*long*/ g_utf16_offset_to_pointer(int /*long*/ str, int /*long*/ offset) {
		lock.lock();
		try {
			return _g_utf16_offset_to_pointer(str, offset);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @param str cast=(const gchar *)
	 * @param pos cast=(const gchar *)
	 * @category custom
	 */
	public static final native int /*long*/ _g_utf16_pointer_to_offset(int /*long*/ str, int /*long*/ pos);
	/** Custom version of g_utf8_pointer_to_offset */
	public static final int /*long*/ g_utf16_pointer_to_offset(int /*long*/ str, int /*long*/ pos) {
		lock.lock();
		try {
			return _g_utf16_pointer_to_offset(str, pos);
		} finally {
			lock.unlock();
		}
	}
	/** @param str cast=(const gchar *)
	 * @category custom
	 */
	public static final native int /*long*/ _g_utf16_strlen(int /*long*/ str, int /*long*/ max);
	/** custom version of g_utf8 for 16 bit */
	public static final int /*long*/ g_utf16_strlen(int /*long*/ str, int /*long*/ max) {
		lock.lock();
		try {
			return _g_utf16_strlen(str, max);
		} finally {
			lock.unlock();
		}
	}
	/** @param str cast=(const gchar *)
	 * @category custom
	 */
	public static final native int /*long*/ _g_utf8_offset_to_utf16_offset(int /*long*/ str, int /*long*/ offset);
	/** custom version of g_utf8 for 16 bit */
	public static final int /*long*/ g_utf8_offset_to_utf16_offset(int /*long*/ str, int /*long*/ offset) {
		lock.lock();
		try {
			return _g_utf8_offset_to_utf16_offset(str, offset);
		} finally {
			lock.unlock();
		}
	}
	/** @param str cast=(const gchar *)
	 * @category custom
	 */
	public static final native int /*long*/ _g_utf16_offset_to_utf8_offset(int /*long*/ str, int /*long*/ offset);
	/** custom version of g_utf8 for 16 bit */
	public static final int /*long*/ g_utf16_offset_to_utf8_offset(int /*long*/ str, int /*long*/ offset) {
		lock.lock();
		try {
			return _g_utf16_offset_to_utf8_offset(str, offset);
		} finally {
			lock.unlock();
		}
	}

	/** CUSTOM_CODE END */






	/**
	 * Gtk has a minimum glib version. (But it's not a 1:1 link, one can have a newer version of glib and older gtk).
	 *
	 * Minimum Glib version requirement of gtk (for gtk2/gtk3) can be found in gtk's 'configure.ac' file, see line 'm4_define([glib_required_version],[2.*.*]).
	 *
	 * For reference:
	 * Gtk2.24 has min version of glib 2.28
	 * Gtk3.0  has min version of glib 2.28
	 * Gtk3.2  has min version of glib 2.29.14
	 * Gtk3.4  has min version of glib 2.32
	 * Gtk3.6  has min version of glib 2.33.1
	 * Gtk3.8  has min version of glib 2.35.3
	 * Gtk3.10 has min version of glib 2.37.5
	 * Gtk3.12 has min version of glib 2.39.5
	 * Gtk3.14 has min version of glib 2.41.2
	 * Gtk3.16 has min version of glib 2.43.4
	 * Gtk3.18 has min version of glib 2.45.8
	 * Gtk3.20 has min version of glib 2.45.8
	 * Gtk3.22 has min version of glib 2.49.4
	 */
	public static final int GLIB_VERSION = VERSION(glib_major_version(), glib_minor_version(), glib_micro_version());
	private static final boolean MIN_GLIB_2_32 = OS.GLIB_VERSION >= VERSION(2, 32, 0);

	/*
	 * New API in GTK3.22 introduced the "popped-up" signal, which provides
	 * information about where a menu was actually positioned after it's been
	 * popped up. Users can set the environment variable SWT_MENU_LOCATION_DEBUGGING
	 * to 1 in order to help them debug menu positioning issues on GTK3.22+.
	 *
	 * For more information see bug 530204.
	 */
	public static final boolean SWT_MENU_LOCATION_DEBUGGING;

	/*
	 * Check for the GTK_THEME environment variable. If set, parse
	 * it to get the theme name and check if a dark variant is specified.
	 * We can make use of this information when loading SWT system colors.
	 * See bug 534007.
	 */
	/**
	 * True if the GTK_THEME environment variable is specified
	 * and is non-empty.
	 */
	public static final boolean GTK_THEME_SET;
	/**
	 * A string containing the theme name supplied via the GTK_THEME
	 * environment variable. Otherwise this will contain an empty string.
	 */
	public static final String GTK_THEME_NAME;
	/**
	 * True if GTK_THEME_SET is true, and if the dark variant was
	 * specified via the GTK_THEME environment variable.
	 */
	public static final boolean GTK_THEME_DARK;

	/* Feature in Gtk: with the switch to GtkMenuItems from GtkImageMenuItems
	* in Gtk3 came a small Gtk shortfall: a small amount of padding on the left hand
	* side of MenuItems was added. This padding is not accessible to the developer,
	* causing vertical alignment issues in menus that have both image and text only
	* MenuItems. As an option, the user can specify the SWT_PADDED_MENU_ITEMS environment
	* variable, which (when enabled), double pads MenuItems so as to create consistent
	* vertical alignment throughout that particular menu.
	*
	* For more information see:
	* Bug 470298
	*/
	public static final boolean SWT_PADDED_MENU_ITEMS;
	static {
		String paddedProperty = "SWT_PADDED_MENU_ITEMS";
		String paddedCheck = getEnvironmentalVariable(paddedProperty);
		boolean usePadded = false;
		if (paddedCheck != null && paddedCheck.equals("1")) {
			usePadded = true;
		}
		SWT_PADDED_MENU_ITEMS = usePadded;

		String menuLocationProperty = "SWT_MENU_LOCATION_DEBUGGING";
		String menuLocationCheck = getEnvironmentalVariable(menuLocationProperty);
		boolean menuLocationDebuggingEnabled = false;
		if (menuLocationCheck != null && menuLocationCheck.equals("1")) {
			menuLocationDebuggingEnabled = true;
		}
		SWT_MENU_LOCATION_DEBUGGING = menuLocationDebuggingEnabled;

		String gtkThemeProperty = "GTK_THEME";
		String gtkThemeCheck = getEnvironmentalVariable(gtkThemeProperty);
		boolean gtkThemeSet = false;
		String gtkThemeName = "";
		boolean gtkThemeDark = false;
		if (gtkThemeCheck != null && !gtkThemeCheck.isEmpty()) {
			gtkThemeSet = true;
			gtkThemeDark = gtkThemeCheck.contains(":dark") ? true : false;
			String [] themeNameSplit = gtkThemeCheck.split(":");
			gtkThemeName = themeNameSplit[0];
		}
		GTK_THEME_SET = gtkThemeSet;
		GTK_THEME_NAME = gtkThemeName;
		GTK_THEME_DARK = gtkThemeDark;

		System.setProperty("org.eclipse.swt.internal.gtk.version",
				(GTK.GTK_VERSION >>> 16) + "." + (GTK.GTK_VERSION >>> 8 & 0xFF) + "." + (GTK.GTK_VERSION & 0xFF));
		// set GDK backend if we are on X11
		if (isX11()) {
			System.setProperty("org.eclipse.swt.internal.gdk.backend", "x11");
		}
	}

protected static byte [] ascii (String name) {
	int length = name.length ();
	char [] chars = new char [length];
	name.getChars (0, length, chars, 0);
	byte [] buffer = new byte [length + 1];
	for (int i=0; i<length; i++) {
		buffer [i] = (byte) chars [i];
	}
	return buffer;
}

public static int VERSION(int major, int minor, int micro) {
	return (major << 16) + (minor << 8) + micro;
}

public static boolean isX11 () {
	return OS.GDK_WINDOWING_X11() && GDK.GDK_IS_X11_DISPLAY(GDK.gdk_display_get_default());
}














/** 64 bit */
public static final native int GInterfaceInfo_sizeof ();
public static final native int GPollFD_sizeof ();
public static final native int GTypeInfo_sizeof ();
public static final native int GTypeQuery_sizeof ();
public static final native int PangoAttribute_sizeof();
public static final native int PangoAttrColor_sizeof();
public static final native int PangoAttrInt_sizeof();
public static final native int PangoItem_sizeof();
public static final native int PangoLayoutLine_sizeof();
public static final native int PangoLayoutRun_sizeof();
public static final native int PangoLogAttr_sizeof();
public static final native int PangoRectangle_sizeof();
public static final native int XAnyEvent_sizeof();
public static final native int XEvent_sizeof();
public static final native int XExposeEvent_sizeof();
public static final native int XFocusChangeEvent_sizeof();
public static final native int /*long*/ localeconv_decimal_point();
/**
 * @param path cast=(const char *)
 * @param realPath cast=(char *)
 */
public static final native int /*long*/ realpath(byte[] path, byte[] realPath);


/** Object private fields accessors */
/** @param object_class cast=(GObjectClass *) */
public static final native int /*long*/ G_OBJECT_CLASS_CONSTRUCTOR(int /*long*/ object_class);
/**
 * @param object_class cast=(GObjectClass *)
 * @paramOFF constructor cast=(GObject* (*) (GType, guint, GObjectConstructParam *))
 */
public static final native void G_OBJECT_CLASS_SET_CONSTRUCTOR(int /*long*/ object_class, int /*long*/ constructor);
/** @param xevent cast=(XEvent *) */
public static final native int X_EVENT_TYPE(int /*long*/ xevent);
/** @param xevent cast=(XAnyEvent *) */
public static final native int /*long*/ X_EVENT_WINDOW(int /*long*/ xevent);

/** X11 Native methods and constants */
public static final int Above = 0;
public static final int Below = 1;
public static final int ButtonRelease = 5;
public static final int CurrentTime = 0;
public static final int CWSibling = 0x20;
public static final int CWStackMode = 0x40;
public static final int EnterNotify = 7;
public static final int Expose = 12;
public static final int FocusChangeMask = 1 << 21;
public static final int FocusIn = 9;
public static final int FocusOut = 10;
public static final int GraphicsExpose = 13;
public static final int NoExpose = 14;
public static final int ExposureMask = 1 << 15;
public static final int /*long*/ NoEventMask = 0;
public static final int NotifyNormal = 0;
public static final int NotifyGrab = 1;
public static final int NotifyHint = 1;
public static final int NotifyUngrab = 2;
public static final int NotifyWhileGrabbed = 3;
public static final int NotifyAncestor = 0;
public static final int NotifyVirtual = 1;
public static final int NotifyNonlinear = 3;
public static final int NotifyNonlinearVirtual = 4;
public static final int NotifyPointer = 5;
public static final int RevertToParent = 2;
public static final native int _Call(int /*long*/ proc, int /*long*/ arg1, int /*long*/ arg2);
public static final int Call(int /*long*/ proc, int /*long*/ arg1, int /*long*/ arg2) {
	lock.lock();
	try {
		return _Call(proc, arg1, arg2);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ arg4, int /*long*/ arg5, int /*long*/ arg6);
public static final int /*long*/ call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ arg4, int /*long*/ arg5, int /*long*/ arg6) {
	lock.lock();
	try {
		return _call(function, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3);
public static final int /*long*/ call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3) {
	lock.lock();
	try {
		return _call(function, arg0, arg1, arg2, arg3);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ arg4, int /*long*/ arg5);
public static final int /*long*/ call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ arg4, int /*long*/ arg5) {
	lock.lock();
	try {
		return _call(function, arg0, arg1, arg2, arg3, arg4, arg5);
	} finally {
		lock.unlock();
	}
}
/**
 * @param display cast=(Display *)
 * @param event_return cast=(XEvent *)
 * @param predicate cast=(Bool (*)())
 * @param arg cast=(XPointer)
 */
public static final native boolean _XCheckIfEvent(int /*long*/ display, int /*long*/ event_return, int /*long*/ predicate, int /*long*/ arg);
public static final boolean XCheckIfEvent(int /*long*/ display, int /*long*/ event_return, int /*long*/ predicate, int /*long*/ arg) {
	lock.lock();
	try {
		return _XCheckIfEvent(display, event_return, predicate, arg);
	} finally {
		lock.unlock();
	}
}
/** @param display cast=(Display *) */
public static final native int _XDefaultScreen(int /*long*/ display);
public static final int XDefaultScreen(int /*long*/ display) {
	lock.lock();
	try {
		return _XDefaultScreen(display);
	} finally {
		lock.unlock();
	}
}
/** @param display cast=(Display *) */
public static final native int /*long*/ _XDefaultRootWindow(int /*long*/ display);
public static final int /*long*/ XDefaultRootWindow(int /*long*/ display) {
	lock.lock();
	try {
		return _XDefaultRootWindow(display);
	} finally {
		lock.unlock();
	}
}
/** @param address cast=(void *) */
public static final native void _XFree(int /*long*/ address);
public static final void XFree(int /*long*/ address) {
	lock.lock();
	try {
		_XFree(address);
	} finally {
		lock.unlock();
	}
}

/**
 * @param display cast=(Display *)
 * @param w cast=(Window)
 * @param root_return cast=(Window *)
 * @param child_return cast=(Window *)
 * @param root_x_return cast=(int *)
 * @param root_y_return cast=(int *)
 * @param win_x_return cast=(int *)
 * @param win_y_return cast=(int *)
 * @param mask_return cast=(unsigned int *)
 */
public static final native int _XQueryPointer(int /*long*/ display, int /*long*/ w, int /*long*/[] root_return, int /*long*/[] child_return, int[] root_x_return, int[] root_y_return, int[] win_x_return, int[] win_y_return, int[] mask_return);
public static final int XQueryPointer(int /*long*/ display, int /*long*/ w, int /*long*/[] root_return, int /*long*/[] child_return, int[] root_x_return, int[] root_y_return, int[] win_x_return, int[] win_y_return, int[] mask_return) {
	lock.lock();
	try {
		return _XQueryPointer(display, w, root_return, child_return, root_x_return, root_y_return, win_x_return, win_y_return, mask_return);
	} finally {
		lock.unlock();
	}
}
/** @param handler cast=(XIOErrorHandler) */
public static final native int /*long*/ _XSetIOErrorHandler(int /*long*/ handler);
public static final int /*long*/ XSetIOErrorHandler(int /*long*/ handler) {
	lock.lock();
	try {
		return _XSetIOErrorHandler(handler);
	} finally {
		lock.unlock();
	}
}
/** @param handler cast=(XErrorHandler) */
public static final native int /*long*/ _XSetErrorHandler(int /*long*/ handler);
public static final int /*long*/ XSetErrorHandler(int /*long*/ handler) {
	lock.lock();
	try {
		return _XSetErrorHandler(handler);
	} finally {
		lock.unlock();
	}
}
/**
 * @param display cast=(Display *)
 * @param window cast=(Window)
 */
public static final native int _XSetInputFocus(int /*long*/ display, int /*long*/ window, int revert, int time);
public static final int XSetInputFocus(int /*long*/ display, int /*long*/ window, int revert, int time) {
	lock.lock();
	try {
		return _XSetInputFocus(display, window, revert, time);
	} finally {
		lock.unlock();
	}
}
/**
 * @param display cast=(Display *)
 * @param w cast=(Window)
 * @param prop_window cast=(Window)
 */
public static final native int _XSetTransientForHint(int /*long*/ display, int /*long*/ w, int /*long*/ prop_window);
public static final int XSetTransientForHint(int /*long*/ display, int /*long*/ w, int /*long*/ prop_window) {
	lock.lock();
	try {
		return _XSetTransientForHint(display, w, prop_window);
	} finally {
		lock.unlock();
	}
}
/** @param display cast=(Display *) */
public static final native int /*long*/ _XSynchronize(int /*long*/ display, boolean onoff);
public static final int /*long*/ XSynchronize(int /*long*/ display, boolean onoff) {
	lock.lock();
	try {
		return _XSynchronize(display, onoff);
	} finally {
		lock.unlock();
	}
}
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, XExposeEvent src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(XExposeEvent dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(XFocusChangeEvent dest, int /*long*/ src, int /*long*/ size);

/** @method flags=const */
public static final native int RTLD_GLOBAL();
/** @method flags=const */
public static final native int RTLD_NOW();
/** @method flags=const */
public static final native int RTLD_LAZY();


/** Natives */
public static final native int Call (int /*long*/ func, int /*long*/ arg0, int arg1, int arg2);
public static final native long Call (int /*long*/ func, int /*long*/ arg0, int arg1, long arg2);
public static final native int /*long*/ _G_OBJECT_CLASS (int /*long*/ klass);
public static final int /*long*/ G_OBJECT_CLASS (int /*long*/ klass) {
	lock.lock();
	try {
		return _G_OBJECT_CLASS(klass);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _G_OBJECT_GET_CLASS (int /*long*/ object);
public static final int /*long*/ G_OBJECT_GET_CLASS (int /*long*/ object) {
	lock.lock();
	try {
		return _G_OBJECT_GET_CLASS(object);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _G_OBJECT_TYPE_NAME (int /*long*/ object);
public static final int /*long*/ G_OBJECT_TYPE_NAME (int /*long*/ object) {
	lock.lock();
	try {
		return _G_OBJECT_TYPE_NAME(object);
	} finally {
		lock.unlock();
	}
}

public static final native boolean _G_TYPE_CHECK_INSTANCE_TYPE (int /*long*/ instance, int /*long*/ type);
/**
 * Note: G_TYPE_CHECK_INSTANCE_TYPE is not a good way to check for instance type,
 * The C-Macro doesn't seem to work reliably in the context of being invoked from Java
 * via JNI on a dynamically loaded library.
 * But webkit1 development has halted and it's not worth the effort to change this.
 * I.e, kept for legacy reason but don't use this. Instead, to identify type, use user_data. (see Webkit proc3 as example).
 */
public static final boolean G_TYPE_CHECK_INSTANCE_TYPE (int /*long*/ instance, int /*long*/ type) {
	lock.lock();
	try {
		return _G_TYPE_CHECK_INSTANCE_TYPE(instance, type);
	} finally {
		lock.unlock();
	}
}
/** @method flags=const */
public static final native int /*long*/ G_TYPE_BOOLEAN();
/** @method flags=const */
public static final native int /*long*/ G_TYPE_DOUBLE();
/** @method flags=const */
public static final native int /*long*/ G_TYPE_FLOAT();
/** @method flags=const */
public static final native int /*long*/ G_TYPE_INT();
/** @method flags=const */
public static final native int /*long*/ G_TYPE_INT64();
public static final native int /*long*/ G_VALUE_TYPE(int /*long*/ value);
public static final native int /*long*/ _G_OBJECT_TYPE (int /*long*/ instance);
public static final int /*long*/ G_OBJECT_TYPE (int /*long*/ instance) {
	lock.lock();
	try {
		return _G_OBJECT_TYPE(instance);
	} finally {
		lock.unlock();
	}
}
/** @method flags=const */
public static final native int /*long*/ _G_TYPE_STRING();
public static final int /*long*/ G_TYPE_STRING() {
	lock.lock();
	try {
		return _G_TYPE_STRING();
	} finally {
		lock.unlock();
	}
}
public static final native int _PANGO_PIXELS(int dimension);
public static final int PANGO_PIXELS(int dimension) {
	lock.lock();
	try {
		return _PANGO_PIXELS(dimension);
	} finally {
		lock.unlock();
	}
}
/** @method flags=const */
public static final native int /*long*/ _PANGO_TYPE_FONT_DESCRIPTION();
public static final int /*long*/ PANGO_TYPE_FONT_DESCRIPTION() {
	lock.lock();
	try {
		return _PANGO_TYPE_FONT_DESCRIPTION();
	} finally {
		lock.unlock();
	}
}
/** @method flags=const */
public static final native int /*long*/ _PANGO_TYPE_FONT_FAMILY();
public static final int /*long*/ PANGO_TYPE_FONT_FAMILY() {
	lock.lock();
	try {
		return _PANGO_TYPE_FONT_FAMILY();
	} finally {
		lock.unlock();
	}
}
/** @method flags=const */
public static final native int /*long*/ _PANGO_TYPE_FONT_FACE();
public static final int /*long*/ PANGO_TYPE_FONT_FACE() {
	lock.lock();
	try {
		return _PANGO_TYPE_FONT_FACE();
	} finally {
		lock.unlock();
	}
}
/** @method flags=const */
public static final native int /*long*/ _PANGO_TYPE_LAYOUT();
public static final int /*long*/ PANGO_TYPE_LAYOUT() {
	lock.lock();
	try {
		return _PANGO_TYPE_LAYOUT();
	} finally {
		lock.unlock();
	}
}
/** @param filename cast=(const char *) */
public static final native int /*long*/ _dlopen(byte[] filename, int flag);
public static final int /*long*/ dlopen(byte[] filename, int flag) {
	lock.lock();
	try {
		return _dlopen(filename, flag);
	} finally {
		lock.unlock();
	}
}
/**
 * @param commandline cast=(gchar *)
 * @param applName cast=(gchar *)
 * @param flags cast=(GAppInfoCreateFlags)
 * @param error cast=(GError **)
 */
public static final native int /*long*/ _g_app_info_create_from_commandline(byte[] commandline, byte[] applName, int /*long*/ flags, int /*long*/ error);
public static final int /*long*/ g_app_info_create_from_commandline(byte[] commandline, byte[] applName, int /*long*/ flags, int /*long*/ error) {
	lock.lock();
	try {
		return _g_app_info_create_from_commandline(commandline, applName, flags, error);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _g_app_info_get_all();
public static final int /*long*/ g_app_info_get_all() {
	lock.lock();
	try {
		return _g_app_info_get_all();
	} finally {
		lock.unlock();
	}
}
/**
 * @param appInfo cast=(GAppInfo *)
 */
public static final native int /*long*/ _g_app_info_get_executable(int /*long*/ appInfo);
public static final int /*long*/ g_app_info_get_executable(int /*long*/ appInfo) {
	lock.lock();
	try {
		return _g_app_info_get_executable(appInfo);
	} finally {
		lock.unlock();
	}
}
/**
 * @param appInfo cast=(GAppInfo *)
 */
public static final native int /*long*/ _g_app_info_get_icon(int /*long*/ appInfo);
public static final int /*long*/ g_app_info_get_icon(int /*long*/ appInfo) {
	lock.lock();
	try {
		return _g_app_info_get_icon(appInfo);
	} finally {
		lock.unlock();
	}
}
/**
 * @param appInfo cast=(GAppInfo *)
 */
public static final native int /*long*/ _g_app_info_get_name(int /*long*/ appInfo);
public static final int /*long*/ g_app_info_get_name(int /*long*/ appInfo) {
	lock.lock();
	try {
		return _g_app_info_get_name(appInfo);
	} finally {
		lock.unlock();
	}
}
/**
 * @param appInfo cast=(GAppInfo *)
 * @param list cast=(GList *)
 * @param launchContext cast=(GAppLaunchContext *)
 * @param error cast=(GError **)
 */
public static final native boolean _g_app_info_launch(int /*long*/ appInfo, int /*long*/ list, int /*long*/ launchContext, int /*long*/ error);
public static final boolean g_app_info_launch(int /*long*/ appInfo, int /*long*/ list, int /*long*/ launchContext, int /*long*/ error) {
	lock.lock();
	try {
		return _g_app_info_launch(appInfo, list, launchContext, error);
	} finally {
		lock.unlock();
	}
}
/**
 * @param mimeType cast=(gchar *)
 * @param mustSupportURIs cast=(gboolean)
 */
public static final native int /*long*/ _g_app_info_get_default_for_type(byte[] mimeType, boolean mustSupportURIs);
public static final int /*long*/ g_app_info_get_default_for_type(byte[] mimeType, boolean mustSupportURIs) {
	lock.lock();
	try {
		return _g_app_info_get_default_for_type(mimeType, mustSupportURIs);
	} finally {
		lock.unlock();
	}
}
/**
 * @param uri cast=(char *)
 * @param launchContext cast=(GAppLaunchContext *)
 * @param error cast=(GError **)
 */
public static final native boolean _g_app_info_launch_default_for_uri(int /*long*/ uri, int /*long*/ launchContext, int /*long*/ error);
public static final boolean g_app_info_launch_default_for_uri(int /*long*/ uri, int /*long*/ launchContext, int /*long*/ error) {
	lock.lock();
	try {
		return _g_app_info_launch_default_for_uri(uri, launchContext, error);
	} finally {
		lock.unlock();
	}
}
/**
 * @param appInfo cast=(GAppInfo *)
 */
public static final native boolean _g_app_info_should_show(int /*long*/ appInfo);
public static final boolean g_app_info_should_show(int /*long*/ appInfo) {
	lock.lock();
	try {
		return _g_app_info_should_show(appInfo);
	} finally {
		lock.unlock();
	}
}
/**
 * @param appInfo cast=(GAppInfo *)
 */
public static final native boolean _g_app_info_supports_uris(int /*long*/ appInfo);
public static final boolean g_app_info_supports_uris(int /*long*/ appInfo) {
	lock.lock();
	try {
		return _g_app_info_supports_uris(appInfo);
	} finally {
		lock.unlock();
	}
}
/**
 * @param error cast=(GError *)
 */
public static final native int /*long*/ _g_error_get_message (int /*long*/ error);
public static final int /*long*/ g_error_get_message (int /*long*/ error) {
	lock.lock();
	try {
		return _g_error_get_message (error);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gerror cast=(GError *)
 */
public static final native void _g_error_free(int /*long*/ gerror);
public static final void g_error_free(int /*long*/ gerror) {
	lock.lock();
	try {
		_g_error_free(gerror);
	} finally {
		lock.unlock();
	}
}

/**
 * @param type1 cast=(gchar *)
 * @param type2 cast=(gchar *)
 */
public static final native boolean _g_content_type_equals(int /*long*/ type1, byte[] type2);
public static final boolean g_content_type_equals(int /*long*/ type1, byte[] type2) {
	lock.lock();
	try {
		return _g_content_type_equals(type1, type2);
	} finally {
		lock.unlock();
	}
}
/**
 * @param type cast=(gchar *)
 * @param supertype cast=(gchar *)
 */
public static final native boolean _g_content_type_is_a(int /*long*/ type, byte[] supertype);
public static final boolean g_content_type_is_a(int /*long*/ type, byte[] supertype) {
	lock.lock();
	try {
		return _g_content_type_is_a(type, supertype);
	} finally {
		lock.unlock();
	}
}
/**
 * @param info cast=(GFileInfo *)
 */
public static final native int /*long*/ _g_file_info_get_content_type (int /*long*/ info);
public static final int /*long*/ g_file_info_get_content_type (int /*long*/ info) {
	lock.lock();
	try {
		return _g_file_info_get_content_type (info);
	} finally {
		lock.unlock();
	}
}
/**
 * @param file cast=(GFile *)
 */
public static final native int /*long*/ _g_file_get_uri(int /*long*/ file);
public static final int /*long*/ g_file_get_uri (int /*long*/ file) {
	lock.lock();
	try {
		return _g_file_get_uri(file);
	} finally {
		lock.unlock();
	}
}
/** @param fileName cast=(const char *) */
public static final native int /*long*/ _g_file_new_for_path(byte[] fileName);
public static final int /*long*/ g_file_new_for_path(byte[] fileName) {
	lock.lock();
	try {
		return _g_file_new_for_path(fileName);
	} finally {
		lock.unlock();
	}
}
/**
 * @param fileName cast=(const char *)
 */
public static final native int /*long*/ _g_file_new_for_commandline_arg(byte[] fileName);
public static final int /*long*/ g_file_new_for_commandline_arg(byte[] fileName) {
	lock.lock();
	try {
		return _g_file_new_for_commandline_arg(fileName);
	} finally {
		lock.unlock();
	}
}
/** @param fileName cast=(const char *) */
public static final native int /*long*/ _g_file_new_for_uri(byte[] fileName);
public static final int /*long*/ g_file_new_for_uri(byte[] fileName) {
	lock.lock();
	try {
		return _g_file_new_for_uri(fileName);
	} finally {
		lock.unlock();
	}
}
/**
 * @param file cast=(GFile *)
 * @param attributes cast=(const char *)
 * @param flags cast=(GFileQueryInfoFlags)
 * @param cancellable cast=(GCancellable *)
 * @param error cast=(GError **)
 */
public static final native int /*long*/ _g_file_query_info  (int /*long*/ file, byte[] attributes, int /*long*/ flags, int /*long*/ cancellable, int /*long*/ error);
public static final int /*long*/ g_file_query_info  (int /*long*/ file,byte[] attributes, int /*long*/ flags, int /*long*/ cancellable, int /*long*/ error) {
	lock.lock();
	try {
		return _g_file_query_info (file, attributes, flags, cancellable, error);
	} finally {
		lock.unlock();
	}
}
/**
 * @param file cast=(const gchar *)
 * @param test cast=(GFileTest)
 */
public static final native boolean /*long*/ _g_file_test(byte[] file, int test);
public static final boolean /*long*/ g_file_test(byte[] file, int test) {
	lock.lock();
	try {
		return _g_file_test(file, test);
	} finally {
		lock.unlock();
	}
}
/** @param icon cast=(GIcon *) */
public static final native int /*long*/ _g_icon_to_string(int /*long*/ icon);
public static final int /*long*/ g_icon_to_string (int /*long*/ icon) {
	lock.lock();
	try {
		return _g_icon_to_string(icon);
	} finally {
		lock.unlock();
	}
}
/**
 * @param str cast=(const gchar *)
 * @param error cast=(GError **)
 */
public static final native int /*long*/ _g_icon_new_for_string(byte[] str, int /*long*/ error[]);
public static final int /*long*/ g_icon_new_for_string (byte[] str, int /*long*/ error[]) {
	lock.lock();
	try {
		return _g_icon_new_for_string(str, error);
	} finally {
		lock.unlock();
	}
}
/**
 * @param signal_id cast=(guint)
 * @param detail cast=(GQuark)
 * @param hook_func cast=(GSignalEmissionHook)
 * @param hook_data cast=(gpointer)
 * @param data_destroy cast=(GDestroyNotify)
 */
public static final native int /*long*/ _g_signal_add_emission_hook(int signal_id, int detail, int /*long*/ hook_func, int /*long*/ hook_data, int /*long*/ data_destroy);
public static final int /*long*/ g_signal_add_emission_hook(int signal_id, int detail, int /*long*/ hook_func, int /*long*/ hook_data, int /*long*/ data_destroy) {
	lock.lock();
	try {
		return _g_signal_add_emission_hook(signal_id, detail, hook_func, hook_data, data_destroy);
	} finally {
		lock.unlock();
	}
}
/**
 * @param signal_id cast=(guint)
 * @param hook_id cast=(gulong)
 */
public static final native void _g_signal_remove_emission_hook(int signal_id, int /*long*/ hook_id);
public static final void g_signal_remove_emission_hook(int signal_id, int /*long*/ hook_id) {
	lock.lock();
	try {
		 _g_signal_remove_emission_hook (signal_id, hook_id);
	} finally {
		lock.unlock();
	}
}
/**
 * @param callback_func cast=(GCallback)
 * @param user_data cast=(gpointer)
 * @param destroy_data cast=(GClosureNotify)
 */
public static final native int /*long*/ _g_cclosure_new(int /*long*/ callback_func, int /*long*/ user_data, int /*long*/ destroy_data);
public static final int /*long*/ g_cclosure_new(int /*long*/ callback_func, int /*long*/ user_data, int /*long*/ destroy_data) {
	lock.lock();
	try {
		return _g_cclosure_new(callback_func, user_data, destroy_data);
	} finally {
		lock.unlock();
	}
}
/** @param closure cast=(GClosure *) */
public static final native int /*long*/ _g_closure_ref(int /*long*/ closure);
public static final int /*long*/ g_closure_ref(int /*long*/ closure) {
	lock.lock();
	try {
		return _g_closure_ref(closure);
	} finally {
		lock.unlock();
	}
}
/** @param closure cast=(GClosure *) */
public static final native void _g_closure_sink(int /*long*/ closure);
public static final void g_closure_sink(int /*long*/ closure) {
	lock.lock();
	try {
		_g_closure_sink(closure);
	} finally {
		lock.unlock();
	}
}
/** @param closure cast=(GClosure *) */
public static final native void _g_closure_unref(int /*long*/ closure);
public static final void g_closure_unref(int /*long*/ closure) {
	lock.lock();
	try {
		_g_closure_unref(closure);
	} finally {
		lock.unlock();
	}
}
/** @param context cast=(GMainContext *) */
public static final native boolean _g_main_context_acquire(int /*long*/ context);
public static final boolean g_main_context_acquire(int /*long*/ context) {
	lock.lock();
	try {
		return _g_main_context_acquire(context);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(GMainContext *)
 * @param fds cast=(GPollFD *)
 */
public static final native int _g_main_context_check(int /*long*/ context, int max_priority, int /*long*/ fds, int n_fds);
public static final int g_main_context_check(int /*long*/ context, int max_priority, int /*long*/ fds, int n_fds) {
	lock.lock();
	try {
		return _g_main_context_check(context, max_priority, fds, n_fds);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _g_main_context_default();
public static final int /*long*/ g_main_context_default() {
	lock.lock();
	try {
		return _g_main_context_default();
	} finally {
		lock.unlock();
	}
}
/** @param context cast=(GMainContext *) */
public static final native boolean _g_main_context_iteration(int /*long*/ context, boolean may_block);
public static final boolean g_main_context_iteration(int /*long*/ context, boolean may_block) {
	lock.lock();
	try {
		return _g_main_context_iteration(context, may_block);
	} finally {
		lock.unlock();
	}
}
/** @param context cast=(GMainContext *) */
public static final native int /*long*/ _g_main_context_get_poll_func(int /*long*/ context);
public static final int /*long*/ g_main_context_get_poll_func(int /*long*/ context) {
	lock.lock();
	try {
		return _g_main_context_get_poll_func(context);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(GMainContext *)
 * @param priority cast=(gint *)
 */
public static final native boolean _g_main_context_prepare(int /*long*/ context, int[] priority);
public static final boolean g_main_context_prepare(int /*long*/ context, int[] priority) {
	lock.lock();
	try {
		return _g_main_context_prepare(context, priority);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(GMainContext *)
 * @param fds cast=(GPollFD *)
 * @param timeout_ cast=(gint *)
 */
public static final native int _g_main_context_query(int /*long*/ context, int max_priority, int[] timeout_, int /*long*/ fds, int n_fds);
public static final int g_main_context_query(int /*long*/ context, int max_priority, int[] timeout_, int /*long*/ fds, int n_fds) {
	lock.lock();
	try {
		return _g_main_context_query(context, max_priority, timeout_, fds, n_fds);
	} finally {
		lock.unlock();
	}
}
/** @param context cast=(GMainContext *) */
public static final native void _g_main_context_release(int /*long*/ context);
public static final void g_main_context_release(int /*long*/ context) {
	lock.lock();
	try {
		_g_main_context_release(context);
	} finally {
		lock.unlock();
	}
}
/** @param context cast=(GMainContext *) */
public static final native void g_main_context_wakeup(int /*long*/ context);
/**
 * @param opsysstring cast=(const gchar *)
 * @param len cast=(gssize)
 * @param bytes_read cast=(gsize *)
 * @param bytes_written cast=(gsize *)
 * @param error cast=(GError **)
 */
public static final native int /*long*/ _g_filename_to_utf8(int /*long*/ opsysstring, int /*long*/ len, int /*long*/[] bytes_read, int /*long*/[] bytes_written, int /*long*/[] error);
public static final int /*long*/ g_filename_to_utf8(int /*long*/ opsysstring, int /*long*/ len, int /*long*/[] bytes_read, int /*long*/[] bytes_written, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_filename_to_utf8(opsysstring, len, bytes_read, bytes_written, error);
	} finally {
		lock.unlock();
	}
}
/** @param filename cast=(const gchar *) */
public static final native int /*long*/ _g_filename_display_name(int /*long*/ filename);
public static final int /*long*/ g_filename_display_name(int /*long*/ filename) {
	lock.lock();
	try {
		return _g_filename_display_name(filename);
	} finally {
		lock.unlock();
	}
}
/**
 * @param filename cast=(const char *)
 * @param hostname cast=(const char *)
 * @param error cast=(GError **)
 */
public static final native int /*long*/ _g_filename_to_uri(int /*long*/ filename, int /*long*/ hostname, int /*long*/[] error);
public static final int /*long*/ g_filename_to_uri(int /*long*/ filename, int /*long*/ hostname, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_filename_to_uri(filename, hostname, error);
	} finally {
		lock.unlock();
	}
}
/**
 * @param opsysstring cast=(const gchar *)
 * @param len cast=(gssize)
 * @param bytes_read cast=(gsize *)
 * @param bytes_written cast=(gsize *)
 * @param error cast=(GError **)
 */
public static final native int /*long*/ _g_filename_from_utf8(int /*long*/ opsysstring, int /*long*/ len,  int /*long*/[] bytes_read, int /*long*/[] bytes_written, int /*long*/[] error);
public static final int /*long*/ g_filename_from_utf8(int /*long*/ opsysstring, int /*long*/ len,  int /*long*/[] bytes_read, int /*long*/[] bytes_written, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_filename_from_utf8(opsysstring, len, bytes_read, bytes_written, error);
	} finally {
		lock.unlock();
	}
}
/**
 * @param uri cast=(const char *)
 * @param hostname cast=(char **)
 * @param error cast=(GError **)
 */
public static final native int /*long*/ _g_filename_from_uri(int /*long*/ uri, int /*long*/[] hostname, int /*long*/[] error);
public static final int /*long*/ g_filename_from_uri(int /*long*/ uri, int /*long*/[] hostname, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_filename_from_uri(uri, hostname, error);
	} finally {
		lock.unlock();
	}
}
/** @param mem cast=(gpointer) */
public static final native void _g_free(int /*long*/ mem);
public static final void g_free(int /*long*/ mem) {
	lock.lock();
	try {
		_g_free(mem);
	} finally {
		lock.unlock();
	}
}
/**
 * @param table cast=(GHashTable *)
 */
public static final native int /*long*/ _g_hash_table_get_values(int /*long*/ table);
public static final int /*long*/ g_hash_table_get_values(int /*long*/ table) {
	lock.lock();
	try {
		return _g_hash_table_get_values(table);
	} finally {
		lock.unlock();
	}
}
/**
 * @param function cast=(GSourceFunc)
 * @param data cast=(gpointer)
 */
public static final native int _g_idle_add(int /*long*/ function, int /*long*/ data);
public static final int g_idle_add(int /*long*/ function, int /*long*/ data) {
	lock.lock();
	try {
		return _g_idle_add(function, data);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(GList *)
 * @param data cast=(gpointer)
 */
public static final native int /*long*/ _g_list_append(int /*long*/ list, int /*long*/ data);
public static final int /*long*/ g_list_append(int /*long*/ list, int /*long*/ data) {
	lock.lock();
	try {
		return _g_list_append(list, data);
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(GList *) */
public static final native int /*long*/ _g_list_data(int /*long*/ list);
public static final int /*long*/ g_list_data(int /*long*/ list) {
	lock.lock();
	try {
		return _g_list_data(list);
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(GList *) */
public static final native void _g_list_free(int /*long*/ list);
public static final void g_list_free(int /*long*/ list) {
	lock.lock();
	try {
		_g_list_free(list);
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(GList *) */
public static final native void _g_list_free_1(int /*long*/ list);
public static final void g_list_free_1(int /*long*/ list) {
	lock.lock();
	try {
		_g_list_free_1(list);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(GList *)
 */
public static final native int /*long*/ _g_list_last(int /*long*/ list);
public static final int /*long*/ g_list_last(int /*long*/ list) {
	lock.lock();
	try {
		return _g_list_last(list);
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(GList *) */
public static final native int _g_list_length(int /*long*/ list);
public static final int g_list_length(int /*long*/ list) {
	lock.lock();
	try {
		return _g_list_length(list);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(GList *)
 * @param llist cast=(GList *)
 */
public static final native void _g_list_set_next(int /*long*/ list, int /*long*/ llist);
public static final void g_list_set_next(int /*long*/ list, int /*long*/ llist) {
	lock.lock();
	try {
		_g_list_set_next(list, llist);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _g_list_next(int /*long*/ list);
public static final int /*long*/ g_list_next(int /*long*/ list) {
	lock.lock();
	try {
		return _g_list_next(list);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(GList *)
 * @param n cast=(guint)
 */
public static final native int /*long*/ _g_list_nth(int /*long*/ list, int n);
public static final int /*long*/ g_list_nth(int /*long*/ list, int n) {
	lock.lock();
	try {
		return _g_list_nth(list, n);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(GList *)
 * @param n cast=(guint)
 */
public static final native int /*long*/ _g_list_nth_data(int /*long*/ list, int n);
public static final int /*long*/ g_list_nth_data(int /*long*/ list, int n) {
	lock.lock();
	try {
		return _g_list_nth_data(list, n);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(GList *)
 * @param data cast=(gpointer)
 */
public static final native int /*long*/ _g_list_prepend(int /*long*/ list, int /*long*/ data);
public static final int /*long*/ g_list_prepend(int /*long*/ list, int /*long*/ data) {
	lock.lock();
	try {
		return _g_list_prepend(list, data);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(GList *)
 * @param llist cast=(GList *)
 */
public static final native void _g_list_set_previous(int /*long*/ list, int /*long*/ llist);
public static final void g_list_set_previous(int /*long*/ list, int /*long*/ llist) {
	lock.lock();
	try {
		_g_list_set_previous(list, llist);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _g_list_previous(int /*long*/ list);
public static final int /*long*/ g_list_previous(int /*long*/ list) {
	lock.lock();
	try {
		return _g_list_previous(list);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(GList *)
 * @param link cast=(GList *)
 */
public static final native int /*long*/ _g_list_remove_link(int /*long*/ list, int /*long*/ link);
public static final int /*long*/ g_list_remove_link(int /*long*/ list, int /*long*/ link) {
	lock.lock();
	try {
		return _g_list_remove_link(list, link);
	} finally {
		lock.unlock();
	}
}
/**
 * @param log_domain cast=(gchar *)
 * @param log_levels cast=(GLogLevelFlags)
 * @param message cast=(gchar *)
 * @param unused_data cast=(gpointer)
 */
public static final native void _g_log_default_handler(int /*long*/ log_domain, int log_levels, int /*long*/ message, int /*long*/ unused_data);
public static final void g_log_default_handler(int /*long*/ log_domain, int log_levels, int /*long*/ message, int /*long*/ unused_data) {
	lock.lock();
	try {
		_g_log_default_handler(log_domain, log_levels, message, unused_data);
	} finally {
		lock.unlock();
	}
}
/**
 * @param log_domain cast=(gchar *),flags=no_out
 * @param handler_id cast=(gint)
 */
public static final native void _g_log_remove_handler(byte[] log_domain, int handler_id);
public static final void g_log_remove_handler(byte[] log_domain, int handler_id) {
	lock.lock();
	try {
		_g_log_remove_handler(log_domain, handler_id);
	} finally {
		lock.unlock();
	}
}
/**
 * @param log_domain cast=(gchar *),flags=no_out
 * @param log_levels cast=(GLogLevelFlags)
 * @param log_func cast=(GLogFunc)
 * @param user_data cast=(gpointer)
 */
public static final native int _g_log_set_handler(byte[] log_domain, int log_levels, int /*long*/ log_func, int /*long*/ user_data);
public static final int g_log_set_handler(byte[] log_domain, int log_levels, int /*long*/ log_func, int /*long*/ user_data) {
	lock.lock();
	try {
		return _g_log_set_handler(log_domain, log_levels, log_func, user_data);
	} finally {
		lock.unlock();
	}
}
/** @param size cast=(gulong) */
public static final native int /*long*/ _g_malloc(int /*long*/ size);
public static final int /*long*/ g_malloc(int /*long*/ size) {
	lock.lock();
	try {
		return _g_malloc(size);
	} finally {
		lock.unlock();
	}
}
/**
 * @param oclass cast=(GObjectClass *)
 * @param property_name cast=(const gchar *)
 */
public static final native int /*long*/ _g_object_class_find_property(int /*long*/ oclass, byte[] property_name);
public static final int /*long*/ g_object_class_find_property(int /*long*/ oclass, byte[] property_name) {
	lock.lock();
	try {
		return _g_object_class_find_property(oclass, property_name);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(GObject *)
 * @param first_property_name cast=(const gchar *),flags=no_out
 * @param terminator cast=(const gchar *),flags=sentinel
 */
public static final native void _g_object_get(int /*long*/ object, byte[] first_property_name, int[] value, int /*long*/ terminator);
public static final void g_object_get(int /*long*/ object, byte[] first_property_name, int[] value, int /*long*/ terminator) {
	lock.lock();
	try {
		_g_object_get(object, first_property_name, value, terminator);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(GObject *)
 * @param first_property_name cast=(const gchar *),flags=no_out
 * @param terminator cast=(const gchar *),flags=sentinel
 */
public static final native void _g_object_get(int /*long*/ object, byte[] first_property_name, long[] value, int /*long*/ terminator);
public static final void g_object_get(int /*long*/ object, byte[] first_property_name, long[] value, int /*long*/ terminator) {
	lock.lock();
	try {
		_g_object_get(object, first_property_name, value, terminator);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(GObject *)
 * @param quark cast=(GQuark)
 */
public static final native int /*long*/ _g_object_get_qdata(int /*long*/ object, int quark);
public static final int /*long*/ g_object_get_qdata(int /*long*/ object, int quark) {
	lock.lock();
	try {
		return _g_object_get_qdata(object, quark);
	} finally {
		lock.unlock();
	}
}
/**
 * @param type cast=(GType)
 * @param first_property_name cast=(const gchar *)
 */
public static final native int /*long*/ _g_object_new (int /*long*/ type, int /*long*/ first_property_name);
public static final int /*long*/ g_object_new (int /*long*/ type, int /*long*/ first_property_name) {
	lock.lock();
	try {
		return _g_object_new(type, first_property_name);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(GObject *)
 * @param property_name cast=(const gchar *)
 */
public static final native void _g_object_notify (int /*long*/ object, byte[] property_name);
public static final void g_object_notify (int /*long*/ object, byte[] property_name) {
	lock.lock();
	try {
		_g_object_notify(object, property_name);
	} finally {
		lock.unlock();
	}
}
/** @param object cast=(gpointer) */
public static final native int /*long*/ _g_object_ref(int /*long*/ object);
public static final int /*long*/ g_object_ref(int /*long*/ object) {
	lock.lock();
	try {
		return _g_object_ref(object);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(gpointer)
 * @param first_property_name cast=(const gchar *),flags=no_out
 * @param terminator cast=(const gchar *),flags=sentinel
 */
public static final native void _g_object_set(int /*long*/ object, byte[] first_property_name, boolean data, int /*long*/ terminator);
public static final void g_object_set(int /*long*/ object, byte[] first_property_name, boolean data, int /*long*/ terminator) {
	lock.lock();
	try {
		_g_object_set(object, first_property_name, data, terminator);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(gpointer)
 * @param first_property_name cast=(const gchar *),flags=no_out
 * @param terminator cast=(const gchar *),flags=sentinel
 */
public static final native void _g_object_set(int /*long*/ object, byte[] first_property_name, byte[] data, int /*long*/ terminator);
public static final void g_object_set(int /*long*/ object, byte[] first_property_name, byte[] data, int /*long*/ terminator) {
	lock.lock();
	try {
		_g_object_set(object, first_property_name, data, terminator);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(gpointer)
 * @param first_property_name cast=(const gchar *)
 * @param terminator cast=(const gchar *),flags=sentinel
 */
public static final native void _g_object_set(int /*long*/ object, byte[] first_property_name, GdkColor data, int /*long*/ terminator);
public static final void g_object_set(int /*long*/ object, byte[] first_property_name, GdkColor data, int /*long*/ terminator) {
	lock.lock();
	try {
		_g_object_set(object, first_property_name, data, terminator);
	} finally {
		lock.unlock();
	}
}

//Note, the function below is handled in a special way in os.h because of the GdkRGBA (gtk3 only) struct. See os.h
//So although it is not marked as dynamic, it is only build on gtk3.
/**
 * @param object cast=(gpointer)
 * @param first_property_name cast=(const gchar *)
 * @param terminator cast=(const gchar *),flags=sentinel
 */
public static final native void _g_object_set(int /*long*/ object, byte[] first_property_name, GdkRGBA data, int /*long*/ terminator);
public static final void g_object_set(int /*long*/ object, byte[] first_property_name, GdkRGBA data, int /*long*/ terminator) {
	lock.lock();
	try {
		_g_object_set(object, first_property_name, data, terminator);
	} finally {
		lock.unlock();
	}
}

/**
 * @param object cast=(gpointer)
 * @param first_property_name cast=(const gchar *),flags=no_out
 * @param terminator cast=(const gchar *),flags=sentinel
 */
public static final native void _g_object_set(int /*long*/ object, byte[] first_property_name, int data, int /*long*/ terminator);
public static final void g_object_set(int /*long*/ object, byte[] first_property_name, int data, int /*long*/ terminator) {
	lock.lock();
	try {
		_g_object_set(object, first_property_name, data, terminator);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(gpointer)
 * @param first_property_name cast=(const gchar *),flags=no_out
 * @param terminator cast=(const gchar *),flags=sentinel
 */
public static final native void _g_object_set(int /*long*/ object, byte[] first_property_name, float data, int /*long*/ terminator);
public static final void g_object_set(int /*long*/ object, byte[] first_property_name, float data, int /*long*/ terminator) {
	lock.lock();
	try {
		_g_object_set(object, first_property_name, data, terminator);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(gpointer)
 * @param first_property_name cast=(const gchar *),flags=no_out
 * @param terminator cast=(const gchar *),flags=sentinel
 */
public static final native void _g_object_set(int /*long*/ object, byte[] first_property_name, long data, int /*long*/ terminator);
public static final void g_object_set(int /*long*/ object, byte[] first_property_name, long data, int /*long*/ terminator) {
	lock.lock();
	try {
		_g_object_set(object, first_property_name, data, terminator);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(GObject *)
 * @param quark cast=(GQuark)
 * @param data cast=(gpointer)
 */
public static final native void _g_object_set_qdata(int /*long*/ object, int quark, int /*long*/ data);
public static final void g_object_set_qdata(int /*long*/ object, int quark, int /*long*/ data) {
	lock.lock();
	try {
		_g_object_set_qdata(object, quark, data);
	} finally {
		lock.unlock();
	}
}
/** @param object cast=(gpointer) */
public static final native void _g_object_unref(int /*long*/ object);
public static final void g_object_unref(int /*long*/ object) {
	lock.lock();
	try {
		_g_object_unref(object);
	} finally {
		lock.unlock();
	}
}


/**
 * @method flags=dynamic
 * @param data cast=(gconstpointer)
 * @param size cast=(gsize)
 */
public static final native int /*long*/ _g_bytes_new (byte [] data, int /*long*/ size);
public static final int /*long*/ g_bytes_new (byte [] data, int /*long*/ size) {
	assert MIN_GLIB_2_32;  // Note Gtk3.4 == glib 2.32
	lock.lock();
	try {
		return _g_bytes_new (data, size);
	} finally {
		lock.unlock();
	}
}

/**
 * @method flags=dynamic
 * @param gBytes cast=(GBytes *)
 */
public static final native void _g_bytes_unref (int /*long*/ gBytes);
public static final void g_bytes_unref (int /*long*/ gBytes) {
	assert MIN_GLIB_2_32;  // Note Gtk3.4 == glib 2.32
	lock.lock();
	try {
		_g_bytes_unref (gBytes);
	} finally {
		lock.unlock();
	}
}

/** @param string cast=(const gchar *),flags=no_out */
public static final native int _g_quark_from_string(byte[] string);
public static final int g_quark_from_string(byte[] string) {
	lock.lock();
	try {
		return _g_quark_from_string(string);
	} finally {
		lock.unlock();
	}
}
/** @param prgname cast=(const gchar *),flags=no_out */
public static final native void _g_set_prgname(byte[] prgname);
public static final void g_set_prgname(byte[] prgname) {
	lock.lock();
	try {
		_g_set_prgname(prgname);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param detailed_signal cast=(const gchar *),flags=no_out
 * @param proc cast=(GCallback)
 * @param data cast=(gpointer)
 */
public static final native int _g_signal_connect(int /*long*/ instance, byte[] detailed_signal, int /*long*/ proc, int /*long*/ data);
public static final int g_signal_connect(int /*long*/ instance, byte[] detailed_signal, int /*long*/ proc, int /*long*/ data) {
	lock.lock();
	try {
		return _g_signal_connect(instance, detailed_signal, proc, data);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param detailed_signal cast=(const gchar *)
 * @param closure cast=(GClosure *)
 * @param after cast=(gboolean)
 */
public static final native int _g_signal_connect_closure(int /*long*/ instance, byte[] detailed_signal, int /*long*/ closure, boolean after);
public static final int g_signal_connect_closure(int /*long*/ instance, byte[] detailed_signal, int /*long*/ closure, boolean after) {
	lock.lock();
	try {
		return _g_signal_connect_closure(instance, detailed_signal, closure, after);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param signal_id cast=(guint)
 * @param detail cast=(GQuark)
 * @param closure cast=(GClosure *)
 * @param after cast=(gboolean)
 */
public static final native int _g_signal_connect_closure_by_id(int /*long*/ instance, int signal_id, int detail, int /*long*/ closure, boolean after);
public static final int g_signal_connect_closure_by_id(int /*long*/ instance, int signal_id, int detail, int /*long*/ closure, boolean after) {
	lock.lock();
	try {
		return _g_signal_connect_closure_by_id(instance, signal_id, detail, closure, after);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param detailed_signal cast=(const gchar *),flags=no_out
 */
public static final native void _g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal);
public static final void g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal) {
	lock.lock();
	try {
		_g_signal_emit_by_name(instance, detailed_signal);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param detailed_signal cast=(const gchar *),flags=no_out
 */
public static final native void _g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal, int /*long*/ data);
public static final void g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal, int /*long*/ data) {
	lock.lock();
	try {
		_g_signal_emit_by_name(instance, detailed_signal, data);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param detailed_signal cast=(const gchar *),flags=no_out
 */
public static final native void _g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal, GdkRectangle data);
public static final void g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal, GdkRectangle data) {
	lock.lock();
	try {
		_g_signal_emit_by_name(instance, detailed_signal, data);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param detailed_signal cast=(const gchar *),flags=no_out
 */
public static final native void _g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal, int /*long*/ data1, int /*long*/ data2);
public static final void g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal, int /*long*/ data1, int /*long*/ data2) {
	lock.lock();
	try {
		_g_signal_emit_by_name(instance, detailed_signal, data1, data2);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param detailed_signal cast=(const gchar *),flags=no_out
 */
public static final native void _g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal, byte [] data);
public static final void g_signal_emit_by_name(int /*long*/ instance, byte[] detailed_signal, byte [] data) {
	lock.lock();
	try {
		_g_signal_emit_by_name(instance, detailed_signal, data);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param handler_id cast=(gulong)
 */
public static final native void _g_signal_handler_disconnect(int /*long*/ instance, int handler_id);
public static final void g_signal_handler_disconnect(int /*long*/ instance, int handler_id) {
	lock.lock();
	try {
		_g_signal_handler_disconnect(instance, handler_id);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param detail cast=(GQuark)
 * @param closure cast=(GClosure *)
 * @param func cast=(gpointer)
 * @param data cast=(gpointer)
 */
public static final native int _g_signal_handler_find(int /*long*/ instance, int mask, int signal_id, int detail, int /*long*/ closure, int /*long*/ func, int /*long*/ data);
public static final int g_signal_handler_find(int /*long*/ instance, int mask, int signal_id, int detail, int /*long*/ closure, int /*long*/ func, int /*long*/ data) {
	lock.lock();
	try {
		return _g_signal_handler_find(instance, mask, signal_id, detail, closure, func, data);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param mask cast=(GSignalMatchType)
 * @param signal_id cast=(guint)
 * @param detail cast=(GQuark)
 * @param closure cast=(GClosure *)
 * @param func cast=(gpointer)
 * @param data cast=(gpointer)
 */
public static final native int _g_signal_handlers_block_matched(int /*long*/ instance, int mask, int signal_id, int detail, int /*long*/ closure, int /*long*/ func, int /*long*/ data);
public static final int g_signal_handlers_block_matched(int /*long*/ instance, int mask, int signal_id, int detail, int /*long*/ closure, int /*long*/ func, int /*long*/ data) {
	lock.lock();
	try {
		return _g_signal_handlers_block_matched(instance, mask, signal_id, detail, closure, func, data);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param mask cast=(GSignalMatchType)
 * @param signal_id cast=(guint)
 * @param detail cast=(GQuark)
 * @param closure cast=(GClosure *)
 * @param func cast=(gpointer)
 * @param data cast=(gpointer)
 */
public static final native int _g_signal_handlers_unblock_matched(int /*long*/ instance, int mask, int signal_id, int detail, int /*long*/ closure, int /*long*/ func, int /*long*/ data);
public static final int g_signal_handlers_unblock_matched(int /*long*/ instance, int mask, int signal_id, int detail, int /*long*/ closure, int /*long*/ func, int /*long*/ data) {
	lock.lock();
	try {
		return _g_signal_handlers_unblock_matched(instance, mask, signal_id, detail, closure, func, data);
	} finally {
		lock.unlock();
	}
}
/** @param name cast=(const gchar *),flags=no_out */
public static final native int _g_signal_lookup (byte[] name, int /*long*/ itype);
public static final int g_signal_lookup (byte[] name, int /*long*/ itype) {
	lock.lock();
	try {
		return _g_signal_lookup(name, itype);
	} finally {
		lock.unlock();
	}
}
/**
 * @param instance cast=(gpointer)
 * @param detailed_signal cast=(const gchar *),flags=no_out
 */
public static final native void _g_signal_stop_emission_by_name(int /*long*/ instance, byte[] detailed_signal);
public static final void g_signal_stop_emission_by_name(int /*long*/ instance, byte[] detailed_signal) {
	lock.lock();
	try {
		_g_signal_stop_emission_by_name(instance, detailed_signal);
	} finally {
		lock.unlock();
	}
}
/** @param tag cast=(guint) */
public static final native boolean /*long*/ _g_source_remove (int /*long*/ tag);
public static final boolean /*long*/ g_source_remove (int /*long*/ tag) {
	lock.lock();
	try {
		return _g_source_remove(tag);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(GSList *)
 * @param data cast=(gpointer)
 */
public static final native int /*long*/ _g_slist_append(int /*long*/ list, int /*long*/ data);
public static final int /*long*/ g_slist_append(int /*long*/ list, int /*long*/ data) {
	lock.lock();
	try {
		return _g_slist_append(list, data);
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(GSList *) */
public static final native int /*long*/ _g_slist_data (int /*long*/ list);
public static final int /*long*/ g_slist_data (int /*long*/ list) {
	lock.lock();
	try {
		return _g_slist_data(list);
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(GSList *) */
public static final native void _g_slist_free (int /*long*/ list);
public static final void g_slist_free (int /*long*/ list) {
	lock.lock();
	try {
		_g_slist_free(list);
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(GSList *) */
public static final native int /*long*/ _g_slist_next (int /*long*/ list);
public static final int /*long*/ g_slist_next (int /*long*/ list) {
	lock.lock();
	try {
		return _g_slist_next(list);
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(GSList *) */
public static final native int _g_slist_length (int /*long*/ list);
public static final int g_slist_length (int /*long*/ list) {
	lock.lock();
	try {
		return _g_slist_length(list);
	} finally {
		lock.unlock();
	}
}
/** @param string_array cast=(gchar **) */
public static final native void _g_strfreev(int /*long*/ string_array);
public static final void g_strfreev(int /*long*/ string_array) {
	lock.lock();
	try {
		_g_strfreev(string_array);
	} finally {
		lock.unlock();
	}
}
/**
 * @method flags=getter
 * @param string cast=(GString *)
 */
public static final native int _GString_len(int /*long*/ string);
public static final int GString_len(int /*long*/ string) {
	lock.lock();
	try {
		return _GString_len(string);
	} finally {
		lock.unlock();
	}
}
/**
 * @method flags=getter
 * @param string cast=(GString *)
 */
public static final native int /*long*/ _GString_str(int /*long*/ string);
public static final int /*long*/ GString_str(int /*long*/ string) {
	lock.lock();
	try {
		return _GString_str(string);
	} finally {
		lock.unlock();
	}
}

/**
 * @param init cast=(const gchar *)
 */
public static final native int /*long*/ _g_string_new_len(int /*long*/ init, int /*long*/ gssize);
/** 				   GString * g_string_new_len (const gchar *init, gssize len); */
public static final int /*long*/ g_string_new_len(int /*long*/ init, int /*long*/ gssize) {
	lock.lock();
	try {
		return _g_string_new_len(init, gssize);
	} finally {
		lock.unlock();
	}
}

/**
 * @param GString cast=(GString *)
 */
public static final native int /*long*/ _g_string_free(int /*long*/ GString, int gboolen_free_segment);
/** 					 gchar * g_string_free (GString *string, gboolean free_segment);*/
public static final int /*long*/ g_string_free(int /*long*/ GString, int gboolen_free_segment) {
	lock.lock();
	try {
		return _g_string_free(GString, gboolen_free_segment);
	} finally {
		lock.unlock();
	}
}

/**
 * @param str cast=(const gchar *)
 * @param endptr cast=(gchar **)
 */
public static final native double _g_strtod(int /*long*/ str, int /*long*/[] endptr);
public static final double g_strtod(int /*long*/ str, int /*long*/[] endptr) {
	lock.lock();
	try {
		return _g_strtod(str, endptr);
	} finally {
		lock.unlock();
	}
}
/** @param str cast=(char *) */
public static final native int /*long*/ g_strdup (int /*long*/ str);
/**
 * @param instance_type cast=(GType)
 * @param interface_type cast=(GType)
 * @param info cast=(const GInterfaceInfo *)
 */
public static final native void _g_type_add_interface_static (int /*long*/ instance_type, int /*long*/ interface_type, int /*long*/ info);
public static final void g_type_add_interface_static (int /*long*/ instance_type, int /*long*/ interface_type, int /*long*/ info) {
	lock.lock();
	try {
		_g_type_add_interface_static(instance_type, interface_type, info);
	} finally {
		lock.unlock();
	}
}
/** @param g_class cast=(GType) */
public static final native int /*long*/ _g_type_class_peek (int /*long*/ g_class);
public static final int /*long*/ g_type_class_peek (int /*long*/ g_class) {
	lock.lock();
	try {
		return _g_type_class_peek(g_class);
	} finally {
		lock.unlock();
	}
}
/** @param g_class cast=(gpointer) */
public static final native int /*long*/ _g_type_class_peek_parent (int /*long*/ g_class);
public static final int /*long*/ g_type_class_peek_parent (int /*long*/ g_class) {
	lock.lock();
	try {
		return _g_type_class_peek_parent(g_class);
	} finally {
		lock.unlock();
	}
}
/** @param g_class cast=(GType) */
public static final native int /*long*/ _g_type_class_ref (int /*long*/ g_class);
public static final int /*long*/ g_type_class_ref (int /*long*/ g_class) {
	lock.lock();
	try {
		return _g_type_class_ref(g_class);
	} finally {
		lock.unlock();
	}
}
/** @param g_class cast=(gpointer) */
public static final native void _g_type_class_unref (int /*long*/ g_class);
public static final void g_type_class_unref (int /*long*/ g_class) {
	lock.lock();
	try {
		_g_type_class_unref(g_class);
	} finally {
		lock.unlock();
	}
}
/** @param name cast=(const gchar *) */
public static final native int /*long*/ _g_type_from_name (byte[] name);
public static final int /*long*/ g_type_from_name (byte[] name) {
	lock.lock();
	try {
		return _g_type_from_name(name);
	} finally {
		lock.unlock();
	}
}
/** @param iface cast=(gpointer) */
public static final native int /*long*/ _g_type_interface_peek_parent (int /*long*/ iface);
public static final int /*long*/ g_type_interface_peek_parent (int /*long*/ iface) {
	lock.lock();
	try {
		return _g_type_interface_peek_parent(iface);
	} finally {
		lock.unlock();
	}
}
/**
 * @param type cast=(GType)
 * @param is_a_type cast=(GType)
 */
public static final native boolean _g_type_is_a (int /*long*/ type, int /*long*/ is_a_type);
public static final boolean g_type_is_a (int /*long*/ type, int /*long*/ is_a_type) {
	lock.lock();
	try {
		return _g_type_is_a(type, is_a_type);
	} finally {
		lock.unlock();
	}
}
/** @param handle cast=(GType) */
public static final native int /*long*/ _g_type_name (int /*long*/ handle);
public static final int /*long*/ g_type_name (int /*long*/ handle) {
	lock.lock();
	try {
		return _g_type_name(handle);
	} finally {
		lock.unlock();
	}
}
/** @param type cast=(GType) */
public static final native int /*long*/ _g_type_parent (int /*long*/ type);
public static final int /*long*/ g_type_parent (int /*long*/ type) {
	lock.lock();
	try {
		return _g_type_parent(type);
	} finally {
		lock.unlock();
	}
}
/**
 * @param type cast=(GType)
 * @param query cast=(GTypeQuery *)
 */
public static final native void _g_type_query (int /*long*/ type, int /*long*/ query);
public static final void g_type_query (int /*long*/ type, int /*long*/ query) {
	lock.lock();
	try {
		_g_type_query(type, query);
	} finally {
		lock.unlock();
	}
}
/**
 * @param parent_type cast=(GType)
 * @param type_name cast=(const gchar *)
 * @param info cast=(const GTypeInfo *)
 * @param flags cast=(GTypeFlags)
 */
public static final native int /*long*/ _g_type_register_static (int /*long*/ parent_type, byte[] type_name, int /*long*/ info, int flags);
public static final int /*long*/ g_type_register_static (int /*long*/ parent_type, byte[] type_name, int /*long*/ info, int flags) {
	lock.lock();
	try {
		return _g_type_register_static(parent_type, type_name, info, flags);
	} finally {
		lock.unlock();
	}
}
/**
 * @method flags=dynamic
 */
public static final native void _g_thread_init(int /*long*/ vtable);
/** Treat with special care, platform specific behaviour. See os_custom.h */
public static final void g_thread_init(int /*long*/ vtable) {
	lock.lock();
	try {
		_g_thread_init(vtable);
	} finally {
		lock.unlock();
	}
}
public static final native boolean _g_thread_supported();
/** Treat with special care, see os_custom.h */
public static final boolean g_thread_supported() {
	lock.lock();
	try {
		return _g_thread_supported();
	} finally {
		lock.unlock();
	}
}
/**
 * @param str cast=(const gunichar2 *),flags=no_out critical
 * @param len cast=(glong)
 * @param items_read cast=(glong *),flags=critical
 * @param items_written cast=(glong *),flags=critical
 * @param error cast=(GError **),flags=critical
 */
public static final native int /*long*/ _g_utf16_to_utf8(char[] str, int /*long*/ len, int /*long*/[] items_read, int /*long*/[] items_written, int /*long*/[] error);
public static final int /*long*/ g_utf16_to_utf8(char[] str, int /*long*/ len, int /*long*/[] items_read, int /*long*/[] items_written, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_utf16_to_utf8(str, len, items_read, items_written, error);
	} finally {
		lock.unlock();
	}
}
/**
 * @param str cast=(const gchar *)
 * @param pos cast=(const gchar *)
 */
public static final native int /*long*/ _g_utf8_pointer_to_offset(int /*long*/ str, int /*long*/ pos);
public static final int /*long*/ g_utf8_pointer_to_offset(int /*long*/ str, int /*long*/ pos) {
	lock.lock();
	try {
		return _g_utf8_pointer_to_offset(str, pos);
	} finally {
		lock.unlock();
	}
}
/** @param str cast=(const gchar *) */
public static final native int /*long*/ _g_utf8_strlen(int /*long*/ str, int /*long*/ max);
public static final int /*long*/ g_utf8_strlen(int /*long*/ str, int /*long*/ max) {
	lock.lock();
	try {
		return _g_utf8_strlen(str, max);
	} finally {
		lock.unlock();
	}
}
/**
 * @param str cast=(const gchar *),flags=no_out critical
 * @param len cast=(glong)
 * @param items_read cast=(glong *),flags=critical
 * @param items_written cast=(glong *),flags=critical
 * @param error cast=(GError **),flags=critical
 */
public static final native int /*long*/ _g_utf8_to_utf16(byte[] str, int /*long*/ len, int /*long*/[] items_read, int /*long*/[] items_written, int /*long*/[] error);
public static final int /*long*/ g_utf8_to_utf16(byte[] str, int /*long*/ len, int /*long*/[] items_read, int /*long*/[] items_written, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_utf8_to_utf16(str, len, items_read, items_written, error);
	} finally {
		lock.unlock();
	}
}
/**
 * @param str cast=(const gchar *)
 * @param len cast=(glong)
 * @param items_read cast=(glong *),flags=critical
 * @param items_written cast=(glong *),flags=critical
 * @param error cast=(GError **),flags=critical
 */
public static final native int /*long*/ _g_utf8_to_utf16(int /*long*/ str, int /*long*/ len, int /*long*/[] items_read, int /*long*/[] items_written, int /*long*/[] error);
public static final int /*long*/ g_utf8_to_utf16(int /*long*/ str, int /*long*/ len, int /*long*/[] items_read, int /*long*/[] items_written, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_utf8_to_utf16(str, len, items_read, items_written, error);
	} finally {
		lock.unlock();
	}
}
/**
 * @param value cast=(GValue *)
 * @param type cast=(GType)
 */
public static final native int /*long*/ g_value_init (int /*long*/ value, int /*long*/ type);
/** @param value cast=(GValue *) */
public static final native int g_value_get_int (int /*long*/ value);
/** @param value cast=(GValue *) */
public static final native void g_value_set_int (int /*long*/ value, int v);
/** @param value cast=(GValue *) */
public static final native double g_value_get_double (int /*long*/ value);
/** @param value cast=(GValue *) */
public static final native void g_value_set_double (int /*long*/ value, double v);
/** @param value cast=(GValue *) */
public static final native float g_value_get_float (int /*long*/ value);
/** @param value cast=(GValue *) */
public static final native void g_value_set_float (int /*long*/ value, float v);
/** @param value cast=(GValue *) */
public static final native long g_value_get_int64 (int /*long*/ value);
/** @param value cast=(GValue *) */
public static final native void g_value_set_int64 (int /*long*/ value, long v);
/** @param value cast=(GValue *) */
public static final native void g_value_unset (int /*long*/ value);
/** @param value cast=(const GValue *) */
public static final native int /*long*/ _g_value_peek_pointer (int /*long*/ value);
public static final  int /*long*/ g_value_peek_pointer (int /*long*/ value) {
	lock.lock();
	try {
		return _g_value_peek_pointer(value);
	} finally {
		lock.unlock();
	}
}

/** @method flags=const */
public static final native int _glib_major_version();
public static final int glib_major_version() {
	lock.lock();
	try {
		return _glib_major_version();
	} finally {
		lock.unlock();
	}
}
/** @method flags=const */
public static final native int _glib_minor_version();
public static final int glib_minor_version() {
	lock.lock();
	try {
		return _glib_minor_version();
	} finally {
		lock.unlock();
	}
}
/** @method flags=const */
public static final native int _glib_micro_version();
public static final int glib_micro_version() {
	lock.lock();
	try {
		return _glib_micro_version();
	} finally {
		lock.unlock();
	}
}

/**
 * @param interval cast=(guint32)
 * @param function cast=(GSourceFunc)
 * @param data cast=(gpointer)
 */
public static final native int _g_timeout_add(int interval, int /*long*/ function, int /*long*/ data);
public static final int g_timeout_add(int interval, int /*long*/ function, int /*long*/ data) {
	lock.lock();
	try {
		return _g_timeout_add(interval, function, data);
	} finally {
		lock.unlock();
	}
}

/** @method flags=dynamic */
public static final native boolean _FcConfigAppFontAddFile(int /*long*/ config, byte[] file);
public static final boolean FcConfigAppFontAddFile(int /*long*/ config, byte[] file) {
	lock.lock();
	try {
		return _FcConfigAppFontAddFile(config, file);
	} finally {
		lock.unlock();
	}
}


// Technically works on OSX also, but currently only used on Linux.
// Once SWT is moved to Java 9, consider using 'ProcessHandle.current().getPid();' instead,
// but for now getpid() should do.
// https://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
public static final native int _getpid ();
public static final int getpid() {
	lock.lock();
	try {
		return _getpid();
	} finally {
		lock.unlock();
	}
}
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, GInterfaceInfo src, int size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *),flags=no_out
 */
public static final native void memmove(int /*long*/ dest, GObjectClass src);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, GTypeInfo src, int size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, GtkTargetEntry src, int /*long*/ size);
//NOTE: Call only on GTK2 as this uses GdkColor.
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, GdkColor src, int /*long*/ size);
//NOTE: Call only on GTK3 as this uses GdkRGBA.
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, GdkRGBA src, int /*long*/ size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, GdkEventButton src, int /*long*/ size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, GdkEventKey src, int /*long*/ size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, GdkEventExpose src, int /*long*/ size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, GdkEventMotion src, int /*long*/ size);
/** @param src flags=no_out */
public static final native void memmove(int /*long*/ dest, GtkWidgetClass src);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove(int /*long*/ dest, PangoAttribute src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 */
public static final native void memmove(GObjectClass  dest, int /*long*/ src);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GTypeQuery dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkDragContext dest, int /*long*/ src, int /*long*/ size);
/** @param dest flags=no_in */
public static final native void memmove(GtkWidgetClass dest, int /*long*/ src);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GtkBorder dest, int /*long*/ src, int /*long*/ size);
//NOTE: Call only on GTK2 as this uses GdkColor.
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkColor dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkKeymapKey dest, int /*long*/ src, int /*long*/ size);
//NOTE: Call only on GTK3 as this uses GdkRGBA.
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkRGBA dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEvent dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEventAny dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEventButton dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEventCrossing dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEventExpose dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEventFocus dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEventKey dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEventMotion dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEventScroll dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkEventWindowState dest, int /*long*/ src, int /*long*/ size);
public static final native void memmove(int /*long*/ dest, GtkCellRendererClass src);
public static final native void memmove(GtkCellRendererClass dest, int /*long*/ src);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 */
public static final native void memmove(GtkFixed dest, int /*long*/ src);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *),flags=no_out
 */
public static final native void memmove(int /*long*/ dest, GtkFixed src);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 */
public static final native void memmove(GdkImage dest, int /*long*/ src);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(GdkRectangle dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(PangoAttribute dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(PangoAttrColor dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(PangoAttrInt dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(PangoItem dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(PangoLayoutLine dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(PangoLayoutRun dest, int /*long*/ src, int /*long*/ size);
/**
 * @param dest cast=(void *),flags=no_in
 * @param src cast=(const void *)
 * @param size cast=(size_t)
 */
public static final native void memmove(PangoLogAttr dest, int /*long*/ src, int /*long*/ size);
/** @param attribute cast=(const PangoAttribute *) */
public static final native int /*long*/ _pango_attribute_copy (int /*long*/ attribute);
public static final int /*long*/ pango_attribute_copy (int /*long*/ attribute) {
	lock.lock();
	try {
		return _pango_attribute_copy(attribute);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_attr_background_new (short red, short green, short blue);
public static final int /*long*/ pango_attr_background_new (short red, short green, short blue) {
	lock.lock();
	try {
		return _pango_attr_background_new(red, green, blue);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(const PangoFontDescription *) */
public static final native int /*long*/ _pango_attr_font_desc_new(int /*long*/ desc);
public static final int /*long*/ pango_attr_font_desc_new(int /*long*/ desc) {
	lock.lock();
	try {
		return _pango_attr_font_desc_new(desc);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_attr_foreground_new (short red, short green, short blue);
public static final int /*long*/ pango_attr_foreground_new (short red, short green, short blue) {
	lock.lock();
	try {
		return _pango_attr_foreground_new(red, green, blue);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_attr_rise_new(int rise);
public static final int /*long*/ pango_attr_rise_new(int rise) {
	lock.lock();
	try {
		return _pango_attr_rise_new(rise);
	} finally {
		lock.unlock();
	}
}
/**
 * @param ink_rect flags=no_out
 * @param logical_rect flags=no_out
 */
public static final native int /*long*/ _pango_attr_shape_new(PangoRectangle ink_rect, PangoRectangle logical_rect);
public static final int /*long*/ pango_attr_shape_new(PangoRectangle ink_rect, PangoRectangle logical_rect) {
	lock.lock();
	try {
		return _pango_attr_shape_new(ink_rect, logical_rect);
	} finally {
		lock.unlock();
	}
}
/**
 * @param list cast=(PangoAttrList *)
 * @param attr cast=(PangoAttribute *)
 */
public static final native void _pango_attr_list_insert(int /*long*/ list, int /*long*/ attr);
public static final void pango_attr_list_insert(int /*long*/ list, int /*long*/ attr) {
	lock.lock();
	try {
		_pango_attr_list_insert(list, attr);
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(PangoAttrList *) */
public static final native int /*long*/ _pango_attr_list_get_iterator(int /*long*/ list);
public static final int /*long*/ pango_attr_list_get_iterator(int /*long*/ list) {
	lock.lock();
	try {
		return _pango_attr_list_get_iterator(list);
	} finally {
		lock.unlock();
	}
}
/** @param iterator cast=(PangoAttrIterator *) */
public static final native boolean _pango_attr_iterator_next(int /*long*/ iterator);
public static final boolean pango_attr_iterator_next(int /*long*/ iterator) {
	lock.lock();
	try {
		return _pango_attr_iterator_next(iterator);
	} finally {
		lock.unlock();
	}
}
/**
 * @param iterator cast=(PangoAttrIterator *)
 * @param start cast=(gint *)
 * @param end cast=(gint *)
 */
public static final native void _pango_attr_iterator_range(int /*long*/ iterator, int[] start, int[] end);
public static final void pango_attr_iterator_range(int /*long*/ iterator, int[] start, int[] end) {
	lock.lock();
	try {
		_pango_attr_iterator_range(iterator, start, end);
	} finally {
		lock.unlock();
	}
}
/**
 * @param iterator cast=(PangoAttrIterator *)
 * @param type cast=(PangoAttrType)
 */
public static final native int /*long*/ _pango_attr_iterator_get(int /*long*/ iterator, int type);
public static final int /*long*/ pango_attr_iterator_get(int /*long*/ iterator, int type) {
	lock.lock();
	try {
		return _pango_attr_iterator_get(iterator, type);
	} finally {
		lock.unlock();
	}
}
/** @param iterator cast=(PangoAttrIterator *) */
public static final native void _pango_attr_iterator_destroy(int /*long*/ iterator);
public static final void pango_attr_iterator_destroy(int /*long*/ iterator) {
	lock.lock();
	try {
		_pango_attr_iterator_destroy(iterator);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_attr_list_new();
public static final int /*long*/ pango_attr_list_new() {
	lock.lock();
	try {
		return _pango_attr_list_new();
	} finally {
		lock.unlock();
	}
}
/** @param list cast=(PangoAttrList *) */
public static final native void _pango_attr_list_unref(int /*long*/ list);
public static final void pango_attr_list_unref(int /*long*/ list) {
	lock.lock();
	try {
		_pango_attr_list_unref(list);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_attr_strikethrough_color_new(short red, short green, short blue);
public static final int /*long*/ pango_attr_strikethrough_color_new(short red, short green, short blue) {
	lock.lock();
	try {
		return _pango_attr_strikethrough_color_new(red, green, blue);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_attr_strikethrough_new(boolean strikethrough);
public static final int /*long*/ pango_attr_strikethrough_new(boolean strikethrough) {
	lock.lock();
	try {
		return _pango_attr_strikethrough_new(strikethrough);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_attr_underline_color_new(short red, short green, short blue);
public static final int /*long*/ pango_attr_underline_color_new(short red, short green, short blue) {
	lock.lock();
	try {
		return _pango_attr_underline_color_new(red, green, blue);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_attr_underline_new(int underline);
public static final int /*long*/ pango_attr_underline_new(int underline) {
	lock.lock();
	try {
		return _pango_attr_underline_new(underline);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_attr_weight_new(int weight);
public static final int /*long*/ pango_attr_weight_new(int weight) {
	lock.lock();
	try {
		return _pango_attr_weight_new(weight);
	} finally {
		lock.unlock();
	}
}
/**
 * @param cairo cast=(cairo_t *)
 */
public static final native int /*long*/ _pango_cairo_create_layout(int /*long*/ cairo);
public static final int /*long*/ pango_cairo_create_layout(int /*long*/ cairo) {
	lock.lock();
	try {
		return _pango_cairo_create_layout(cairo);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(PangoContext *)
 */
public static final native int /*long*/ _pango_cairo_context_get_font_options(int /*long*/ context);
public static final int /*long*/ pango_cairo_context_get_font_options(int /*long*/ context) {
	lock.lock();
	try {
		return _pango_cairo_context_get_font_options(context);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(PangoContext *)
 * @param options cast=( cairo_font_options_t *)
 */
public static final native void _pango_cairo_context_set_font_options(int /*long*/ context, int /*long*/ options);
public static final void pango_cairo_context_set_font_options(int /*long*/ context, int /*long*/ options) {
	lock.lock();
	try {
		_pango_cairo_context_set_font_options(context, options);
	} finally {
		lock.unlock();
	}
}
/**
 * @param cairo cast=(cairo_t *)
 * @param layout cast=(PangoLayout *)
 */
public static final native void _pango_cairo_layout_path(int /*long*/ cairo, int /*long*/ layout);
public static final void pango_cairo_layout_path(int /*long*/ cairo, int /*long*/ layout) {
	lock.lock();
	try {
		_pango_cairo_layout_path(cairo, layout);
	} finally {
		lock.unlock();
	}
}
/**
 * @param cairo cast=(cairo_t *)
 * @param layout cast=(PangoLayout *)
 */
public static final native void _pango_cairo_show_layout(int /*long*/ cairo, int /*long*/ layout);
public static final void pango_cairo_show_layout(int /*long*/ cairo, int /*long*/ layout) {
	lock.lock();
	try {
		_pango_cairo_show_layout(cairo, layout);
	} finally {
		lock.unlock();
	}
}
/** @param context cast=(PangoContext *) */
public static final native int _pango_context_get_base_dir(int /*long*/ context);
public static final int pango_context_get_base_dir(int /*long*/ context) {
	lock.lock();
	try {
		return _pango_context_get_base_dir(context);
	} finally {
		lock.unlock();
	}
}
/** @param context cast=(PangoContext *) */
public static final native int /*long*/ _pango_context_get_language(int /*long*/ context);
public static final int /*long*/ pango_context_get_language(int /*long*/ context) {
	lock.lock();
	try {
		return _pango_context_get_language(context);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(PangoContext *)
 * @param desc cast=(const PangoFontDescription *)
 * @param language cast=(PangoLanguage *)
 */
public static final native int /*long*/ _pango_context_get_metrics(int /*long*/ context, int /*long*/ desc, int /*long*/ language);
public static final int /*long*/ pango_context_get_metrics(int /*long*/ context, int /*long*/ desc, int /*long*/ language) {
	lock.lock();
	try {
		return _pango_context_get_metrics(context, desc, language);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(PangoContext *)
 * @param families cast=(PangoFontFamily ***)
 * @param n_families cast=(int *)
 */
public static final native void _pango_context_list_families(int /*long*/ context, int /*long*/[] families, int[] n_families);
public static final void pango_context_list_families(int /*long*/ context, int /*long*/[] families, int[] n_families) {
	lock.lock();
	try {
		_pango_context_list_families(context, families, n_families);
	} finally {
		lock.unlock();
	}
}
/** @param context cast=(PangoContext *) */
public static final native void _pango_context_set_base_dir(int /*long*/ context, int direction);
public static final void pango_context_set_base_dir(int /*long*/ context, int direction) {
	lock.lock();
	try {
		_pango_context_set_base_dir(context, direction);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(PangoContext *)
 * @param language cast=(PangoLanguage *)
 */
public static final native void _pango_context_set_language(int /*long*/ context, int /*long*/ language);
public static final void pango_context_set_language(int /*long*/ context, int /*long*/ language) {
	lock.lock();
	try {
		_pango_context_set_language(context, language);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(PangoFontDescription *) */
public static final native int /*long*/ _pango_font_description_copy(int /*long*/ desc);
public static final int /*long*/ pango_font_description_copy(int /*long*/ desc) {
	lock.lock();
	try {
		return _pango_font_description_copy(desc);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(PangoFontDescription *) */
public static final native void _pango_font_description_free(int /*long*/ desc);
public static final void pango_font_description_free(int /*long*/ desc) {
	lock.lock();
	try {
		_pango_font_description_free(desc);
	} finally {
		lock.unlock();
	}
}
/** @param str cast=(const char *),flags=no_out critical */
public static final native int /*long*/ _pango_font_description_from_string(byte[] str);
public static final int /*long*/ pango_font_description_from_string(byte[] str) {
	lock.lock();
	try {
		return _pango_font_description_from_string(str);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(PangoFontDescription *) */
public static final native int /*long*/ _pango_font_description_get_family(int /*long*/ desc);
public static final int /*long*/ pango_font_description_get_family(int /*long*/ desc) {
	lock.lock();
	try {
		return _pango_font_description_get_family(desc);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(PangoFontDescription *) */
public static final native int _pango_font_description_get_size(int /*long*/ desc);
public static final int pango_font_description_get_size(int /*long*/ desc) {
	lock.lock();
	try {
		return _pango_font_description_get_size(desc);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(PangoFontDescription *) */
public static final native int _pango_font_description_get_stretch(int /*long*/ desc);
public static final int pango_font_description_get_stretch(int /*long*/ desc) {
	lock.lock();
	try {
		return _pango_font_description_get_stretch(desc);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(PangoFontDescription *) */
public static final native int _pango_font_description_get_variant(int /*long*/ desc);
public static final int pango_font_description_get_variant(int /*long*/ desc) {
	lock.lock();
	try {
		return _pango_font_description_get_variant(desc);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(PangoFontDescription *) */
public static final native int _pango_font_description_get_style(int /*long*/ desc);
public static final int pango_font_description_get_style(int /*long*/ desc) {
	lock.lock();
	try {
		return _pango_font_description_get_style(desc);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(PangoFontDescription *) */
public static final native int _pango_font_description_get_weight(int /*long*/ desc);
public static final int pango_font_description_get_weight(int /*long*/ desc) {
	lock.lock();
	try {
		return _pango_font_description_get_weight(desc);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _pango_font_description_new();
public static final int /*long*/ pango_font_description_new() {
	lock.lock();
	try {
		return _pango_font_description_new();
	} finally {
		lock.unlock();
	}
}
/**
 * @param desc cast=(PangoFontDescription *)
 * @param family cast=(const char *),flags=no_out critical
 */
public static final native void _pango_font_description_set_family(int /*long*/ desc, byte[] family);
public static final void pango_font_description_set_family(int /*long*/ desc, byte[] family) {
	lock.lock();
	try {
		_pango_font_description_set_family(desc, family);
	} finally {
		lock.unlock();
	}
}
/**
 * @param desc cast=(PangoFontDescription *)
 * @param size cast=(gint)
 */
public static final native void _pango_font_description_set_size(int /*long*/ desc, int size);
public static final void pango_font_description_set_size(int /*long*/ desc, int size) {
	lock.lock();
	try {
		_pango_font_description_set_size(desc, size);
	} finally {
		lock.unlock();
	}
}
/**
 * @param desc cast=(PangoFontDescription *)
 * @param stretch cast=(PangoStretch)
 */
public static final native void _pango_font_description_set_stretch(int /*long*/ desc, int stretch);
public static final void pango_font_description_set_stretch(int /*long*/ desc, int stretch) {
	lock.lock();
	try {
		_pango_font_description_set_stretch(desc, stretch);
	} finally {
		lock.unlock();
	}
}
/**
 * @param desc cast=(PangoFontDescription *)
 * @param weight cast=(PangoStyle)
 */
public static final native void _pango_font_description_set_style(int /*long*/ desc, int weight);
public static final void pango_font_description_set_style(int /*long*/ desc, int weight) {
	lock.lock();
	try {
		_pango_font_description_set_style(desc, weight);
	} finally {
		lock.unlock();
	}
}
/**
 * @param desc cast=(PangoFontDescription *)
 * @param weight cast=(PangoWeight)
 */
public static final native void _pango_font_description_set_weight(int /*long*/ desc, int weight);
public static final void pango_font_description_set_weight(int /*long*/ desc, int weight) {
	lock.lock();
	try {
		_pango_font_description_set_weight(desc, weight);
	} finally {
		lock.unlock();
	}
}
/**
 * @param desc cast=(PangoFontDescription *)
 * @param variant cast=(PangoVariant)
 */
public static final native void _pango_font_description_set_variant(int /*long*/ desc, int variant);
public static final void pango_font_description_set_variant(int /*long*/ desc, int variant) {
	lock.lock();
	try {
		_pango_font_description_set_variant(desc, variant);
	} finally {
		lock.unlock();
	}
}
/** @param desc cast=(PangoFontDescription *) */
public static final native int /*long*/ _pango_font_description_to_string(int /*long*/ desc);
public static final int /*long*/ pango_font_description_to_string(int /*long*/ desc) {
	lock.lock();
	try {
		return _pango_font_description_to_string(desc);
	} finally {
		lock.unlock();
	}
}
/** @param face cast=(PangoFontFace *) */
public static final native int /*long*/ _pango_font_face_describe(int /*long*/ face);
public static final int /*long*/ pango_font_face_describe(int /*long*/ face) {
	lock.lock();
	try {
		return _pango_font_face_describe(face);
	} finally {
		lock.unlock();
	}
}
/** @param family cast=(PangoFontFamily *) */
public static final native int /*long*/ _pango_font_family_get_name(int /*long*/ family);
public static final int /*long*/ pango_font_family_get_name(int /*long*/ family) {
	lock.lock();
	try {
		return _pango_font_family_get_name(family);
	} finally {
		lock.unlock();
	}
}
/**
 * @param family cast=(PangoFontFamily *)
 * @param faces cast=(PangoFontFace ***)
 * @param n_faces cast=(int *)
 */
public static final native void _pango_font_family_list_faces(int /*long*/ family, int /*long*/[] faces, int[] n_faces);
public static final void pango_font_family_list_faces(int /*long*/ family, int /*long*/[] faces, int[] n_faces) {
	lock.lock();
	try {
		_pango_font_family_list_faces(family, faces, n_faces);
	} finally {
		lock.unlock();
	}
}
/**
 * @param font cast=(PangoFont *)
 * @param language cast=(PangoLanguage *)
 */
public static final native int /*long*/ _pango_font_get_metrics(int /*long*/ font, int /*long*/ language);
public static final int /*long*/ pango_font_get_metrics(int /*long*/ font, int /*long*/ language) {
	lock.lock();
	try {
		return _pango_font_get_metrics(font, language);
	} finally {
		lock.unlock();
	}
}
/** @param metrics cast=(PangoFontMetrics *) */
public static final native int _pango_font_metrics_get_approximate_char_width(int /*long*/ metrics);
public static final int pango_font_metrics_get_approximate_char_width(int /*long*/ metrics) {
	lock.lock();
	try {
		return _pango_font_metrics_get_approximate_char_width(metrics);
	} finally {
		lock.unlock();
	}
}
/** @param metrics cast=(PangoFontMetrics *) */
public static final native int _pango_font_metrics_get_ascent(int /*long*/ metrics);
public static final int pango_font_metrics_get_ascent(int /*long*/ metrics) {
	lock.lock();
	try {
		return _pango_font_metrics_get_ascent(metrics);
	} finally {
		lock.unlock();
	}
}
/** @param metrics cast=(PangoFontMetrics *) */
public static final native int _pango_font_metrics_get_descent(int /*long*/ metrics);
public static final int pango_font_metrics_get_descent(int /*long*/ metrics) {
	lock.lock();
	try {
		return _pango_font_metrics_get_descent(metrics);
	} finally {
		lock.unlock();
	}
}
/** @param metrics cast=(PangoFontMetrics *) */
public static final native void _pango_font_metrics_unref(int /*long*/ metrics);
public static final void pango_font_metrics_unref(int /*long*/ metrics) {
	lock.lock();
	try {
		_pango_font_metrics_unref(metrics);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout *) */
public static final native void _pango_layout_context_changed(int /*long*/ layout);
public static final void pango_layout_context_changed(int /*long*/ layout) {
	lock.lock();
	try {
		_pango_layout_context_changed(layout);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout*) */
public static final native int _pango_layout_get_alignment(int /*long*/ layout);
public static final int pango_layout_get_alignment(int /*long*/ layout) {
	lock.lock();
	try {
		return _pango_layout_get_alignment(layout);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout *) */
public static final native int /*long*/ _pango_layout_get_context(int /*long*/ layout);
public static final int /*long*/ pango_layout_get_context(int /*long*/ layout) {
	lock.lock();
	try {
		return _pango_layout_get_context(layout);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout*) */
public static final native int _pango_layout_get_indent(int /*long*/ layout);
public static final int pango_layout_get_indent(int /*long*/ layout) {
	lock.lock();
	try {
		return _pango_layout_get_indent(layout);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout*) */
public static final native int /*long*/ _pango_layout_get_iter(int /*long*/ layout);
public static final int /*long*/ pango_layout_get_iter(int /*long*/ layout) {
	lock.lock();
	try {
		return _pango_layout_get_iter(layout);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout*) */
public static final native boolean _pango_layout_get_justify(int /*long*/ layout);
public static final boolean pango_layout_get_justify(int /*long*/ layout) {
	lock.lock();
	try {
		return _pango_layout_get_justify(layout);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout *) */
public static final native int /*long*/ _pango_layout_get_line(int /*long*/ layout, int line);
public static final int /*long*/ pango_layout_get_line(int /*long*/ layout, int line) {
	lock.lock();
	try {
		return _pango_layout_get_line(layout, line);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout*) */
public static final native int _pango_layout_get_line_count(int /*long*/ layout);
public static final int pango_layout_get_line_count(int /*long*/ layout) {
	lock.lock();
	try {
		return _pango_layout_get_line_count(layout);
	} finally {
		lock.unlock();
	}
}
/**
 * @param layout cast=(PangoLayout*)
 * @param attrs cast=(PangoLogAttr **)
 * @param n_attrs cast=(int *)
 */
public static final native void _pango_layout_get_log_attrs(int /*long*/ layout, int /*long*/[] attrs, int[] n_attrs);
public static final void pango_layout_get_log_attrs(int /*long*/ layout, int /*long*/[] attrs, int[] n_attrs) {
	lock.lock();
	try {
		_pango_layout_get_log_attrs(layout, attrs, n_attrs);
	} finally {
		lock.unlock();
	}
}
/**
 * @param layout cast=(PangoLayout *)
 * @param width cast=(int *)
 * @param height cast=(int *)
 */
public static final native void _pango_layout_get_size(int /*long*/ layout, int[] width, int[] height);
public static final void pango_layout_get_size(int /*long*/ layout, int[] width, int[] height) {
	lock.lock();
	try {
		_pango_layout_get_size(layout, width, height);
	} finally {
		lock.unlock();
	}
}
/**
 * @param layout cast=(PangoLayout *)
 * @param width cast=(int *)
 * @param height cast=(int *)
 */
public static final native void _pango_layout_get_pixel_size(int /*long*/ layout, int[] width, int[] height);
public static final void pango_layout_get_pixel_size(int /*long*/ layout, int[] width, int[] height) {
	lock.lock();
	try {
		_pango_layout_get_pixel_size(layout, width, height);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout*) */
public static final native int _pango_layout_get_spacing(int /*long*/ layout);
public static final int pango_layout_get_spacing(int /*long*/ layout) {
	lock.lock();
	try {
		return _pango_layout_get_spacing(layout);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout *) */
public static final native int /*long*/ _pango_layout_get_text(int /*long*/ layout);
public static final int /*long*/ pango_layout_get_text(int /*long*/ layout) {
	lock.lock();
	try {
		return _pango_layout_get_text(layout);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout *) */
public static final native int _pango_layout_get_width(int /*long*/ layout);
public static final int pango_layout_get_width(int /*long*/ layout) {
	lock.lock();
	try {
		return _pango_layout_get_width(layout);
	} finally {
		lock.unlock();
	}
}
/**
 * @param layout cast=(PangoLayout*)
 * @param pos flags=no_in
 */
public static final native void _pango_layout_index_to_pos(int /*long*/ layout, int index, PangoRectangle pos);
public static final void pango_layout_index_to_pos(int /*long*/ layout, int index, PangoRectangle pos) {
	lock.lock();
	try {
		_pango_layout_index_to_pos(layout, index, pos);
	} finally {
		lock.unlock();
	}
}
/** @param iter cast=(PangoLayoutIter*) */
public static final native void _pango_layout_iter_free(int /*long*/ iter);
public static final void pango_layout_iter_free(int /*long*/ iter) {
	lock.lock();
	try {
		_pango_layout_iter_free(iter);
	} finally {
		lock.unlock();
	}
}
/**
 * @param iter cast=(PangoLayoutIter*)
 * @param ink_rect flags=no_in
 * @param logical_rect flags=no_in
 */
public static final native void _pango_layout_iter_get_line_extents(int /*long*/ iter, PangoRectangle ink_rect, PangoRectangle logical_rect);
public static final void pango_layout_iter_get_line_extents(int /*long*/ iter, PangoRectangle ink_rect, PangoRectangle logical_rect) {
	lock.lock();
	try {
		_pango_layout_iter_get_line_extents(iter, ink_rect, logical_rect);
	} finally {
		lock.unlock();
	}
}
/** @param iter cast=(PangoLayoutIter*) */
public static final native int _pango_layout_iter_get_index(int /*long*/ iter);
public static final int pango_layout_iter_get_index(int /*long*/ iter) {
	lock.lock();
	try {
		return _pango_layout_iter_get_index(iter);
	} finally {
		lock.unlock();
	}
}
/** @param iter cast=(PangoLayoutIter*) */
public static final native int /*long*/ _pango_layout_iter_get_run(int /*long*/ iter);
public static final int /*long*/ pango_layout_iter_get_run(int /*long*/ iter) {
	lock.lock();
	try {
		return _pango_layout_iter_get_run(iter);
	} finally {
		lock.unlock();
	}
}
/** @param iter cast=(PangoLayoutIter*) */
public static final native boolean _pango_layout_iter_next_line(int /*long*/ iter);
public static final boolean pango_layout_iter_next_line(int /*long*/ iter) {
	lock.lock();
	try {
		return _pango_layout_iter_next_line(iter);
	} finally {
		lock.unlock();
	}
}
/** @param iter cast=(PangoLayoutIter*) */
public static final native boolean _pango_layout_iter_next_run(int /*long*/ iter);
public static final boolean pango_layout_iter_next_run(int /*long*/ iter) {
	lock.lock();
	try {
		return _pango_layout_iter_next_run(iter);
	} finally {
		lock.unlock();
	}
}
/**
 * @param line cast=(PangoLayoutLine*)
 * @param ink_rect cast=(PangoRectangle *),flags=no_in
 * @param logical_rect cast=(PangoRectangle *),flags=no_in
 */
public static final native void _pango_layout_line_get_extents(int /*long*/ line, PangoRectangle ink_rect, PangoRectangle logical_rect);
public static final void pango_layout_line_get_extents(int /*long*/ line, PangoRectangle ink_rect, PangoRectangle logical_rect) {
	lock.lock();
	try {
		_pango_layout_line_get_extents(line, ink_rect, logical_rect);
	} finally {
		lock.unlock();
	}
}
/** @param context cast=(PangoContext *) */
public static final native int /*long*/ _pango_layout_new(int /*long*/ context);
public static final int /*long*/ pango_layout_new(int /*long*/ context) {
	lock.lock();
	try {
		return _pango_layout_new(context);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout *) */
public static final native void _pango_layout_set_alignment (int /*long*/ layout, int alignment);
public static final void pango_layout_set_alignment (int /*long*/ layout, int alignment) {
	lock.lock();
	try {
		_pango_layout_set_alignment(layout, alignment);
	} finally {
		lock.unlock();
	}
}
/**
 * @param layout cast=(PangoLayout *)
 * @param attrs cast=(PangoAttrList *)
 */
public static final native void _pango_layout_set_attributes(int /*long*/ layout, int /*long*/ attrs);
public static final void pango_layout_set_attributes(int /*long*/ layout, int /*long*/ attrs) {
	lock.lock();
	try {
		_pango_layout_set_attributes(layout, attrs);
	} finally {
		lock.unlock();
	}
}
/**
 * @param layout cast=(PangoLayout *)
 */
public static final native void _pango_layout_set_auto_dir(int /*long*/ layout, boolean auto_dir);
public static final void pango_layout_set_auto_dir(int /*long*/ layout, boolean auto_dir) {
	lock.lock();
	try {
		_pango_layout_set_auto_dir(layout, auto_dir);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(PangoLayout *)
 * @param descr cast=(PangoFontDescription *)
 */
public static final native void _pango_layout_set_font_description(int /*long*/ context, int /*long*/ descr);
public static final void pango_layout_set_font_description(int /*long*/ context, int /*long*/ descr) {
	lock.lock();
	try {
		_pango_layout_set_font_description(context, descr);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout*) */
public static final native void _pango_layout_set_indent(int /*long*/ layout, int indent);
public static final void pango_layout_set_indent(int /*long*/ layout, int indent) {
	lock.lock();
	try {
		_pango_layout_set_indent(layout, indent);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout*) */
public static final native void _pango_layout_set_justify(int /*long*/ layout, boolean justify);
public static final void pango_layout_set_justify(int /*long*/ layout, boolean justify) {
	lock.lock();
	try {
		_pango_layout_set_justify(layout, justify);
	} finally {
		lock.unlock();
	}
}
/**
 * @param context cast=(PangoLayout *)
 * @param setting cast=(gboolean)
 */
public static final native void _pango_layout_set_single_paragraph_mode(int /*long*/ context, boolean setting);
public static final void pango_layout_set_single_paragraph_mode(int /*long*/ context, boolean setting) {
	lock.lock();
	try {
		_pango_layout_set_single_paragraph_mode(context, setting);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout *) */
public static final native void _pango_layout_set_spacing(int /*long*/ layout, int spacing);
public static final void pango_layout_set_spacing(int /*long*/ layout, int spacing) {
	lock.lock();
	try {
		_pango_layout_set_spacing(layout, spacing);
	} finally {
		lock.unlock();
	}
}
/**
 * @param layout cast=(PangoLayout *)
 * @param tabs cast=(PangoTabArray *)
 */
public static final native void _pango_layout_set_tabs(int /*long*/ layout, int /*long*/ tabs);
public static final void pango_layout_set_tabs(int /*long*/ layout, int /*long*/ tabs) {
	lock.lock();
	try {
		_pango_layout_set_tabs(layout, tabs);
	} finally {
		lock.unlock();
	}
}
/**
 * @param layout cast=(PangoLayout *)
 * @param text cast=(const char *),flags=no_out critical
 * @param length cast=(int)
 */
public static final native void _pango_layout_set_text(int /*long*/ layout, byte[] text, int length);
public static final void pango_layout_set_text(int /*long*/ layout, byte[] text, int length) {
	lock.lock();
	try {
		_pango_layout_set_text(layout, text, length);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout *) */
public static final native void _pango_layout_set_width(int /*long*/ layout, int width);
public static final void pango_layout_set_width(int /*long*/ layout, int width) {
	lock.lock();
	try {
		_pango_layout_set_width(layout, width);
	} finally {
		lock.unlock();
	}
}
/** @param layout cast=(PangoLayout *) */
public static final native void _pango_layout_set_wrap (int /*long*/ layout, int wrap);
public static final void pango_layout_set_wrap (int /*long*/ layout, int wrap) {
	lock.lock();
	try {
		_pango_layout_set_wrap(layout, wrap);
	} finally {
		lock.unlock();
	}
}
/**
 * @param layout cast=(PangoLayout *)
 * @param index cast=(int *)
 * @param trailing cast=(int *)
 */
public static final native boolean _pango_layout_xy_to_index(int /*long*/ layout, int x, int y, int[] index, int[] trailing);
public static final boolean pango_layout_xy_to_index(int /*long*/ layout, int x, int y, int[] index, int[] trailing) {
	lock.lock();
	try {
		return _pango_layout_xy_to_index(layout, x, y, index, trailing);
	} finally {
		lock.unlock();
	}
}
/** @param tab_array cast=(PangoTabArray *) */
public static final native void _pango_tab_array_free(int /*long*/ tab_array);
public static final void pango_tab_array_free(int /*long*/ tab_array) {
	lock.lock();
	try {
		_pango_tab_array_free(tab_array);
	} finally {
		lock.unlock();
	}
}
/**
 * @param initial_size cast=(gint)
 * @param positions_in_pixels cast=(gboolean)
 */
public static final native int /*long*/ _pango_tab_array_new(int initial_size, boolean positions_in_pixels);
public static final int /*long*/ pango_tab_array_new(int initial_size, boolean positions_in_pixels) {
	lock.lock();
	try {
		return _pango_tab_array_new(initial_size, positions_in_pixels);
	} finally {
		lock.unlock();
	}
}
/**
 * @param tab_array cast=(PangoTabArray *)
 * @param tab_index cast=(gint)
 * @param alignment cast=(PangoTabAlign)
 * @param location cast=(gint)
 */
public static final native void _pango_tab_array_set_tab(int /*long*/ tab_array, int tab_index, int /*long*/ alignment, int location);
public static final void pango_tab_array_set_tab(int /*long*/ tab_array, int tab_index, int /*long*/ alignment, int location) {
	lock.lock();
	try {
		_pango_tab_array_set_tab(tab_array, tab_index, alignment, location);
	} finally {
		lock.unlock();
	}
}
/**
 * @method flags=dynamic
 */
public static final native int /*long*/_ubuntu_menu_proxy_get();
public static final int /*long*/ ubuntu_menu_proxy_get() {
	lock.lock();
	try {
		return _ubuntu_menu_proxy_get();
	} finally {
		lock.unlock();
	}
}
/**
 * @param path cast=(const char*)
 */
public static final native int _access (byte [] path, int amode);
public static final int access (byte [] path, int amode) {
	lock.lock();
	try {
		return _access(path, amode);
	} finally {
		lock.unlock();
	}
}
/**
 * @param s1 cast=(const char*)
 * @param s2 cast=(const char*)
 */
public static final native int strcmp (int /*long*/ s1, byte [] s2);

/**
 * Theme name as given by OS.
 * You can see the exact theme name via Tweak Tools -> Appearance -> Themes.
 * E.g
 * 		Adwaita
 * 		Adwaita-Dark
 * 		Ambiance 		(Ubuntu).
 *
 * See also: Device.overrideThemeValues();
 */
public static final String getThemeName() {
	byte[] themeNameBytes = getThemeNameBytes();
	String themeName = "unknown";
	if (themeNameBytes != null && themeNameBytes.length > 0) {
		themeName = new String (Converter.mbcsToWcs (themeNameBytes));
	}
	return themeName;
}

public static final byte [] getThemeNameBytes() {
	byte [] buffer = null;
	int length;
	int /*long*/ settings = GTK.gtk_settings_get_default ();
	int /*long*/ [] ptr = new int /*long*/ [1];
	OS.g_object_get (settings, GTK.gtk_theme_name, ptr, 0);
	if (ptr [0] == 0) {
		return buffer;
	}
	length = C.strlen (ptr [0]);
	if (length == 0) {
		return buffer;
	}
	buffer = new byte [length];
	C.memmove (buffer, ptr [0], length);
	OS.g_free (ptr [0]);
	return buffer;
}

/**
 * Hint GTK 3 to natively prefer a dark or light theme.
 * <p>
 * Note: This method gets called from the org.eclipse.e4.ui.swt.gtk fragment.
 * </p>
 *
 * @since 3.104
 */
public static final void setDarkThemePreferred(boolean preferred){
	if (!GTK.GTK3) return; //only applicable to GTK3
	g_object_set(GTK.gtk_settings_get_default(), GTK.gtk_application_prefer_dark_theme,
			preferred, 0);
	g_object_notify(GTK.gtk_settings_get_default(),
			GTK.gtk_application_prefer_dark_theme);
}

/**
 * @param info cast=(GDBusInterfaceInfo *)
 * @param name cast=(const gchar *)
 * @param object_path cast=(const gchar *)
 * @param interface_name cast=(const gchar *)
 * @param cancellable cast=(GCancellable *)
 * @param error cast=(GError **)
 * @category gdbus
 */
public static final native int /*long*/ _g_dbus_proxy_new_for_bus_sync (int bus_type, int flags, int /*long*/ info, byte [] name, byte [] object_path, byte [] interface_name,
		int /*long*/ cancellable, int /*long*/[] error);
public static final int /*long*/ g_dbus_proxy_new_for_bus_sync (int bus_type, int flags, int /*long*/ info, byte [] name, byte [] object_path, byte [] interface_name,
		int /*long*/ cancellable, int /*long*/[] error) {
  lock.lock();
  try {
    return _g_dbus_proxy_new_for_bus_sync (bus_type, flags, info, name, object_path, interface_name, cancellable, error);
  } finally {
    lock.unlock();
  }
}

/**
 * @param proxy cast=(GDBusProxy *)
 * @param method_name cast=(const gchar *)
 * @param parameters cast=(GVariant *)
 * @param cancellable cast=(GCancellable *)
 * @param error cast=(GError **)
 * @category gdbus
 */
public static final native int /*long*/ _g_dbus_proxy_call_sync (int /*long*/ proxy, byte[] method_name, int /*long*/ parameters, int flags, int timeout_msec, int /*long*/ cancellable, int /*long*/[] error);
public static final int /*long*/ g_dbus_proxy_call_sync (int /*long*/ proxy, byte[] method_name, int /*long*/ parameters, int flags, int timeout_msec, int /*long*/ cancellable, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_dbus_proxy_call_sync (proxy, method_name, parameters, flags, timeout_msec, cancellable, error);
	} finally {
		lock.unlock();
	}
}

/**
 * @method flags=dynamic
 * @param proxy cast=(GDBusProxy *)
 * @param method_name cast=(const gchar *)
 * @param parameters cast=(GVariant *)
 * @param cancellable cast=(GCancellable *)
 * @param error cast=(GError **)
 * @category gdbus
 */
public static final native void _g_dbus_proxy_call (int /*long*/ proxy, byte[] method_name, int /*long*/ parameters, int flags, int timeout_msec, int /*long*/ cancellable, int /*long*/ callback, int /*long*/[] error);
public static final void g_dbus_proxy_call (int /*long*/ proxy, byte[] method_name, int /*long*/ parameters, int flags, int timeout_msec, int /*long*/ cancellable, int /*long*/ callback, int /*long*/[] error) {
	lock.lock();
	try {
		_g_dbus_proxy_call (proxy, method_name, parameters, flags, timeout_msec, cancellable, callback, error);
	} finally {
		lock.unlock();
	}
}

/**
 * @param proxy cast=(GDBusProxy *)
 * @param res cast=(GAsyncResult *)
 * @param error cast=(GError **)
 * @category gdbus
 */
public static final native int /*long*/ _g_dbus_proxy_call_finish (int /*long*/ proxy, int /*long*/ res, int /*long*/[] error);
public static final int /*long*/ g_dbus_proxy_call_finish (int /*long*/ proxy, int /*long*/ res, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_dbus_proxy_call_finish (proxy, res, error);
	} finally {
		lock.unlock();
	}
}

/**
 * @param xml_data cast=(const gchar *)
 * @param error cast=(GError **)
 * @category gdbus
 */
public static final native int /*long*/ _g_dbus_node_info_new_for_xml (byte[] xml_data, int /*long*/[] error);
/** @category gdbus */
public static final int /*long*/ g_dbus_node_info_new_for_xml (byte[] xml_data, int /*long*/[] error) {
  lock.lock();
  try {
    return _g_dbus_node_info_new_for_xml (xml_data, error);
  } finally {
    lock.unlock();
  }
}

/**
 * @param bus_type cast=(GBusType)
 * @param name cast=(const gchar *)
 * @param flags cast=(GBusNameOwnerFlags)
 * @param bus_acquired_handler cast=(GBusAcquiredCallback)
 * @param name_acquired_handler cast=(GBusNameAcquiredCallback)
 * @param name_lost_handler cast=(GBusNameLostCallback)
 * @param user_data cast=(gpointer)
 * @param user_data_free_func cast=(GDestroyNotify)
 * @category gdbus
 */
public static final native int _g_bus_own_name (int bus_type, byte[] name, int flags, int /*long*/ bus_acquired_handler, int /*long*/ name_acquired_handler, int /*long*/ name_lost_handler, int /*long*/  user_data, int /*long*/ user_data_free_func);
/** @category gdbus */
public static final int g_bus_own_name (int bus_type, byte[] name, int flags, int /*long*/ bus_acquired_handler, int /*long*/ name_acquired_handler, int /*long*/ name_lost_handler, int /*long*/  user_data, int /*long*/ user_data_free_func) {
	lock.lock();
	try {
		return _g_bus_own_name(bus_type, name, flags, bus_acquired_handler, name_acquired_handler, name_lost_handler, user_data, user_data_free_func);
	} finally {
		lock.unlock();
	}
}

/**
 * @param connection cast=(GDBusConnection *)
 * @param object_path cast=(const gchar *)
 * @param interface_info cast=(GDBusInterfaceInfo *)
 * @param vtable cast=(const GDBusInterfaceVTable *)
 * @param user_data cast=(gpointer)
 * @param user_data_free_func cast=(GDestroyNotify)
 * @param error cast=(GError **)
 * @category gdbus
 */
public static final native int _g_dbus_connection_register_object (int /*long*/ connection, byte[] object_path, int /*long*/ interface_info, int /*long*/[] vtable, int /*long*/ user_data, int /*long*/ user_data_free_func, int /*long*/[] error);
/** @category gdbus */
public static final int g_dbus_connection_register_object (int /*long*/ connection, byte[] object_path, int /*long*/ interface_info, int /*long*/[] vtable, int /*long*/ user_data, int /*long*/ user_data_free_func, int /*long*/[] error) {
	lock.lock();
	try {
		return _g_dbus_connection_register_object( connection,  object_path,  interface_info,  vtable,  user_data,  user_data_free_func, error);
	} finally {
		lock.unlock();
	}
}

/**
 * @param info cast=(GDBusNodeInfo *)
 * @param name cast=(const gchar *)
 * @category gdbus
 */
public static final native int /*long*/ _g_dbus_node_info_lookup_interface (int /*long*/ info, byte [] name);
/** @category gdbus */
public static final int /*long*/ g_dbus_node_info_lookup_interface (int /*long*/ info, byte [] name) {
	lock.lock();
	try {
		return _g_dbus_node_info_lookup_interface(info, name);
	} finally {
		lock.unlock();
	}
}

/**
 * @param invocation cast=(GDBusMethodInvocation *)
 * @param parameters cast=(GVariant *)
 * @category gdbus
 */
public static final native void _g_dbus_method_invocation_return_value (int /*long*/ invocation, int /*long*/ parameters);
/** @category gdbus */
public static final void g_dbus_method_invocation_return_value (int /*long*/ invocation, int /*long*/ parameters) {
	lock.lock();
	try {
		_g_dbus_method_invocation_return_value (invocation, parameters);
	} finally {
		lock.unlock();
	}
}

/**
 * @param type cast=(const GVariantType *)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_builder_new (int /*long*/ type);
/** @category gdbus */
public static final int /*long*/ g_variant_builder_new (int /*long*/ type) {
	lock.lock();
	try {
		return _g_variant_builder_new(type);
	} finally {
		lock.unlock();
	}
}

/**
 * @param builder cast=(GVariantBuilder *)
 * @param value cast=(GVariant *)
 * @category gdbus
 */
public static final native void /*int*/ _g_variant_builder_add_value (int /*long*/ builder, int /*long*/ value);
/** @category gdbus */
public static final void /*int*/ g_variant_builder_add_value (int /*long*/ builder, int /*long*/ value) {
	lock.lock();
	try {
		_g_variant_builder_add_value(builder, value);
	} finally {
		lock.unlock();
	}
}

/**
 * @param type cast=(const gchar *)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_type_new (byte [] type);
/** @category gdbus */
public static final int /*long*/ g_variant_type_new (byte [] type) {
	lock.lock();
	try {
		return _g_variant_type_new(type);
	} finally {
		lock.unlock();
	}
}

/**
 * @param builder cast=(GVariantBuilder *)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_builder_end (int /*long*/ builder);
/** @category gdbus */
public static final int /*long*/ g_variant_builder_end (int /*long*/ builder) {
	lock.lock();
	try {
		return _g_variant_builder_end(builder);
	} finally {
		lock.unlock();
	}
}

/**
 * @param builder cast=(GVariantBuilder *)
 * @category gdbus
 */
public static final native void /*int*/ _g_variant_builder_unref (int /*long*/ builder);
/** @category gdbus */
public static final void /*int*/ g_variant_builder_unref (int /*long*/ builder) {
	lock.lock();
	try {
		_g_variant_builder_unref(builder);
	} finally {
		lock.unlock();
	}
}

/**
 * @param intval cast=(gint32)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_new_int32 (int intval);
/** @category gdbus */
public static final int /*long*/ g_variant_new_int32 (int intval) {
	lock.lock();
	try {
		return _g_variant_new_int32(intval);
	} finally {
		lock.unlock();
	}
}


/**
 * @param gvariant cast=(GVariant *)
 * @category gdbus
 * @return int
 */
public static final native int _g_variant_get_int32 (int /*long*/ gvariant);
/** @category gdbus */
public static final int g_variant_get_int32 (int /*long*/ gvariant) {
	lock.lock();
	try {
		return _g_variant_get_int32 (gvariant);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @category gdbus
 * @return guchar
 */
public static final native byte _g_variant_get_byte (int /*long*/ gvariant);
/** @category gdbus */
public static final byte g_variant_get_byte (int /*long*/ gvariant) {
	lock.lock();
	try {
		return _g_variant_get_byte (gvariant);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @category gdbus
 */
public static final native boolean /*int*/ _g_variant_get_boolean (int /*long*/ gvariant);
/** @category gdbus */
public static final boolean /*int*/ g_variant_get_boolean (int /*long*/ gvariant) {
	lock.lock();
	try {
		return _g_variant_get_boolean (gvariant);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @param index cast=(gsize)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_get_child_value (int /*long*/ gvariant, int index);
/** @category gdbus */
public static final int /*long*/ g_variant_get_child_value (int /*long*/ gvariant, int index) {
	lock.lock();
	try {
		return _g_variant_get_child_value (gvariant, index);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @category gdbus
 */
public static final native double _g_variant_get_double (int /*long*/ gvariant);
/** @category gdbus */
public static final double g_variant_get_double (int /*long*/ gvariant) {
	lock.lock();
	try {
		return _g_variant_get_double (gvariant);
	} finally {
		lock.unlock();
	}
}

public static final native int /*long*/ _g_variant_new_uint64 (long value);
public static final int /*long*/ g_variant_new_uint64 (long value) {
	lock.lock();
	try {
		return _g_variant_new_uint64 (value);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @category gdbus
 */
public static final native long _g_variant_get_uint64 (int /*long*/ gvariant);
/** @category gdbus */
public static final long g_variant_get_uint64 (int /*long*/ gvariant) {
	lock.lock();
	try {
		return _g_variant_get_uint64 (gvariant);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @param length cast=(gsize *)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_get_string (int /*long*/ gvariant, long[] length);
/** @category gdbus */
public static final int /*long*/ g_variant_get_string (int /*long*/ gvariant, long[] length) {
	lock.lock();
	try {
		return _g_variant_get_string (gvariant, length);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @return const GVariantType *
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_get_type (int /*long*/ gvariant);
/** @category gdbus */
public static final int /*long*/ g_variant_get_type (int /*long*/ gvariant) {
	lock.lock();
	try {
		return _g_variant_get_type (gvariant);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @category gdbus
 */
public static final native int /*long*/  _g_variant_get_type_string (int /*long*/ gvariant);
/** @category gdbus */
public static final int /*long*/ g_variant_get_type_string (int /*long*/ gvariant) {
	lock.lock();
	try {
		return _g_variant_get_type_string (gvariant);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @param type cast=(const GVariantType *)
 * @category gdbus
 */
public static final native boolean _g_variant_is_of_type (int /*long*/ gvariant, byte[] type);
/** @category gdbus */
public static final boolean g_variant_is_of_type (int /*long*/ gvariant, byte[] type) {
	lock.lock();
	try {
		return _g_variant_is_of_type (gvariant, type);
	} finally {
		lock.unlock();
	}
}

/**
 * @param gvariant cast=(GVariant *)
 * @category gdbus
 */
public static final native long _g_variant_n_children (long gvariant);
/** @category gdbus */
public static final long g_variant_n_children (long gvariant) {
	lock.lock();
	try {
		return _g_variant_n_children (gvariant);
	} finally {
		lock.unlock();
	}
}

/**
 * @param value cast=(gboolean)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_new_boolean (boolean value);
/** @category gdbus */
public static final int /*long*/ g_variant_new_boolean (boolean value) {
	lock.lock();
	try {
		return _g_variant_new_boolean (value);
	} finally {
		lock.unlock();
	}
}

/**
 * @param value cast=(gboolean)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_new_double (double value);
/** @category gdbus */
public static final int /*long*/ g_variant_new_double (double value) {
	lock.lock();
	try {
		return _g_variant_new_double (value);
	} finally {
		lock.unlock();
	}
}

/**
 * @param value cast=(guchar)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_new_byte (byte value);
/** @category gdbus */
public static final int /*long*/ g_variant_new_byte (byte value) {
	lock.lock();
	try {
		return _g_variant_new_byte (value);
	} finally {
		lock.unlock();
	}
}

/**
 * @param items cast=(GVariant * const *)
 * @param length cast=(gsize)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_new_tuple (int /*long*/[] items, long length);
/** @category gdbus */
public static final int /*long*/ g_variant_new_tuple (int /*long*/[] items, long length ) {
	lock.lock();
	try {
		return _g_variant_new_tuple (items, length);
	} finally {
		lock.unlock();
	}
}

/**
 * @param string cast=(const gchar *)
 * @category gdbus
 */
public static final native int /*long*/ _g_variant_new_string (byte[] string);
/** @category gdbus */
public static final int /*long*/ g_variant_new_string (byte[] string) {
	lock.lock();
	try {
		return _g_variant_new_string (string);
	} finally {
		lock.unlock();
	}
}

/**
 * @param object cast=(GObject *)
 */
public static final native int /*long*/ _g_object_ref_sink(int /*long*/ object);

public static final int /*long*/ g_object_ref_sink(int /*long*/ object) {
	lock.lock();
	try {
		return _g_object_ref_sink(object);
	} finally {
		lock.unlock();
	}
}
}
