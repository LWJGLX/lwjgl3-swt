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
 *     Stefan Xenos (Google) - bug 468854 - Add a requestLayout method to Control
 *******************************************************************************/
package org.eclipse.swt.widgets;


import java.lang.reflect.*;
import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.cairo.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * Control is the abstract superclass of all windowed user interface classes.
 * <p>
 * <dl>
 * <dt><b>Styles:</b>
 * <dd>BORDER</dd>
 * <dd>LEFT_TO_RIGHT, RIGHT_TO_LEFT, FLIP_TEXT_DIRECTION</dd>
 * <dt><b>Events:</b>
 * <dd>DragDetect, FocusIn, FocusOut, Help, KeyDown, KeyUp, MenuDetect, MouseDoubleClick, MouseDown, MouseEnter,
 *     MouseExit, MouseHover, MouseUp, MouseMove, MouseWheel, MouseHorizontalWheel, MouseVerticalWheel, Move,
 *     Paint, Resize, Traverse</dd>
 * </dl>
 * </p><p>
 * Only one of LEFT_TO_RIGHT or RIGHT_TO_LEFT may be specified.
 * </p><p>
 * IMPORTANT: This class is intended to be subclassed <em>only</em>
 * within the SWT implementation.
 * </p>
 *
 * @see <a href="http://www.eclipse.org/swt/snippets/#control">Control snippets</a>
 * @see <a href="http://www.eclipse.org/swt/examples.php">SWT Example: ControlExample</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class Control extends Widget implements Drawable {
	int /*long*/ fixedHandle;
	int /*long*/ redrawWindow, enableWindow, provider;
	int drawCount, backgroundAlpha = 255;
	int /*long*/ enterNotifyEventId;
	int /*long*/ dragGesture, zoomGesture, rotateGesture, panGesture;
	Composite parent;
	Cursor cursor;
	Menu menu;
	Image backgroundImage;
	Font font;
	Region region;
	int /*long*/ eventRegion;
	/**
	 * The handle to the Region, which is neccessary in the case
	 * that <code>region</code> is disposed before this Control.
	 */
	int /*long*/ regionHandle;
	String toolTipText;
	Object layoutData;
	Accessible accessible;
	Control labelRelation;
	String cssBackground, cssForeground = " ";
	boolean drawRegion;
	/**
	 * Cache the NO_BACKGROUND flag as it gets removed automatically in
	 * Composite. Only relevant for GTK3.10+ as we need it for Cairo setRegion()
	 * functionality. See bug 475784.
	 */
	boolean cachedNoBackground;
	/**
	 * Point for storing the (x, y) coordinate of the last input (click/scroll/etc.).
	 * This is useful for checking input event coordinates in methods that act on input,
	 * but do not receive coordinates (like gtk_clicked, for example). See bug 529431.
	 */
	Point lastInput = new Point(0, 0);

	LinkedList <Event> dragDetectionQueue;

	/* these class variables are for the workaround for bug #427776 */
	static Callback enterNotifyEventFunc;
	static int enterNotifyEventSignalId;
	static int GTK_POINTER_WINDOW;
	static int SWT_GRAB_WIDGET;

	static Callback gestureZoom, gestureRotation, gestureSwipe, gestureBegin, gestureEnd;
	static {
		gestureZoom = new Callback (Control.class, "magnifyProc", void.class, new Type[] {
				long.class, double.class, long.class}); //$NON-NLS-1$
		if (gestureZoom.getAddress() == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
		gestureRotation = new Callback (Control.class, "rotateProc", void.class, new Type[] {
				long.class, double.class, double.class, long.class}); //$NON-NLS-1$
		if (gestureRotation.getAddress() == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);
		gestureSwipe = new Callback (Control.class, "swipeProc", void.class, new Type[] {
				long.class, double.class, double.class, long.class}); //$NON-NLS-1$
		if (gestureSwipe.getAddress() == 0) SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
		gestureBegin = new Callback (Control.class, "gestureBeginProc", void.class, new Type[] {
				long.class, long.class, long.class}); //$NON-NLS-1$
		if (gestureBegin.getAddress() == 0) SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
		gestureEnd = new Callback (Control.class, "gestureEndProc", void.class, new Type[] {
				long.class, long.class, long.class}); //$NON-NLS-1$
		if (gestureEnd.getAddress() == 0) SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
	}

Control () {
}

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
 * @see SWT#BORDER
 * @see SWT#LEFT_TO_RIGHT
 * @see SWT#RIGHT_TO_LEFT
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public Control (Composite parent, int style) {
	super (parent, style);
	this.parent = parent;
	createWidget (0);
}

void connectPaint () {
	int /*long*/ paintHandle = paintHandle ();
	int paintMask = GDK.GDK_EXPOSURE_MASK;
	GTK.gtk_widget_add_events (paintHandle, paintMask);

	OS.g_signal_connect_closure_by_id (paintHandle, display.signalIds [DRAW], 0, display.getClosure (EXPOSE_EVENT_INVERSE), false);

	OS.g_signal_connect_closure_by_id (paintHandle, display.signalIds [DRAW], 0, display.getClosure (DRAW), true);
}

Font defaultFont () {
	return display.getSystemFont ();
}

GdkRGBA defaultBackground () {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	return display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).handleRGBA;
}

@Override
void deregister () {
	super.deregister ();
	if (fixedHandle != 0) display.removeWidget (fixedHandle);
	int /*long*/ imHandle = imHandle ();
	if (imHandle != 0) display.removeWidget (imHandle);
}

void drawBackground (Control control, int /*long*/ window, int /*long*/ region, int x, int y, int width, int height) {
	drawBackground(control, window, 0, region, x, y, width, height);
}

void drawBackground (Control control, int /*long*/ window, int /*long*/ cr, int /*long*/ region, int x, int y, int width, int height) {
	int /*long*/ cairo = cr != 0 ? cr : GDK.gdk_cairo_create(window);
	/*
	 * It's possible that a client is using an SWT.NO_BACKGROUND Composite with custom painting
	 * and a region to provide "overlay" functionality. In this case we don't want to paint
	 * any background color, as it will likely break desired behavior. The fix is to paint
	 * this Control as transparent. See bug 475784.
	 */
	boolean noBackgroundRegion = drawRegion && hooks(SWT.Paint) && cachedNoBackground;
	if (cairo == 0) error (SWT.ERROR_NO_HANDLES);
	if (region != 0) {
		GDK.gdk_cairo_region(cairo, region);
		Cairo.cairo_clip(cairo);
	}
	if (control.backgroundImage != null) {
		Point pt = display.mapInPixels (this, control, 0, 0);
		Cairo.cairo_translate (cairo, -pt.x, -pt.y);
		x += pt.x;
		y += pt.y;
		int /*long*/ pattern = Cairo.cairo_pattern_create_for_surface (control.backgroundImage.surface);
		if (pattern == 0) error (SWT.ERROR_NO_HANDLES);
		Cairo.cairo_pattern_set_extend (pattern, Cairo.CAIRO_EXTEND_REPEAT);
		if ((style & SWT.MIRRORED) != 0) {
			double[] matrix = {-1, 0, 0, 1, 0, 0};
			Cairo.cairo_pattern_set_matrix(pattern, matrix);
		}
		Cairo.cairo_set_source (cairo, pattern);
		Cairo.cairo_pattern_destroy (pattern);
	} else {
		GdkRGBA rgba;
		if (GTK.GTK3) {
			rgba = control.getBackgroundGdkRGBA();
			if (noBackgroundRegion) {
				Cairo.cairo_set_source_rgba (cairo, 0.0, 0.0, 0.0, 0.0);
			} else {
				Cairo.cairo_set_source_rgba (cairo, rgba.red, rgba.green, rgba.blue, rgba.alpha);
			}
		} else {
			GdkColor color = control.getBackgroundGdkColor ();
			Cairo.cairo_set_source_rgba_compatibility (cairo, color);
		}
	}
	Cairo.cairo_rectangle (cairo, x, y, width, height);
	Cairo.cairo_fill (cairo);
	if (cairo != cr) Cairo.cairo_destroy(cairo);
}

boolean drawGripper (GC gc, int x, int y, int width, int height, boolean vertical) {
	int /*long*/ paintHandle = paintHandle ();
	int /*long*/ window = gtk_widget_get_window (paintHandle);
	if (window == 0) return false;
	int orientation = vertical ? GTK.GTK_ORIENTATION_HORIZONTAL : GTK.GTK_ORIENTATION_VERTICAL;
	if ((style & SWT.MIRRORED) != 0) x = getClientWidth () - width - x;
	if (GTK.GTK3) {
		int /*long*/ context = GTK.gtk_widget_get_style_context (paintHandle);
		GTK.gtk_style_context_save (context);
		GTK.gtk_style_context_add_class (context, GTK.GTK_STYLE_CLASS_PANE_SEPARATOR);
		GTK.gtk_style_context_set_state (context, GTK.GTK_STATE_FLAG_NORMAL);
		GTK.gtk_render_handle (context, gc.handle, x, y, width, height);
		GTK.gtk_style_context_restore (context);
	} else {
		GTK.gtk_paint_handle (GTK.gtk_widget_get_style (paintHandle), window, GTK.GTK_STATE_NORMAL, GTK.GTK_SHADOW_OUT, null, paintHandle, new byte [1], x, y, width, height, orientation);
	}
	return true;
}

void drawWidget (GC gc) {
}

void enableWidget (boolean enabled) {
	GTK.gtk_widget_set_sensitive (handle, enabled);
}

int /*long*/ enterExitHandle () {
	return eventHandle ();
}

int /*long*/ eventHandle () {
	return handle;
}

int /*long*/ eventWindow () {
	int /*long*/ eventHandle = eventHandle ();
	GTK.gtk_widget_realize (eventHandle);
	return gtk_widget_get_window (eventHandle);
}

void fixFocus (Control focusControl) {
	Shell shell = getShell ();
	Control control = this;
	while (control != shell && (control = control.parent) != null) {
		if (control.setFocus ()) return;
	}
	shell.setSavedFocus (focusControl);
	int /*long*/ focusHandle = shell.vboxHandle;
	GTK.gtk_widget_set_can_focus (focusHandle, true);
	GTK.gtk_widget_grab_focus (focusHandle);
	// widget could be disposed at this point
	if (isDisposed ()) return;
	GTK.gtk_widget_set_can_focus (focusHandle, false);
}

void fixStyle () {
	if (fixedHandle != 0) fixStyle (fixedHandle);
}

void fixStyle (int /*long*/ handle) {
	/*
	* Feature in GTK.  Some GTK themes apply a different background to
	* the contents of a GtkNotebook.  However, in an SWT TabFolder, the
	* children are not parented below the GtkNotebook widget, and usually
	* have their own GtkFixed.  The fix is to look up the correct style
	* for a child of a GtkNotebook and apply its background to any GtkFixed
	* widgets that are direct children of an SWT TabFolder.
	*
	* Note that this has to be when the theme settings changes and that it
	* should not override the application background.
	*/
	if ((state & BACKGROUND) != 0) return;
	if ((state & THEME_BACKGROUND) == 0) return;
	if (!GTK.GTK3) {
		int /*long*/ childStyle = parent.childStyle ();
		if (childStyle != 0) {
			GdkColor color = new GdkColor();
			GTK.gtk_style_get_bg (childStyle, 0, color);
			setBackgroundGdkColor (color);
		}
	}
}

int /*long*/ focusHandle () {
	return handle;
}

int /*long*/ fontHandle () {
	return handle;
}

int /*long*/ gestureHandle () {
	return handle;
}

/**
 * Returns the orientation of the receiver, which will be one of the
 * constants <code>SWT.LEFT_TO_RIGHT</code> or <code>SWT.RIGHT_TO_LEFT</code>.
 *
 * @return the orientation style
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.7
 */
public int getOrientation () {
	checkWidget();
	return style & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT);
}

/**
 * Returns the text direction of the receiver, which will be one of the
 * constants <code>SWT.LEFT_TO_RIGHT</code> or <code>SWT.RIGHT_TO_LEFT</code>.
 *
 * @return the text direction style
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.102
 */
public int getTextDirection() {
	checkWidget ();
	/* return the widget orientation */
	return style & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT);
}

boolean hasFocus () {
	return this == display.getFocusControl();
}

@Override
void hookEvents () {
	/* Connect the keyboard signals */
	int /*long*/ focusHandle = focusHandle ();
	int focusMask = GDK.GDK_KEY_PRESS_MASK | GDK.GDK_KEY_RELEASE_MASK | GDK.GDK_FOCUS_CHANGE_MASK;
	GTK.gtk_widget_add_events (focusHandle, focusMask);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [POPUP_MENU], 0, display.getClosure (POPUP_MENU), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [SHOW_HELP], 0, display.getClosure (SHOW_HELP), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [KEY_PRESS_EVENT], 0, display.getClosure (KEY_PRESS_EVENT), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [KEY_RELEASE_EVENT], 0, display.getClosure (KEY_RELEASE_EVENT), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [FOCUS], 0, display.getClosure (FOCUS), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [FOCUS_IN_EVENT], 0, display.getClosure (FOCUS_IN_EVENT), false);
	OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [FOCUS_OUT_EVENT], 0, display.getClosure (FOCUS_OUT_EVENT), false);

	/* Connect the mouse signals */
	int /*long*/ eventHandle = eventHandle ();
	int eventMask = GDK.GDK_POINTER_MOTION_MASK | GDK.GDK_BUTTON_PRESS_MASK | GDK.GDK_BUTTON_RELEASE_MASK | GDK.GDK_SCROLL_MASK | GDK.GDK_SMOOTH_SCROLL_MASK;
	GTK.gtk_widget_add_events (eventHandle, eventMask);
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [BUTTON_PRESS_EVENT], 0, display.getClosure (BUTTON_PRESS_EVENT), false);
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [BUTTON_RELEASE_EVENT], 0, display.getClosure (BUTTON_RELEASE_EVENT), false);
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [MOTION_NOTIFY_EVENT], 0, display.getClosure (MOTION_NOTIFY_EVENT), false);
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [SCROLL_EVENT], 0, display.getClosure (SCROLL_EVENT), false);

	/* Connect enter/exit signals */
	int /*long*/ enterExitHandle = enterExitHandle ();
	int enterExitMask = GDK.GDK_ENTER_NOTIFY_MASK | GDK.GDK_LEAVE_NOTIFY_MASK;
	GTK.gtk_widget_add_events (enterExitHandle, enterExitMask);
	OS.g_signal_connect_closure_by_id (enterExitHandle, display.signalIds [ENTER_NOTIFY_EVENT], 0, display.getClosure (ENTER_NOTIFY_EVENT), false);
	OS.g_signal_connect_closure_by_id (enterExitHandle, display.signalIds [LEAVE_NOTIFY_EVENT], 0, display.getClosure (LEAVE_NOTIFY_EVENT), false);

	/*Connect gesture signals */
	setZoomGesture();
	setDragGesture();
	setRotateGesture();

	/*
	* Feature in GTK.  Events such as mouse move are propagate up
	* the widget hierarchy and are seen by the parent.  This is the
	* correct GTK behavior but not correct for SWT.  The fix is to
	* hook a signal after and stop the propagation using a negative
	* event number to distinguish this case.
	*
	* The signal is hooked to the fixedHandle to catch events sent to
	* lightweight widgets.
	*/
	int /*long*/ blockHandle = fixedHandle != 0 ? fixedHandle : eventHandle;
	OS.g_signal_connect_closure_by_id (blockHandle, display.signalIds [BUTTON_PRESS_EVENT], 0, display.getClosure (BUTTON_PRESS_EVENT_INVERSE), true);
	OS.g_signal_connect_closure_by_id (blockHandle, display.signalIds [BUTTON_RELEASE_EVENT], 0, display.getClosure (BUTTON_RELEASE_EVENT_INVERSE), true);
	OS.g_signal_connect_closure_by_id (blockHandle, display.signalIds [MOTION_NOTIFY_EVENT], 0, display.getClosure (MOTION_NOTIFY_EVENT_INVERSE), true);

	/* Connect the event_after signal for both key and mouse */
	OS.g_signal_connect_closure_by_id (eventHandle, display.signalIds [EVENT_AFTER], 0, display.getClosure (EVENT_AFTER), false);
	if (focusHandle != eventHandle) {
		OS.g_signal_connect_closure_by_id (focusHandle, display.signalIds [EVENT_AFTER], 0, display.getClosure (EVENT_AFTER), false);
	}

	/* Connect the paint signal */
	connectPaint ();

	/* Connect the Input Method signals */
	OS.g_signal_connect_closure_by_id (handle, display.signalIds [REALIZE], 0, display.getClosure (REALIZE), true);
	OS.g_signal_connect_closure_by_id (handle, display.signalIds [UNREALIZE], 0, display.getClosure (UNREALIZE), false);
	int /*long*/ imHandle = imHandle ();
	if (imHandle != 0) {
		OS.g_signal_connect_closure (imHandle, OS.commit, display.getClosure (COMMIT), false);
		OS.g_signal_connect_closure (imHandle, OS.preedit_changed, display.getClosure (PREEDIT_CHANGED), false);
	}

	OS.g_signal_connect_closure_by_id (paintHandle (), display.signalIds [STYLE_SET], 0, display.getClosure (STYLE_SET), false);

	int /*long*/ topHandle = topHandle ();
	OS.g_signal_connect_closure_by_id (topHandle, display.signalIds [MAP], 0, display.getClosure (MAP), true);

	if (enterNotifyEventFunc == null && GTK.GTK3 && GTK.GTK_VERSION < OS.VERSION (3, 11, 9)) {
		enterNotifyEventFunc = new Callback (Control.class, "enterNotifyEventProc", 4);
		if (enterNotifyEventFunc.getAddress () == 0) SWT.error (SWT.ERROR_NO_MORE_CALLBACKS);

		enterNotifyEventSignalId = OS.g_signal_lookup (OS.enter_notify_event, GTK.GTK_TYPE_WIDGET ());

		byte [] buffer = Converter.wcsToMbcs ("gtk-pointer-window", true);
		GTK_POINTER_WINDOW = OS.g_quark_from_string (buffer);
		buffer = Converter.wcsToMbcs ("swt-grab-widget", true);
		SWT_GRAB_WIDGET = OS.g_quark_from_string (buffer);
	}
}

boolean hooksPaint () {
	return hooks (SWT.Paint) || filters (SWT.Paint);
}

@Override
int /*long*/ hoverProc (int /*long*/ widget) {
	int [] x = new int [1], y = new int [1], mask = new int [1];
	gdk_window_get_device_position (0, x, y, mask);
	if (containedInRegion(x[0], y[0])) return 0;
	sendMouseEvent (SWT.MouseHover, 0, /*time*/0, x [0], y [0], false, mask [0]);
	/* Always return zero in order to cancel the hover timer */
	return 0;
}

@Override
int /*long*/ topHandle() {
	if (fixedHandle != 0) return fixedHandle;
	return super.topHandle ();
}

int /*long*/ paintHandle () {
	int /*long*/ topHandle = topHandle ();
	int /*long*/ paintHandle = handle;
	while (paintHandle != topHandle) {
		if (GTK.gtk_widget_get_has_window (paintHandle)) break;
		paintHandle = GTK.gtk_widget_get_parent (paintHandle);
	}
	return paintHandle;
}

@Override
int /*long*/ paintWindow () {
	int /*long*/ paintHandle = paintHandle ();
	GTK.gtk_widget_realize (paintHandle);
	return gtk_widget_get_window (paintHandle);
}

/**
 * Prints the receiver and all children.
 *
 * @param gc the gc where the drawing occurs
 * @return <code>true</code> if the operation was successful and <code>false</code> otherwise
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the gc is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the gc has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.4
 */
public boolean print (GC gc) {
	checkWidget ();
	if (gc == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (gc.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
	int /*long*/ topHandle = topHandle ();
	GTK.gtk_widget_realize (topHandle);
	if (GTK.GTK3) {
		/*
		 * Feature in GTK: gtk_widget_draw() will only draw if the
		 * widget's priv->alloc_needed field is set to TRUE. Since
		 * this field is private and inaccessible, get and set the
		 * allocation to trigger it to be TRUE. See bug 530969.
		 */
		GtkAllocation allocation = new GtkAllocation ();
		GTK.gtk_widget_get_allocation(topHandle, allocation);
		// Prevent allocation warnings
		if (GTK.GTK_VERSION >= OS.VERSION(3, 20, 0)) {
			GTK.gtk_widget_get_preferred_size(topHandle, null, null);
		}
		GTK.gtk_widget_size_allocate(topHandle, allocation);
		GTK.gtk_widget_draw(topHandle, gc.handle);
		return true;
	}
	int /*long*/ window = gtk_widget_get_window (topHandle);
	GCData data = gc.getGCData ();
	GDK.gdk_window_process_updates (window, true);
	int /*long*/ drawable = data.drawable;
	if (drawable == 0) drawable = GDK.gdk_get_default_root_window();
	printWidget (gc, drawable, GDK.gdk_drawable_get_depth (drawable), 0, 0);
	return true;
}

void printWidget (GC gc, int /*long*/ drawable, int depth, int x, int y) {
	boolean obscured = (state & OBSCURED) != 0;
	state &= ~OBSCURED;
	int /*long*/ topHandle = topHandle ();
	int /*long*/ window = gtk_widget_get_window (topHandle);
	printWindow (true, this, gc, drawable, depth, window, x, y);
	if (obscured) state |= OBSCURED;
}

void printWindow (boolean first, Control control, GC gc, int /*long*/ drawable, int depth, int /*long*/ window, int x, int y) {
	if (GDK.gdk_drawable_get_depth (window) != depth) return;
	GdkRectangle rect = new GdkRectangle ();
	int [] width = new int [1], height = new int [1];
	gdk_window_get_size (window, width, height);
	rect.width = width [0];
	rect.height = height [0];
	GDK.gdk_window_begin_paint_rect (window, rect);
	int /*long*/ [] real_drawable = new int /*long*/ [1];
	int [] x_offset = new int [1], y_offset = new int [1];
	GDK.gdk_window_get_internal_paint_info (window, real_drawable, x_offset, y_offset);
	int /*long*/ [] userData = new int /*long*/ [1];
	GDK.gdk_window_get_user_data (window, userData);
	if (userData [0] != 0) {
		int /*long*/ eventPtr = GDK.gdk_event_new (GDK.GDK_EXPOSE);
		GdkEventExpose event = new GdkEventExpose ();
		event.type = GDK.GDK_EXPOSE;
		event.window = OS.g_object_ref (window);
		event.area_width = rect.width;
		event.area_height = rect.height;
		event.region = GDK.gdk_region_rectangle (rect);
		OS.memmove (eventPtr, event, GdkEventExpose.sizeof);
		GTK.gtk_widget_send_expose (userData [0], eventPtr);
		GDK.gdk_event_free (eventPtr);
	}
	int destX = x, destY = y, destWidth = width [0], destHeight = height [0];
	if (!first) {
		int [] cX = new int [1], cY = new int [1];
		GDK.gdk_window_get_position (window, cX, cY);
		int /*long*/ parentWindow = GDK.gdk_window_get_parent (window);
		int [] pW = new int [1], pH = new int [1];
		gdk_window_get_size (parentWindow, pW, pH);
		destX = x - cX [0];
		destY = y - cY [0];
		destWidth = Math.min (cX [0] + width [0], pW [0]);
		destHeight = Math.min (cY [0] + height [0], pH [0]);
	}
	GCData gcData = gc.getGCData();
	int /*long*/ cairo = gcData.cairo;
	int /*long*/ xDisplay = GDK.gdk_x11_display_get_xdisplay(GDK.gdk_display_get_default());
	int /*long*/ xVisual = GDK.gdk_x11_visual_get_xvisual(GDK.gdk_visual_get_system());
	int /*long*/ xDrawable = GDK.GDK_PIXMAP_XID(real_drawable [0]);
	int /*long*/ surface = Cairo.cairo_xlib_surface_create(xDisplay, xDrawable, xVisual, width [0], height [0]);
	if (surface == 0) error(SWT.ERROR_NO_HANDLES);
	Cairo.cairo_save(cairo);
	Cairo.cairo_rectangle(cairo, destX , destY, destWidth, destHeight);
	Cairo.cairo_clip(cairo);
	Cairo.cairo_translate(cairo, destX, destY);
	int /*long*/ pattern = Cairo.cairo_pattern_create_for_surface(surface);
	if (pattern == 0) error(SWT.ERROR_NO_HANDLES);
	Cairo.cairo_pattern_set_filter(pattern, Cairo.CAIRO_FILTER_BEST);
	Cairo.cairo_set_source(cairo, pattern);
	if (gcData.alpha != 0xFF) {
		Cairo.cairo_paint_with_alpha(cairo, gcData.alpha / (float)0xFF);
	} else {
		Cairo.cairo_paint(cairo);
	}
	Cairo.cairo_restore(cairo);
	Cairo.cairo_pattern_destroy(pattern);
	Cairo.cairo_surface_destroy(surface);
	GDK.gdk_window_end_paint (window);
	int /*long*/ children = GDK.gdk_window_get_children (window);
	if (children != 0) {
		int /*long*/ windows = children;
		while (windows != 0) {
			int /*long*/ child = OS.g_list_data (windows);
			if (GDK.gdk_window_is_visible (child)) {
				int /*long*/ [] data = new int /*long*/ [1];
				GDK.gdk_window_get_user_data (child, data);
				if (data [0] != 0) {
					Widget widget = display.findWidget (data [0]);
					if (widget == null || widget == control) {
						int [] x_pos = new int [1], y_pos = new int [1];
						GDK.gdk_window_get_position (child, x_pos, y_pos);
						printWindow (false, control, gc, drawable, depth, child, x + x_pos [0], y + y_pos [0]);
					}
				}
			}
			windows = OS.g_list_next (windows);
		}
		OS.g_list_free (children);
	}
}

/**
 * Returns the preferred size (in points) of the receiver.
 * <p>
 * The <em>preferred size</em> of a control is the size that it would
 * best be displayed at. The width hint and height hint arguments
 * allow the caller to ask a control questions such as "Given a particular
 * width, how high does the control need to be to show all of the contents?"
 * To indicate that the caller does not wish to constrain a particular
 * dimension, the constant <code>SWT.DEFAULT</code> is passed for the hint.
 * </p>
 *
 * @param wHint the width hint (can be <code>SWT.DEFAULT</code>)
 * @param hHint the height hint (can be <code>SWT.DEFAULT</code>)
 * @return the preferred size of the control
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Layout
 * @see #getBorderWidth
 * @see #getBounds
 * @see #getSize
 * @see #pack(boolean)
 * @see "computeTrim, getClientArea for controls that implement them"
 */
public Point computeSize (int wHint, int hHint) {
	return computeSize (wHint, hHint, true);
}

Point computeSizeInPixels (int wHint, int hHint) {
	return computeSizeInPixels (wHint, hHint, true);
}

Widget computeTabGroup () {
	if (isTabGroup()) return this;
	return parent.computeTabGroup ();
}

Widget[] computeTabList() {
	if (isTabGroup()) {
		if (getVisible() && getEnabled()) {
			return new Widget[] {this};
		}
	}
	return new Widget[0];
}

Control computeTabRoot () {
	Control[] tabList = parent._getTabList();
	if (tabList != null) {
		int index = 0;
		while (index < tabList.length) {
			if (tabList [index] == this) break;
			index++;
		}
		if (index == tabList.length) {
			if (isTabGroup ()) return this;
		}
	}
	return parent.computeTabRoot ();
}

void checkBuffered () {
	style |= SWT.DOUBLE_BUFFERED;
}

void checkBackground () {
	Shell shell = getShell ();
	if (this == shell) return;
	state &= ~PARENT_BACKGROUND;
	Composite composite = parent;
	do {
		int mode = composite.backgroundMode;
		if (mode != SWT.INHERIT_NONE || backgroundAlpha == 0) {
			if (mode == SWT.INHERIT_DEFAULT || backgroundAlpha == 0) {
				Control control = this;
				do {
					if ((control.state & THEME_BACKGROUND) == 0) {
						return;
					}
					control = control.parent;
				} while (control != composite);
			}
			state |= PARENT_BACKGROUND;
			return;
		}
		if (composite == shell) break;
		composite = composite.parent;
	} while (true);
}

void checkForeground () {
	/*
	* Feature in GTK 3. The widget foreground is inherited from the immediate
	* parent. This is not the expected behavior for SWT. The fix is to avoid
	* the inheritance by explicitly setting the default foreground on the widget.
	*
	* This can be removed on GTK3.16+.
	*/
	if (GTK.GTK_VERSION < OS.VERSION(3, 14, 0) && GTK.GTK_VERSION >= OS.VERSION(3, 0, 0)) {
		setForegroundGdkRGBA (topHandle (), display.COLOR_WIDGET_FOREGROUND_RGBA);
	}
}

void checkBorder () {
	if (getBorderWidthInPixels () == 0) style &= ~SWT.BORDER;
}

void checkMirrored () {
	if ((style & SWT.RIGHT_TO_LEFT) != 0) style |= SWT.MIRRORED;
}

/**
 * Convenience method for checking whether an (x, y) coordinate is in the set
 * region. Only relevant for GTK3.10+.
 *
 * @param x an x coordinate
 * @param y a y coordinate
 * @return true if the coordinate (x, y) is in the region, false otherwise
 */
boolean containedInRegion (int x, int y) {
	if (drawRegion && eventRegion != 0) {
		return Cairo.cairo_region_contains_point(eventRegion, x, y);
	}
	return false;
}

int /*long*/ childStyle () {
	return parent.childStyle ();
}

@Override
void createWidget (int index) {
	state |= DRAG_DETECT;
	checkOrientation (parent);
	super.createWidget (index);
	checkBackground ();
	checkForeground ();
	if ((state & PARENT_BACKGROUND) != 0) setParentBackground ();
	checkBuffered ();
	showWidget ();
	setInitialBounds ();
	setZOrder (null, false, false);
	setRelations ();
	checkMirrored ();
	checkBorder ();
}

/**
 * Returns the preferred size (in points) of the receiver.
 * <p>
 * The <em>preferred size</em> of a control is the size that it would
 * best be displayed at. The width hint and height hint arguments
 * allow the caller to ask a control questions such as "Given a particular
 * width, how high does the control need to be to show all of the contents?"
 * To indicate that the caller does not wish to constrain a particular
 * dimension, the constant <code>SWT.DEFAULT</code> is passed for the hint.
 * </p><p>
 * If the changed flag is <code>true</code>, it indicates that the receiver's
 * <em>contents</em> have changed, therefore any caches that a layout manager
 * containing the control may have been keeping need to be flushed. When the
 * control is resized, the changed flag will be <code>false</code>, so layout
 * manager caches can be retained.
 * </p>
 *
 * @param wHint the width hint (can be <code>SWT.DEFAULT</code>)
 * @param hHint the height hint (can be <code>SWT.DEFAULT</code>)
 * @param changed <code>true</code> if the control's contents have changed, and <code>false</code> otherwise
 * @return the preferred size of the control.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Layout
 * @see #getBorderWidth
 * @see #getBounds
 * @see #getSize
 * @see #pack(boolean)
 * @see "computeTrim, getClientArea for controls that implement them"
 */
public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget();
	if (wHint != SWT.DEFAULT && wHint < 0) wHint = 0;
	if (hHint != SWT.DEFAULT && hHint < 0) hHint = 0;
	wHint = DPIUtil.autoScaleUp(wHint);
	hHint = DPIUtil.autoScaleUp(hHint);
	return DPIUtil.autoScaleDown (computeSizeInPixels (wHint, hHint, changed));
}

Point computeSizeInPixels (int wHint, int hHint, boolean changed) {
	checkWidget();
	if (wHint != SWT.DEFAULT && wHint < 0) wHint = 0;
	if (hHint != SWT.DEFAULT && hHint < 0) hHint = 0;
	return computeNativeSize (handle, wHint, hHint, changed);
}

Point computeNativeSize (int /*long*/ h, int wHint, int hHint, boolean changed) {
	int width = wHint, height = hHint;
	if (GTK.GTK3){
		if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
			GtkRequisition requisition = new GtkRequisition ();
			GTK.gtk_widget_get_preferred_size (h, null, requisition);
			width = requisition.width;
			height = requisition.height;
		} else if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT) {
			int [] natural_size = new int [1];
			if (wHint == SWT.DEFAULT) {
				GTK.gtk_widget_get_preferred_width_for_height (h, height, null, natural_size);
				width = natural_size [0];
			} else {
				GTK.gtk_widget_get_preferred_height_for_width (h, width, null, natural_size);
				height = natural_size [0];
			}
		}
		return new Point(width, height);
	}
	if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
		GtkRequisition requisition = new GtkRequisition ();
		gtk_widget_size_request (h, requisition);
		width = GTK.GTK_WIDGET_REQUISITION_WIDTH (h);
		height = GTK.GTK_WIDGET_REQUISITION_HEIGHT (h);
	} else if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT) {
		int [] reqWidth = new int [1], reqHeight = new int [1];
		GTK.gtk_widget_get_size_request (h, reqWidth, reqHeight);
		GTK.gtk_widget_set_size_request (h, wHint, hHint);
		GtkRequisition requisition = new GtkRequisition ();
		gtk_widget_size_request (h, requisition);
		GTK.gtk_widget_set_size_request (h, reqWidth [0], reqHeight [0]);
		width = wHint == SWT.DEFAULT ? requisition.width : wHint;
		height = hHint == SWT.DEFAULT ? requisition.height : hHint;
	}
	return new Point (width, height);
}

void forceResize () {
	/*
	* Force size allocation on all children of this widget's
	* topHandle.  Note that all calls to gtk_widget_size_allocate()
	* must be preceded by a call to gtk_widget_size_request().
	*/
	int /*long*/ topHandle = topHandle ();
	GtkRequisition requisition = new GtkRequisition ();
	gtk_widget_size_request (topHandle, requisition);
	GtkAllocation allocation = new GtkAllocation ();
	GTK.gtk_widget_get_allocation(topHandle, allocation);
	GTK.gtk_widget_size_allocate (topHandle, allocation);
}

/**
 * Returns the accessible object for the receiver.
 * <p>
 * If this is the first time this object is requested,
 * then the object is created and returned. The object
 * returned by getAccessible() does not need to be disposed.
 * </p>
 *
 * @return the accessible object
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Accessible#addAccessibleListener
 * @see Accessible#addAccessibleControlListener
 *
 * @since 2.0
 */
public Accessible getAccessible () {
	checkWidget ();
	return _getAccessible ();
}

Accessible _getAccessible () {
	if (accessible == null) {
		accessible = Accessible.internal_new_Accessible (this);
	}
	return accessible;
}

/**
 * Returns a rectangle describing the receiver's size and location in points
 * relative to its parent (or its display if its parent is null),
 * unless the receiver is a shell. In this case, the location is
 * relative to the display.
 *
 * @return the receiver's bounding rectangle
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Rectangle getBounds () {
	checkWidget();
	return DPIUtil.autoScaleDown(getBoundsInPixels());
}

Rectangle getBoundsInPixels () {
	checkWidget();
	int /*long*/ topHandle = topHandle ();
	GtkAllocation allocation = new GtkAllocation ();
	GTK.gtk_widget_get_allocation (topHandle, allocation);
	int x = allocation.x;
	int y = allocation.y;
	int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
	int height = (state & ZERO_HEIGHT) != 0 ? 0 :allocation.height;
	if ((parent.style & SWT.MIRRORED) != 0) x = parent.getClientWidth () - width - x;
	return new Rectangle (x, y, width, height);
}

/**
 * Sets the receiver's size and location in points to the rectangular
 * area specified by the argument. The <code>x</code> and
 * <code>y</code> fields of the rectangle are relative to
 * the receiver's parent (or its display if its parent is null).
 * <p>
 * Note: Attempting to set the width or height of the
 * receiver to a negative number will cause that
 * value to be set to zero instead.
 * </p>
 * <p>
 * Note: On GTK, attempting to set the width or height of the
 * receiver to a number higher or equal 2^14 will cause them to be
 * set to (2^14)-1 instead.
 * </p>
 *
 * @param rect the new bounds for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setBounds (Rectangle rect) {
	checkWidget ();
	if (rect == null) error (SWT.ERROR_NULL_ARGUMENT);
	rect = DPIUtil.autoScaleUp(rect);
	setBounds (rect.x, rect.y, Math.max (0, rect.width), Math.max (0, rect.height), true, true);
}

void setBoundsInPixels (Rectangle rect) {
	checkWidget ();
	if (rect == null) error (SWT.ERROR_NULL_ARGUMENT);
	setBounds (rect.x, rect.y, Math.max (0, rect.width), Math.max (0, rect.height), true, true);
}

/**
 * Sets the receiver's size and location in points to the rectangular
 * area specified by the arguments. The <code>x</code> and
 * <code>y</code> arguments are relative to the receiver's
 * parent (or its display if its parent is null), unless
 * the receiver is a shell. In this case, the <code>x</code>
 * and <code>y</code> arguments are relative to the display.
 * <p>
 * Note: Attempting to set the width or height of the
 * receiver to a negative number will cause that
 * value to be set to zero instead.
 * </p>
 * <p>
 * Note: On GTK, attempting to set the width or height of the
 * receiver to a number higher or equal 2^14 will cause them to be
 * set to (2^14)-1 instead.
 * </p>
 *
 * @param x the new x coordinate for the receiver
 * @param y the new y coordinate for the receiver
 * @param width the new width for the receiver
 * @param height the new height for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setBounds (int x, int y, int width, int height) {
	checkWidget();
	Rectangle rect = DPIUtil.autoScaleUp(new Rectangle (x, y, width, height));
	setBounds (rect.x, rect.y, Math.max (0, rect.width), Math.max (0, rect.height), true, true);
}

void setBoundsInPixels (int x, int y, int width, int height) {
	checkWidget();
	setBounds (x, y, Math.max (0, width), Math.max (0, height), true, true);
}

void markLayout (boolean changed, boolean all) {
	/* Do nothing */
}

@Override
void modifyStyle (int /*long*/ handle, int /*long*/ style) {
	super.modifyStyle(handle, style);
	/*
	* Bug in GTK.  When changing the style of a control that
	* has had a region set on it, the region is lost.  The
	* fix is to set the region again.
	*/
	if (region != null) GDK.gdk_window_shape_combine_region (gtk_widget_get_window (topHandle ()), region.handle, 0, 0);
}

void moveHandle (int x, int y) {
	int /*long*/ topHandle = topHandle ();
	int /*long*/ parentHandle = parent.parentingHandle ();
	if (GTK.GTK3) {
		OS.swt_fixed_move (parentHandle, topHandle, x, y);
	} else {
		/*
		* Feature in GTK.  Calling gtk_fixed_move() to move a child causes
		* the whole parent to redraw.  This is a performance problem. The
		* fix is temporarily mark the parent not visible during the move.
		*
		* NOTE: Because every widget in SWT has an X window, the new and
		* old bounds of the child are correctly redrawn.
		*
		* NOTE: There is no API in GTK 3 to only set the GTK_VISIBLE bit.
		*/
		boolean reset = GTK.gtk_widget_get_visible (parentHandle);
		gtk_widget_set_visible (parentHandle, false);
		GTK.gtk_fixed_move (parentHandle, topHandle, x, y);
		gtk_widget_set_visible (parentHandle, reset);
	}
}

void resizeHandle (int width, int height) {
	int /*long*/ topHandle = topHandle ();
	if (GTK.GTK3) {
		OS.swt_fixed_resize (GTK.gtk_widget_get_parent (topHandle), topHandle, width, height);
		if (topHandle != handle) {
			Point sizes = resizeCalculationsGTK3 (handle, width, height);
			width = sizes.x;
			height = sizes.y;
			OS.swt_fixed_resize (GTK.gtk_widget_get_parent (handle), handle, width, height);
		}
	} else {
		GTK.gtk_widget_set_size_request (topHandle, width, height);
		if (topHandle != handle) GTK.gtk_widget_set_size_request (handle, width, height);
	}
}

Point resizeCalculationsGTK3 (int /*long*/ widget, int width, int height) {
	Point sizes = new Point (width, height);
	/*
	 * Feature in GTK3.20+: size calculations take into account GtkCSSNode
	 * elements which we cannot access. If the to-be-allocated size minus
	 * these elements is < 0, allocate the preferred size instead. See bug 486068.
	 */
	if (GTK.GTK_VERSION >= OS.VERSION(3, 20, 0)) {
		GtkRequisition minimumSize = new GtkRequisition();
		GtkRequisition naturalSize = new GtkRequisition();
		GTK.gtk_widget_get_preferred_size(widget, minimumSize, naturalSize);
		/*
		 * Use the smallest of the minimum/natural sizes to prevent oversized
		 * widgets.
		 */
		int smallestWidth = Math.min(minimumSize.width, naturalSize.width);
		int smallestHeight = Math.min(minimumSize.height, naturalSize.height);
		sizes.x = (width - (smallestWidth - width)) < 0 ? smallestWidth : width;
		sizes.y = (height - (smallestHeight - height)) < 0 ? smallestHeight : height;
	}
	return sizes;
}

int setBounds (int x, int y, int width, int height, boolean move, boolean resize) {
	// bug in GTK2 crashes JVM, in GTK3 the new shell only. See bug 472743
	width = Math.min(width, (2 << 14) - 1);
	height = Math.min(height, (2 << 14) - 1);

	int /*long*/ topHandle = topHandle ();
	boolean sendMove = move;
	GtkAllocation allocation = new GtkAllocation ();
	GTK.gtk_widget_get_allocation (topHandle, allocation);
	if ((parent.style & SWT.MIRRORED) != 0) {
		int clientWidth = parent.getClientWidth ();
		int oldWidth = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		int oldX = clientWidth - oldWidth - allocation.x;
		if (move) {
			sendMove &= x != oldX;
			x = clientWidth - (resize ? width : oldWidth) - x;
		} else {
			move = true;
			x = clientWidth - (resize ? width : oldWidth) - oldX;
			y = allocation.y;
		}
	}
	boolean sameOrigin = true, sameExtent = true;
	if (move) {
		int oldX = allocation.x;
		int oldY = allocation.y;
		sameOrigin = x == oldX && y == oldY;
		if (!sameOrigin) {
			if (enableWindow != 0) {
				GDK.gdk_window_move (enableWindow, x, y);
			}
			moveHandle (x, y);
		}
	}
	int clientWidth = 0;
	if (resize) {
		int oldWidth = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		int oldHeight = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
		sameExtent = width == oldWidth && height == oldHeight;
		if (!sameExtent && (style & SWT.MIRRORED) != 0) clientWidth = getClientWidth ();
		if (!sameExtent && !(width == 0 && height == 0)) {
			int newWidth = Math.max (1, width);
			int newHeight = Math.max (1, height);
			if (redrawWindow != 0) {
				GDK.gdk_window_resize (redrawWindow, newWidth, newHeight);
			}
			if (enableWindow != 0) {
				GDK.gdk_window_resize (enableWindow, newWidth, newHeight);
			}
			resizeHandle (newWidth, newHeight);
		}
	}
	if (!sameOrigin || !sameExtent) {
		/*
		* Cause a size allocation this widget's topHandle.  Note that
		* all calls to gtk_widget_size_allocate() must be preceded by
		* a call to gtk_widget_size_request().
		*/
		GtkRequisition requisition = new GtkRequisition ();
		gtk_widget_size_request (topHandle, requisition);
		if (move) {
			allocation.x = x;
			allocation.y = y;
		}
		if (resize) {
			allocation.width = width;
			allocation.height = height;
		}
		/*
		 * The widget needs to be shown before its size is allocated
		 * in GTK 3.8 otherwise its allocation return 0
                 * See org.eclipse.swt.tests.gtk.snippets.Bug497705_setBoundsAfterSetVisible
		 */
		if (GTK.GTK_VERSION >= OS.VERSION (3, 8, 0) && !GTK.gtk_widget_get_visible(topHandle))  {
			GTK.gtk_widget_show(topHandle);
			gtk_widget_get_preferred_size (topHandle, requisition);
			GTK.gtk_widget_size_allocate (topHandle, allocation);
			GTK.gtk_widget_hide(topHandle);
		} else {
			GTK.gtk_widget_size_allocate (topHandle, allocation);
		}
	}
	/*
	* Bug in GTK.  Widgets cannot be sized smaller than 1x1.
	* The fix is to hide zero-sized widgets and show them again
	* when they are resized larger.
	*/
	if (!sameExtent) {
		state = (width == 0) ? state | ZERO_WIDTH : state & ~ZERO_WIDTH;
		state = (height == 0) ? state | ZERO_HEIGHT : state & ~ZERO_HEIGHT;
		if ((state & (ZERO_WIDTH | ZERO_HEIGHT)) != 0) {
			if (enableWindow != 0) {
				GDK.gdk_window_hide (enableWindow);
			}
			GTK.gtk_widget_hide (topHandle);
		} else {
			if ((state & HIDDEN) == 0) {
				if (enableWindow != 0) {
					GDK.gdk_window_show_unraised (enableWindow);
				}
				GTK.gtk_widget_show (topHandle);
			}
		}
		if ((style & SWT.MIRRORED) != 0) moveChildren (clientWidth);
	}
	int result = 0;
	if (move && !sameOrigin) {
		Control control = findBackgroundControl ();
		if (control != null && control.backgroundImage != null) {
			if (isVisible ()) redrawWidget (0, 0, 0, 0, true, true, true);
		}
		if (sendMove) sendEvent (SWT.Move);
		result |= MOVED;
	}
	if (resize && !sameExtent) {
		sendEvent (SWT.Resize);
		result |= RESIZED;
	}
	return result;
}

/**
 * Returns a point describing the receiver's location relative
 * to its parent in points (or its display if its parent is null), unless
 * the receiver is a shell. In this case, the point is
 * relative to the display.
 *
 * @return the receiver's location
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point getLocation () {
	checkWidget();
	return DPIUtil.autoScaleDown(getLocationInPixels());
}

Point getLocationInPixels () {
	checkWidget();
	int /*long*/ topHandle = topHandle ();
	GtkAllocation allocation = new GtkAllocation ();
	GTK.gtk_widget_get_allocation (topHandle, allocation);
	int x = allocation.x;
	int y = allocation.y;
	if ((parent.style & SWT.MIRRORED) != 0) {
		int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		x = parent.getClientWidth () - width - x;
	}
	return new Point (x, y);
}

/**
 * Sets the receiver's location to the point specified by
 * the arguments which are relative to the receiver's
 * parent (or its display if its parent is null), unless
 * the receiver is a shell. In this case, the point is
 * relative to the display.
 *
 * @param location the new location for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLocation (Point location) {
	checkWidget ();
	if (location == null) error (SWT.ERROR_NULL_ARGUMENT);
	location = DPIUtil.autoScaleUp(location);
	setBounds (location.x, location.y, 0, 0, true, false);
}

void setLocationInPixels (Point location) {
	checkWidget ();
	if (location == null) error (SWT.ERROR_NULL_ARGUMENT);
	setBounds (location.x, location.y, 0, 0, true, false);
}

/**
 * Sets the receiver's location to the point specified by
 * the arguments which are relative to the receiver's
 * parent (or its display if its parent is null), unless
 * the receiver is a shell. In this case, the point is
 * relative to the display.
 *
 * @param x the new x coordinate for the receiver
 * @param y the new y coordinate for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLocation(int x, int y) {
	checkWidget();
	Point loc = DPIUtil.autoScaleUp(new Point (x, y));
	setBounds (loc.x, loc.y, 0, 0, true, false);
}

void setLocationInPixels(int x, int y) {
	checkWidget();
	setBounds (x, y, 0, 0, true, false);
}

/**
 * Returns a point describing the receiver's size in points. The
 * x coordinate of the result is the width of the receiver.
 * The y coordinate of the result is the height of the
 * receiver.
 *
 * @return the receiver's size
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point getSize () {
	checkWidget();
	return DPIUtil.autoScaleDown(getSizeInPixels());
}

Point getSizeInPixels () {
	checkWidget();
	int /*long*/ topHandle = topHandle ();
	GtkAllocation allocation = new GtkAllocation ();
	GTK.gtk_widget_get_allocation (topHandle, allocation);
	int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
	int height = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
	return new Point (width, height);
}

/**
 * Sets the receiver's size to the point specified by the argument.
 * <p>
 * Note: Attempting to set the width or height of the
 * receiver to a negative number will cause them to be
 * set to zero instead.
 * </p>
 * <p>
 * Note: On GTK, attempting to set the width or height of the
 * receiver to a number higher or equal 2^14 will cause them to be
 * set to (2^14)-1 instead.
 * </p>
 *
 * @param size the new size in points for the receiver
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setSize (Point size) {
	checkWidget ();
	if (size == null) error (SWT.ERROR_NULL_ARGUMENT);
	size = DPIUtil.autoScaleUp(size);
	setBounds (0, 0, Math.max (0, size.x), Math.max (0, size.y), false, true);
}

void setSizeInPixels (Point size) {
	checkWidget ();
	if (size == null) error (SWT.ERROR_NULL_ARGUMENT);
	setBounds (0, 0, Math.max (0, size.x), Math.max (0, size.y), false, true);
}

/**
 * Sets the shape of the control to the region specified
 * by the argument.  When the argument is null, the
 * default shape of the control is restored.
 *
 * @param region the region that defines the shape of the control (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the region has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.4
 */
public void setRegion (Region region) {
	checkWidget ();
	if (region != null && region.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
	int /*long*/ shape_region = (region == null) ? 0 : region.handle;
	this.region = region;
	int /*long*/ topHandle = topHandle ();
	/*
	 * Only call gdk_window_shape_combine_region on GTK3.10-, or if the widget is
	 * a GtkWindow.
	 */
	if (GTK.GTK_VERSION < OS.VERSION(3, 10, 0) || OS.G_OBJECT_TYPE(topHandle) == GTK.GTK_TYPE_WINDOW()) {
		int /*long*/ window = gtk_widget_get_window (topHandle);
		GDK.gdk_window_shape_combine_region (window, shape_region, 0, 0);
	} else {
		drawRegion = (this.region != null && this.region.handle != 0);
		if (drawRegion) {
			cairoCopyRegion(this.region);
		} else {
			cairoDisposeRegion();
		}
		GTK.gtk_widget_queue_draw(topHandle());
	}
}

void setRelations () {
	int /*long*/ parentHandle = parent.parentingHandle ();
	int /*long*/ list = GTK.gtk_container_get_children (parentHandle);
	if (list == 0) return;
	int count = OS.g_list_length (list);
	if (count > 1) {
		/*
		 * the receiver is the last item in the list, so its predecessor will
		 * be the second-last item in the list
		 */
		int /*long*/ handle = OS.g_list_nth_data (list, count - 2);
		if (handle != 0) {
			Widget widget = display.getWidget (handle);
			if (widget != null && widget != this) {
				if (widget instanceof Control) {
					Control sibling = (Control)widget;
					sibling.addRelation (this);
				}
			}
		}
	}
	OS.g_list_free (list);
}

/**
 * Sets the receiver's size to the point specified by the arguments.
 * <p>
 * Note: Attempting to set the width or height of the
 * receiver to a negative number will cause that
 * value to be set to zero instead.
 * </p>
 * <p>
 * Note: On GTK, attempting to set the width or height of the
 * receiver to a number higher or equal 2^14 will cause them to be
 * set to (2^14)-1 instead.
 * </p>
 *
 * @param width the new width in points for the receiver
 * @param height the new height in points for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setSize (int width, int height) {
	checkWidget();
	Point size = DPIUtil.autoScaleUp(new Point (width, height));
	setBounds (0, 0, Math.max (0, size.x), Math.max (0, size.y), false, true);
}

void setSizeInPixels (int width, int height) {
	checkWidget();
	setBounds (0, 0, Math.max (0, width), Math.max (0, height), false, true);
}


@Override
boolean isActive () {
	return getShell ().getModalShell () == null && display.getModalDialog () == null;
}

/*
 * Answers a boolean indicating whether a Label that precedes the receiver in
 * a layout should be read by screen readers as the recevier's label.
 */
boolean isDescribedByLabel () {
	return true;
}

boolean isFocusHandle (int /*long*/ widget) {
	return widget == focusHandle ();
}

/**
 * Moves the receiver above the specified control in the
 * drawing order. If the argument is null, then the receiver
 * is moved to the top of the drawing order. The control at
 * the top of the drawing order will not be covered by other
 * controls even if they occupy intersecting areas.
 *
 * @param control the sibling control (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the control has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Control#moveBelow
 * @see Composite#getChildren
 */
public void moveAbove (Control control) {
	checkWidget();
	if (control != null) {
		if (control.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
		if (parent != control.parent) return;
		if (this == control) return;
	}
	setZOrder (control, true, true);
}

/**
 * Moves the receiver below the specified control in the
 * drawing order. If the argument is null, then the receiver
 * is moved to the bottom of the drawing order. The control at
 * the bottom of the drawing order will be covered by all other
 * controls which occupy intersecting areas.
 *
 * @param control the sibling control (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the control has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Control#moveAbove
 * @see Composite#getChildren
 */
public void moveBelow (Control control) {
	checkWidget();
	if (control != null) {
		if (control.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
		if (parent != control.parent) return;
		if (this == control) return;
	}
	setZOrder (control, false, true);
}

void moveChildren (int oldWidth) {
}

/**
 * Causes the receiver to be resized to its preferred size.
 * For a composite, this involves computing the preferred size
 * from its layout, if there is one.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #computeSize(int, int, boolean)
 */
public void pack () {
	pack (true);
}

/**
 * Causes the receiver to be resized to its preferred size.
 * For a composite, this involves computing the preferred size
 * from its layout, if there is one.
 * <p>
 * If the changed flag is <code>true</code>, it indicates that the receiver's
 * <em>contents</em> have changed, therefore any caches that a layout manager
 * containing the control may have been keeping need to be flushed. When the
 * control is resized, the changed flag will be <code>false</code>, so layout
 * manager caches can be retained.
 * </p>
 *
 * @param changed whether or not the receiver's contents have changed
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #computeSize(int, int, boolean)
 */
public void pack (boolean changed) {
	setSize (computeSize (SWT.DEFAULT, SWT.DEFAULT, changed));
}

/**
 * Sets the layout data associated with the receiver to the argument.
 *
 * @param layoutData the new layout data for the receiver.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLayoutData (Object layoutData) {
	checkWidget();
	this.layoutData = layoutData;
}

/**
 * Returns a point which is the result of converting the
 * argument, which is specified in display relative coordinates,
 * to coordinates relative to the receiver.
 * <p>
 * NOTE: To properly map a rectangle or a corner of a rectangle on a right-to-left platform, use
 * {@link Display#map(Control, Control, Rectangle)}.
 * </p>
 *
 * @param x the x coordinate in points to be translated
 * @param y the y coordinate in points to be translated
 * @return the translated coordinates
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 2.1
 */
public Point toControl (int x, int y) {
	checkWidget ();
	int /*long*/ window = eventWindow ();
	int [] origin_x = new int [1], origin_y = new int [1];
	GDK.gdk_window_get_origin (window, origin_x, origin_y);
	x -= DPIUtil.autoScaleDown (origin_x [0]);
	y -= DPIUtil.autoScaleDown (origin_y [0]);
	if ((style & SWT.MIRRORED) != 0) x = DPIUtil.autoScaleDown (getClientWidth ()) - x;
	return new Point (x, y);
}

/**
 * Returns a point which is the result of converting the
 * argument, which is specified in display relative coordinates,
 * to coordinates relative to the receiver.
 * <p>
 * NOTE: To properly map a rectangle or a corner of a rectangle on a right-to-left platform, use
 * {@link Display#map(Control, Control, Rectangle)}.
 * </p>
 *
 * @param point the point to be translated (must not be null)
 * @return the translated coordinates
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point toControl (Point point) {
	checkWidget ();
	if (point == null) error (SWT.ERROR_NULL_ARGUMENT);
	return toControl (point.x, point.y);
}

/**
 * Returns a point which is the result of converting the
 * argument, which is specified in coordinates relative to
 * the receiver, to display relative coordinates.
 * <p>
 * NOTE: To properly map a rectangle or a corner of a rectangle on a right-to-left platform, use
 * {@link Display#map(Control, Control, Rectangle)}.
 * </p>
 *
 * @param x the x coordinate to be translated
 * @param y the y coordinate to be translated
 * @return the translated coordinates
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 2.1
 */
public Point toDisplay (int x, int y) {
	checkWidget();
	int /*long*/ window = eventWindow ();
	int [] origin_x = new int [1], origin_y = new int [1];
	GDK.gdk_window_get_origin (window, origin_x, origin_y);
	if ((style & SWT.MIRRORED) != 0) x = DPIUtil.autoScaleDown (getClientWidth ()) - x;
	x += DPIUtil.autoScaleDown (origin_x [0]);
	y += DPIUtil.autoScaleDown (origin_y [0]);
	return new Point (x, y);
}

Point toDisplayInPixels (int x, int y) {
	checkWidget();
	int /*long*/ window = eventWindow ();
	int [] origin_x = new int [1], origin_y = new int [1];
	GDK.gdk_window_get_origin (window, origin_x, origin_y);
	if ((style & SWT.MIRRORED) != 0) x = getClientWidth () - x;
	x += origin_x [0];
	y += origin_y [0];
	return new Point (x, y);
}

/**
 * Returns a point which is the result of converting the
 * argument, which is specified in coordinates relative to
 * the receiver, to display relative coordinates.
 * <p>
 * NOTE: To properly map a rectangle or a corner of a rectangle on a right-to-left platform, use
 * {@link Display#map(Control, Control, Rectangle)}.
 * </p>
 *
 * @param point the point to be translated (must not be null)
 * @return the translated coordinates
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point toDisplay (Point point) {
	checkWidget();
	if (point == null) error (SWT.ERROR_NULL_ARGUMENT);
	return toDisplay (point.x, point.y);
}

Point toDisplayInPixels (Point point) {
	checkWidget();
	if (point == null) error (SWT.ERROR_NULL_ARGUMENT);
	return toDisplayInPixels (point.x, point.y);
}
/**
 * Adds the listener to the collection of listeners who will
 * be notified when the control is moved or resized, by sending
 * it one of the messages defined in the <code>ControlListener</code>
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
 * @see ControlListener
 * @see #removeControlListener
 */
public void addControlListener(ControlListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Resize,typedListener);
	addListener (SWT.Move,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when a drag gesture occurs, by sending it
 * one of the messages defined in the <code>DragDetectListener</code>
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
 * @see DragDetectListener
 * @see #removeDragDetectListener
 *
 * @since 3.3
 */
public void addDragDetectListener (DragDetectListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.DragDetect,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the control gains or loses focus, by sending
 * it one of the messages defined in the <code>FocusListener</code>
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
 * @see FocusListener
 * @see #removeFocusListener
 */
public void addFocusListener(FocusListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.FocusIn,typedListener);
	addListener(SWT.FocusOut,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when gesture events are generated for the control,
 * by sending it one of the messages defined in the
 * <code>GestureListener</code> interface.
 * <p>
 * NOTE: If <code>setTouchEnabled(true)</code> has previously been
 * invoked on the receiver then <code>setTouchEnabled(false)</code>
 * must be invoked on it to specify that gesture events should be
 * sent instead of touch events.
 * </p>
 * <p>
 * <b>Warning</b>: This API is currently only implemented on Windows and Cocoa.
 * SWT doesn't send Gesture or Touch events on GTK.
 * </p>
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
 * @see GestureListener
 * @see #removeGestureListener
 * @see #setTouchEnabled
 *
 * @since 3.7
 */
public void addGestureListener (GestureListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Gesture, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when help events are generated for the control,
 * by sending it one of the messages defined in the
 * <code>HelpListener</code> interface.
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
 * @see HelpListener
 * @see #removeHelpListener
 */
public void addHelpListener (HelpListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Help, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when keys are pressed and released on the system keyboard, by sending
 * it one of the messages defined in the <code>KeyListener</code>
 * interface.
 * <p>
 * When a key listener is added to a control, the control
 * will take part in widget traversal.  By default, all
 * traversal keys (such as the tab key and so on) are
 * delivered to the control.  In order for a control to take
 * part in traversal, it should listen for traversal events.
 * Otherwise, the user can traverse into a control but not
 * out.  Note that native controls such as table and tree
 * implement key traversal in the operating system.  It is
 * not necessary to add traversal listeners for these controls,
 * unless you want to override the default traversal.
 * </p>
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
 * @see KeyListener
 * @see #removeKeyListener
 */
public void addKeyListener(KeyListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.KeyUp,typedListener);
	addListener(SWT.KeyDown,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the platform-specific context menu trigger
 * has occurred, by sending it one of the messages defined in
 * the <code>MenuDetectListener</code> interface.
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
 * @see MenuDetectListener
 * @see #removeMenuDetectListener
 *
 * @since 3.3
 */
public void addMenuDetectListener (MenuDetectListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.MenuDetect, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when mouse buttons are pressed and released, by sending
 * it one of the messages defined in the <code>MouseListener</code>
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
 * @see MouseListener
 * @see #removeMouseListener
 */
public void addMouseListener(MouseListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.MouseDown,typedListener);
	addListener(SWT.MouseUp,typedListener);
	addListener(SWT.MouseDoubleClick,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the mouse moves, by sending it one of the
 * messages defined in the <code>MouseMoveListener</code>
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
 * @see MouseMoveListener
 * @see #removeMouseMoveListener
 */
public void addMouseMoveListener(MouseMoveListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.MouseMove,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the mouse passes or hovers over controls, by sending
 * it one of the messages defined in the <code>MouseTrackListener</code>
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
 * @see MouseTrackListener
 * @see #removeMouseTrackListener
 */
public void addMouseTrackListener (MouseTrackListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.MouseEnter,typedListener);
	addListener (SWT.MouseExit,typedListener);
	addListener (SWT.MouseHover,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the mouse wheel is scrolled, by sending
 * it one of the messages defined in the
 * <code>MouseWheelListener</code> interface.
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
 * @see MouseWheelListener
 * @see #removeMouseWheelListener
 *
 * @since 3.3
 */
public void addMouseWheelListener (MouseWheelListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.MouseWheel, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the receiver needs to be painted, by sending it
 * one of the messages defined in the <code>PaintListener</code>
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
 * @see PaintListener
 * @see #removePaintListener
 */
public void addPaintListener(PaintListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener(SWT.Paint,typedListener);
}

void addRelation (Control control) {
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when touch events occur, by sending it
 * one of the messages defined in the <code>TouchListener</code>
 * interface.
 * <p>
 * NOTE: You must also call <code>setTouchEnabled(true)</code> to
 * specify that touch events should be sent, which will cause gesture
 * events to not be sent.
 * </p>
 * <p>
 * <b>Warning</b>: This API is currently only implemented on Windows and Cocoa.
 * SWT doesn't send Gesture or Touch events on GTK.
 * </p>
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
 * @see TouchListener
 * @see #removeTouchListener
 * @see #setTouchEnabled
 *
 * @since 3.7
 */
public void addTouchListener (TouchListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Touch,typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when traversal events occur, by sending it
 * one of the messages defined in the <code>TraverseListener</code>
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
 * @see TraverseListener
 * @see #removeTraverseListener
 */
public void addTraverseListener (TraverseListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Traverse,typedListener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control is moved or resized.
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
 * @see ControlListener
 * @see #addControlListener
 */
public void removeControlListener (ControlListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Move, listener);
	eventTable.unhook (SWT.Resize, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when a drag gesture occurs.
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
 * @see DragDetectListener
 * @see #addDragDetectListener
 *
 * @since 3.3
 */
public void removeDragDetectListener(DragDetectListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.DragDetect, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the control gains or loses focus.
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
 * @see FocusListener
 * @see #addFocusListener
 */
public void removeFocusListener(FocusListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.FocusIn, listener);
	eventTable.unhook (SWT.FocusOut, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when gesture events are generated for the control.
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
 * @see GestureListener
 * @see #addGestureListener
 *
 * @since 3.7
 */
public void removeGestureListener (GestureListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook(SWT.Gesture, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when the help events are generated for the control.
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
 * @see HelpListener
 * @see #addHelpListener
 */
public void removeHelpListener (HelpListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Help, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when keys are pressed and released on the system keyboard.
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
 * @see KeyListener
 * @see #addKeyListener
 */
public void removeKeyListener(KeyListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.KeyUp, listener);
	eventTable.unhook (SWT.KeyDown, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when the platform-specific context menu trigger has
 * occurred.
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
 * @see MenuDetectListener
 * @see #addMenuDetectListener
 *
 * @since 3.3
 */
public void removeMenuDetectListener (MenuDetectListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MenuDetect, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when mouse buttons are pressed and released.
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
 * @see MouseListener
 * @see #addMouseListener
 */
public void removeMouseListener (MouseListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MouseDown, listener);
	eventTable.unhook (SWT.MouseUp, listener);
	eventTable.unhook (SWT.MouseDoubleClick, listener);
}
/**
 * Removes the listener from the collection of listeners who will
 * be notified when the mouse moves.
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
 * @see MouseMoveListener
 * @see #addMouseMoveListener
 */
public void removeMouseMoveListener(MouseMoveListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MouseMove, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the mouse passes or hovers over controls.
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
 * @see MouseTrackListener
 * @see #addMouseTrackListener
 */
public void removeMouseTrackListener(MouseTrackListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MouseEnter, listener);
	eventTable.unhook (SWT.MouseExit, listener);
	eventTable.unhook (SWT.MouseHover, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the mouse wheel is scrolled.
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
 * @see MouseWheelListener
 * @see #addMouseWheelListener
 *
 * @since 3.3
 */
public void removeMouseWheelListener (MouseWheelListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.MouseWheel, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the receiver needs to be painted.
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
 * @see PaintListener
 * @see #addPaintListener
 */
public void removePaintListener(PaintListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook(SWT.Paint, listener);
}

/*
 * Remove "Labelled by" relation from the receiver.
 */
void removeRelation () {
	if (!isDescribedByLabel ()) return;		/* there will not be any */
	if (labelRelation != null) {
		_getAccessible().removeRelation (ACC.RELATION_LABELLED_BY, labelRelation._getAccessible());
		labelRelation = null;
	}
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when touch events occur.
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
 * @see TouchListener
 * @see #addTouchListener
 *
 * @since 3.7
 */
public void removeTouchListener(TouchListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Touch, listener);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when traversal events occur.
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
 * @see TraverseListener
 * @see #addTraverseListener
 */
public void removeTraverseListener(TraverseListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Traverse, listener);
}

/**
 * Detects a drag and drop gesture.  This method is used
 * to detect a drag gesture when called from within a mouse
 * down listener.
 *
 * <p>By default, a drag is detected when the gesture
 * occurs anywhere within the client area of a control.
 * Some controls, such as tables and trees, override this
 * behavior.  In addition to the operating system specific
 * drag gesture, they require the mouse to be inside an
 * item.  Custom widget writers can use <code>setDragDetect</code>
 * to disable the default detection, listen for mouse down,
 * and then call <code>dragDetect()</code> from within the
 * listener to conditionally detect a drag.
 * </p>
 *
 * @param event the mouse down event
 *
 * @return <code>true</code> if the gesture occurred, and <code>false</code> otherwise.
 *
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT if the event is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see DragDetectListener
 * @see #addDragDetectListener
 *
 * @see #getDragDetect
 * @see #setDragDetect
 *
 * @since 3.3
 */
public boolean dragDetect (Event event) {
	checkWidget ();
	if (event == null) error (SWT.ERROR_NULL_ARGUMENT);
	return dragDetect (event.button, event.count, event.stateMask, event.x, event.y);
}

/**
 * Detects a drag and drop gesture.  This method is used
 * to detect a drag gesture when called from within a mouse
 * down listener.
 *
 * <p>By default, a drag is detected when the gesture
 * occurs anywhere within the client area of a control.
 * Some controls, such as tables and trees, override this
 * behavior.  In addition to the operating system specific
 * drag gesture, they require the mouse to be inside an
 * item.  Custom widget writers can use <code>setDragDetect</code>
 * to disable the default detection, listen for mouse down,
 * and then call <code>dragDetect()</code> from within the
 * listener to conditionally detect a drag.
 * </p>
 *
 * @param event the mouse down event
 *
 * @return <code>true</code> if the gesture occurred, and <code>false</code> otherwise.
 *
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT if the event is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see DragDetectListener
 * @see #addDragDetectListener
 *
 * @see #getDragDetect
 * @see #setDragDetect
 *
 * @since 3.3
 */
public boolean dragDetect (MouseEvent event) {
	checkWidget ();
	if (event == null) error (SWT.ERROR_NULL_ARGUMENT);
	return dragDetect (event.button, event.count, event.stateMask, event.x, event.y);
}

boolean dragDetect (int button, int count, int stateMask, int x, int y) {
	if (button != 1 || count != 1) return false;
	if (!dragDetect (x, y, false, true, null)) {
		return false;
	}
	return sendDragEvent (button, stateMask, x, y, true);
}

boolean dragDetect (int x, int y, boolean filter, boolean dragOnTimeout, boolean [] consume) {
	boolean dragging = false;
	/*
	 * Feature in GTK: In order to support both X.11/Wayland, GTKGestures are used
	 *  as of GTK3.14 in order to acquire mouse position offsets to decide on dragging.
	 *  See Bug 503431.
	 */
	if (!OS.isX11()) { // Wayland
  		double [] offsetX = new double[1];
		double [] offsetY = new double [1];
		double [] startX = new double[1];
		double [] startY = new double [1];
		if (GTK.gtk_gesture_drag_get_start_point(dragGesture, startX, startY)) {
			GTK.gtk_gesture_drag_get_offset(dragGesture, offsetX, offsetY);
			if (GTK.gtk_drag_check_threshold(handle, (int)startX[0], (int) startY[0], (int) startX[0]
					+ (int) offsetX[0], (int) startY[0] + (int) offsetY[0])) {
				dragging = true;
			}
		} else {
			return false;
		}
	} else {
		boolean quit = false;
		//428852 DND workaround for GTk3.
		//Gtk3 no longer sends motion events on the same control during thread sleep
		//before a drag started. This is due to underlying gdk changes.
		//Thus for gtk3 we check mouse coords manually
		//Note, input params x/y are relative, the two points below are absolute coords.
		Point startPos = null;
		Point currPos = null;
		if (GTK.GTK3) {
			startPos = display.getCursorLocationInPixels();
		}

		while (!quit) {
			int /*long*/ eventPtr = 0;
			/*
			* There should be an event on the queue already, but
			* in cases where there isn't one, stop trying after
			* half a second.
			*/
			long timeout = System.currentTimeMillis() + 500;
			display.sendPreExternalEventDispatchEvent();
			while (System.currentTimeMillis() < timeout) {
				eventPtr = GDK.gdk_event_get ();
				if (eventPtr != 0) {
					break;
				} else {
					if (GTK.GTK3) { //428852
						currPos = display.getCursorLocationInPixels();
						dragging = GTK.gtk_drag_check_threshold (handle,
									startPos.x, startPos.y, currPos.x, currPos.y);
						if (dragging) break;
					} else {
					try {Thread.sleep(50);}
					catch (Exception ex) {}
					}
				}
			}
			display.sendPostExternalEventDispatchEvent();
			if (dragging) return true;  //428852
			if (eventPtr == 0) return dragOnTimeout;
			switch (GDK.GDK_EVENT_TYPE (eventPtr)) {
				case GDK.GDK_MOTION_NOTIFY: {
					GdkEventMotion gdkMotionEvent = new GdkEventMotion ();
					OS.memmove (gdkMotionEvent, eventPtr, GdkEventMotion.sizeof);
					if ((gdkMotionEvent.state & GDK.GDK_BUTTON1_MASK) != 0) {
						if (GTK.gtk_drag_check_threshold (handle, x, y, (int) gdkMotionEvent.x, (int) gdkMotionEvent.y)) {
							dragging = true;
							quit = true;
						}
					} else {
						quit = true;
					}
					int [] newX = new int [1], newY = new int [1];
					gdk_window_get_device_position (gdkMotionEvent.window, newX, newY, null);
					break;
				}
				case GDK.GDK_KEY_PRESS:
				case GDK.GDK_KEY_RELEASE: {
					GdkEventKey gdkEvent = new GdkEventKey ();
					OS.memmove (gdkEvent, eventPtr, GdkEventKey.sizeof);
					if (gdkEvent.keyval == GDK.GDK_Escape) quit = true;
					break;
				}
				case GDK.GDK_BUTTON_RELEASE:
				case GDK.GDK_BUTTON_PRESS:
				case GDK.GDK_2BUTTON_PRESS:
				case GDK.GDK_3BUTTON_PRESS: {
					GDK.gdk_event_put (eventPtr);
					quit = true;
					break;
				}
				default:
					GTK.gtk_main_do_event (eventPtr);
			}
			GDK.gdk_event_free (eventPtr);
		}
	}
	return dragging;
}

boolean filterKey (int keyval, int /*long*/ event) {
	int /*long*/ imHandle = imHandle ();
	if (imHandle != 0) {
		return GTK.gtk_im_context_filter_keypress (imHandle, event);
	}
	return false;
}

Control findBackgroundControl () {
	if (((state & BACKGROUND) != 0 || backgroundImage != null) && backgroundAlpha > 0) return this;
	return (parent != null && (state & PARENT_BACKGROUND) != 0) ? parent.findBackgroundControl () : null;
}

Menu [] findMenus (Control control) {
	if (menu != null && this != control) return new Menu [] {menu};
	return new Menu [0];
}

void fixChildren (Shell newShell, Shell oldShell, Decorations newDecorations, Decorations oldDecorations, Menu [] menus) {
	oldShell.fixShell (newShell, this);
	oldDecorations.fixDecorations (newDecorations, this, menus);
}

/**
 * In some situations, a control has a non-standard parent GdkWindow (Note gDk, not gTk).
 * E.g, an TreeEditor who's parent is a Tree should have the Tree Viewer's inner bin as parent window.
 *
 * Note, composites should treat this differently and take child controls into consideration.
 */
void fixParentGdkWindow() {
	assert GTK.GTK3;
	// Changes to this method should be verified via
	// org.eclipse.swt.tests.gtk/*/Bug510803_TabFolder_TreeEditor_Regression.java (part one)
	parent.setParentGdkWindow(this);
}

/**
 * Native gtkwidget re-parenting in SWT on Gtk3 needs to be handled in a special way because
 * some controls have non-standard GdkWindow as parents. (E.g ControlEditors), and other controls
 * like TabItem and ExpandBar use reparenting to preserve proper hierarchy for correct event traversal (like dnd).
 *
 * Note, GdkWindows != GtkWindows.
 *
 * You should never call gtk_widget_reparent() directly or reparent widgets outside this method,
 * otherwise you can break TabItem/TreeEditors.
 *
 * @param control that should be reparented.
 * @param newParentHandle pointer/handle to the new GtkWidget parent.
 */
static void gtk_widget_reparent (Control control, int /*long*/ newParentHandle) {
	if (GTK.GTK3) {
		// Changes to this method should be verified via both parts in:
		// org.eclipse.swt.tests.gtk/*/Bug510803_TabFolder_TreeEditor_Regression.java
		int /*long*/ widget = control.topHandle();
		int /*long*/ parentContainer = GTK.gtk_widget_get_parent (widget);
		assert parentContainer != 0 : "Improper use of Control.gtk_widget_reparent. Widget currently has no parent.";
		if (parentContainer != 0) {

			// gtk_widget_reparent (..) is deprecated as of Gtk 3.14 and removed in Gtk4.
			// However, the current alternative of removing/adding widget from/to a container causes errors. (see note below).
			// TODO - research a better way to reparent. See 534089.
			GTK.gtk_widget_reparent(widget, newParentHandle);

			// Removing/Adding containers doesn't seem to reparent sub-gdkWindows properly and throws errors.
			// Steps to reproduce:
			//  - From bug 534089, download the first attachment plugin: "Plug-in to reproduce the problem with"
			//  - Import it into your eclipse. Launch a child eclipse with this plugin. Ensure child workspace is cleaned upon launch so that you see welcome screen.
			//  - Upon closing the welcome screen, you will see an eclipse error message: "org.eclipse.swt.SWTError: No more handles"
			//  - The following is printed into the console: 'gdk_window_new(): parent is destroyed'
			// After some research, I found that gtk_widget_repartent(..) also reparents sub-windows, but moving widget between containers doesn't do this,
			// This seems to leave some gdkWindows with incorrect parents.
//			OS.g_object_ref (widget);
//			GTK.gtk_container_remove (parentContainer, widget);
//			GTK.gtk_container_add (newParentHandle, widget);
//			OS.g_object_unref (widget);

			control.fixParentGdkWindow();
		}
	} else { // Gtk2.
		GTK.gtk_widget_reparent(control.topHandle(), newParentHandle);
	}
}

@Override
int /*long*/ fixedMapProc (int /*long*/ widget) {
	GTK.gtk_widget_set_mapped (widget, true);
	int /*long*/ widgetList = GTK.gtk_container_get_children (widget);
	if (widgetList != 0) {
		int /*long*/ widgets = widgetList;
		while (widgets != 0) {
			int /*long*/ child = OS.g_list_data (widgets);
			if (GTK.gtk_widget_get_visible (child) && GTK.gtk_widget_get_child_visible (child) && !GTK.gtk_widget_get_mapped (child)) {
				GTK.gtk_widget_map (child);
			}
			widgets = OS.g_list_next (widgets);
		}
		OS.g_list_free (widgetList);
	}
	if (GTK.gtk_widget_get_has_window (widget)) {
		GDK.gdk_window_show_unraised (gtk_widget_get_window (widget));
	}
	return 0;
}

void fixModal(int /*long*/ group, int /*long*/ modalGroup) {
}

/**
 * Forces the receiver to have the <em>keyboard focus</em>, causing
 * all keyboard events to be delivered to it.
 *
 * @return <code>true</code> if the control got focus, and <code>false</code> if it was unable to.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #setFocus
 */
public boolean forceFocus () {
	checkWidget();
	if (display.focusEvent == SWT.FocusOut) return false;
	Shell shell = getShell ();
	shell.setSavedFocus (this);
	if (!isEnabled () || !isVisible ()) return false;
	shell.bringToTop (false);
	return forceFocus (focusHandle ());
}

boolean forceFocus (int /*long*/ focusHandle) {
	if (GTK.gtk_widget_has_focus (focusHandle)) return true;
	/* When the control is zero sized it must be realized */
	GTK.gtk_widget_realize (focusHandle);
	GTK.gtk_widget_grab_focus (focusHandle);
	// widget could be disposed at this point
	if (isDisposed ()) return false;
	Shell shell = getShell ();
	int /*long*/ shellHandle = shell.shellHandle;
	int /*long*/ handle = GTK.gtk_window_get_focus (shellHandle);
	while (handle != 0) {
		if (handle == focusHandle) {
			/* Cancel any previous ignoreFocus requests */
			display.ignoreFocus = false;
			return true;
		}
		Widget widget = display.getWidget (handle);
		if (widget != null && widget instanceof Control) {
			return widget == this;
		}
		handle = GTK.gtk_widget_get_parent (handle);
	}
	return false;
}

/**
 * Returns the receiver's background color.
 * <p>
 * Note: This operation is a hint and may be overridden by the platform.
 * For example, on some versions of Windows the background of a TabFolder,
 * is a gradient rather than a solid color.
 * </p>
 * @return the background color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Color getBackground () {
	checkWidget();
	Color color;
	if (GTK.GTK3) {
		if (backgroundAlpha == 0) {
			color = Color.gtk_new (display, this.getBackgroundGdkRGBA (), 0);
			return color;
		}
		else {
			Control control = findBackgroundControl ();
			if (control == null) control = this;
			return Color.gtk_new (display, control.getBackgroundGdkRGBA (), backgroundAlpha);
		}
	} else {
		if (backgroundAlpha == 0) {
			color = Color.gtk_new (display, this.getBackgroundGdkColor (), 0);
			return color;
		}
		else {
			Control control = findBackgroundControl ();
			if (control == null) control = this;
			return Color.gtk_new (display, control.getBackgroundGdkColor (), backgroundAlpha);
		}
	}
}

GdkRGBA getBackgroundGdkRGBA () {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	return getBgGdkRGBA ();
}

GdkColor getBackgroundGdkColor () {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	return getBgGdkColor ();
}

/**
 * Returns the receiver's background image.
 *
 * @return the background image
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.2
 */
public Image getBackgroundImage () {
	checkWidget ();
	Control control = findBackgroundControl ();
	if (control == null) control = this;
	return control.backgroundImage;
}

GdkRGBA getContextBackgroundGdkRGBA () {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	int /*long*/ fontHandle = fontHandle ();
	if ((state & BACKGROUND) == 0) {
		return defaultBackground();
	}
	if (GTK.GTK_VERSION >= OS.VERSION(3, 14, 0)) {
		if (provider != 0) {
			return display.gtk_css_parse_background (display.gtk_css_provider_to_string(provider), null);
		} else {
			return defaultBackground();
		}
	} else {
		int /*long*/ context = GTK.gtk_widget_get_style_context (fontHandle);
		GdkRGBA rgba = new GdkRGBA ();
		GTK.gtk_style_context_get_background_color (context, GTK.GTK_STATE_FLAG_NORMAL, rgba);
		return rgba;
	}
}

GdkRGBA getContextColorGdkRGBA () {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	int /*long*/ fontHandle = fontHandle ();
	if (GTK.GTK_VERSION >= OS.VERSION(3, 14, 0)) {
		return display.gtk_css_parse_foreground(display.gtk_css_provider_to_string(provider), null);
	} else {
		int /*long*/ context = GTK.gtk_widget_get_style_context (fontHandle);
		GdkRGBA rgba = display.styleContextGetColor (context, GTK.GTK_STATE_FLAG_NORMAL);
		return rgba;
	}
}

GdkRGBA getBgGdkRGBA () {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	return getContextBackgroundGdkRGBA ();
}

GdkColor getBgGdkColor () {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	int /*long*/ fontHandle = fontHandle ();
	GTK.gtk_widget_realize (fontHandle);
	GdkColor color = new GdkColor ();

	int /*long*/ style = GTK.gtk_widget_get_style (fontHandle);
	if (style != 0){
		GTK.gtk_style_get_bg (style, GTK.GTK_STATE_NORMAL, color);
	}

	return color;
}

GdkRGBA getBaseGdkRGBA () {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	return getContextBackgroundGdkRGBA ();
}

GdkColor getBaseGdkColor () {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	int /*long*/ fontHandle = fontHandle ();
	GTK.gtk_widget_realize (fontHandle);
	GdkColor color = new GdkColor ();
	GTK.gtk_style_get_base (GTK.gtk_widget_get_style (fontHandle), GTK.GTK_STATE_NORMAL, color);
	return color;
}

/**
 * Returns the receiver's border width in points.
 *
 * @return the border width
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getBorderWidth () {
	return DPIUtil.autoScaleDown(getBorderWidthInPixels());
}

int getBorderWidthInPixels () {
	checkWidget();
	return 0;
}

int getClientWidth () {
	if (handle == 0 || (state & ZERO_WIDTH) != 0) return 0;
	GtkAllocation allocation = new GtkAllocation();
	GTK.gtk_widget_get_allocation (handle, allocation);
	return allocation.width;
}

/**
 * Returns the receiver's cursor, or null if it has not been set.
 * <p>
 * When the mouse pointer passes over a control its appearance
 * is changed to match the control's cursor.
 * </p>
 *
 * @return the receiver's cursor or <code>null</code>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.3
 */
public Cursor getCursor () {
	checkWidget ();
	return cursor;
}

/**
 * Returns <code>true</code> if the receiver is detecting
 * drag gestures, and  <code>false</code> otherwise.
 *
 * @return the receiver's drag detect state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.3
 */
public boolean getDragDetect () {
	checkWidget ();
	return (state & DRAG_DETECT) != 0;
}

/**
 * Returns <code>true</code> if the receiver is enabled, and
 * <code>false</code> otherwise. A disabled control is typically
 * not selectable from the user interface and draws with an
 * inactive or "grayed" look.
 *
 * @return the receiver's enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #isEnabled
 */
public boolean getEnabled () {
	checkWidget ();
	return (state & DISABLED) == 0;
}

/**
 * Returns the font that the receiver will use to paint textual information.
 *
 * @return the receiver's font
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Font getFont () {
	checkWidget();
	return font != null ? font : defaultFont ();
}

int /*long*/ getFontDescription () {
	int /*long*/ fontHandle = fontHandle ();
	int /*long*/ [] fontDesc = new int /*long*/ [1];
	if (GTK.GTK3) {
		int /*long*/ context = GTK.gtk_widget_get_style_context (fontHandle);
		if ((GTK.GTK_VERSION < OS.VERSION(3, 8, 0) || ("ppc64le".equals(System.getProperty("os.arch"))))) {
			return GTK.gtk_style_context_get_font(context, GTK.GTK_STATE_FLAG_NORMAL);
		} else if (GTK.GTK_VERSION >= OS.VERSION(3, 18, 0)) {
			GTK.gtk_style_context_save(context);
			GTK.gtk_style_context_set_state(context, GTK.GTK_STATE_FLAG_NORMAL);
			GTK.gtk_style_context_get(context, GTK.GTK_STATE_FLAG_NORMAL, GTK.gtk_style_property_font, fontDesc, 0);
			GTK.gtk_style_context_restore(context);
			return fontDesc [0];
		} else {
			GTK.gtk_style_context_get(context, GTK.GTK_STATE_FLAG_NORMAL, GTK.gtk_style_property_font, fontDesc, 0);
			return fontDesc [0];
		}
	}
	GTK.gtk_widget_realize (fontHandle);
	return GTK.gtk_style_get_font_desc (GTK.gtk_widget_get_style (fontHandle));
}

/**
 * Returns the foreground color that the receiver will use to draw.
 *
 * @return the receiver's foreground color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Color getForeground () {
	checkWidget();
	Color color;
	color = GTK.GTK3? Color.gtk_new (display, getForegroundGdkRGBA ()) : Color.gtk_new (display, getForegroundGdkColor ());
	return color;
}

GdkRGBA getForegroundGdkRGBA () {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	return getContextColorGdkRGBA();
}

GdkColor getForegroundGdkColor () {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	return getFgColor ();
}

GdkColor getFgColor () {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	int /*long*/ fontHandle = fontHandle ();
	GTK.gtk_widget_realize (fontHandle);
	GdkColor color = new GdkColor ();

	int /*long*/ style = GTK.gtk_widget_get_style (fontHandle);
	if (style != 0){
		GTK.gtk_style_get_fg (style, GTK.GTK_STATE_NORMAL, color);
	}
	return color;
}

Point getIMCaretPos () {
	return new Point (0, 0);
}

GdkColor getTextColor () {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	int /*long*/ fontHandle = fontHandle ();
	GTK.gtk_widget_realize (fontHandle);
	GdkColor color = new GdkColor ();
	GTK.gtk_style_get_text (GTK.gtk_widget_get_style (fontHandle), GTK.GTK_STATE_NORMAL, color);
	return color;
}

/**
 * Returns layout data which is associated with the receiver.
 *
 * @return the receiver's layout data
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Object getLayoutData () {
	checkWidget();
	return layoutData;
}

/**
 * Returns the receiver's pop up menu if it has one, or null
 * if it does not. All controls may optionally have a pop up
 * menu that is displayed when the user requests one for
 * the control. The sequence of key strokes, button presses
 * and/or button releases that are used to request a pop up
 * menu is platform specific.
 *
 * @return the receiver's menu
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Menu getMenu () {
	checkWidget();
	return menu;
}

/**
 * Returns the receiver's monitor.
 *
 * @return the receiver's monitor
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.0
 */
public Monitor getMonitor () {
	checkWidget ();
	int /*long*/ screen = GDK.gdk_screen_get_default ();
	if (screen != 0) {
		int monitorNumber = GDK.gdk_screen_get_monitor_at_window (screen, paintWindow ());
		Monitor[] monitors = display.getMonitors ();

		if (monitorNumber >= 0 && monitorNumber < monitors.length) {
			return monitors [monitorNumber];
		}
	}
	return display.getPrimaryMonitor ();
}

/**
 * Returns the receiver's parent, which must be a <code>Composite</code>
 * or null when the receiver is a shell that was created with null or
 * a display for a parent.
 *
 * @return the receiver's parent
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Composite getParent () {
	checkWidget();
	return parent;
}

Control [] getPath () {
	int count = 0;
	Shell shell = getShell ();
	Control control = this;
	while (control != shell) {
		count++;
		control = control.parent;
	}
	control = this;
	Control [] result = new Control [count];
	while (control != shell) {
		result [--count] = control;
		control = control.parent;
	}
	return result;
}

/**
 * Returns the region that defines the shape of the control,
 * or null if the control has the default shape.
 *
 * @return the region that defines the shape of the shell (or null)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.4
 */
public Region getRegion () {
	checkWidget ();
	return region;
}

/**
 * Returns the receiver's shell. For all controls other than
 * shells, this simply returns the control's nearest ancestor
 * shell. Shells return themselves, even if they are children
 * of other shells.
 *
 * @return the receiver's shell
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #getParent
 */
public Shell getShell() {
	checkWidget();
	return _getShell();
}

Shell _getShell() {
	return parent._getShell();
}

/**
 * Returns the receiver's tool tip text, or null if it has
 * not been set.
 *
 * @return the receiver's tool tip text
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public String getToolTipText () {
	checkWidget();
	return toolTipText;
}

/**
 * Returns <code>true</code> if this control is set to send touch events, or
 * <code>false</code> if it is set to send gesture events instead.  This method
 * also returns <code>false</code> if a touch-based input device is not detected
 * (this can be determined with <code>Display#getTouchEnabled()</code>).  Use
 * {@link #setTouchEnabled(boolean)} to switch the events that a control sends
 * between touch events and gesture events.
 *
 * @return <code>true</code> if the control is set to send touch events, or <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #setTouchEnabled
 * @see Display#getTouchEnabled
 *
 * @since 3.7
 */
public boolean getTouchEnabled() {
	checkWidget();
	return false;
}

/**
 * Returns <code>true</code> if the receiver is visible, and
 * <code>false</code> otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, this method
 * may still indicate that it is considered visible even though
 * it may not actually be showing.
 * </p>
 *
 * @return the receiver's visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean getVisible () {
	checkWidget();
	return (state & HIDDEN) == 0;
}

Point getThickness (int /*long*/ widget) {
	if (GTK.GTK3) {
		int xthickness = 0, ythickness = 0;
		GtkBorder tmp = new GtkBorder();
		int /*long*/ context = GTK.gtk_widget_get_style_context (widget);

		if (GTK.GTK_VERSION < OS.VERSION(3, 18, 0)) {
			GTK.gtk_style_context_get_padding (context, GTK.GTK_STATE_FLAG_NORMAL, tmp);
		} else {
			GTK.gtk_style_context_get_padding (context, GTK.gtk_widget_get_state_flags(widget), tmp);
		}
		GTK.gtk_style_context_save (context);
		GTK.gtk_style_context_add_class (context, GTK.GTK_STYLE_CLASS_FRAME);
		xthickness += tmp.left;
		ythickness += tmp.top;
		if (GTK.GTK_VERSION < OS.VERSION(3, 18, 0)) {
			GTK.gtk_style_context_get_border (context, GTK.GTK_STATE_FLAG_NORMAL, tmp);
		} else {
			GTK.gtk_style_context_get_border (context, GTK.gtk_widget_get_state_flags(widget), tmp);
		}
		xthickness += tmp.left;
		ythickness += tmp.top;
		GTK.gtk_style_context_restore (context);
		return new Point (xthickness, ythickness);
	}
	int /*long*/ style = GTK.gtk_widget_get_style (widget);
	return new Point (GTK.gtk_style_get_xthickness (style), GTK.gtk_style_get_ythickness (style));
}

@Override
int /*long*/ gtk_button_press_event (int /*long*/ widget, int /*long*/ event) {
	return gtk_button_press_event (widget, event, true);
}

int /*long*/ gtk_button_press_event (int /*long*/ widget, int /*long*/ event, boolean sendMouseDown) {
	GdkEventButton gdkEvent = new GdkEventButton ();
	OS.memmove (gdkEvent, event, GdkEventButton.sizeof);
	lastInput.x = (int) gdkEvent.x;
	lastInput.y = (int) gdkEvent.y;
	if (containedInRegion(lastInput.x, lastInput.y)) return 0;
	if (gdkEvent.type == GDK.GDK_3BUTTON_PRESS) return 0;

	/*
	* When a shell is created with SWT.ON_TOP and SWT.NO_FOCUS,
	* do not activate the shell when the user clicks on the
	* the client area or on the border or a control within the
	* shell that does not take focus.
	*/
	Shell shell = _getShell ();
	if (((shell.style & SWT.ON_TOP) != 0) && (((shell.style & SWT.NO_FOCUS) == 0) || ((style & SWT.NO_FOCUS) == 0))) {
		shell.forceActive();
	}
	int /*long*/ result = 0;
	if (gdkEvent.type == GDK.GDK_BUTTON_PRESS) {
		boolean dragging = false;
		display.clickCount = 1;
		int /*long*/ nextEvent = GDK.gdk_event_peek ();
		if (nextEvent != 0) {
			int eventType = GDK.GDK_EVENT_TYPE (nextEvent);
			if (eventType == GDK.GDK_2BUTTON_PRESS) display.clickCount = 2;
			if (eventType == GDK.GDK_3BUTTON_PRESS) display.clickCount = 3;
			GDK.gdk_event_free (nextEvent);
		}
		/*
		 * Feature in GTK: DND detection for X.11 & Wayland support is done through motion notify event
		 * instead of mouse click event. See Bug 503431.
		 */
		if (OS.isX11()) { // Wayland
			if ((state & DRAG_DETECT) != 0 && hooks (SWT.DragDetect)) {
				if (gdkEvent.button == 1) {
					boolean [] consume = new boolean [1];
					if (dragDetect ((int) gdkEvent.x, (int) gdkEvent.y, true, true, consume)) {
						dragging = true;
						if (consume [0]) result = 1;
					}
					if (isDisposed ()) return 1;
				}
			}
		}
		if (sendMouseDown && !sendMouseEvent (SWT.MouseDown, gdkEvent.button, display.clickCount, 0, false, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state)) {
			result = 1;
		}
		if (isDisposed ()) return 1;
		/*
		 * Feature in GTK: DND detection for X.11 & Wayland support is done through motion notify event
		 * instead of mouse click event. See Bug 503431.
		 */
		if (OS.isX11()) { // Wayland
			if (dragging) {
				Point scaledEvent = DPIUtil.autoScaleDown(new Point((int)gdkEvent.x, (int) gdkEvent.y));
				sendDragEvent (gdkEvent.button, gdkEvent.state, scaledEvent.x, scaledEvent.y, false);
				if (isDisposed ()) return 1;
			}
		}
		/*
		* Pop up the context menu in the button press event for widgets
		* that have default operating system menus in order to stop the
		* operating system from displaying the menu if necessary.
		*/
		if ((state & MENU) != 0) {
			if (gdkEvent.button == 3) {
				if (showMenu ((int)gdkEvent.x_root, (int)gdkEvent.y_root)) {
					result = 1;
				}
			}
		}
	} else {
		display.clickCount = 2;
		result = sendMouseEvent (SWT.MouseDoubleClick, gdkEvent.button, display.clickCount, 0, false, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		if (isDisposed ()) return 1;
	}
	if (!shell.isDisposed ()) shell.setActiveControl (this, SWT.MouseDown);
	return result;
}

@Override
int /*long*/ gtk_button_release_event (int /*long*/ widget, int /*long*/ event) {
	GdkEventButton gdkEvent = new GdkEventButton ();
	OS.memmove (gdkEvent, event, GdkEventButton.sizeof);
	lastInput.x = (int) gdkEvent.x;
	lastInput.y = (int) gdkEvent.y;
	if (containedInRegion(lastInput.x, lastInput.y)) return 0;
	return sendMouseEvent (SWT.MouseUp, gdkEvent.button, display.clickCount, 0, false, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
}

@Override
int /*long*/ gtk_commit (int /*long*/ imcontext, int /*long*/ text) {
	if (text == 0) return 0;
	int length = C.strlen (text);
	if (length == 0) return 0;
	byte [] buffer = new byte [length];
	C.memmove (buffer, text, length);
	char [] chars = Converter.mbcsToWcs (buffer);
	sendIMKeyEvent (SWT.KeyDown, null, chars);
	return 0;
}

@Override
int /*long*/ gtk_enter_notify_event (int /*long*/ widget, int /*long*/ event) {
	/*
	 * Feature in GTK. Children of a shell will inherit and display the shell's
	 * tooltip if they do not have a tooltip of their own. The fix is to use the
	 * new tooltip API in GTK 2.12 to null the shell's tooltip when the control
	 * being entered does not have any tooltip text set.
	 */
	byte [] buffer = null;
	if (toolTipText != null && toolTipText.length() != 0) {
		char [] chars = fixMnemonic (toolTipText, false);
		buffer = Converter.wcsToMbcs (chars, true);
	}
	int /*long*/ toolHandle = getShell().handle;
	GTK.gtk_widget_set_tooltip_text (toolHandle, buffer);

	if (display.currentControl == this) return 0;
	GdkEventCrossing gdkEvent = new GdkEventCrossing ();
	OS.memmove (gdkEvent, event, GdkEventCrossing.sizeof);
	lastInput.x = (int) gdkEvent.x;
	lastInput.y = (int) gdkEvent.y;
	if (containedInRegion(lastInput.x, lastInput.y)) return 0;

	/*
	 * It is possible to send out too many enter/exit events if entering a
	 * control through a subwindow. The fix is to return without sending any
	 * events if the GdkEventCrossing subwindow field is set and the control
	 * requests to check the field.
	 */
	if (gdkEvent.subwindow != 0 && checkSubwindow ()) return 0;
	if (gdkEvent.mode != GDK.GDK_CROSSING_NORMAL && gdkEvent.mode != GDK.GDK_CROSSING_UNGRAB) return 0;
	if ((gdkEvent.state & (GDK.GDK_BUTTON1_MASK | GDK.GDK_BUTTON2_MASK | GDK.GDK_BUTTON3_MASK)) != 0) return 0;
	if (display.currentControl != null && !display.currentControl.isDisposed ()) {
		display.removeMouseHoverTimeout (display.currentControl.handle);
		display.currentControl.sendMouseEvent (SWT.MouseExit,  0, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state);
	}
	if (!isDisposed ()) {
		display.currentControl = this;
		return sendMouseEvent (SWT.MouseEnter, 0, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
	}
	return 0;
}

boolean checkSubwindow () {
	return false;
}

@Override
int /*long*/ gtk_event_after (int /*long*/ widget, int /*long*/ gdkEvent) {
	GdkEvent event = new GdkEvent ();
	OS.memmove (event, gdkEvent, GdkEvent.sizeof);
	switch (event.type) {
		case GDK.GDK_BUTTON_PRESS: {
			if (widget != eventHandle ()) break;
			/*
			* Pop up the context menu in the event_after signal to allow
			* the widget to process the button press.  This allows widgets
			* such as GtkTreeView to select items before a menu is shown.
			*/
			if ((state & MENU) == 0) {
				GdkEventButton gdkEventButton = new GdkEventButton ();
				OS.memmove (gdkEventButton, gdkEvent, GdkEventButton.sizeof);
				if (gdkEventButton.button == 3) {
					showMenu ((int) gdkEventButton.x_root, (int) gdkEventButton.y_root);
				}
			}
			break;
		}
		case GDK.GDK_FOCUS_CHANGE: {
			if (!isFocusHandle (widget)) break;
			GdkEventFocus gdkEventFocus = new GdkEventFocus ();
			OS.memmove (gdkEventFocus, gdkEvent, GdkEventFocus.sizeof);

			/*
			 * Feature in GTK. The GTK combo box popup under some window managers
			 * is implemented as a GTK_MENU.  When it pops up, it causes the combo
			 * box to lose focus when focus is received for the menu.  The
			 * fix is to check the current grab handle and see if it is a GTK_MENU
			 * and ignore the focus event when the menu is both shown and hidden.
			 *
			 * NOTE: This code runs for all menus.
			 */
			Display display = this.display;
			if (gdkEventFocus.in != 0) {
				if (display.ignoreFocus) {
					display.ignoreFocus = false;
					break;
				}
			} else {
				display.ignoreFocus = false;
				int /*long*/ grabHandle = GTK.gtk_grab_get_current ();
				if (grabHandle != 0) {
					if (OS.G_OBJECT_TYPE (grabHandle) == GTK.GTK_TYPE_MENU ()) {
						display.ignoreFocus = true;
						break;
					}
				}
			}

			sendFocusEvent (gdkEventFocus.in != 0 ? SWT.FocusIn : SWT.FocusOut);
			break;
		}
	}
	return 0;
}

/**
 * Copies the region at the Cairo level, as we need to re-use these resources
 * after the Region object is disposed.
 *
 * @param region the Region object to copy to this Control
 */
void cairoCopyRegion (Region region) {
	if (region == null || region.isDisposed() || region.handle == 0) return;
	regionHandle = Cairo.cairo_region_copy(region.handle);
	return;
}

void cairoDisposeRegion () {
	if (regionHandle != 0) GDK.gdk_region_destroy(regionHandle);
	if (eventRegion != 0) GDK.gdk_region_destroy(eventRegion);
	regionHandle = 0;
	eventRegion = 0;
}
/**
 * Convenience method that applies a region to the Control using cairo_clip.
 *
 * @param cairo the cairo context to apply the region to
 */
void cairoClipRegion (int /*long*/ cairo) {
	GdkRectangle rect = new GdkRectangle ();
	GDK.gdk_cairo_get_clip_rectangle (cairo, rect);
	int /*long*/ regionHandle = this.regionHandle;
	// Disposal check just in case
	if (regionHandle == 0) {
		drawRegion = false;
		return;
	}
	/*
	 * These gdk_region_* functions actually map to the proper
	 * cairo_* functions in os.h.
	 */
	int /*long*/ actualRegion = GDK.gdk_region_rectangle(rect);
	GDK.gdk_region_subtract(actualRegion, regionHandle);
	// Draw the Shell bg using cairo, only if it's a different color
	Shell shell = getShell();
	Color shellBg = shell.getBackground();
	if (shellBg != this.getBackground()) {
		GdkRGBA rgba = shellBg.handleRGBA;
		Cairo.cairo_set_source_rgba (cairo, rgba.red, rgba.green, rgba.blue, rgba.alpha);
	} else {
		Cairo.cairo_set_source_rgba (cairo, 0.0, 0.0, 0.0, 0.0);
	}
	GDK.gdk_cairo_region(cairo, actualRegion);
	Cairo.cairo_clip(cairo);
	Cairo.cairo_paint(cairo);
	eventRegion = actualRegion;
}

@Override
int /*long*/ gtk_draw (int /*long*/ widget, int /*long*/ cairo) {
	if ((state & OBSCURED) != 0) return 0;
	GdkRectangle rect = new GdkRectangle ();
	GDK.gdk_cairo_get_clip_rectangle (cairo, rect);
	/*
	 * Modify the drawing of the widget with cairo_clip.
	 * Doesn't modify input handling at this time.
	 * See bug 529431.
	 */
	if (drawRegion) {
		cairoClipRegion(cairo);
	}
	if (!hooksPaint ()) return 0;
	Event event = new Event ();
	event.count = 1;
	Rectangle eventBounds = DPIUtil.autoScaleDown (new Rectangle (rect.x, rect.y, rect.width, rect.height));
	if ((style & SWT.MIRRORED) != 0) eventBounds.x = DPIUtil.autoScaleDown (getClientWidth ()) - eventBounds.width - eventBounds.x;
	event.setBounds (eventBounds);
	GCData data = new GCData ();
	/*
	 * Pass the region into the GCData so that GC.fill* methods can be aware of the region
	 * and clip themselves accordingly. Only relevant on GTK3.10+, see bug 475784.
	 */
	if (drawRegion) data.regionSet = eventRegion;
//	data.damageRgn = gdkEvent.region;
	if (GTK.GTK_VERSION <= OS.VERSION (3, 9, 0) || GTK.GTK_VERSION >= OS.VERSION (3, 14, 0)) {
		data.cairo = cairo;
	}
	GC gc = event.gc = GC.gtk_new (this, data);
	// Note: use GC#setClipping(x,y,width,height) because GC#setClipping(Rectangle) got broken by bug 446075
	gc.setClipping (eventBounds.x, eventBounds.y, eventBounds.width, eventBounds.height);
	drawWidget (gc);
	sendEvent (SWT.Paint, event);
	gc.dispose ();
	event.gc = null;
	return 0;
}

@Override
int /*long*/ gtk_expose_event (int /*long*/ widget, int /*long*/ eventPtr) {
	if ((state & OBSCURED) != 0) return 0;
	if (!hooksPaint ()) return 0;
	GdkEventExpose gdkEvent = new GdkEventExpose ();
	OS.memmove(gdkEvent, eventPtr, GdkEventExpose.sizeof);
	Event event = new Event ();
	event.count = gdkEvent.count;
	Rectangle eventRect = new Rectangle (gdkEvent.area_x, gdkEvent.area_y, gdkEvent.area_width, gdkEvent.area_height);
	event.setBounds (DPIUtil.autoScaleDown (eventRect));
	if ((style & SWT.MIRRORED) != 0) event.x = DPIUtil.autoScaleDown (getClientWidth ()) - event.width - event.x;
	GCData data = new GCData ();
	data.damageRgn = gdkEvent.region;
	GC gc = event.gc = GC.gtk_new (this, data);
	drawWidget (gc);
	sendEvent (SWT.Paint, event);
	gc.dispose ();
	event.gc = null;
	return 0;
}

@Override
int /*long*/ gtk_focus (int /*long*/ widget, int /*long*/ directionType) {
	/* Stop GTK traversal for every widget */
	return 1;
}

@Override
int /*long*/ gtk_focus_in_event (int /*long*/ widget, int /*long*/ event) {
	// widget could be disposed at this point
	if (handle != 0) {
		Control oldControl = display.imControl;
		if (oldControl != this)  {
			if (oldControl != null && !oldControl.isDisposed ()) {
				int /*long*/ oldIMHandle = oldControl.imHandle ();
				if (oldIMHandle != 0) GTK.gtk_im_context_reset (oldIMHandle);
			}
		}
		if (hooks (SWT.KeyDown) || hooks (SWT.KeyUp)) {
			int /*long*/ imHandle = imHandle ();
			if (imHandle != 0) GTK.gtk_im_context_focus_in (imHandle);
		}
	}
	return 0;
}

@Override
int /*long*/ gtk_focus_out_event (int /*long*/ widget, int /*long*/ event) {
	// widget could be disposed at this point
	if (handle != 0) {
		if (hooks (SWT.KeyDown) || hooks (SWT.KeyUp)) {
			int /*long*/ imHandle = imHandle ();
			if (imHandle != 0) {
				GTK.gtk_im_context_focus_out (imHandle);
			}
		}
	}
	return 0;
}

@Override
int /*long*/ gtk_key_press_event (int /*long*/ widget, int /*long*/ event) {
	if (!hasFocus ()) {
		/*
		* Feature in GTK.  On AIX, the IME window deactivates the current shell and even
		* though the widget receiving the key event is not in focus, it should filter the input in
		* order to get it committed.  The fix is to detect that the widget shell is not active
		* and call filterKey() only.
		*/
		if (display.getActiveShell () == null) {
			GdkEventKey gdkEvent = new GdkEventKey ();
			OS.memmove (gdkEvent, event, GdkEventKey.sizeof);
			if (filterKey (gdkEvent.keyval, event)) return 1;
		}
		return 0;
	}
	GdkEventKey gdkEvent = new GdkEventKey ();
	OS.memmove (gdkEvent, event, GdkEventKey.sizeof);

	if (translateMnemonic (gdkEvent.keyval, gdkEvent)) return 1;
	// widget could be disposed at this point
	if (isDisposed ()) return 0;

	if (filterKey (gdkEvent.keyval, event)) return 1;
	// widget could be disposed at this point
	if (isDisposed ()) return 0;

	if (translateTraversal (gdkEvent)) return 1;
	// widget could be disposed at this point
	if (isDisposed ()) return 0;
	return super.gtk_key_press_event (widget, event);
}

@Override
int /*long*/ gtk_key_release_event (int /*long*/ widget, int /*long*/ event) {
	if (!hasFocus ()) return 0;
	int /*long*/ imHandle = imHandle ();
	if (imHandle != 0) {
		if (GTK.gtk_im_context_filter_keypress (imHandle, event)) return 1;
	}
	return super.gtk_key_release_event (widget, event);
}

@Override
int /*long*/ gtk_leave_notify_event (int /*long*/ widget, int /*long*/ event) {
	if (display.currentControl != this) return 0;
	GdkEventCrossing gdkEvent = new GdkEventCrossing ();
	OS.memmove (gdkEvent, event, GdkEventCrossing.sizeof);
	lastInput.x = (int) gdkEvent.x;
	lastInput.y = (int) gdkEvent.y;
	if (containedInRegion(lastInput.x, lastInput.y)) return 0;
	display.removeMouseHoverTimeout (handle);
	int result = 0;
	if (sendLeaveNotify () || display.getCursorControl () == null) {
		if (gdkEvent.mode != GDK.GDK_CROSSING_NORMAL && gdkEvent.mode != GDK.GDK_CROSSING_UNGRAB) return 0;
		if ((gdkEvent.state & (GDK.GDK_BUTTON1_MASK | GDK.GDK_BUTTON2_MASK | GDK.GDK_BUTTON3_MASK)) != 0) return 0;
		result = sendMouseEvent (SWT.MouseExit, 0, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		display.currentControl = null;
	}
	return result;
}

@Override
int /*long*/ gtk_mnemonic_activate (int /*long*/ widget, int /*long*/ arg1) {
	int result = 0;
	int /*long*/ eventPtr = GTK.gtk_get_current_event ();
	if (eventPtr != 0) {
		GdkEventKey keyEvent = new GdkEventKey ();
		OS.memmove (keyEvent, eventPtr, GdkEventKey.sizeof);
		if (keyEvent.type == GDK.GDK_KEY_PRESS) {
			Control focusControl = display.getFocusControl ();
			int /*long*/ focusHandle = focusControl != null ? focusControl.focusHandle () : 0;
			if (focusHandle != 0) {
				display.mnemonicControl = this;
				GTK.gtk_widget_event (focusHandle, eventPtr);
				display.mnemonicControl = null;
			}
			result = 1;
		}
		GDK.gdk_event_free (eventPtr);
	}
	return result;
}

@Override
int /*long*/ gtk_motion_notify_event (int /*long*/ widget, int /*long*/ event) {
	int result;
	GdkEventMotion gdkEvent = new GdkEventMotion ();
	OS.memmove (gdkEvent, event, GdkEventMotion.sizeof);
	lastInput.x = (int) gdkEvent.x;
	lastInput.y = (int) gdkEvent.y;
	if (containedInRegion(lastInput.x, lastInput.y)) return 0;
	/*
	 * Feature in GTK: DND detection for X.11 & Wayland support is done through motion notify event
	 * instead of mouse click event. See Bug 503431.
	 */
	if (!OS.isX11()) { // Wayland
		boolean dragging = false;
		if ((state & DRAG_DETECT) != 0 && hooks (SWT.DragDetect)) {
				boolean [] consume = new boolean [1];
				if (dragDetect ((int) gdkEvent.x, (int) gdkEvent.y, true, true, consume)) {
					dragging = true;
					if (consume [0]) result = 1;
				if (isDisposed ()) return 1;
			} else {
			}
		}
		if (dragging) {
			GTK.gtk_event_controller_handle_event(dragGesture,event);
			GdkEventButton gdkEvent1 = new GdkEventButton ();
			OS.memmove (gdkEvent1, event, GdkEventButton.sizeof);
			if (gdkEvent1.type == GDK.GDK_3BUTTON_PRESS) return 0;
			Point scaledEvent = DPIUtil.autoScaleDown(new Point((int)gdkEvent1.x, (int) gdkEvent1.y));
			if (sendDragEvent (gdkEvent1.button, gdkEvent1.state, scaledEvent.x, scaledEvent.y, false)){
				return 1;
		}
	}
}
	if (this == display.currentControl && (hooks (SWT.MouseHover) || filters (SWT.MouseHover))) {
		display.addMouseHoverTimeout (handle);
	}
	double x = gdkEvent.x_root, y = gdkEvent.y_root;
	int state = gdkEvent.state;
	if (gdkEvent.is_hint != 0) {
		int [] pointer_x = new int [1], pointer_y = new int [1], mask = new int [1];
		int /*long*/ window = eventWindow ();
		gdk_window_get_device_position (window, pointer_x, pointer_y, mask);
		x = pointer_x [0];
		y = pointer_y [0];
		state = mask [0];
	}
	if (GTK.GTK3 && this != display.currentControl) {
		if (display.currentControl != null && !display.currentControl.isDisposed ()) {
			display.removeMouseHoverTimeout (display.currentControl.handle);
			Point pt = display.mapInPixels (this, display.currentControl, (int) x, (int) y);
			display.currentControl.sendMouseEvent (SWT.MouseExit,  0, gdkEvent.time, pt.x, pt.y, gdkEvent.is_hint != 0, state);
		}
		if (!isDisposed ()) {
			display.currentControl = this;
			sendMouseEvent (SWT.MouseEnter, 0, gdkEvent.time, x, y, gdkEvent.is_hint != 0, state);
		}
	}
	result = sendMouseEvent (SWT.MouseMove, 0, gdkEvent.time, x, y, gdkEvent.is_hint != 0, state) ? 0 : 1;
	return result;
}

@Override
int /*long*/ gtk_popup_menu (int /*long*/ widget) {
	if (!hasFocus()) return 0;
	int [] x = new int [1], y = new int [1];
	gdk_window_get_device_position (0, x, y, null);
	return showMenu (x [0], y [0], SWT.MENU_KEYBOARD) ? 1 : 0;
}

@Override
int /*long*/ gtk_preedit_changed (int /*long*/ imcontext) {
	display.showIMWindow (this);
	return 0;
}

@Override
int /*long*/ gtk_realize (int /*long*/ widget) {
	int /*long*/ imHandle = imHandle ();
	if (imHandle != 0) {
		int /*long*/ window = gtk_widget_get_window (paintHandle ());
		GTK.gtk_im_context_set_client_window (imHandle, window);
	}
	if (backgroundImage != null) {
		setBackgroundPixmap (backgroundImage);
	}
	return 0;
}

@Override
int /*long*/ gtk_scroll_event (int /*long*/ widget, int /*long*/ eventPtr) {
	GdkEventScroll gdkEvent = new GdkEventScroll ();
	OS.memmove (gdkEvent, eventPtr, GdkEventScroll.sizeof);
	lastInput.x = (int) gdkEvent.x;
	lastInput.y = (int) gdkEvent.y;
	if (containedInRegion(lastInput.x, lastInput.y)) return 0;
	switch (gdkEvent.direction) {
		case GDK.GDK_SCROLL_UP:
			return sendMouseEvent (SWT.MouseWheel, 0, 3, SWT.SCROLL_LINE, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		case GDK.GDK_SCROLL_DOWN:
			return sendMouseEvent (SWT.MouseWheel, 0, -3, SWT.SCROLL_LINE, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		case GDK.GDK_SCROLL_LEFT:
			return sendMouseEvent (SWT.MouseHorizontalWheel, 0, 3, 0, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		case GDK.GDK_SCROLL_RIGHT:
			return sendMouseEvent (SWT.MouseHorizontalWheel, 0, -3, 0, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1;
		case GDK.GDK_SCROLL_SMOOTH:
			int /*long*/ result = 0;
			double[] delta_x = new double[1], delta_y = new double [1];
			if (GDK.gdk_event_get_scroll_deltas (eventPtr, delta_x, delta_y)) {
				if (delta_x [0] != 0) {
					result = (sendMouseEvent (SWT.MouseHorizontalWheel, 0, (int)(-3 * delta_x [0]), 0, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1);
				}
				if (delta_y [0] != 0) {
					result = (sendMouseEvent (SWT.MouseWheel, 0, (int)(-3 * delta_y [0]), SWT.SCROLL_LINE, true, gdkEvent.time, gdkEvent.x_root, gdkEvent.y_root, false, gdkEvent.state) ? 0 : 1);
				}
			}
			return result;
	}
	return 0;
}

@Override
int /*long*/ gtk_show_help (int /*long*/ widget, int /*long*/ helpType) {
	if (!hasFocus ()) return 0;
	return sendHelpEvent (helpType) ? 1 : 0;
}

@Override
int /*long*/ gtk_style_set (int /*long*/ widget, int /*long*/ previousStyle) {
	if (backgroundImage != null) {
		setBackgroundPixmap (backgroundImage);
	}
	return 0;
}

@Override
int /*long*/ gtk_unrealize (int /*long*/ widget) {
	int /*long*/ imHandle = imHandle ();
	if (imHandle != 0) GTK.gtk_im_context_set_client_window (imHandle, 0);
	return 0;
}

void gtk_widget_size_request (int /*long*/ widget, GtkRequisition requisition) {
	gtk_widget_get_preferred_size (widget, requisition);
}

/**
 * Invokes platform specific functionality to allocate a new GC handle.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Control</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param data the platform specific GC data
 * @return the platform specific GC handle
 *
 * @noreference This method is not intended to be referenced by clients.
 */
@Override
public int /*long*/ internal_new_GC (GCData data) {
	checkWidget ();
	int /*long*/ window = paintWindow ();
	if (window == 0) error (SWT.ERROR_NO_HANDLES);
	int /*long*/ gc = data.cairo;
	if (gc != 0) {
		Cairo.cairo_reference (gc);
	} else {
		gc = GDK.gdk_cairo_create (window);
	}
	if (gc == 0) error (SWT.ERROR_NO_HANDLES);
	if (data != null) {
		int mask = SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		if ((data.style & mask) == 0) {
			data.style |= style & (mask | SWT.MIRRORED);
		} else {
			if ((data.style & SWT.RIGHT_TO_LEFT) != 0) {
				data.style |= SWT.MIRRORED;
			}
		}
		data.drawable = window;
		data.device = display;

		Control control = findBackgroundControl ();
		if (control == null) control = this;
		data.font = font != null ? font : defaultFont ();
		if (GTK.GTK3) {
			data.foregroundRGBA = getForegroundGdkRGBA ();
			data.backgroundRGBA = control.getBackgroundGdkRGBA ();
		} else {
			data.foreground = getForegroundGdkColor ();
			data.background = control.getBackgroundGdkColor ();
		}
	}
	return gc;
}

int /*long*/ imHandle () {
	return 0;
}

/**
 * Invokes platform specific functionality to dispose a GC handle.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Control</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param hDC the platform specific GC handle
 * @param data the platform specific GC data
 *
 * @noreference This method is not intended to be referenced by clients.
 */
@Override
public void internal_dispose_GC (int /*long*/ hDC, GCData data) {
	checkWidget ();
	Cairo.cairo_destroy (hDC);
}

/**
 * Returns <code>true</code> if the underlying operating
 * system supports this reparenting, otherwise <code>false</code>
 *
 * @return <code>true</code> if the widget can be reparented, otherwise <code>false</code>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean isReparentable () {
	checkWidget();
	return true;
}
boolean isShowing () {
	/*
	* This is not complete.  Need to check if the
	* widget is obscurred by a parent or sibling.
	*/
	if (!isVisible ()) return false;
	Control control = this;
	while (control != null) {
		Point size = control.getSizeInPixels ();
		if (size.x == 0 || size.y == 0) {
			return false;
		}
		control = control.parent;
	}
	return true;
}
boolean isTabGroup () {
	Control [] tabList = parent._getTabList ();
	if (tabList != null) {
		for (int i=0; i<tabList.length; i++) {
			if (tabList [i] == this) return true;
		}
	}
	int code = traversalCode (0, null);
	if ((code & (SWT.TRAVERSE_ARROW_PREVIOUS | SWT.TRAVERSE_ARROW_NEXT)) != 0) return false;
	return (code & (SWT.TRAVERSE_TAB_PREVIOUS | SWT.TRAVERSE_TAB_NEXT)) != 0;
}
boolean isTabItem () {
	Control [] tabList = parent._getTabList ();
	if (tabList != null) {
		for (int i=0; i<tabList.length; i++) {
			if (tabList [i] == this) return false;
		}
	}
	int code = traversalCode (0, null);
	return (code & (SWT.TRAVERSE_ARROW_PREVIOUS | SWT.TRAVERSE_ARROW_NEXT)) != 0;
}

/**
 * Returns <code>true</code> if the receiver is enabled and all
 * ancestors up to and including the receiver's nearest ancestor
 * shell are enabled.  Otherwise, <code>false</code> is returned.
 * A disabled control is typically not selectable from the user
 * interface and draws with an inactive or "grayed" look.
 *
 * @return the receiver's enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #getEnabled
 */
public boolean isEnabled () {
	checkWidget ();
	return getEnabled () && parent.isEnabled ();
}

boolean isFocusAncestor (Control control) {
	while (control != null && control != this && !(control instanceof Shell)) {
		control = control.parent;
	}
	return control == this;
}

/**
 * Returns <code>true</code> if the receiver has the user-interface
 * focus, and <code>false</code> otherwise.
 *
 * @return the receiver's focus state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean isFocusControl () {
	checkWidget();
	Control focusControl = display.focusControl;
	if (focusControl != null && !focusControl.isDisposed ()) {
		return this == focusControl;
	}
	return hasFocus ();
}

/**
 * Returns <code>true</code> if the receiver is visible and all
 * ancestors up to and including the receiver's nearest ancestor
 * shell are visible. Otherwise, <code>false</code> is returned.
 *
 * @return the receiver's visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #getVisible
 */
public boolean isVisible () {
	checkWidget();
	return getVisible () && parent.isVisible ();
}

Decorations menuShell () {
	return parent.menuShell ();
}

boolean mnemonicHit (char key) {
	return false;
}

boolean mnemonicMatch (char key) {
	return false;
}

@Override
void register () {
	super.register ();
	if (fixedHandle != 0) display.addWidget (fixedHandle, this);
	int /*long*/ imHandle = imHandle ();
	if (imHandle != 0) display.addWidget (imHandle, this);
}

/**
 * Requests that this control and all of its ancestors be repositioned by
 * their layouts at the earliest opportunity. This should be invoked after
 * modifying the control in order to inform any dependent layouts of
 * the change.
 * <p>
 * The control will not be repositioned synchronously. This method is
 * fast-running and only marks the control for future participation in
 * a deferred layout.
 * <p>
 * Invoking this method multiple times before the layout occurs is an
 * inexpensive no-op.
 *
 * @since 3.105
 */
public void requestLayout () {
	getShell ().layout (new Control[] {this}, SWT.DEFER);
}

/**
 * Causes the entire bounds of the receiver to be marked
 * as needing to be redrawn. The next time a paint request
 * is processed, the control will be completely painted,
 * including the background.
 * <p>
 * Schedules a paint request if the invalidated area is visible
 * or becomes visible later. It is not necessary for the caller
 * to explicitly call {@link #update()} after calling this method,
 * but depending on the platform, the automatic repaints may be
 * delayed considerably.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #update()
 * @see PaintListener
 * @see SWT#Paint
 * @see SWT#NO_BACKGROUND
 * @see SWT#NO_REDRAW_RESIZE
 * @see SWT#NO_MERGE_PAINTS
 * @see SWT#DOUBLE_BUFFERED
 */
public void redraw () {
	checkWidget();
	redraw (false);
}

void redraw (boolean all) {
//	checkWidget();
	if (!GTK.gtk_widget_get_visible (topHandle ())) return;
	redrawWidget (0, 0, 0, 0, true, all, false);
}

/**
 * Causes the rectangular area of the receiver specified by
 * the arguments to be marked as needing to be redrawn.
 * The next time a paint request is processed, that area of
 * the receiver will be painted, including the background.
 * If the <code>all</code> flag is <code>true</code>, any
 * children of the receiver which intersect with the specified
 * area will also paint their intersecting areas. If the
 * <code>all</code> flag is <code>false</code>, the children
 * will not be painted.
 * <p>
 * Schedules a paint request if the invalidated area is visible
 * or becomes visible later. It is not necessary for the caller
 * to explicitly call {@link #update()} after calling this method,
 * but depending on the platform, the automatic repaints may be
 * delayed considerably.
 * </p>
 *
 * @param x the x coordinate of the area to draw
 * @param y the y coordinate of the area to draw
 * @param width the width of the area to draw
 * @param height the height of the area to draw
 * @param all <code>true</code> if children should redraw, and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #update()
 * @see PaintListener
 * @see SWT#Paint
 * @see SWT#NO_BACKGROUND
 * @see SWT#NO_REDRAW_RESIZE
 * @see SWT#NO_MERGE_PAINTS
 * @see SWT#DOUBLE_BUFFERED
 */
public void redraw (int x, int y, int width, int height, boolean all) {
	checkWidget();
	Rectangle rect = DPIUtil.autoScaleUp(new Rectangle(x, y, width, height));
	redrawInPixels(rect.x, rect.y, rect.width, rect.height, all);
}

void redrawInPixels (int x, int y, int width, int height, boolean all) {
	checkWidget();
	if (!GTK.gtk_widget_get_visible (topHandle ())) return;
	if ((style & SWT.MIRRORED) != 0) x = getClientWidth () - width - x;
	redrawWidget (x, y, width, height, false, all, false);
}

void redrawChildren () {
}

void redrawWidget (int x, int y, int width, int height, boolean redrawAll, boolean all, boolean trim) {
	if (!GTK.gtk_widget_get_realized(handle)) return;
	int /*long*/ window = paintWindow ();
	GdkRectangle rect = new GdkRectangle ();
	if (redrawAll) {
		int [] w = new int [1], h = new int [1];
		gdk_window_get_size (window, w, h);
		rect.width = w [0];
		rect.height = h [0];
	} else {
		rect.x = x;
		rect.y = y;
		rect.width = Math.max (0, width);
		rect.height = Math.max (0, height);
	}
	GDK.gdk_window_invalidate_rect (window, rect, all);
}

@Override
void release (boolean destroy) {
	Control next = null, previous = null;
	if (destroy && parent != null) {
		Control[] children = parent._getChildren ();
		int index = 0;
		while (index < children.length) {
			if (children [index] == this) break;
			index++;
		}
		if (index > 0) {
			previous = children [index - 1];
		}
		if (index + 1 < children.length) {
			next = children [index + 1];
			next.removeRelation ();
		}
		removeRelation ();
	}
	super.release (destroy);
	if (destroy) {
		if (previous != null && next != null) previous.addRelation (next);
	}
}

@Override
void releaseHandle () {
	super.releaseHandle ();
	fixedHandle = 0;
	parent = null;
	cairoDisposeRegion();
}

@Override
void releaseParent () {
	parent.removeControl (this);
}

@Override
void releaseWidget () {
	boolean hadFocus = display.getFocusControl() == this;
	super.releaseWidget ();
	if (hadFocus) fixFocus (this);
	if (display.currentControl == this) display.currentControl = null;
	display.removeMouseHoverTimeout (handle);
	int /*long*/ imHandle = imHandle ();
	if (imHandle != 0) {
		GTK.gtk_im_context_reset (imHandle);
		GTK.gtk_im_context_set_client_window (imHandle, 0);
	}
	if (enableWindow != 0) {
		GDK.gdk_window_set_user_data (enableWindow, 0);
		GDK.gdk_window_destroy (enableWindow);
		enableWindow = 0;
	}
	redrawWindow = 0;
	if (menu != null && !menu.isDisposed ()) {
		menu.dispose ();
	}
	menu = null;
	cursor = null;
	toolTipText = null;
	layoutData = null;
	if (accessible != null) {
		accessible.internal_dispose_Accessible ();
	}
	accessible = null;
	region = null;
}

void restackWindow (int /*long*/ window, int /*long*/ sibling, boolean above) {
   	GDK.gdk_window_restack (window, sibling, above);
}

boolean sendDragEvent (int button, int stateMask, int x, int y, boolean isStateMask) {
	Event event = new Event ();
	event.button = button;
	Rectangle eventRect = new Rectangle (x, y, 0, 0);
	event.setBounds (eventRect);
	if ((style & SWT.MIRRORED) != 0) event.x = DPIUtil.autoScaleDown(getClientWidth ()) - event.x;
	if (isStateMask) {
		event.stateMask = stateMask;
	} else {
		setInputState (event, stateMask);
	}
	postEvent (SWT.DragDetect, event);
	if (isDisposed ()) return false;
	return event.doit;
}

void sendFocusEvent (int type) {
	Shell shell = _getShell ();
	Display display = this.display;
	display.focusControl = this;
	display.focusEvent = type;
	sendEvent (type);
	display.focusControl = null;
	display.focusEvent = SWT.None;
	/*
	* It is possible that the shell may be
	* disposed at this point.  If this happens
	* don't send the activate and deactivate
	* events.
	*/
	if (!shell.isDisposed ()) {
		switch (type) {
			case SWT.FocusIn:
				shell.setActiveControl (this);
				break;
			case SWT.FocusOut:
				if (shell != display.activeShell) {
					shell.setActiveControl (null);
				}
				break;
		}
	}
}

 boolean sendGestureEvent (int stateMask, int detail, int x, int y, double delta) {
	 if (containedInRegion(x, y)) return false;
	switch (detail) {
	case SWT.GESTURE_ROTATE: {
		return sendGestureEvent(stateMask, detail, x, y, delta, 0, 0, 0);
	}
	case SWT.GESTURE_MAGNIFY: {
		return sendGestureEvent(stateMask, detail, x, y, 0,0,0, delta);
	}
	case SWT.GESTURE_BEGIN: {
		return sendGestureEvent(stateMask, detail, x, y, 0, 0, 0, delta);
	}
	case SWT.GESTURE_END: {
		return sendGestureEvent(stateMask, detail, 0, 0, 0, 0, 0, 0);
	}
	default:
		//case not supported.
		return false;
	}
}

boolean sendGestureEvent (int stateMask, int detail, int x, int y, double xDirection, double yDirection) {
	if (containedInRegion(x, y)) return false;
	if (detail == SWT.GESTURE_SWIPE) {
		return sendGestureEvent(stateMask, detail, x, y, 0, (int)xDirection, (int)yDirection, 0);
	} else return false;
}

boolean sendGestureEvent (int stateMask, int detail, int x, int y, double rotation, int xDirection, int yDirection, double magnification) {
	if (containedInRegion(x, y)) return false;
	Event event = new Event ();
	event.stateMask = stateMask;
	event.detail = detail;
	event.x = x;
	event.y = y;
	switch (detail) {
		case SWT.GESTURE_ROTATE: {
			event.rotation = rotation;
			break;
		}
		case SWT.GESTURE_MAGNIFY: {
			event.magnification = magnification;
			break;
		}
		case SWT.GESTURE_SWIPE: {
			event.xDirection = xDirection;
			event.yDirection = yDirection;
			break;
		}
		case SWT.GESTURE_BEGIN:
		case SWT.GESTURE_END: {
			break;
		}
	}
	postEvent(SWT.Gesture, event);
	if (isDisposed ()) return false;
	return event.doit;
}

boolean sendHelpEvent (int /*long*/ helpType) {
	Control control = this;
	while (control != null) {
		if (control.hooks (SWT.Help)) {
			control.postEvent (SWT.Help);
			return true;
		}
		control = control.parent;
	}
	return false;
}

boolean sendLeaveNotify() {
	return false;
}

boolean sendMouseEvent (int type, int button, int time, double x, double y, boolean is_hint, int state) {
	if (containedInRegion((int) x, (int) y)) return true;
	return sendMouseEvent (type, button, 0, 0, false, time, x, y, is_hint, state);
}

/*
 * @return
 * 	true - event sending not canceled by user.
 *  false - event sending canceled by user.
 */
boolean sendMouseEvent (int type, int button, int count, int detail, boolean send, int time, double x, double y, boolean is_hint, int state) {
	if (containedInRegion((int) x, (int) y)) return true;
	if (!hooks (type) && !filters (type)) {
		/*
		 * On Wayland, MouseDown events are cached for DnD purposes, but
		 * unfortunately this breaks simple cases with a single MouseDown
		 * listener. The MouseDown event is cached but never sent, as MouseUp
		 * isn't hooked and thus the logic to send cached events is never run.
		 *
		 * The solution is to check for MouseUp events even if MouseUp isn't
		 * hooked. We can check the queue and flush it by sending the MouseDown
		 * event, similar to the way the caching logic does it when receiving a
		 * MouseMove event. See bug 529126.
		 */
		if (!OS.isX11() && dragDetectionQueue != null) {
			/*
			 * The first event in the queue will always be a MouseDown, as
			 * the queue is only ever created if a MouseDown event is being cached.
			 * Thus, if the queue only has one element, it is guaranteed to be a
			 * MouseDown event. More than 1 element implies MouseMove: let the caching
			 * logic handle this case.
			 */
			if (type == SWT.MouseUp && dragDetectionQueue.size() == 1) {
				Event mouseDownEvent = dragDetectionQueue.getFirst();
				dragDetectionQueue = null;
				sendOrPost(SWT.MouseDown, mouseDownEvent);
			}
		}
		return true;
	}
	Event event = new Event ();
	event.time = time;
	event.button = button;
	event.detail = detail;
	event.count = count;
	if (is_hint) {
		// coordinates are already window-relative, see #gtk_motion_notify_event(..) and bug 94502
		Rectangle eventRect = new Rectangle ((int)x, (int)y, 0, 0);
		event.setBounds (DPIUtil.autoScaleDown (eventRect));
	} else {
		int /*long*/ window = eventWindow ();
		int [] origin_x = new int [1], origin_y = new int [1];
		GDK.gdk_window_get_origin (window, origin_x, origin_y);
		Rectangle eventRect = new Rectangle ((int)x - origin_x [0], (int)y - origin_y [0], 0, 0);
		event.setBounds (DPIUtil.autoScaleDown (eventRect));
	}
	if ((style & SWT.MIRRORED) != 0) event.x = DPIUtil.autoScaleDown (getClientWidth ()) - event.x;
	setInputState (event, state);

	/**
	 * Bug 510446:
	 * In the original gtk2 DnD architecture, Drag detection was done in mouseDown.
	 * For Wayland support, Drag detection is now done in mouseMove (as does gtk internally).
	 *
	 * However, traditionally external widgets (e.g StyledText or non-SWT widgets) expect to
	 * know if a drag has started by the time mouseDown is sent.
	 * As such, for backwards compatibility with external widgets (e.g StyledText.java), we
	 * delay sending of SWT.MouseDown (and also queue up SWT.MouseMove) until we know if a
	 * drag started or not.
	 *
	 * Technical notes:
	 * - To ensure we follow 'send/post' contract as per parameter, we
	 *   temporarily utilize event.data to hold send/post flag.
	 *   There's also logic in place such that mouseDown/mouseMotion is always sent before mouseUp.
	 * - On Gtk2, mouseMove is sent during DnD. On Gtk3x11 it's not due to hacky implementation of DnD.
	 *   On Wayland mouseMove is once again sent during DnD as per improved architecture.
	 */
	event.data = Boolean.valueOf(send);
	if (!OS.isX11()) {
		if (type == SWT.MouseDown) {
			// Delay MouseDown
			dragDetectionQueue = new LinkedList<>();
			dragDetectionQueue.add(event);
			return true; // event never canceled as not yet sent.
		} else {
			if (dragDetectionQueue != null) {
				switch (type) {
				case SWT.MouseMove:
					if (dragDetect (event.x, event.y, false, true, null)) {
						// Case where mouse motion triggered a DnD:
						// Send only initial MouseDown but not the MouseMove events that were used
						// to determine DnD threshold.
						// This is to preserve backwards Cocoa/Win32 compatibility.
						Event mouseDownEvent = dragDetectionQueue.getFirst();
						mouseDownEvent.data = Boolean.valueOf(true); // force send MouseDown to avoid subsequent MouseMove before MouseDown.
						dragDetectionQueue = null;
						sendOrPost(SWT.MouseDown, mouseDownEvent);
					} else {
						dragDetectionQueue.add(event);
					}
					break;
				case SWT.MouseUp:
					// Case where mouse up was released before DnD threshold was hit.

					// Decide if we should send or post the queued up MouseDown and MouseMovement events.
					// If mouseUp is send, then send all. If mouseUp is post, then decide based on previous
					// send flag.
					boolean sendOrPostAll = send ? true : (Boolean) dragDetectionQueue.getFirst().data;
					dragDetectionQueue.forEach(queuedEvent -> queuedEvent.data = Boolean.valueOf(sendOrPostAll));

					// Flush queued up MouseDown/MouseMotion events, so they are triggered before MouseUp
					sendOrPost(SWT.MouseDown, dragDetectionQueue.removeFirst());
					dragDetectionQueue.forEach(queuedEvent -> sendOrPost(SWT.MouseMove, queuedEvent));
					dragDetectionQueue = null;
				}
			}
		}
	}
	return sendOrPost(type, event);
}

private boolean sendOrPost(int type, Event event) {
	assert event.data != null : "event.data should have been a Boolean, but received null";
	boolean send = (Boolean) event.data;
	event.data = null;

	if (send) {
		sendEvent (type, event);
		if (isDisposed ()) return false;
	} else {
		postEvent (type, event);
	}
	return event.doit;
}

/**
 * Not direct gtk api, but useful to have them combined as they are usually called together.
 * @param widget the GTK reference.
 * @param hAlign is of type GTKAlign enum. See OS.java
 * @param vAlign is of type GTKAlign enum. See OS.java
 */
void gtk_widget_set_align(int /*long*/ widget, int hAlign, int vAlign) {
	GTK.gtk_widget_set_halign (widget, hAlign);
	GTK.gtk_widget_set_valign (widget, vAlign);
}

void gtk_label_set_align(int /*long*/ label, float xAlign, float yAlign) {
	GTK.gtk_label_set_xalign(label, xAlign);
	GTK.gtk_label_set_yalign(label, yAlign);
}

void setBackground () {
	if ((state & BACKGROUND) == 0 && backgroundImage == null) {
		if ((state & PARENT_BACKGROUND) != 0) {
			setParentBackground ();
		} else {
			setWidgetBackground ();
		}
		redrawWidget (0, 0, 0, 0, true, false, false);
	}
}

/**
 * Sets the receiver's background color to the color specified
 * by the argument, or to the default system color for the control
 * if the argument is null.
 * <p>
 * Note: This operation is a hint and may be overridden by the platform.
 * </p>
 * @param color the new color (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setBackground (Color color) {
	checkWidget ();
	_setBackground (color);
	if (color != null) {
		this.updateBackgroundMode ();
	}
}

private void _setBackground (Color color) {
	if (((state & BACKGROUND) == 0) && color == null) return;
	boolean set = false;
	if (GTK.GTK3) {
		GdkRGBA rgba = null;
		if (color != null) {
			rgba = color.handleRGBA;
			backgroundAlpha = color.getAlpha();
		}
		set = true;
		if (set) {
			if (color == null) {
				state &= ~BACKGROUND;
			} else {
				state |= BACKGROUND;
			}
			setBackgroundGdkRGBA (rgba);
		}
	} else {
		GdkColor gdkColor = null;
		if (color != null) {
			if (color.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
			gdkColor = color.handle;
			backgroundAlpha = color.getAlpha ();
		}
		if (gdkColor == null) {
			int /*long*/ style = GTK.gtk_widget_get_modifier_style (handle);
			set = (GTK.gtk_rc_style_get_color_flags (style, GTK.GTK_STATE_NORMAL) & GTK.GTK_RC_BG) != 0;
		} else {
			GdkColor oldColor = getBackgroundGdkColor ();
			set = oldColor.pixel != gdkColor.pixel;
		}
		if (set) {

			if (color == null) {
				state &= ~BACKGROUND;
			} else {
				state |= BACKGROUND;
			}
			setBackgroundGdkColor (gdkColor);
		}
	}
	redrawChildren ();
}

void setBackgroundGdkRGBA (int /*long*/ context, int /*long*/ handle, GdkRGBA rgba) {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	GdkRGBA selectedBackground = display.getSystemColor(SWT.COLOR_LIST_SELECTION).handleRGBA;
    if (GTK.GTK_VERSION >= OS.VERSION(3, 14, 0)) {
    	// Form background string
        String name = GTK.GTK_VERSION >= OS.VERSION(3, 20, 0) ? display.gtk_widget_class_get_css_name(handle)
        		: display.gtk_widget_get_name(handle);
        String css = name + " {background-color: " + display.gtk_rgba_to_css_string(rgba) + ";}\n"
                + name + ":selected" + " {background-color: " + display.gtk_rgba_to_css_string(selectedBackground) + ";}";

        // Cache background
        cssBackground = css;

        // Apply background color and any cached foreground color
        String finalCss = display.gtk_css_create_css_color_string (cssBackground, cssForeground, SWT.BACKGROUND);
        gtk_css_provider_load_from_css (context, finalCss);
    } else {
        GTK.gtk_widget_override_background_color (handle, GTK.GTK_STATE_FLAG_NORMAL, rgba);
        GTK.gtk_widget_override_background_color(handle, GTK.GTK_STATE_FLAG_SELECTED, selectedBackground);
    }
}

void setBackgroundGradientGdkRGBA (int /*long*/ context, int /*long*/ handle, GdkRGBA rgba) {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	String css ="* {\n";
	if (rgba != null) {
		String color = display.gtk_rgba_to_css_string (rgba);
		//Note, use 'background-image' CSS class with caution. Not all themes/widgets support it. (e.g button doesn't).
		//Use 'background' CSS class where possible instead unless 'background-image' is explicidly supported.
		css += "background-image: -gtk-gradient (linear, 0 0, 0 1, color-stop(0, " + color + "), color-stop(1, " + color + "));\n";
	}
	css += "}\n";
	//Cache background color
	cssBackground = css;

	// Apply background color and any cached foreground color
	String finalCss = display.gtk_css_create_css_color_string (cssBackground, cssForeground, SWT.BACKGROUND);
	gtk_css_provider_load_from_css (context, finalCss);
}

void gtk_css_provider_load_from_css (int /*long*/ context, String css) {
	/* Utility function. */
	//@param css : a 'css java' string like "{\nbackground: red;\n}".

	if (provider == 0) {
		provider = GTK.gtk_css_provider_new ();
		GTK.gtk_style_context_add_provider (context, provider, GTK.GTK_STYLE_PROVIDER_PRIORITY_APPLICATION);
		OS.g_object_unref (provider);
	}
	GTK.gtk_css_provider_load_from_data (provider, Converter.wcsToMbcs (css, true), -1, null);
}

void setBackgroundGdkColor (int /*long*/ handle, GdkColor color) {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	int index = GTK.GTK_STATE_NORMAL;
	int /*long*/ style = GTK.gtk_widget_get_modifier_style (handle);
	int /*long*/ ptr = GTK.gtk_rc_style_get_bg_pixmap_name (style, index);
	if (ptr != 0) OS.g_free (ptr);
	ptr = 0;

	String pixmapName = null;
	int flags = GTK.gtk_rc_style_get_color_flags (style, index);
	if (color != null) {
		flags |= GTK.GTK_RC_BG;
		pixmapName = "<none>"; //$NON-NLS-1$
	} else {
		flags &= ~GTK.GTK_RC_BG;
		if (backgroundImage == null && (state & PARENT_BACKGROUND) != 0) {
			pixmapName = "<parent>"; //$NON-NLS-1$
		}
	}
	if (pixmapName != null) {
		byte[] buffer = Converter.wcsToMbcs (pixmapName, true);
		ptr = OS.g_malloc (buffer.length);
		C.memmove (ptr, buffer, buffer.length);
	}

	GTK.gtk_rc_style_set_bg_pixmap_name (style, index, ptr);
	GTK.gtk_rc_style_set_bg (style, index, color);
	GTK.gtk_rc_style_set_color_flags (style, index, flags);
	modifyStyle (handle, style);
}

void setBackgroundGdkColor (GdkColor color) {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	setBackgroundGdkColor (handle, color);
}

void setBackgroundGdkRGBA(GdkRGBA rgba) {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	setBackgroundGdkRGBA (handle, rgba);
}

void setBackgroundGdkRGBA (int /*long*/ handle, GdkRGBA rgba) {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	double alpha = 1.0;
	if (rgba == null) {
		if ((state & PARENT_BACKGROUND) != 0) {
			alpha = 0;
			Control control = findBackgroundControl();
			if (control == null) control = this;
			rgba = control.getBackgroundGdkRGBA();
		}
	}
	else {
		alpha = backgroundAlpha;
	}
	if (rgba != null) {
		rgba.alpha = alpha / (float)255;
	}
	int /*long*/ context = GTK.gtk_widget_get_style_context (handle);
	setBackgroundGdkRGBA (context, handle, rgba);
	GTK.gtk_style_context_invalidate (context);
	return;
}
/**
 * Sets the receiver's background image to the image specified
 * by the argument, or to the default system color for the control
 * if the argument is null.  The background image is tiled to fill
 * the available space.
 * <p>
 * Note: This operation is a hint and may be overridden by the platform.
 * For example, on Windows the background of a Button cannot be changed.
 * </p>
 * @param image the new image (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument is not a bitmap</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.2
 */
public void setBackgroundImage (Image image) {
	checkWidget ();
	if (image != null && image.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
	if (image == backgroundImage && backgroundAlpha > 0) return;
	backgroundAlpha = 255;
	this.backgroundImage = image;
	if (backgroundImage != null) {
		setBackgroundPixmap (backgroundImage);
		redrawWidget (0, 0, 0, 0, true, false, false);
	} else {
		setWidgetBackground ();
	}
	redrawChildren ();
}

void setBackgroundPixmap (Image image) {
	int /*long*/ window = gtk_widget_get_window (paintHandle ());
	if (window != 0) {
		if (image.pixmap != 0) {
			GDK.gdk_window_set_back_pixmap (window, image.pixmap, false);
		} else if (image.surface != 0) {
			if (GTK.GTK3) {
				int /*long*/ pattern = Cairo.cairo_pattern_create_for_surface(image.surface);
				if (pattern == 0) SWT.error(SWT.ERROR_NO_HANDLES);
				Cairo.cairo_pattern_set_extend(pattern, Cairo.CAIRO_EXTEND_REPEAT);
				GDK.gdk_window_set_background_pattern(window, pattern);
				Cairo.cairo_pattern_destroy(pattern);
			}
			/*
			* TODO This code code is commented because it does not work since the pixmap
			* created with gdk_pixmap_foreign_new() does not have colormap. Another option
			* would be to create a pixmap on the fly from the surface.
			*
			* For now draw background in windowProc().
			*/
//			int /*long*/ surface = image.surface;
//			int type = Cairo.cairo_surface_get_type(surface);
//			switch (type) {
//				case Cairo.CAIRO_SURFACE_TYPE_XLIB:
//					int /*long*/ pixmap = OS.gdk_pixmap_foreign_new(Cairo.cairo_xlib_surface_get_drawable(surface));
//					OS.gdk_window_set_back_pixmap (window, pixmap, false);
//					OS.g_object_unref(pixmap);
//					break;
//			}
		}
	}
}

/**
 * If the argument is <code>true</code>, causes the receiver to have
 * all mouse events delivered to it until the method is called with
 * <code>false</code> as the argument.  Note that on some platforms,
 * a mouse button must currently be down for capture to be assigned.
 *
 * @param capture <code>true</code> to capture the mouse, and <code>false</code> to release it
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setCapture (boolean capture) {
	checkWidget();
	/* FIXME !!!!! */
	/*
	if (capture) {
		OS.gtk_widget_grab_focus (handle);
	} else {
		OS.gtk_widget_grab_default (handle);
	}
	*/
}
/**
 * Sets the receiver's cursor to the cursor specified by the
 * argument, or to the default cursor for that kind of control
 * if the argument is null.
 * <p>
 * When the mouse pointer passes over a control its appearance
 * is changed to match the control's cursor.
 * </p>
 *
 * @param cursor the new cursor (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setCursor (Cursor cursor) {
	checkWidget();
	if (cursor != null && cursor.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
	this.cursor = cursor;
	setCursor (cursor != null ? cursor.handle : 0);
}

void setCursor (int /*long*/ cursor) {
	int /*long*/ window = eventWindow ();
	if (window != 0) {
		GDK.gdk_window_set_cursor (window, cursor);
		GDK.gdk_flush ();
	}
}

/**
 * Sets the receiver's drag detect state. If the argument is
 * <code>true</code>, the receiver will detect drag gestures,
 * otherwise these gestures will be ignored.
 *
 * @param dragDetect the new drag detect state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.3
 */
public void setDragDetect (boolean dragDetect) {
	checkWidget ();
	if (dragDetect) {
		state |= DRAG_DETECT;
	} else {
		state &= ~DRAG_DETECT;
	}
}

static int /*long*/ enterNotifyEventProc (int /*long*/ ihint, int /*long*/ n_param_values, int /*long*/ param_values, int /*long*/ data) {
	/* 427776: this workaround listens to the enter-notify-event signal on all
	 * GtkWidgets. If enableWindow (the data parameter) has been added to the
	 * internal hash table of the widget, a record is kept as the lifetime of
	 * enableWindow is controlled here, so we'll need to remove that reference
	 * when we destroy enableWindow. this internal hash table was removed in
	 * GTK 3.11.9 so once only newer GTK is targeted, this workaround can be
	 * removed. */
	int /*long*/ instance = OS.g_value_peek_pointer (param_values);
	int /*long*/ hashTable = OS.g_object_get_qdata (instance, GTK_POINTER_WINDOW);

	// there will only ever be one item in the hash table
	if (hashTable != 0) {
		int /*long*/ firstItem = OS.g_hash_table_get_values (hashTable);
		int /*long*/ gdkWindow = OS.g_list_data (firstItem);
		// data is actually enableWindow
		if (gdkWindow == data)
			OS.g_object_set_qdata(gdkWindow, SWT_GRAB_WIDGET, instance);
	}

	return 1; // keep the signal connected
}

/**
 * Enables the receiver if the argument is <code>true</code>,
 * and disables it otherwise. A disabled control is typically
 * not selectable from the user interface and draws with an
 * inactive or "grayed" look.
 *
 * @param enabled the new enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setEnabled (boolean enabled) {
	checkWidget();
	if (((state & DISABLED) == 0) == enabled) return;
	Control control = null;
	boolean fixFocus = false;
	if (!enabled) {
		if (display.focusEvent != SWT.FocusOut) {
			control = display.getFocusControl ();
			fixFocus = isFocusAncestor (control);
		}
	}
	if (enabled) {
		state &= ~DISABLED;
	} else {
		state |= DISABLED;
	}
	enableWidget (enabled);
	if (isDisposed ()) return;
	if (enabled) {
		if (enableWindow != 0) {
			cleanupEnableWindow();
		}
	} else {
		GTK.gtk_widget_realize (handle);
		int /*long*/ parentHandle = parent.eventHandle ();
		int /*long*/ window = parent.eventWindow ();
		int /*long*/ topHandle = topHandle ();
		GdkWindowAttr attributes = new GdkWindowAttr ();
		GtkAllocation allocation = new GtkAllocation ();
		GTK.gtk_widget_get_allocation (topHandle, allocation);
		attributes.x = allocation.x;
		attributes.y = allocation.y;
		attributes.width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		attributes.height = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
		attributes.event_mask = (0xFFFFFFFF & ~OS.ExposureMask);
		attributes.wclass = GDK.GDK_INPUT_ONLY;
		attributes.window_type = GDK.GDK_WINDOW_CHILD;
		enableWindow = GDK.gdk_window_new (window, attributes, GDK.GDK_WA_X | GDK.GDK_WA_Y);
		if (enableWindow != 0) {
			/* 427776: we need to listen to all enter-notify-event signals to
			 * see if this new GdkWindow has been added to a widget's internal
			 * hash table, so when the GdkWindow is destroyed we can also remove
			 * that reference. */
			if (enterNotifyEventFunc != null)
				enterNotifyEventId = OS.g_signal_add_emission_hook (enterNotifyEventSignalId, 0, enterNotifyEventFunc.getAddress (), enableWindow, 0);

			GDK.gdk_window_set_user_data (enableWindow, parentHandle);
			restackWindow (enableWindow, gtk_widget_get_window (topHandle), true);
			if (GTK.gtk_widget_get_visible (topHandle)) GDK.gdk_window_show_unraised (enableWindow);
		}
	}
	if (fixFocus) fixFocus (control);
}

void cleanupEnableWindow() {
	if (enterNotifyEventFunc != null) {
		if (enterNotifyEventId > 0)
			OS.g_signal_remove_emission_hook(enterNotifyEventSignalId, enterNotifyEventId);
		enterNotifyEventId = 0;

		/*
		 * 427776: now we can remove any reference to the GdkWindow
		 * in a widget's internal hash table. this internal hash
		 * table was removed in GTK 3.11.9 so once only newer GTK is
		 * targeted, this workaround can be removed.
		 */
		int /*long*/ grabWidget = OS.g_object_get_qdata(enableWindow, SWT_GRAB_WIDGET);
		if (grabWidget != 0) {
			OS.g_object_set_qdata(grabWidget, GTK_POINTER_WINDOW, 0);
			OS.g_object_set_qdata(enableWindow, SWT_GRAB_WIDGET, 0);
		}
	}

	GDK.gdk_window_set_user_data (enableWindow, 0);
	GDK.gdk_window_destroy (enableWindow);
	enableWindow = 0;
}

/**
 * Causes the receiver to have the <em>keyboard focus</em>,
 * such that all keyboard events will be delivered to it.  Focus
 * reassignment will respect applicable platform constraints.
 *
 * @return <code>true</code> if the control got focus, and <code>false</code> if it was unable to.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #forceFocus
 */
public boolean setFocus () {
	checkWidget();
	if ((style & SWT.NO_FOCUS) != 0) return false;
	return forceFocus ();
}

/**
 * Sets the font that the receiver will use to paint textual information
 * to the font specified by the argument, or to the default font for that
 * kind of control if the argument is null.
 *
 * @param font the new font (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setFont (Font font) {
	checkWidget();
	if (((state & FONT) == 0) && font == null) return;
	this.font = font;
	int /*long*/ fontDesc;
	if (font == null) {
		fontDesc = defaultFont ().handle;
	} else {
		if (font.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
		fontDesc = font.handle;
	}
	if (font == null) {
		state &= ~FONT;
	} else {
		state |= FONT;
	}
	setFontDescription (fontDesc);
}

void setFontDescription (int /*long*/ font) {
	setFontDescription (handle, font);
}

/**
 * Sets the receiver's foreground color to the color specified
 * by the argument, or to the default system color for the control
 * if the argument is null.
 * <p>
 * Note: This operation is a hint and may be overridden by the platform.
 * </p>
 * @param color the new color (or null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setForeground (Color color) {
	checkWidget();
	if (((state & FOREGROUND) == 0) && color == null) return;
	GdkColor gdkColor = null;
	if (color != null) {
		if (color.isDisposed ()) error(SWT.ERROR_INVALID_ARGUMENT);
		gdkColor = color.handle;
	}
	boolean set = false;
	if (GTK.GTK3) {
		set = !getForeground().equals(color);
	} else {
		if (gdkColor == null) {
			int /*long*/ style = GTK.gtk_widget_get_modifier_style (handle);
			set = (GTK.gtk_rc_style_get_color_flags (style, GTK.GTK_STATE_NORMAL) & GTK.GTK_RC_FG) != 0;
		} else {
			GdkColor oldColor = getForegroundGdkColor ();
			set = oldColor.pixel != gdkColor.pixel;
		}
	}
	if (set) {
		if (color == null) {
			state &= ~FOREGROUND;
		} else {
			state |= FOREGROUND;
		}
		if (GTK.GTK3) {
			GdkRGBA rgba = color == null ? null : color.handleRGBA;
			setForegroundGdkRGBA (rgba);
		} else {
			setForegroundGdkColor (gdkColor);
		}
	}
}

void setForegroundGdkRGBA (GdkRGBA rgba) {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	setForegroundGdkRGBA (handle, rgba);
}

void setForegroundGdkColor (GdkColor color) {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	setForegroundColor (handle, color);
}

void setForegroundGdkRGBA (int /*long*/ handle, GdkRGBA rgba) {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	if (GTK.GTK_VERSION >= OS.VERSION(3, 14, 0)) {
		GdkRGBA toSet;
		if (rgba != null) {
			toSet = rgba;
		} else {
			toSet = display.COLOR_WIDGET_FOREGROUND_RGBA;
		}
		int /*long*/ context = GTK.gtk_widget_get_style_context (handle);
		// Form foreground string
		String color = display.gtk_rgba_to_css_string(toSet);
		String name = GTK.GTK_VERSION >= OS.VERSION(3, 20, 0) ? display.gtk_widget_class_get_css_name(handle)
	    		: display.gtk_widget_get_name(handle);
		GdkRGBA selectedForeground = display.COLOR_LIST_SELECTION_TEXT_RGBA;
		String selection = GTK.GTK_VERSION >= OS.VERSION(3, 20, 0) &&
				!name.contains("treeview") ? " selection" : ":selected";
		String css = "* {color: " + color + ";}\n"
				+ name + selection + " {color: " + display.gtk_rgba_to_css_string(selectedForeground) + ";}";

		// Cache foreground color
		cssForeground = css;

		// Apply foreground color and any cached background color
		String finalCss = display.gtk_css_create_css_color_string (cssBackground, cssForeground, SWT.FOREGROUND);
		gtk_css_provider_load_from_css(context, finalCss);
	} else {
		GdkRGBA selectedForeground = display.COLOR_LIST_SELECTION_TEXT_RGBA;
		GTK.gtk_widget_override_color (handle, GTK.GTK_STATE_FLAG_NORMAL, rgba);
		GTK.gtk_widget_override_color (handle, GTK.GTK_STATE_FLAG_SELECTED, selectedForeground);
		int /*long*/ context = GTK.gtk_widget_get_style_context (handle);
		GTK.gtk_style_context_invalidate (context);
		return;
	}
}

void setInitialBounds () {
	if ((state & ZERO_WIDTH) != 0 && (state & ZERO_HEIGHT) != 0) {
		/*
		* Feature in GTK.  On creation, each widget's allocation is
		* initialized to a position of (-1, -1) until the widget is
		* first sized.  The fix is to set the value to (0, 0) as
		* expected by SWT.
		*/
		int /*long*/ topHandle = topHandle ();
		GtkAllocation allocation = new GtkAllocation();
		if ((parent.style & SWT.MIRRORED) != 0) {
			allocation.x = parent.getClientWidth ();
		} else {
			allocation.x = 0;
		}
		allocation.y = 0;
		if (GTK.GTK3) {
			GTK.gtk_widget_set_visible(topHandle, true);
		}
		GTK.gtk_widget_set_allocation(topHandle, allocation);
	} else {
		resizeHandle (1, 1);
		forceResize ();
	}
}

/*
 * Sets the receivers Drag Gestures in order to do drag detection correctly for
 * X11/Wayland window managers after GTK3.14.
 * TODO currently phase is set to BUBBLE = 2. Look into using groups perhaps.
 */
private void setDragGesture () {
        if (GTK.GTK_VERSION >= OS.VERSION(3, 14, 0)) {
			dragGesture = GTK.gtk_gesture_drag_new (handle);
			GTK.gtk_event_controller_set_propagation_phase (dragGesture,
			        2);
			GTK.gtk_gesture_single_set_button (dragGesture, 0);
			OS.g_signal_connect(dragGesture, OS.begin, gestureBegin.getAddress(), this.handle);
			OS.g_signal_connect(dragGesture, OS.end, gestureEnd.getAddress(), this.handle);
			return;
        }
}

//private void setPanGesture () {
///* TODO: Panning gesture requires a GtkOrientation object. Need to discuss what orientation should be default. */
//}

private void setRotateGesture () {
    if (GTK.GTK_VERSION >= OS.VERSION(3, 14, 0)) {
		rotateGesture = GTK.gtk_gesture_rotate_new(handle);
		GTK.gtk_event_controller_set_propagation_phase (rotateGesture,
		        2);
		OS.g_signal_connect (rotateGesture, OS.angle_changed, gestureRotation.getAddress(), this.handle);
		OS.g_signal_connect(rotateGesture, OS.begin, gestureBegin.getAddress(), this.handle);
		OS.g_signal_connect(rotateGesture, OS.end, gestureEnd.getAddress(), this.handle);
		return;
    }
}

private void setZoomGesture () {
        if (GTK.GTK_VERSION >= OS.VERSION(3, 14, 0)) {
			zoomGesture = GTK.gtk_gesture_zoom_new(handle);
			GTK.gtk_event_controller_set_propagation_phase (zoomGesture,
			        2);
			OS.g_signal_connect(zoomGesture, OS.scale_changed, gestureZoom.getAddress(), this.handle);
			OS.g_signal_connect(zoomGesture, OS.begin, gestureBegin.getAddress(), this.handle);
			OS.g_signal_connect(zoomGesture, OS.end, gestureEnd.getAddress(), this.handle);
			return;
        }
}

static Control getControl(int /*long*/ handle) {
	Display display = Display.findDisplay(Thread.currentThread());
	if (display ==null || display.isDisposed()) return null;
	Widget widget = display.findWidget(handle);
	if (widget == null) return null;
	return (Control) widget;
}

static void rotateProc(int /*long*/ gesture, double angle, double angle_delta, int /*long*/ user_data) {
	if (GTK.gtk_gesture_is_recognized(gesture)) {
		int [] state = new int[1];
		double [] x = new double[1];
		double [] y = new double[1];
		GTK.gtk_get_current_event_state(state);
		GTK.gtk_gesture_get_point(gesture, GTK.gtk_gesture_get_last_updated_sequence(gesture), x, y);
		/*
		 * Returning delta is off by two decimal points and is returning negative numbers on
		 * counter clockwise rotations from GTK. From the java doc of GestureEvent.rotation,
		 * we have to invert the rotation number so that positive/negative numbers are returned
		 * correctly (inverted).
		 */
		double delta = -(GTK.gtk_gesture_rotate_get_angle_delta(gesture)*100);
		Control control = getControl(user_data);
		control.sendGestureEvent(state[0], SWT.GESTURE_ROTATE, (int) x[0], (int) y[0], delta);
	}
}

static void magnifyProc(int /*long*/ gesture, double zoom, int /*long*/ user_data) {
	if (GTK.gtk_gesture_is_recognized(gesture)) {
		int [] state = new int[1];
		double [] x = new double[1];
		double [] y = new double[1];
		GTK.gtk_get_current_event_state(state);
		GTK.gtk_gesture_get_point(gesture, GTK.gtk_gesture_get_last_updated_sequence(gesture), x, y);
		double delta = GTK.gtk_gesture_zoom_get_scale_delta(gesture);
		Control control = getControl(user_data);
		control.sendGestureEvent(state[0], SWT.GESTURE_MAGNIFY, (int) x[0], (int) y[0], delta);
	}
}

static void swipeProc(int /*long*/ gesture, double velocity_x, double velocity_y, int /*long*/ user_data) {
	if (GTK.gtk_gesture_is_recognized(gesture)) {
		double [] xVelocity = new double [1];
		double [] yVelocity = new double [1];
		if (GTK.gtk_gesture_swipe_get_velocity(gesture, xVelocity, yVelocity)) {
			int [] state = new int[1];
			double [] x = new double[1];
			double [] y = new double[1];
			GTK.gtk_get_current_event_state(state);
			GTK.gtk_gesture_get_point(gesture, GTK.gtk_gesture_get_last_updated_sequence(gesture), x, y);
			Control control = getControl(user_data);
			control.sendGestureEvent(state[0], SWT.GESTURE_SWIPE, (int) x[0], (int) y[0], xVelocity[0], yVelocity[0]);
		}
	}
}

static void gestureBeginProc(int /*long*/ gesture, int /*long*/ sequence, int /*long*/ user_data) {
	if (GTK.gtk_gesture_is_recognized(gesture)) {
		int [] state = new int[1];
		double [] x = new double[1];
		double [] y = new double[1];
		GTK.gtk_get_current_event_state(state);
		GTK.gtk_gesture_get_point(gesture, sequence, x, y);
		Control control = getControl(user_data);
		control.sendGestureEvent(state[0], SWT.GESTURE_BEGIN, (int) x[0], (int) y[0], 0);
	}
}

static void gestureEndProc(int /*long*/ gesture, int /*long*/ sequence, int /*long*/ user_data) {
	if (GTK.gtk_gesture_is_recognized(gesture)) {
		int [] state = new int[1];
		double [] x = new double[1];
		double [] y = new double[1];
		GTK.gtk_get_current_event_state(state);
		GTK.gtk_gesture_get_point(gesture, GTK.gtk_gesture_get_last_updated_sequence(gesture), x, y);
		Control control = getControl(user_data);
		control.sendGestureEvent(state[0], SWT.GESTURE_END, (int) x[0], (int) y[0], 0);
	}
}
/**
 * Sets the receiver's pop up menu to the argument.
 * All controls may optionally have a pop up
 * menu that is displayed when the user requests one for
 * the control. The sequence of key strokes, button presses
 * and/or button releases that are used to request a pop up
 * menu is platform specific.
 * <p>
 * Note: Disposing of a control that has a pop up menu will
 * dispose of the menu.  To avoid this behavior, set the
 * menu to null before the control is disposed.
 * </p>
 *
 * @param menu the new pop up menu
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_MENU_NOT_POP_UP - the menu is not a pop up menu</li>
 *    <li>ERROR_INVALID_PARENT - if the menu is not in the same widget tree</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the menu has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setMenu (Menu menu) {
	checkWidget();
	if (menu != null) {
		if ((menu.style & SWT.POP_UP) == 0) {
			error (SWT.ERROR_MENU_NOT_POP_UP);
		}
		if (menu.parent != menuShell ()) {
			error (SWT.ERROR_INVALID_PARENT);
		}
	}
	this.menu = menu;
}

@Override
void setOrientation (boolean create) {
	if ((style & SWT.RIGHT_TO_LEFT) != 0 || !create) {
		int dir = (style & SWT.RIGHT_TO_LEFT) != 0 ? GTK.GTK_TEXT_DIR_RTL : GTK.GTK_TEXT_DIR_LTR;
		if (handle != 0) GTK.gtk_widget_set_direction (handle, dir);
		if (fixedHandle != 0) GTK.gtk_widget_set_direction (fixedHandle, dir);
	}
}

/**
 * Sets the orientation of the receiver, which must be one
 * of the constants <code>SWT.LEFT_TO_RIGHT</code> or <code>SWT.RIGHT_TO_LEFT</code>.
 * <p>
 *
 * @param orientation new orientation style
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.7
 */
public void setOrientation (int orientation) {
	checkWidget ();
	int flags = SWT.RIGHT_TO_LEFT | SWT.LEFT_TO_RIGHT;
	if ((orientation & flags) == 0 || (orientation & flags) == flags) return;
	style &= ~flags;
	style |= orientation & flags;
	setOrientation (false);
	style &= ~SWT.MIRRORED;
	checkMirrored ();
}

/**
 * Changes the parent of the widget to be the one provided.
 * Returns <code>true</code> if the parent is successfully changed.
 *
 * @param parent the new parent for the control.
 * @return <code>true</code> if the parent is changed and <code>false</code> otherwise.
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is <code>null</code></li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *	</ul>
 */
public boolean setParent (Composite parent) {
	checkWidget ();
	if (parent == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (parent.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
	if (this.parent == parent) return true;
	if (!isReparentable ()) return false;
	GTK.gtk_widget_realize (parent.handle);
	int /*long*/ topHandle = topHandle ();
	GtkAllocation allocation = new GtkAllocation ();
	GTK.gtk_widget_get_allocation (topHandle, allocation);
	int x = allocation.x;
	int y = allocation.y;
	int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
	int height = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
	if ((this.parent.style & SWT.MIRRORED) != 0) {
		x =  this.parent.getClientWidth () - width - x;
	}
	if ((parent.style & SWT.MIRRORED) != 0) {
		x = parent.getClientWidth () - width - x;
	}
	releaseParent ();
	Shell newShell = parent.getShell (), oldShell = getShell ();
	Decorations newDecorations = parent.menuShell (), oldDecorations = menuShell ();
	Menu [] menus = oldShell.findMenus (this);
	if (oldShell != newShell || oldDecorations != newDecorations) {
		fixChildren (newShell, oldShell, newDecorations, oldDecorations, menus);
		newDecorations.fixAccelGroup ();
		oldDecorations.fixAccelGroup ();
	}
	int /*long*/ newParent = parent.parentingHandle();
	gtk_widget_reparent(this, newParent);
	if (GTK.GTK3) {
		OS.swt_fixed_move (newParent, topHandle, x, y);
	} else {
		GTK.gtk_fixed_move (newParent, topHandle, x, y);
	}
	/*
	* Restore the original widget size since GTK does not keep it.
	*/
	resizeHandle(width, height);
	/*
	* Cause a size allocation this widget's topHandle.  Note that
	* all calls to gtk_widget_size_allocate() must be preceded by
	* a call to gtk_widget_size_request().
	*/
	GtkRequisition requisition = new GtkRequisition ();
	gtk_widget_size_request (topHandle, requisition);
	allocation.x = x;
	allocation.y = y;
	allocation.width = width;
	allocation.height = height;
	GTK.gtk_widget_size_allocate (topHandle, allocation);
	this.parent = parent;
	setZOrder (null, false, true);
	reskin (SWT.ALL);
	return true;
}

void setParentBackground () {
	if (GTK.GTK3) {
		setBackgroundGdkRGBA (handle, null);
		if (fixedHandle != 0) setBackgroundGdkRGBA (fixedHandle, null);
	} else {
		setBackgroundGdkColor (handle, null);
		if (fixedHandle != 0) setBackgroundGdkColor (fixedHandle, null);
	}
}

void setParentGdkWindow (Control child) {
}

boolean setRadioSelection (boolean value) {
	return false;
}

/**
 * If the argument is <code>false</code>, causes subsequent drawing
 * operations in the receiver to be ignored. No drawing of any kind
 * can occur in the receiver until the flag is set to true.
 * Graphics operations that occurred while the flag was
 * <code>false</code> are lost. When the flag is set to <code>true</code>,
 * the entire widget is marked as needing to be redrawn.  Nested calls
 * to this method are stacked.
 * <p>
 * Note: This operation is a hint and may not be supported on some
 * platforms or for some widgets.
 * </p>
 *
 * @param redraw the new redraw state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #redraw(int, int, int, int, boolean)
 * @see #update()
 */
public void setRedraw (boolean redraw) {
	checkWidget();
	if (redraw) {
		if (--drawCount == 0) {
			if (redrawWindow != 0) {
				int /*long*/ window = paintWindow ();
				/* Explicitly hiding the window avoids flicker on GTK+ >= 2.6 */
				GDK.gdk_window_hide (redrawWindow);
				GDK.gdk_window_destroy (redrawWindow);
				GDK.gdk_window_set_events (window, GTK.gtk_widget_get_events (paintHandle ()));
				redrawWindow = 0;
			}
		}
	} else {
		if (drawCount++ == 0) {
			if (GTK.gtk_widget_get_realized (handle)) {
				int /*long*/ window = paintWindow ();
				Rectangle rect = getBoundsInPixels ();
				GdkWindowAttr attributes = new GdkWindowAttr ();
				attributes.width = rect.width;
				attributes.height = rect.height;
				attributes.event_mask = GDK.GDK_EXPOSURE_MASK;
				attributes.window_type = GDK.GDK_WINDOW_CHILD;
				redrawWindow = GDK.gdk_window_new (window, attributes, 0);
				if (redrawWindow != 0) {
					int mouseMask = GDK.GDK_BUTTON_PRESS_MASK | GDK.GDK_BUTTON_RELEASE_MASK |
						GDK.GDK_ENTER_NOTIFY_MASK | GDK.GDK_LEAVE_NOTIFY_MASK |
						GDK.GDK_POINTER_MOTION_MASK | GDK.GDK_POINTER_MOTION_HINT_MASK |
						GDK.GDK_BUTTON_MOTION_MASK | GDK.GDK_BUTTON1_MOTION_MASK |
						GDK.GDK_BUTTON2_MOTION_MASK | GDK.GDK_BUTTON3_MOTION_MASK;
					GDK.gdk_window_set_events (window, GDK.gdk_window_get_events (window) & ~mouseMask);
					if (GTK.GTK3) {
						GDK.gdk_window_set_background_pattern(redrawWindow, 0);
					} else {
						GDK.gdk_window_set_back_pixmap (redrawWindow, 0, false);
					}
					GDK.gdk_window_show (redrawWindow);
				}
			}
		}
	}
}

@Override
boolean setTabItemFocus (boolean next) {
	if (!isShowing ()) return false;
	return forceFocus ();
}

/**
 * Sets the base text direction (a.k.a. "paragraph direction") of the receiver,
 * which must be one of the constants <code>SWT.LEFT_TO_RIGHT</code>,
 * <code>SWT.RIGHT_TO_LEFT</code>, or <code>SWT.AUTO_TEXT_DIRECTION</code>.
 * <p>
 * <code>setOrientation</code> would override this value with the text direction
 * that is consistent with the new orientation.
 * </p>
 * <p>
 * <b>Warning</b>: This API is currently only implemented on Windows.
 * It doesn't set the base text direction on GTK and Cocoa.
 * </p>
 *
 * @param textDirection the base text direction style
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SWT#LEFT_TO_RIGHT
 * @see SWT#RIGHT_TO_LEFT
 * @see SWT#AUTO_TEXT_DIRECTION
 * @see SWT#FLIP_TEXT_DIRECTION
 *
 * @since 3.102
 */
public void setTextDirection(int textDirection) {
	checkWidget ();
}

/**
 * Sets the receiver's tool tip text to the argument, which
 * may be null indicating that the default tool tip for the
 * control will be shown. For a control that has a default
 * tool tip, such as the Tree control on Windows, setting
 * the tool tip text to an empty string replaces the default,
 * causing no tool tip text to be shown.
 * <p>
 * The mnemonic indicator (character '&amp;') is not displayed in a tool tip.
 * To display a single '&amp;' in the tool tip, the character '&amp;' can be
 * escaped by doubling it in the string.
 * </p>
 * <p>
 * NOTE: This operation is a hint and behavior is platform specific, on Windows
 * for CJK-style mnemonics of the form " (&C)" at the end of the tooltip text
 * are not shown in tooltip.
 * </p>
 *
 * @param string the new tool tip text (or null)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setToolTipText (String string) {
	checkWidget();
	setToolTipText (_getShell (), string);
	toolTipText = string;
}

void setToolTipText (Shell shell, String newString) {
	/*
	* Feature in GTK.  In order to prevent children widgets
	* from inheriting their parent's tooltip, the tooltip is
	* a set on a shell only. In order to force the shell tooltip
	* to update when a new tip string is set, the existing string
	* in the tooltip is set to null, followed by running a query.
	* The real tip text can then be set.
	*
	* Note that this will only run if the control for which the
	* tooltip is being set is the current control (i.e. the control
	* under the pointer).
	*/
	if (display.currentControl == this) {
		shell.setToolTipText (shell.handle, eventHandle (), newString);
	}
}

/**
 * Sets whether this control should send touch events (by default controls do not).
 * Setting this to <code>false</code> causes the receiver to send gesture events
 * instead.  No exception is thrown if a touch-based input device is not
 * detected (this can be determined with <code>Display#getTouchEnabled()</code>).
 *
 * @param enabled the new touch-enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *
 * @see Display#getTouchEnabled
 *
 * @since 3.7
 */
public void setTouchEnabled(boolean enabled) {
	checkWidget();
}

/**
 * Marks the receiver as visible if the argument is <code>true</code>,
 * and marks it invisible otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, marking
 * it visible may not actually cause it to be displayed.
 * </p>
 *
 * @param visible the new visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setVisible (boolean visible) {
	checkWidget();
	if (((state & HIDDEN) == 0) == visible) return;
	int /*long*/ topHandle = topHandle();
	if (visible) {
		/*
		* It is possible (but unlikely), that application
		* code could have disposed the widget in the show
		* event.  If this happens, just return.
		*/
		sendEvent (SWT.Show);
		if (isDisposed ()) return;
		state &= ~HIDDEN;
		if ((state & (ZERO_WIDTH | ZERO_HEIGHT)) == 0) {
			if (enableWindow != 0) GDK.gdk_window_show_unraised (enableWindow);
			GTK.gtk_widget_show (topHandle);
		}
	} else {
		/*
		* Bug in GTK.  Invoking gtk_widget_hide() on a widget that has
		* focus causes a focus_out_event to be sent. If the client disposes
		* the widget inside the event, GTK GP's.  The fix is to reassign focus
		* before hiding the widget.
		*
		* NOTE: In order to stop the same widget from taking focus,
		* temporarily clear and set the GTK_VISIBLE flag.
		*/
		Control control = null;
		boolean fixFocus = false;
		if (display.focusEvent != SWT.FocusOut) {
			control = display.getFocusControl ();
			fixFocus = isFocusAncestor (control);
		}
		state |= HIDDEN;
		if (fixFocus) {
			if (GTK.GTK3) {
				GTK.gtk_widget_set_can_focus (topHandle, false);
			} else {
				gtk_widget_set_visible (topHandle, false);
			}
			fixFocus (control);
			if (isDisposed ()) return;
			if (GTK.GTK3) {
				GTK.gtk_widget_set_can_focus (topHandle, true);
			} else {
				gtk_widget_set_visible (topHandle, true);
			}
		}
		GTK.gtk_widget_hide (topHandle);
		if (isDisposed ()) return;
		if (enableWindow != 0) GDK.gdk_window_hide (enableWindow);
		sendEvent (SWT.Hide);
	}
}

void setZOrder (Control sibling, boolean above, boolean fixRelations) {
	 setZOrder (sibling, above, fixRelations, true);
}

void setZOrder (Control sibling, boolean above, boolean fixRelations, boolean fixChildren) {
	int index = 0, siblingIndex = 0, oldNextIndex = -1;
	Control[] children = null;
	if (fixRelations) {
		/* determine the receiver's and sibling's indexes in the parent */
		children = parent._getChildren ();
		while (index < children.length) {
			if (children [index] == this) break;
			index++;
		}
		if (sibling != null) {
			while (siblingIndex < children.length) {
				if (children [siblingIndex] == sibling) break;
				siblingIndex++;
			}
		}
		/* remove "Labelled by" relationships that will no longer be valid */
		removeRelation ();
		if (index + 1 < children.length) {
			oldNextIndex = index + 1;
			children [oldNextIndex].removeRelation ();
		}
		if (sibling != null) {
			if (above) {
				sibling.removeRelation ();
			} else {
				if (siblingIndex + 1 < children.length) {
					children [siblingIndex + 1].removeRelation ();
				}
			}
		}
	}

	int /*long*/ topHandle = topHandle ();
	int /*long*/ siblingHandle = sibling != null ? sibling.topHandle () : 0;
	int /*long*/ window = gtk_widget_get_window (topHandle);
	if (window != 0) {
		int /*long*/ siblingWindow = 0;
		if (sibling != null) {
			if (above && sibling.enableWindow != 0) {
				siblingWindow = enableWindow;
			} else {
				siblingWindow = GTK.gtk_widget_get_window (siblingHandle);
			}
		}
		int /*long*/ redrawWindow = fixChildren ? parent.redrawWindow : 0;
		if (!OS.GDK_WINDOWING_X11 () || (siblingWindow == 0 && (!above || redrawWindow == 0))) {
			if (above) {
				GDK.gdk_window_raise (window);
				if (redrawWindow != 0) GDK.gdk_window_raise (redrawWindow);
				if (enableWindow != 0) GDK.gdk_window_raise (enableWindow);
			} else {
				if (enableWindow != 0) GDK.gdk_window_lower (enableWindow);
				GDK.gdk_window_lower (window);
			}
		} else {
			int /*long*/ siblingW = siblingWindow != 0 ? siblingWindow : redrawWindow;
			boolean stack_mode = above;
			if (redrawWindow != 0 && siblingWindow == 0) stack_mode = false;
			restackWindow (window, siblingW, stack_mode);
			if (enableWindow != 0) {
				 restackWindow (enableWindow, window, true);
			}
		}
	}
	if (fixChildren) {
		if (above) {
			parent.moveAbove (topHandle, siblingHandle);
		} else {
			parent.moveBelow (topHandle, siblingHandle);
		}
	}
	/*  Make sure that the parent internal windows are on the bottom of the stack	*/
	if (!above && fixChildren) 	parent.fixZOrder ();

	if (fixRelations) {
		/* determine the receiver's new index in the parent */
		if (sibling != null) {
			if (above) {
				index = siblingIndex - (index < siblingIndex ? 1 : 0);
			} else {
				index = siblingIndex + (siblingIndex < index ? 1 : 0);
			}
		} else {
			if (above) {
				index = 0;
			} else {
				index = children.length - 1;
			}
		}

		/* add new "Labelled by" relations as needed */
		children = parent._getChildren ();
		if (0 < index) {
			children [index - 1].addRelation (this);
		}
		if (index + 1 < children.length) {
			addRelation (children [index + 1]);
		}
		if (oldNextIndex != -1) {
			if (oldNextIndex <= index) oldNextIndex--;
			/* the last two conditions below ensure that duplicate relations are not hooked */
			if (0 < oldNextIndex && oldNextIndex != index && oldNextIndex != index + 1) {
				children [oldNextIndex - 1].addRelation (children [oldNextIndex]);
			}
		}
	}
}

void setWidgetBackground  () {
	if (GTK.GTK3) {
		GdkRGBA rgba = (state & BACKGROUND) != 0 ? getBackgroundGdkRGBA () : null;
		if (fixedHandle != 0) setBackgroundGdkRGBA (fixedHandle, rgba);
		setBackgroundGdkRGBA (handle, rgba);
	} else {
		GdkColor color = (state & BACKGROUND) != 0 ? getBackgroundGdkColor () : null;
		if (fixedHandle != 0) setBackgroundGdkColor (fixedHandle, color);
		setBackgroundGdkColor (handle, color);
	}
}

boolean showMenu (int x, int y) {
	return showMenu (x, y, SWT.MENU_MOUSE);
}

boolean showMenu (int x, int y, int detail) {
	Event event = new Event ();
	Rectangle eventRect = new Rectangle (x, y, 0, 0);
	event.setBounds (DPIUtil.autoScaleDown (eventRect));
	event.detail = detail;
	sendEvent (SWT.MenuDetect, event);
	//widget could be disposed at this point
	if (isDisposed ()) return false;
	if (event.doit) {
		if (menu != null && !menu.isDisposed ()) {
			boolean hooksKeys = hooks (SWT.KeyDown) || hooks (SWT.KeyUp);
			menu.createIMMenu (hooksKeys ? imHandle() : 0);
			Rectangle rect = DPIUtil.autoScaleUp (event.getBounds ());
			if (rect.x != x || rect.y != y) {
				menu.setLocationInPixels (rect.x, rect.y);
			}
			menu.setVisible (true);
			return true;
		}
	}
	return false;
}

void showWidget () {
	// Comment this line to disable zero-sized widgets
	state |= ZERO_WIDTH | ZERO_HEIGHT;
	int /*long*/ topHandle = topHandle ();
	int /*long*/ parentHandle = parent.parentingHandle ();
	parent.setParentGdkWindow (this);
	GTK.gtk_container_add (parentHandle, topHandle);
	if (handle != 0 && handle != topHandle) GTK.gtk_widget_show (handle);
	if ((state & (ZERO_WIDTH | ZERO_HEIGHT)) == 0) {
		if (fixedHandle != 0) GTK.gtk_widget_show (fixedHandle);
	}
	if (fixedHandle != 0) fixStyle (fixedHandle);
}

void sort (int [] items) {
	/* Shell Sort from K&R, pg 108 */
	int length = items.length;
	for (int gap=length/2; gap>0; gap/=2) {
		for (int i=gap; i<length; i++) {
			for (int j=i-gap; j>=0; j-=gap) {
		   		if (items [j] <= items [j + gap]) {
					int swap = items [j];
					items [j] = items [j + gap];
					items [j + gap] = swap;
		   		}
	    	}
	    }
	}
}

/**
 * Based on the argument, perform one of the expected platform
 * traversal action. The argument should be one of the constants:
 * <code>SWT.TRAVERSE_ESCAPE</code>, <code>SWT.TRAVERSE_RETURN</code>,
 * <code>SWT.TRAVERSE_TAB_NEXT</code>, <code>SWT.TRAVERSE_TAB_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_ARROW_NEXT</code>, <code>SWT.TRAVERSE_ARROW_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_PAGE_NEXT</code> and <code>SWT.TRAVERSE_PAGE_PREVIOUS</code>.
 *
 * @param traversal the type of traversal
 * @return true if the traversal succeeded
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean traverse (int traversal) {
	checkWidget ();
	Event event = new Event ();
	event.doit = true;
	event.detail = traversal;
	return traverse (event);
}

/**
 * Performs a platform traversal action corresponding to a <code>KeyDown</code> event.
 *
 * <p>Valid traversal values are
 * <code>SWT.TRAVERSE_NONE</code>, <code>SWT.TRAVERSE_MNEMONIC</code>,
 * <code>SWT.TRAVERSE_ESCAPE</code>, <code>SWT.TRAVERSE_RETURN</code>,
 * <code>SWT.TRAVERSE_TAB_NEXT</code>, <code>SWT.TRAVERSE_TAB_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_ARROW_NEXT</code>, <code>SWT.TRAVERSE_ARROW_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_PAGE_NEXT</code> and <code>SWT.TRAVERSE_PAGE_PREVIOUS</code>.
 * If <code>traversal</code> is <code>SWT.TRAVERSE_NONE</code> then the Traverse
 * event is created with standard values based on the KeyDown event.  If
 * <code>traversal</code> is one of the other traversal constants then the Traverse
 * event is created with this detail, and its <code>doit</code> is taken from the
 * KeyDown event.
 * </p>
 *
 * @param traversal the type of traversal, or <code>SWT.TRAVERSE_NONE</code> to compute
 * this from <code>event</code>
 * @param event the KeyDown event
 *
 * @return <code>true</code> if the traversal succeeded
 *
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT if the event is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.6
 */
public boolean traverse (int traversal, Event event) {
	checkWidget ();
	if (event == null) error (SWT.ERROR_NULL_ARGUMENT);
	return traverse (traversal, event.character, event.keyCode, event.keyLocation, event.stateMask, event.doit);
}

/**
 * Performs a platform traversal action corresponding to a <code>KeyDown</code> event.
 *
 * <p>Valid traversal values are
 * <code>SWT.TRAVERSE_NONE</code>, <code>SWT.TRAVERSE_MNEMONIC</code>,
 * <code>SWT.TRAVERSE_ESCAPE</code>, <code>SWT.TRAVERSE_RETURN</code>,
 * <code>SWT.TRAVERSE_TAB_NEXT</code>, <code>SWT.TRAVERSE_TAB_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_ARROW_NEXT</code>, <code>SWT.TRAVERSE_ARROW_PREVIOUS</code>,
 * <code>SWT.TRAVERSE_PAGE_NEXT</code> and <code>SWT.TRAVERSE_PAGE_PREVIOUS</code>.
 * If <code>traversal</code> is <code>SWT.TRAVERSE_NONE</code> then the Traverse
 * event is created with standard values based on the KeyDown event.  If
 * <code>traversal</code> is one of the other traversal constants then the Traverse
 * event is created with this detail, and its <code>doit</code> is taken from the
 * KeyDown event.
 * </p>
 *
 * @param traversal the type of traversal, or <code>SWT.TRAVERSE_NONE</code> to compute
 * this from <code>event</code>
 * @param event the KeyDown event
 *
 * @return <code>true</code> if the traversal succeeded
 *
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT if the event is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.6
 */
public boolean traverse (int traversal, KeyEvent event) {
	checkWidget ();
	if (event == null) error (SWT.ERROR_NULL_ARGUMENT);
	return traverse (traversal, event.character, event.keyCode, event.keyLocation, event.stateMask, event.doit);
}

boolean traverse (int traversal, char character, int keyCode, int keyLocation, int stateMask, boolean doit) {
	if (traversal == SWT.TRAVERSE_NONE) {
		switch (keyCode) {
			case SWT.ESC: {
				traversal = SWT.TRAVERSE_ESCAPE;
				doit = true;
				break;
			}
			case SWT.CR: {
				traversal = SWT.TRAVERSE_RETURN;
				doit = true;
				break;
			}
			case SWT.ARROW_DOWN:
			case SWT.ARROW_RIGHT: {
				traversal = SWT.TRAVERSE_ARROW_NEXT;
				doit = false;
				break;
			}
			case SWT.ARROW_UP:
			case SWT.ARROW_LEFT: {
				traversal = SWT.TRAVERSE_ARROW_PREVIOUS;
				doit = false;
				break;
			}
			case SWT.TAB: {
				traversal = (stateMask & SWT.SHIFT) != 0 ? SWT.TRAVERSE_TAB_PREVIOUS : SWT.TRAVERSE_TAB_NEXT;
				doit = true;
				break;
			}
			case SWT.PAGE_DOWN: {
				if ((stateMask & SWT.CTRL) != 0) {
					traversal = SWT.TRAVERSE_PAGE_NEXT;
					doit = true;
				}
				break;
			}
			case SWT.PAGE_UP: {
				if ((stateMask & SWT.CTRL) != 0) {
					traversal = SWT.TRAVERSE_PAGE_PREVIOUS;
					doit = true;
				}
				break;
			}
			default: {
				if (character != 0 && (stateMask & (SWT.ALT | SWT.CTRL)) == SWT.ALT) {
					traversal = SWT.TRAVERSE_MNEMONIC;
					doit = true;
				}
				break;
			}
		}
	}

	Event event = new Event ();
	event.character = character;
	event.detail = traversal;
	event.doit = doit;
	event.keyCode = keyCode;
	event.keyLocation = keyLocation;
	event.stateMask = stateMask;
	Shell shell = getShell ();

	boolean all = false;
	switch (traversal) {
		case SWT.TRAVERSE_ESCAPE:
		case SWT.TRAVERSE_RETURN:
		case SWT.TRAVERSE_PAGE_NEXT:
		case SWT.TRAVERSE_PAGE_PREVIOUS: {
			all = true;
			// FALL THROUGH
		}
		case SWT.TRAVERSE_ARROW_NEXT:
		case SWT.TRAVERSE_ARROW_PREVIOUS:
		case SWT.TRAVERSE_TAB_NEXT:
		case SWT.TRAVERSE_TAB_PREVIOUS: {
			/* traversal is a valid traversal action */
			break;
		}
		case SWT.TRAVERSE_MNEMONIC: {
			return translateMnemonic (event, null) || shell.translateMnemonic (event, this);
		}
		default: {
			/* traversal is not a valid traversal action */
			return false;
		}
	}

	Control control = this;
	do {
		if (control.traverse (event)) return true;
		if (!event.doit && control.hooks (SWT.Traverse)) return false;
		if (control == shell) return false;
		control = control.parent;
	} while (all && control != null);
	return false;
}

boolean translateMnemonic (Event event, Control control) {
	if (control == this) return false;
	if (!isVisible () || !isEnabled ()) return false;
	event.doit = this == display.mnemonicControl || mnemonicMatch (event.character);
	return traverse (event);
}

boolean translateMnemonic (int keyval, GdkEventKey gdkEvent) {
	long key = GDK.gdk_keyval_to_unicode (keyval);
	if (key < 0x20) return false;
	if (gdkEvent.state == 0) {
		int code = traversalCode (keyval, gdkEvent);
		if ((code & SWT.TRAVERSE_MNEMONIC) == 0) return false;
	} else {
		Shell shell = _getShell ();
		int mask = GDK.GDK_CONTROL_MASK | GDK.GDK_SHIFT_MASK | GDK.GDK_MOD1_MASK;
		if ((gdkEvent.state & mask) != GTK.gtk_window_get_mnemonic_modifier (shell.shellHandle)) return false;
	}
	Decorations shell = menuShell ();
	if (shell.isVisible () && shell.isEnabled ()) {
		Event event = new Event ();
		event.detail = SWT.TRAVERSE_MNEMONIC;
		if (setKeyState (event, gdkEvent)) {
			return translateMnemonic (event, null) || shell.translateMnemonic (event, this);
		}
	}
	return false;
}

boolean translateTraversal (GdkEventKey keyEvent) {
	int detail = SWT.TRAVERSE_NONE;
	int key = keyEvent.keyval;
	int code = traversalCode (key, keyEvent);
	boolean all = false;
	switch (key) {
		case GDK.GDK_Escape: {
			all = true;
			detail = SWT.TRAVERSE_ESCAPE;
			break;
		}
		case GDK.GDK_KP_Enter:
		case GDK.GDK_Return: {
			all = true;
			detail = SWT.TRAVERSE_RETURN;
			break;
		}
		case GDK.GDK_ISO_Left_Tab:
		case GDK.GDK_Tab: {
			boolean next = (keyEvent.state & GDK.GDK_SHIFT_MASK) == 0;
			detail = next ? SWT.TRAVERSE_TAB_NEXT : SWT.TRAVERSE_TAB_PREVIOUS;
			break;
		}
		case GDK.GDK_Up:
		case GDK.GDK_Left:
		case GDK.GDK_Down:
		case GDK.GDK_Right: {
			boolean next = key == GDK.GDK_Down || key == GDK.GDK_Right;
			if (parent != null && (parent.style & SWT.MIRRORED) != 0) {
				if (key == GDK.GDK_Left || key == GDK.GDK_Right) next = !next;
			}
			detail = next ? SWT.TRAVERSE_ARROW_NEXT : SWT.TRAVERSE_ARROW_PREVIOUS;
			break;
		}
		case GDK.GDK_Page_Up:
		case GDK.GDK_Page_Down: {
			all = true;
			if ((keyEvent.state & GDK.GDK_CONTROL_MASK) == 0) return false;
			detail = key == GDK.GDK_Page_Down ? SWT.TRAVERSE_PAGE_NEXT : SWT.TRAVERSE_PAGE_PREVIOUS;
			break;
		}
		default:
			return false;
	}
	Event event = new Event ();
	event.doit = (code & detail) != 0;
	event.detail = detail;
	event.time = keyEvent.time;
	if (!setKeyState (event, keyEvent)) return false;
	Shell shell = getShell ();
	Control control = this;
	do {
		if (control.traverse (event)) return true;
		if (!event.doit && control.hooks (SWT.Traverse)) return false;
		if (control == shell) return false;
		control = control.parent;
	} while (all && control != null);
	return false;
}

int traversalCode (int key, GdkEventKey event) {
	int code = SWT.TRAVERSE_RETURN | SWT.TRAVERSE_TAB_NEXT |  SWT.TRAVERSE_TAB_PREVIOUS | SWT.TRAVERSE_PAGE_NEXT | SWT.TRAVERSE_PAGE_PREVIOUS;
	Shell shell = getShell ();
	if (shell.parent != null) code |= SWT.TRAVERSE_ESCAPE;
	return code;
}

boolean traverse (Event event) {
	/*
	* It is possible (but unlikely), that application
	* code could have disposed the widget in the traverse
	* event.  If this happens, return true to stop further
	* event processing.
	*/
	sendEvent (SWT.Traverse, event);
	if (isDisposed ()) return true;
	if (!event.doit) return false;
	switch (event.detail) {
		case SWT.TRAVERSE_NONE:			return true;
		case SWT.TRAVERSE_ESCAPE:			return traverseEscape ();
		case SWT.TRAVERSE_RETURN:			return traverseReturn ();
		case SWT.TRAVERSE_TAB_NEXT:		return traverseGroup (true);
		case SWT.TRAVERSE_TAB_PREVIOUS:	return traverseGroup (false);
		case SWT.TRAVERSE_ARROW_NEXT:		return traverseItem (true);
		case SWT.TRAVERSE_ARROW_PREVIOUS:	return traverseItem (false);
		case SWT.TRAVERSE_MNEMONIC:		return traverseMnemonic (event.character);
		case SWT.TRAVERSE_PAGE_NEXT:		return traversePage (true);
		case SWT.TRAVERSE_PAGE_PREVIOUS:	return traversePage (false);
	}
	return false;
}

boolean traverseEscape () {
	return false;
}

boolean traverseGroup (boolean next) {
	Control root = computeTabRoot ();
	Widget group = computeTabGroup ();
	Widget [] list = root.computeTabList ();
	int length = list.length;
	int index = 0;
	while (index < length) {
		if (list [index] == group) break;
		index++;
	}
	/*
	* It is possible (but unlikely), that application
	* code could have disposed the widget in focus in
	* or out events.  Ensure that a disposed widget is
	* not accessed.
	*/
	if (index == length) return false;
	int start = index, offset = (next) ? 1 : -1;
	while ((index = ((index + offset + length) % length)) != start) {
		Widget widget = list [index];
		if (!widget.isDisposed () && widget.setTabGroupFocus (next)) {
			return true;
		}
	}
	if (group.isDisposed ()) return false;
	return group.setTabGroupFocus (next);
}

boolean traverseItem (boolean next) {
	Control [] children = parent._getChildren ();
	int length = children.length;
	int index = 0;
	while (index < length) {
		if (children [index] == this) break;
		index++;
	}
	/*
	* It is possible (but unlikely), that application
	* code could have disposed the widget in focus in
	* or out events.  Ensure that a disposed widget is
	* not accessed.
	*/
	if (index == length) return false;
	int start = index, offset = (next) ? 1 : -1;
	while ((index = (index + offset + length) % length) != start) {
		Control child = children [index];
		if (!child.isDisposed () && child.isTabItem ()) {
			if (child.setTabItemFocus (next)) return true;
		}
	}
	return false;
}

boolean traverseReturn () {
	return false;
}

boolean traversePage (boolean next) {
	return false;
}

boolean traverseMnemonic (char key) {
	return mnemonicHit (key);
}

/**
 * Forces all outstanding paint requests for the widget
 * to be processed before this method returns. If there
 * are no outstanding paint request, this method does
 * nothing.
 * <p>
 * Note: This method does not cause a redraw.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #redraw()
 * @see #redraw(int, int, int, int, boolean)
 * @see PaintListener
 * @see SWT#Paint
 */
public void update () {
	checkWidget ();
	update (false, true);
}

void update (boolean all, boolean flush) {
//	checkWidget();
	if (!GTK.gtk_widget_get_visible (topHandle ())) return;
	if (!GTK.gtk_widget_get_realized (handle)) return;
	int /*long*/ window = paintWindow ();
	if (flush) display.flushExposes (window, all);
	/*
	 * Do not send expose events on GTK 3.16.0+
	 * It's worth checking whether can be removed on all GTK 3 versions.
	 */
	if (GTK.GTK_VERSION < OS.VERSION(3, 16, 0)) {
		GDK.gdk_window_process_updates (window, all);
	}
	GDK.gdk_flush ();
}

void updateBackgroundMode () {
	int oldState = state & PARENT_BACKGROUND;
	checkBackground ();
	if (oldState != (state & PARENT_BACKGROUND)) {
		setBackground ();
	}
}

void updateLayout (boolean all) {
	/* Do nothing */
}

@Override
int /*long*/ windowProc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ user_data) {
	switch ((int)/*64*/user_data) {
		case EXPOSE_EVENT_INVERSE: {
			if ((state & OBSCURED) != 0) break;
			Control control = findBackgroundControl ();
			boolean draw = control != null && control.backgroundImage != null;
			if (GTK.GTK3 && !draw && (state & CANVAS) != 0) {
				if (GTK.GTK_VERSION < OS.VERSION(3, 14, 0)) {
					GdkRGBA rgba = new GdkRGBA();
					int /*long*/ context = GTK.gtk_widget_get_style_context (handle);
					GTK.gtk_style_context_get_background_color (context, GTK.GTK_STATE_FLAG_NORMAL, rgba);
					draw = rgba.alpha == 0;
				} else {
					draw = (state & BACKGROUND) == 0;
				}
			}
			if (draw) {
				if (GTK.GTK3) {
					int /*long*/ cairo = arg0;
					GdkRectangle rect = new GdkRectangle ();
					GDK.gdk_cairo_get_clip_rectangle (cairo, rect);
					if (control == null) control = this;
					int /*long*/ window = GTK.gtk_widget_get_window (handle);
					if (window != 0) {
						drawBackground (control, window, 0, 0, rect.x, rect.y, rect.width, rect.height);
					} else {
						drawBackground (control, 0, cairo, 0, rect.x, rect.y, rect.width, rect.height);
					}
				} else {
					GdkEventExpose gdkEvent = new GdkEventExpose ();
					OS.memmove (gdkEvent, arg0, GdkEventExpose.sizeof);
					int /*long*/ paintWindow = paintWindow();
					int /*long*/ window = gdkEvent.window;
					if (window != paintWindow) break;
					drawBackground(control, window, gdkEvent.region, gdkEvent.area_x, gdkEvent.area_y, gdkEvent.area_width, gdkEvent.area_height);
				}
			}
			break;
		}
		case DRAW: {
			if (GTK.GTK_VERSION >= OS.VERSION(3, 10, 0) && paintHandle() == handle && drawRegion) {
				return gtk_draw(handle, arg0);
			}
		}
	}
	return super.windowProc (handle, arg0, user_data);
}

/**
 * Gets the position of the top left corner of the control in root window (display) coordinates.
 *
 * @return the origin
 */
Point getWindowOrigin () {
	int [] x = new int [1];
	int [] y = new int [1];

	int /*long*/ window = eventWindow ();
	GDK.gdk_window_get_origin (window, x, y);

	return new Point (x [0], y [0]);
}
}
