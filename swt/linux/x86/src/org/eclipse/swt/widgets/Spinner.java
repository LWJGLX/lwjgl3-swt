/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.widgets;


import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * Instances of this class are selectable user interface
 * objects that allow the user to enter and modify numeric
 * values.
 * <p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to add children to it, or set a layout on it.
 * </p><p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>READ_ONLY, WRAP</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection, Modify, Verify</dd>
 * </dl>
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 *
 * @see <a href="http://www.eclipse.org/swt/snippets/#spinner">Spinner snippets</a>
 * @see <a href="http://www.eclipse.org/swt/examples.php">SWT Example: ControlExample</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 *
 * @since 3.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class Spinner extends Composite {
	static final int MIN_ARROW_WIDTH = 6;
	int lastEventTime = 0;
	int /*long*/ imContext;
	int /*long*/ gdkEventKey = 0;
	int fixStart = -1, fixEnd = -1;
	double climbRate = 1;

	/**
	 * the operating system limit for the number of characters
	 * that the text field in an instance of this class can hold
	 *
	 * @since 3.4
	 */
	public final static int LIMIT;
	/*
	* These values can be different on different platforms.
	* Therefore they are not initialized in the declaration
	* to stop the compiler from inlining.
	*/
	static {
		LIMIT = 0xFFFF;
	}
	/* Spinner uses non-standard CSS to set its background color, so we need
	 * a global variable to keep track of its background color.
	 */
	GdkRGBA background;

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a composite control which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT#READ_ONLY
 * @see SWT#WRAP
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public Spinner (Composite parent, int style) {
	super (parent, checkStyle (style));
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the receiver's text is modified, by sending
 * it one of the messages defined in the <code>ModifyListener</code>
 * interface.
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see ModifyListener
 * @see #removeModifyListener
 */
public void addModifyListener (ModifyListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Modify, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the control is selected by the user, by sending
 * it one of the messages defined in the <code>SelectionListener</code>
 * interface.
 * <p>
 * <code>widgetSelected</code> is not called for texts.
 * <code>widgetDefaultSelected</code> is typically called when ENTER is pressed in a single-line text.
 * </p>
 *
 * @param listener the listener which should be notified when the control is selected by the user
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #removeSelectionListener
 * @see SelectionEvent
 */
public void addSelectionListener(SelectionListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener(listener);
	addListener(SWT.Selection,typedListener);
	addListener(SWT.DefaultSelection,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the receiver's text is verified, by sending
 * it one of the messages defined in the <code>VerifyListener</code>
 * interface.
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see VerifyListener
 * @see #removeVerifyListener
 */
void addVerifyListener (VerifyListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Verify, typedListener);
}

static int checkStyle (int style) {
	/*
	* Even though it is legal to create this widget
	* with scroll bars, they serve no useful purpose
	* because they do not automatically scroll the
	* widget's client area.  The fix is to clear
	* the SWT style.
	*/
	return style & ~(SWT.H_SCROLL | SWT.V_SCROLL);
}

@Override
protected void checkSubclass () {
	if (!isValidSubclass ()) error (SWT.ERROR_INVALID_SUBCLASS);
}

@Override
Point computeSizeInPixels (int wHint, int hHint, boolean changed) {
	checkWidget ();
	if (wHint != SWT.DEFAULT && wHint < 0) wHint = 0;
	if (hHint != SWT.DEFAULT && hHint < 0) hHint = 0;
	int[] w = new int [1], h = new int [1];
	GTK.gtk_widget_realize (handle);
	int /*long*/ layout = GTK.gtk_entry_get_layout (handle);
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	double upper = GTK.gtk_adjustment_get_upper (hAdjustment);
	int digits = GTK.gtk_spin_button_get_digits (handle);
	for (int i = 0; i < digits; i++) upper *= 10;
	String string = String.valueOf ((int) upper);
	if (digits > 0) {
		StringBuilder buffer = new StringBuilder ();
		buffer.append (string);
		buffer.append (getDecimalSeparator ());
		int count = digits - string.length ();
		while (count >= 0) {
			buffer.append ("0");
			count--;
		}
		string = buffer.toString ();
	}
	byte [] buffer1 = Converter.wcsToMbcs (string, false);
	int /*long*/ ptr = OS.pango_layout_get_text (layout);
	int length = C.strlen (ptr);
	byte [] buffer2 = new byte [length];
	C.memmove (buffer2, ptr, length);
	OS.pango_layout_set_text (layout, buffer1, buffer1.length);
	int width, height = 0 ;
	GTK.gtk_widget_realize (handle);
	if (GTK.GTK3) {
		GTK.gtk_widget_set_size_request (handle, wHint, hHint);
		GtkRequisition requisition = new GtkRequisition ();
		GTK.gtk_widget_get_preferred_size (handle, requisition, null);
		width = wHint == SWT.DEFAULT ? requisition.width : Math.max(wHint, requisition.width);
		height = hHint == SWT.DEFAULT ? requisition.height : hHint;
	} else {
		OS.pango_layout_get_pixel_size (layout, w, h);
		width = wHint == SWT.DEFAULT ? w [0] : wHint;
		height = hHint == SWT.DEFAULT ? h [0] : hHint;
	}
	OS.pango_layout_set_text (layout, buffer2, buffer2.length);
	Rectangle trim = computeTrimInPixels (0, 0, width, height);
	return new Point (trim.width, trim.height);
}

@Override
Rectangle computeTrimInPixels (int x, int y, int width, int height) {
	checkWidget ();
	int xborder = 0, yborder = 0;
	Rectangle trim = super.computeTrimInPixels (x, y, width, height);
	if (GTK.GTK3) {
		GtkBorder tmp = new GtkBorder();
		int /*long*/ context = GTK.gtk_widget_get_style_context (handle);
		if (GTK.GTK_VERSION < OS.VERSION(3, 18, 0)) {
			GTK.gtk_style_context_get_padding (context, GTK.GTK_STATE_FLAG_NORMAL, tmp);
		} else {
			GTK.gtk_style_context_get_padding (context, GTK.gtk_widget_get_state_flags(handle), tmp);
		}
		if ((style & SWT.BORDER) != 0) {
			if (GTK.GTK_VERSION < OS.VERSION(3, 18, 0)) {
				GTK.gtk_style_context_get_border (context, GTK.GTK_STATE_FLAG_NORMAL, tmp);
			} else {
				GTK.gtk_style_context_get_border (context, GTK.gtk_widget_get_state_flags(handle), tmp);
			}
			trim.x -= tmp.left;
			trim.y -= tmp.top;
			trim.width += tmp.left + tmp.right;
			trim.height += tmp.top + tmp.bottom;
		}
	}else {
		Point thickness = getThickness (handle);
		if ((this.style & SWT.BORDER) != 0) {
			xborder += thickness.x;
			yborder += thickness.y;
		}
		int /*long*/ fontDesc = getFontDescription ();
		int fontSize = OS.pango_font_description_get_size (fontDesc);
		int arrowSize = Math.max (OS.PANGO_PIXELS (fontSize), MIN_ARROW_WIDTH);
		arrowSize = arrowSize - arrowSize % 2;
		trim.width += arrowSize + (2 * thickness.x);
	}
	int [] property = new int [1];
	GTK.gtk_widget_style_get (handle, OS.interior_focus, property, 0);
	if (property [0] == 0) {
		GTK.gtk_widget_style_get (handle, OS.focus_line_width, property, 0);
		xborder += property [0];
		yborder += property [0];
	}
	trim.x -= xborder;
	trim.y -= yborder;
	trim.width += 2 * xborder;
	trim.height += 2 * yborder;
	GtkBorder innerBorder = Display.getEntryInnerBorder (handle);
	trim.x -= innerBorder.left;
	trim.y -= innerBorder.top;
	trim.width += innerBorder.left + innerBorder.right;
	trim.height += innerBorder.top + innerBorder.bottom;
	return new Rectangle (trim.x, trim.y, trim.width, trim.height);
}

/**
 * Copies the selected text.
 * <p>
 * The current selection is copied to the clipboard.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void copy () {
	checkWidget ();
	GTK.gtk_editable_copy_clipboard (handle);
}

@Override
void createHandle (int index) {
	state |= HANDLE | MENU;
	fixedHandle = OS.g_object_new (display.gtk_fixed_get_type (), 0);
	if (fixedHandle == 0) error (SWT.ERROR_NO_HANDLES);
	GTK.gtk_widget_set_has_window (fixedHandle, true);
	int /*long*/ adjustment = GTK.gtk_adjustment_new (0, 0, 100, 1, 10, 0);
	if (adjustment == 0) error (SWT.ERROR_NO_HANDLES);
	handle = GTK.gtk_spin_button_new (adjustment, climbRate, 0);
	if (handle == 0) error (SWT.ERROR_NO_HANDLES);
	GTK.gtk_container_add (fixedHandle, handle);
	GTK.gtk_editable_set_editable (handle, (style & SWT.READ_ONLY) == 0);
	if (GTK.GTK_VERSION <= OS.VERSION(3, 20, 0)) {
		GTK.gtk_entry_set_has_frame (handle, (style & SWT.BORDER) != 0);
	}
	GTK.gtk_spin_button_set_wrap (handle, (style & SWT.WRAP) != 0);
	if (GTK.GTK3) {
		imContext = OS.imContextLast();
	}
	// In GTK 3 font description is inherited from parent widget which is not how SWT has always worked,
	// reset to default font to get the usual behavior
	if (GTK.GTK3) {
		setFontDescription(defaultFont().handle);
	}
}

/**
 * Cuts the selected text.
 * <p>
 * The current selection is first copied to the
 * clipboard and then deleted from the widget.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void cut () {
	checkWidget ();
	GTK.gtk_editable_cut_clipboard (handle);
}

@Override
GdkRGBA defaultBackground () {
	return display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).handleRGBA;
}

@Override
void deregister () {
	super.deregister ();
	int /*long*/ imContext = imContext ();
	if (imContext != 0) display.removeWidget (imContext);
}

@Override
int /*long*/ eventWindow () {
	return paintWindow ();
}

@Override
int /*long*/ enterExitHandle () {
	return fixedHandle;
}

@Override
boolean filterKey (int keyval, int /*long*/ event) {
	int time = GDK.gdk_event_get_time (event);
	if (time != lastEventTime) {
		lastEventTime = time;
		int /*long*/ imContext = imContext ();
		if (imContext != 0) {
			return GTK.gtk_im_context_filter_keypress (imContext, event);
		}
	}
	gdkEventKey = event;
	return false;
}

void fixIM () {
	/*
	*  The IM filter has to be called one time for each key press event.
	*  When the IM is open the key events are duplicated. The first event
	*  is filtered by SWT and the second event is filtered by GTK.  In some
	*  cases the GTK handler does not run (the widget is destroyed, the
	*  application code consumes the event, etc), for these cases the IM
	*  filter has to be called by SWT.
	*/
	if (gdkEventKey != 0 && gdkEventKey != -1) {
		int /*long*/ imContext = imContext ();
		if (imContext != 0) {
			GTK.gtk_im_context_filter_keypress (imContext, gdkEventKey);
			gdkEventKey = -1;
			return;
		}
	}
	gdkEventKey = 0;
}

@Override
GdkColor getBackgroundGdkColor () {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	return getBaseGdkColor ();
}

@Override
int getBorderWidthInPixels () {
	checkWidget();
	if ((this.style & SWT.BORDER) != 0) {
		return getThickness (handle).x;
	}
	return 0;
}

@Override
GdkColor getForegroundGdkColor () {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	return getTextColor ();
}

/**
 * Returns the amount that the receiver's value will be
 * modified by when the up/down arrows are pressed.
 *
 * @return the increment
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getIncrement () {
	checkWidget ();
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	int digits = GTK.gtk_spin_button_get_digits (handle);
	double value = GTK.gtk_adjustment_get_step_increment (hAdjustment);
	for (int i = 0; i < digits; i++) value *= 10;
	return (int) (value > 0 ? value + 0.5 : value - 0.5);
}

/**
 * Returns the maximum value which the receiver will allow.
 *
 * @return the maximum
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getMaximum () {
	checkWidget ();
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	int digits = GTK.gtk_spin_button_get_digits (handle);
	double value = GTK.gtk_adjustment_get_upper (hAdjustment);
	for (int i = 0; i < digits; i++) value *= 10;
	return (int) (value > 0 ? value + 0.5 : value - 0.5);
}

/**
 * Returns the minimum value which the receiver will allow.
 *
 * @return the minimum
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getMinimum () {
	checkWidget ();
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	int digits = GTK.gtk_spin_button_get_digits (handle);
	double value = GTK.gtk_adjustment_get_lower (hAdjustment);
	for (int i = 0; i < digits; i++) value *= 10;
	return (int) (value > 0 ? value + 0.5 : value - 0.5);
}

/**
 * Returns the amount that the receiver's position will be
 * modified by when the page up/down keys are pressed.
 *
 * @return the page increment
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getPageIncrement () {
	checkWidget ();
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	int digits = GTK.gtk_spin_button_get_digits (handle);
	double value = GTK.gtk_adjustment_get_page_increment (hAdjustment);
	for (int i = 0; i < digits; i++) value *= 10;
	return (int) (value > 0 ? value + 0.5 : value - 0.5);
}

/**
 * Returns the <em>selection</em>, which is the receiver's position.
 *
 * @return the selection
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getSelection () {
	checkWidget ();
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	int digits = GTK.gtk_spin_button_get_digits (handle);
	double value = GTK.gtk_adjustment_get_value (hAdjustment);
	for (int i = 0; i < digits; i++) value *= 10;
	return (int) (value > 0 ? value + 0.5 : value - 0.5);
}

/**
 * Returns a string containing a copy of the contents of the
 * receiver's text field, or an empty string if there are no
 * contents.
 *
 * @return the receiver's text
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.4
 */
public String getText () {
	checkWidget ();
	int /*long*/ str = GTK.gtk_entry_get_text (handle);
	if (str == 0) return "";
	int length = C.strlen (str);
	byte [] buffer = new byte [length];
	C.memmove (buffer, str, length);
	return new String (Converter.mbcsToWcs (buffer));
}

/**
 * Returns the maximum number of characters that the receiver's
 * text field is capable of holding. If this has not been changed
 * by <code>setTextLimit()</code>, it will be the constant
 * <code>Spinner.LIMIT</code>.
 *
 * @return the text limit
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #LIMIT
 *
 * @since 3.4
 */
public int getTextLimit () {
	checkWidget ();
	int limit = GTK.gtk_entry_get_max_length (handle);
	return limit == 0 ? LIMIT : limit;
}

/**
 * Returns the number of decimal places used by the receiver.
 *
 * @return the digits
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getDigits () {
	checkWidget ();
	return GTK.gtk_spin_button_get_digits (handle);
}

String getDecimalSeparator () {
	int /*long*/ ptr = OS.localeconv_decimal_point ();
	int length = C.strlen (ptr);
	byte [] buffer = new byte [length];
	C.memmove (buffer, ptr, length);
	return new String (Converter.mbcsToWcs (buffer));
}

@Override
int /*long*/ gtk_activate (int /*long*/ widget) {
	sendSelectionEvent (SWT.DefaultSelection);
	return 0;
}

@Override
int /*long*/ gtk_changed (int /*long*/ widget) {
	int /*long*/ str = GTK.gtk_entry_get_text (handle);
	int length = C.strlen (str);
	if (length > 0) {
		int /*long*/ [] endptr = new int /*long*/ [1];
		double value = OS.g_strtod (str, endptr);
		int valueLength = (getDigits() == 0) ? String.valueOf((int)value).length() : String.valueOf(value).length();
		if ((endptr [0] == str + length) && valueLength == length) {
			int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
			GtkAdjustment adjustment = new GtkAdjustment ();
			gtk_adjustment_get (hAdjustment, adjustment);
			if (value != adjustment.value && adjustment.lower <= value && value <= adjustment.upper) {
				GTK.gtk_spin_button_update (handle);
			}
		}
	}

	/*
	* Feature in GTK.  When the user types, GTK positions
	* the caret after sending the changed signal.  This
	* means that application code that attempts to position
	* the caret during a changed signal will fail.  The fix
	* is to post the modify event when the user is typing.
	*/
	boolean keyPress = false;
	int /*long*/ eventPtr = GTK.gtk_get_current_event ();
	if (eventPtr != 0) {
		GdkEventKey gdkEvent = new GdkEventKey ();
		OS.memmove (gdkEvent, eventPtr, GdkEventKey.sizeof);
		switch (gdkEvent.type) {
			case GDK.GDK_KEY_PRESS:
				keyPress = true;
				break;
		}
		GDK.gdk_event_free (eventPtr);
	}
	if (keyPress) {
		postEvent (SWT.Modify);
	} else {
		sendEvent (SWT.Modify);
	}
	return 0;
}

@Override
int /*long*/ gtk_commit (int /*long*/ imContext, int /*long*/ text) {
	if (text == 0) return 0;
	if (!GTK.gtk_editable_get_editable (handle)) return 0;
	int length = C.strlen (text);
	if (length == 0) return 0;
	byte [] buffer = new byte [length];
	C.memmove (buffer, text, length);
	char [] chars = Converter.mbcsToWcs (buffer);
	char [] newChars = sendIMKeyEvent (SWT.KeyDown, null, chars);
	if (newChars == null) return 0;
	/*
	* Feature in GTK.  For a GtkEntry, during the insert-text signal,
	* GTK allows the programmer to change only the caret location,
	* not the selection.  If the programmer changes the selection,
	* the new selection is lost.  The fix is to detect a selection
	* change and set it after the insert-text signal has completed.
	*/
	fixStart = fixEnd = -1;
	OS.g_signal_handlers_block_matched (imContext, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, COMMIT);
	int id = OS.g_signal_lookup (OS.commit, GTK.gtk_im_context_get_type ());
	int mask =  OS.G_SIGNAL_MATCH_DATA | OS.G_SIGNAL_MATCH_ID;
	OS.g_signal_handlers_unblock_matched (imContext, mask, id, 0, 0, 0, handle);
	if (newChars == chars) {
		OS.g_signal_emit_by_name (imContext, OS.commit, text);
	} else {
		buffer = Converter.wcsToMbcs (newChars, true);
		OS.g_signal_emit_by_name (imContext, OS.commit, buffer);
	}
	OS.g_signal_handlers_unblock_matched (imContext, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, COMMIT);
	OS.g_signal_handlers_block_matched (imContext, mask, id, 0, 0, 0, handle);
	if (fixStart != -1 && fixEnd != -1) {
		GTK.gtk_editable_set_position (handle, fixStart);
		GTK.gtk_editable_select_region (handle, fixStart, fixEnd);
	}
	fixStart = fixEnd = -1;
	return 0;
}

@Override
int /*long*/ gtk_delete_text (int /*long*/ widget, int /*long*/ start_pos, int /*long*/ end_pos) {
	if (!hooks (SWT.Verify) && !filters (SWT.Verify)) return 0;
	int /*long*/ ptr = GTK.gtk_entry_get_text (handle);
	if (end_pos == -1) end_pos = OS.g_utf8_strlen (ptr, -1);
	int start = (int)/*64*/OS.g_utf8_offset_to_utf16_offset (ptr, start_pos);
	int end = (int)/*64*/OS.g_utf8_offset_to_utf16_offset (ptr, end_pos);
	String newText = verifyText ("", start, end);
	if (newText == null) {
		OS.g_signal_stop_emission_by_name (handle, OS.delete_text);
	} else {
		if (newText.length () > 0) {
			int [] pos = new int [1];
			pos [0] = (int)/*64*/end_pos;
			byte [] buffer = Converter.wcsToMbcs (newText, false);
			OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, CHANGED);
			OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, INSERT_TEXT);
			GTK.gtk_editable_insert_text (handle, buffer, buffer.length, pos);
			OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, INSERT_TEXT);
			OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, CHANGED);
			GTK.gtk_editable_set_position (handle, pos [0]);
		}
	}
	return 0;
}

@Override
int /*long*/ gtk_event_after (int /*long*/ widget, int /*long*/ gdkEvent) {
	if (cursor != null) setCursor (cursor.handle);
	return super.gtk_event_after (widget, gdkEvent);
}

@Override
int /*long*/ gtk_focus_out_event (int /*long*/ widget, int /*long*/ event) {
	fixIM ();
	return super.gtk_focus_out_event (widget, event);
}

@Override
int /*long*/ gtk_insert_text (int /*long*/ widget, int /*long*/ new_text, int /*long*/ new_text_length, int /*long*/ position) {
//	if (!hooks (SWT.Verify) && !filters (SWT.Verify)) return 0;
	if (new_text == 0 || new_text_length == 0) return 0;
	byte [] buffer = new byte [(int)/*64*/new_text_length];
	C.memmove (buffer, new_text, buffer.length);
	String oldText = new String (Converter.mbcsToWcs (buffer));
	int [] pos = new int [1];
	C.memmove (pos, position, 4);
	int /*long*/ ptr = GTK.gtk_entry_get_text (handle);
	if (pos [0] == -1) pos [0] = (int)/*64*/OS.g_utf8_strlen (ptr, -1);
	int start = (int)/*64*/OS.g_utf16_pointer_to_offset (ptr, pos [0]);
	String newText = verifyText (oldText, start, start);
	if (newText != oldText) {
		int [] newStart = new int [1], newEnd = new int [1];
		GTK.gtk_editable_get_selection_bounds (handle, newStart, newEnd);
		if (newText != null) {
			if (newStart [0] != newEnd [0]) {
				OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, DELETE_TEXT);
				OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, CHANGED);
				GTK.gtk_editable_delete_selection (handle);
				OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, DELETE_TEXT);
				OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, CHANGED);
			}
			byte [] buffer3 = Converter.wcsToMbcs (newText, false);
			OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, INSERT_TEXT);
			GTK.gtk_editable_insert_text (handle, buffer3, buffer3.length, pos);
			OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, INSERT_TEXT);
			newStart [0] = newEnd [0] = pos [0];
		}
		pos [0] = newEnd [0];
		if (newStart [0] != newEnd [0]) {
			fixStart = newStart [0];
			fixEnd = newEnd [0];
		}
		C.memmove (position, pos, 4);
		OS.g_signal_stop_emission_by_name (handle, OS.insert_text);
	}
	return 0;
}

@Override
int /*long*/ gtk_key_press_event (int /*long*/ widget, int /*long*/ event) {
	int /*long*/ result = super.gtk_key_press_event (widget, event);
	if (result != 0) fixIM ();
	if (gdkEventKey == -1) result = 1;
	gdkEventKey = 0;
	return result;
}

@Override
int /*long*/ gtk_populate_popup (int /*long*/ widget, int /*long*/ menu) {
	if ((style & SWT.RIGHT_TO_LEFT) != 0) {
		GTK.gtk_widget_set_direction (menu, GTK.GTK_TEXT_DIR_RTL);
		GTK.gtk_container_forall (menu, display.setDirectionProc, GTK.GTK_TEXT_DIR_RTL);
	}
	return 0;
}

@Override
int /*long*/ gtk_value_changed (int /*long*/ widget) {
	sendSelectionEvent (SWT.Selection);
	return 0;
}

@Override
void hookEvents () {
	super.hookEvents();
	OS.g_signal_connect_closure (handle, OS.changed, display.getClosure (CHANGED), true);
	OS.g_signal_connect_closure (handle, OS.insert_text, display.getClosure (INSERT_TEXT), false);
	OS.g_signal_connect_closure (handle, OS.delete_text, display.getClosure (DELETE_TEXT), false);
	OS.g_signal_connect_closure (handle, OS.value_changed, display.getClosure (VALUE_CHANGED), false);
	OS.g_signal_connect_closure (handle, OS.activate, display.getClosure (ACTIVATE), false);
	OS.g_signal_connect_closure (handle, OS.populate_popup, display.getClosure (POPULATE_POPUP), false);
	int /*long*/ imContext = imContext ();
	if (imContext != 0) {
		OS.g_signal_connect_closure (imContext, OS.commit, display.getClosure (COMMIT), false);
		int id = OS.g_signal_lookup (OS.commit, GTK.gtk_im_context_get_type ());
		int mask =  OS.G_SIGNAL_MATCH_DATA | OS.G_SIGNAL_MATCH_ID;
		OS.g_signal_handlers_block_matched (imContext, mask, id, 0, 0, 0, handle);
	}
}

int /*long*/ imContext () {
	if (imContext != 0) return imContext;
	return GTK.GTK_ENTRY_IM_CONTEXT (handle);
}

@Override
int /*long*/ paintWindow () {
	int /*long*/ window = super.paintWindow ();
	int /*long*/ children = GDK.gdk_window_get_children (window);
	if (children != 0) window = OS.g_list_data (children);
	OS.g_list_free (children);
	return window;
}

/**
 * Pastes text from clipboard.
 * <p>
 * The selected text is deleted from the widget
 * and new text inserted from the clipboard.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void paste () {
	checkWidget ();
	GTK.gtk_editable_paste_clipboard (handle);
}

@Override
void register () {
	super.register ();
	int /*long*/ imContext = imContext ();
	if (imContext != 0) display.addWidget (imContext, this);
}

@Override
void releaseWidget () {
	super.releaseWidget ();
	fixIM ();
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the receiver's text is modified.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see ModifyListener
 * @see #addModifyListener
 */
public void removeModifyListener (ModifyListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Modify, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control is selected by the user.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #addSelectionListener
 */
public void removeSelectionListener(SelectionListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook(SWT.Selection, listener);
	eventTable.unhook(SWT.DefaultSelection,listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control is verified.
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see VerifyListener
 * @see #addVerifyListener
 */
void removeVerifyListener (VerifyListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Verify, listener);
}

@Override
GdkRGBA getContextBackgroundGdkRGBA () {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	if (background != null && (state & BACKGROUND) != 0) {
		return background;
	}
	return defaultBackground();
}

@Override
void setBackgroundGdkRGBA (int /*long*/ context, int /*long*/ handle, GdkRGBA rgba) {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	background = rgba;
	setBackgroundGradientGdkRGBA (context, handle, rgba);
}

@Override
void setBackgroundGdkColor (GdkColor color) {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	super.setBackgroundGdkColor (color);
	GTK.gtk_widget_modify_base (handle, 0, color);
}

@Override
void setCursor (int /*long*/ cursor) {
	int /*long*/ defaultCursor = 0;
	if (cursor == 0) defaultCursor = GDK.gdk_cursor_new_for_display (GDK.gdk_display_get_default(), GDK.GDK_XTERM);
	super.setCursor (cursor != 0 ? cursor : defaultCursor);
	if (cursor == 0) gdk_cursor_unref (defaultCursor);
}

@Override
void setForegroundGdkColor (GdkColor color) {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	setForegroundColor (handle, color, false);
}

/**
 * Sets the amount that the receiver's value will be
 * modified by when the up/down arrows are pressed to
 * the argument, which must be at least one.
 *
 * @param value the new increment (must be greater than zero)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setIncrement (int value) {
	checkWidget ();
	if (value < 1) return;
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	double page_increment = GTK.gtk_adjustment_get_page_increment (hAdjustment);
	double newValue = value;
	int digits = GTK.gtk_spin_button_get_digits (handle);
	for (int i = 0; i < digits; i++) newValue /= 10;
	OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
	GTK.gtk_spin_button_set_increments (handle, newValue, page_increment);
	OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
}

/**
 * Sets the maximum value that the receiver will allow.  This new
 * value will be ignored if it is less than the receiver's current
 * minimum value.  If the new maximum is applied then the receiver's
 * selection value will be adjusted if necessary to fall within its new range.
 *
 * @param value the new maximum, which must be greater than or equal to the current minimum
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setMaximum (int value) {
	checkWidget ();
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	double lower = GTK.gtk_adjustment_get_lower (hAdjustment);
	double newValue = value;
	int digits = GTK.gtk_spin_button_get_digits (handle);
	for (int i = 0; i < digits; i++) newValue /= 10;
	if (newValue < lower) return;
	OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
	GTK.gtk_spin_button_set_range (handle, lower, newValue);
	OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
}

/**
 * Sets the minimum value that the receiver will allow.  This new
 * value will be ignored if it is greater than the receiver's
 * current maximum value.  If the new minimum is applied then the receiver's
 * selection value will be adjusted if necessary to fall within its new range.
 *
 * @param value the new minimum, which must be less than or equal to the current maximum
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setMinimum (int value) {
	checkWidget ();
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	double upper = GTK.gtk_adjustment_get_upper (hAdjustment);
	double newValue = value;
	int digits = GTK.gtk_spin_button_get_digits (handle);
	for (int i = 0; i < digits; i++) newValue /= 10;
	if (newValue > upper) return;
	OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
	GTK.gtk_spin_button_set_range (handle, newValue, upper);
	OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
}

/**
 * Sets the amount that the receiver's position will be
 * modified by when the page up/down keys are pressed
 * to the argument, which must be at least one.
 *
 * @param value the page increment (must be greater than zero)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setPageIncrement (int value) {
	checkWidget ();
	if (value < 1) return;
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	double step_increment = GTK.gtk_adjustment_get_step_increment(hAdjustment);
	double newValue = value;
	int digits = GTK.gtk_spin_button_get_digits (handle);
	for (int i = 0; i < digits; i++) newValue /= 10;
	OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
	GTK.gtk_spin_button_set_increments (handle, step_increment, newValue);
	OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
}

/**
 * Sets the <em>selection</em>, which is the receiver's
 * position, to the argument. If the argument is not within
 * the range specified by minimum and maximum, it will be
 * adjusted to fall within this range.
 *
 * @param value the new selection (must be zero or greater)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setSelection (int value) {
	checkWidget ();
	double newValue = value;
	int digits = GTK.gtk_spin_button_get_digits (handle);
	for (int i = 0; i < digits; i++) newValue /= 10;
	OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
	GTK.gtk_spin_button_set_value (handle, newValue);
	OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
}

/**
 * Sets the maximum number of characters that the receiver's
 * text field is capable of holding to be the argument.
 * <p>
 * To reset this value to the default, use <code>setTextLimit(Spinner.LIMIT)</code>.
 * Specifying a limit value larger than <code>Spinner.LIMIT</code> sets the
 * receiver's limit to <code>Spinner.LIMIT</code>.
 * </p>
 * @param limit new text limit
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_CANNOT_BE_ZERO - if the limit is zero</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #LIMIT
 *
 * @since 3.4
 */
public void setTextLimit (int limit) {
	checkWidget ();
	if (limit == 0) error (SWT.ERROR_CANNOT_BE_ZERO);
	GTK.gtk_entry_set_max_length (handle, limit);
}

/**
 * Sets the number of decimal places used by the receiver.
 * <p>
 * The digit setting is used to allow for floating point values in the receiver.
 * For example, to set the selection to a floating point value of 1.37 call setDigits() with
 * a value of 2 and setSelection() with a value of 137. Similarly, if getDigits() has a value
 * of 2 and getSelection() returns 137 this should be interpreted as 1.37. This applies to all
 * numeric APIs.
 * </p>
 *
 * @param value the new digits (must be greater than or equal to zero)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the value is less than zero</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setDigits (int value) {
	checkWidget ();
	if (value < 0) error (SWT.ERROR_INVALID_ARGUMENT);
	int digits = GTK.gtk_spin_button_get_digits (handle);
	if (value == digits) return;
	int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
	GtkAdjustment adjustment = new GtkAdjustment ();
	gtk_adjustment_get (hAdjustment, adjustment);
	int diff = Math.abs (value - digits);
	int factor = 1;
	for (int i = 0; i < diff; i++) factor *= 10;
	if (digits > value) {
		adjustment.value *= factor;
		adjustment.upper *= factor;
		adjustment.lower *= factor;
		adjustment.step_increment *= factor;
		adjustment.page_increment *= factor;
		climbRate *= factor;
	} else {
		adjustment.value /= factor;
		adjustment.upper /= factor;
		adjustment.lower /= factor;
		adjustment.step_increment /= factor;
		adjustment.page_increment /= factor;
		climbRate /= factor;
	}
	GTK.gtk_adjustment_configure(hAdjustment, adjustment.value, adjustment.lower, adjustment.upper,
		adjustment.step_increment, adjustment.page_increment, adjustment.page_size);
	OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
	GTK.gtk_spin_button_configure (handle, hAdjustment, climbRate, value);
	OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
}

/**
 * Sets the receiver's selection, minimum value, maximum
 * value, digits, increment and page increment all at once.
 * <p>
 * Note: This is similar to setting the values individually
 * using the appropriate methods, but may be implemented in a
 * more efficient fashion on some platforms.
 * </p>
 *
 * @param selection the new selection value
 * @param minimum the new minimum value
 * @param maximum the new maximum value
 * @param digits the new digits value
 * @param increment the new increment value
 * @param pageIncrement the new pageIncrement value
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.2
 */
public void setValues (int selection, int minimum, int maximum, int digits, int increment, int pageIncrement) {
	checkWidget ();
	if (maximum < minimum) return;
	if (digits < 0) return;
	if (increment < 1) return;
	if (pageIncrement < 1) return;
	selection = Math.min (Math.max (minimum, selection), maximum);
	double factor = 1;
	for (int i = 0; i < digits; i++) factor *= 10;
	/*
	* The value of climb-rate indicates the acceleration rate
	* to spin the value when the button is pressed and hold
	* on the arrow button. This value should be varied
	* depending upon the value of digits.
	*/
	OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
	climbRate = 1.0 / factor;
	int /*long*/ adjustment = GTK.gtk_spin_button_get_adjustment(handle);
	GTK.gtk_spin_button_configure (handle, adjustment, climbRate, digits);

	GTK.gtk_spin_button_set_range (handle, minimum / factor, maximum / factor);
	GTK.gtk_spin_button_set_increments (handle, increment / factor, pageIncrement / factor);
	GTK.gtk_spin_button_set_value (handle, selection / factor);
	OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, VALUE_CHANGED);
}

@Override
boolean checkSubwindow () {
	return false;
}

@Override
boolean translateTraversal (GdkEventKey keyEvent) {
	int key = keyEvent.keyval;
	switch (key) {
		case GDK.GDK_KP_Enter:
		case GDK.GDK_Return: {
			int /*long*/ imContext =  imContext ();
			if (imContext != 0) {
				int /*long*/ [] preeditString = new int /*long*/ [1];
				GTK.gtk_im_context_get_preedit_string (imContext, preeditString, null, null);
				if (preeditString [0] != 0) {
					int length = C.strlen (preeditString [0]);
					OS.g_free (preeditString [0]);
					if (length != 0) return false;
				}
			}
		}
	}
	return super.translateTraversal (keyEvent);
}

String verifyText (String string, int start, int end) {
	if (string.length () == 0 && start == end) return null;
	Event event = new Event ();
	event.text = string;
	event.start = start;
	event.end = end;
	int /*long*/ eventPtr = GTK.gtk_get_current_event ();
	if (eventPtr != 0) {
		GdkEventKey gdkEvent = new GdkEventKey ();
		OS.memmove (gdkEvent, eventPtr, GdkEventKey.sizeof);
		switch (gdkEvent.type) {
			case GDK.GDK_KEY_PRESS:
				setKeyState (event, gdkEvent);
				break;
		}
		GDK.gdk_event_free (eventPtr);
	}
	int index = 0;
	if (GTK.gtk_spin_button_get_digits (handle) > 0) {
		String decimalSeparator = getDecimalSeparator ();
		index = string.indexOf (decimalSeparator);
		if (index != -1) {
			string = string.substring (0, index) + string.substring (index + 1);
		}
		index = 0;
	}
	if (string.length () > 0) {
		int /*long*/ hAdjustment = GTK.gtk_spin_button_get_adjustment (handle);
		double lower = GTK.gtk_adjustment_get_lower (hAdjustment);
		if (lower < 0 && string.charAt (0) == '-') index++;
	}
	while (index < string.length ()) {
		if (!Character.isDigit (string.charAt (index))) break;
		index++;
	}
	event.doit = index == string.length ();
	/*
	 * It is possible (but unlikely), that application
	 * code could have disposed the widget in the verify
	 * event.  If this happens, answer null to cancel
	 * the operation.
	 */
	sendEvent (SWT.Verify, event);
	if (!event.doit || isDisposed ()) return null;
	return event.text;
}

}
