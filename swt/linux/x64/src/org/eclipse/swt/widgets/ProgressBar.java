/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * Instances of the receiver represent an unselectable
 * user interface object that is used to display progress,
 * typically in the form of a bar.
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SMOOTH, HORIZONTAL, VERTICAL, INDETERMINATE</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * <p>
 * Note: Only one of the styles HORIZONTAL and VERTICAL may be specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 *
 * @see <a href="http://www.eclipse.org/swt/snippets/#progressbar">ProgressBar snippets</a>
 * @see <a href="http://www.eclipse.org/swt/examples.php">SWT Example: ControlExample</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ProgressBar extends Control {
	int timerId, minimum = 0, maximum = 100, selection = 0;
	static final int DELAY = 100;

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
 * @see SWT#SMOOTH
 * @see SWT#HORIZONTAL
 * @see SWT#VERTICAL
 * @see SWT#INDETERMINATE
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public ProgressBar (Composite parent, int style) {
	super (parent, checkStyle(style));
}

static int checkStyle (int style) {
	style |= SWT.NO_FOCUS;
	return checkBits (style, SWT.HORIZONTAL, SWT.VERTICAL, 0, 0, 0, 0);
}

@Override
void createHandle (int index) {
	state |= HANDLE;
	fixedHandle = OS.g_object_new (display.gtk_fixed_get_type (), 0);
	if (fixedHandle == 0) error (SWT.ERROR_NO_HANDLES);
	gtk_widget_set_has_surface_or_window (fixedHandle, true);
	handle = GTK.gtk_progress_bar_new ();
	if (handle == 0) error (SWT.ERROR_NO_HANDLES);
	GTK.gtk_container_add (fixedHandle, handle);
	int orientation = (style & SWT.VERTICAL) != 0 ? GTK.GTK_PROGRESS_BOTTOM_TO_TOP : GTK.GTK_PROGRESS_LEFT_TO_RIGHT;
	gtk_orientable_set_orientation (handle, orientation);
	if ((style & SWT.INDETERMINATE) != 0) {
		timerId = OS.g_timeout_add (DELAY, display.windowTimerProc, handle);
	}
}

@Override
long eventHandle () {
	return fixedHandle;
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
	return maximum;
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
	return minimum;
}

/**
 * Returns the single 'selection' that is the receiver's position.
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
	return selection;
}

/**
 * Returns the state of the receiver. The value will be one of:
 * <ul>
 * 	<li>{@link SWT#NORMAL}</li>
 * 	<li>{@link SWT#ERROR}</li>
 * 	<li>{@link SWT#PAUSED}</li>
 * </ul>
 *
 * @return the state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.4
 */
public int getState () {
	checkWidget ();
	return SWT.NORMAL;
}

@Override
long gtk_realize (long widget) {
	long result = super.gtk_realize (widget);
	if (result != 0) return result;
	/*
	* Bug in GTK.  When a progress bar has been unrealized after being
	* realized at least once, gtk_progress_bar_set_fraction() GP's.  The
	* fix is to update the progress bar state only when realized and restore
	* the state when the progress bar becomes realized.
	*/
	updateBar (selection, minimum, maximum);
	return 0;
}

/*
 * Feature in GTK3.20+: ProgressBar has a very large minimum size,
 * too large to use for SWT. It's necessary to shrink it even though it emits
 * 2 warnings. For this reason, do not perform GtkCSSNode calculations.
 */
@Override
Point resizeCalculationsGTK3 (long widget, int width, int height) {
	if (GTK.GTK_VERSION >= OS.VERSION(3, 20, 0)) {
		// avoid warnings in GTK caused by too narrow progress bars
		width = Math.max(2, width);
	}
	return new Point (width, height);
}

@Override
void releaseWidget () {
	super.releaseWidget ();
	if (timerId != 0) OS.g_source_remove (timerId);
	timerId = 0;
}

@Override
void setParentBackground () {
	/*
	* Bug in GTK.  For some reason, some theme managers will crash
	* when the progress bar is inheriting the background from a parent.
	* The fix is to stop inheriting the background. This is acceptable
	* since progress bars do not use the inherited background.
	*/
}

/**
 * Sets the maximum value that the receiver will allow.  This new
 * value will be ignored if it is not greater than the receiver's current
 * minimum value.  If the new maximum is applied then the receiver's
 * selection value will be adjusted if necessary to fall within its new range.
 *
 * @param value the new maximum, which must be greater than the current minimum
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setMaximum (int value) {
	checkWidget ();
	if (value <= minimum) return;
	maximum = value;
	selection = Math.min (selection, maximum);
	updateBar (selection, minimum, maximum);
}

/**
 * Sets the minimum value that the receiver will allow.  This new
 * value will be ignored if it is negative or is not less than the receiver's
 * current maximum value.  If the new minimum is applied then the receiver's
 * selection value will be adjusted if necessary to fall within its new range.
 *
 * @param value the new minimum, which must be nonnegative and less than the current maximum
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setMinimum (int value) {
	checkWidget ();
	if (value < 0 || value >= maximum) return;
	minimum = value;
	selection = Math.max (selection, minimum);
	updateBar (selection, minimum, maximum);
}

/**
 * Sets the single 'selection' that is the receiver's
 * position to the argument which must be greater than or equal
 * to zero.
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
	selection = Math.max (minimum, Math.min (maximum, value));
	updateBar (selection, minimum, maximum);
}

/**
 * Sets the state of the receiver. The state must be one of these values:
 * <ul>
 * 	<li>{@link SWT#NORMAL}</li>
 * 	<li>{@link SWT#ERROR}</li>
 * 	<li>{@link SWT#PAUSED}</li>
 * </ul>
 * <p>
 * Note: This operation is a hint and is not supported on
 * platforms that do not have this concept.
 * </p>
 *
 * @param state the new state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.4
 */
public void setState (int state) {
	checkWidget ();
	//NOT IMPLEMENTED
}

@Override
long timerProc (long widget) {
	if (isVisible ()) GTK.gtk_progress_bar_pulse (handle);
	return 1;
}

void updateBar (int selection, int minimum, int maximum) {
	/*
	* Bug in GTK.  When a progress bar has been unrealized after being
	* realized at least once, gtk_progress_bar_set_fraction() GP's.  The
	* fix is to update the progress bar state only when realized and restore
	* the state when the progress bar becomes realized.
	*/
	if (!GTK.gtk_widget_get_realized (handle)) return;

	double fraction = minimum == maximum ? 1 : (double)(selection - minimum) / (maximum - minimum);
	GTK.gtk_progress_bar_set_fraction (handle, fraction);
}

void gtk_orientable_set_orientation (long pbar, int orientation) {
	switch (orientation) {
		case GTK.GTK_PROGRESS_BOTTOM_TO_TOP:
			GTK.gtk_orientable_set_orientation(pbar, GTK.GTK_ORIENTATION_VERTICAL);
			GTK.gtk_progress_bar_set_inverted(pbar, true);
			break;
		case GTK.GTK_PROGRESS_LEFT_TO_RIGHT:
			GTK.gtk_orientable_set_orientation(pbar, GTK.GTK_ORIENTATION_HORIZONTAL);
			GTK.gtk_progress_bar_set_inverted(pbar, false);
			break;
	}
}
}
