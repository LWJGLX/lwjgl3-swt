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
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * Instances of this class support the layout of selectable
 * expand bar items.
 * <p>
 * The item children that may be added to instances of this class
 * must be of type <code>ExpandItem</code>.
 * </p><p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>V_SCROLL</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Expand, Collapse</dd>
 * </dl>
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 *
 * @see ExpandItem
 * @see ExpandEvent
 * @see ExpandListener
 * @see ExpandAdapter
 * @see <a href="http://www.eclipse.org/swt/snippets/#expandbar">ExpandBar snippets</a>
 * @see <a href="http://www.eclipse.org/swt/examples.php">SWT Example: ControlExample</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 *
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ExpandBar extends Composite {
	ExpandItem [] items;
	ExpandItem lastFocus;
	int itemCount;
	int spacing;
	int yCurrentScroll;

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
 * @see SWT#V_SCROLL
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public ExpandBar (Composite parent, int style) {
	super (parent, style);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when an item in the receiver is expanded or collapsed
 * by sending it one of the messages defined in the <code>ExpandListener</code>
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
 * @see ExpandListener
 * @see #removeExpandListener
 */
public void addExpandListener (ExpandListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Expand, typedListener);
	addListener (SWT.Collapse, typedListener);
}

@Override
protected void checkSubclass () {
	if (!isValidSubclass ()) error (SWT.ERROR_INVALID_SUBCLASS);
}

@Override
Point computeSizeInPixels (int wHint, int hHint, boolean changed) {
	if (wHint != SWT.DEFAULT && wHint < 0) wHint = 0;
	if (hHint != SWT.DEFAULT && hHint < 0) hHint = 0;
	Point size = computeNativeSize (handle, wHint, hHint, changed);
	if (size.x == 0 && wHint == SWT.DEFAULT) size.x = DEFAULT_WIDTH;
	if (size.y == 0 && hHint == SWT.DEFAULT) size.y = DEFAULT_HEIGHT;
	int border = GTK.gtk_container_get_border_width (handle);
	size.x += 2 * border;
	size.y += 2 * border;
	return size;
}

@Override
void createHandle (int index) {
	state |= HANDLE;
	fixedHandle = OS.g_object_new (display.gtk_fixed_get_type (), 0);
	if (fixedHandle == 0) error (SWT.ERROR_NO_HANDLES);
	GTK.gtk_widget_set_has_window (fixedHandle, true);
	handle = gtk_box_new (GTK.GTK_ORIENTATION_VERTICAL, false, 0);
	if (handle == 0) error (SWT.ERROR_NO_HANDLES);
	if ((style & SWT.V_SCROLL) != 0) {
		scrolledHandle = GTK.gtk_scrolled_window_new (0, 0);
		if (scrolledHandle == 0) error (SWT.ERROR_NO_HANDLES);
		GTK.gtk_scrolled_window_set_policy (scrolledHandle, GTK.GTK_POLICY_NEVER, GTK.GTK_POLICY_AUTOMATIC);
		GTK.gtk_container_add (fixedHandle, scrolledHandle);
		if (GTK.GTK_VERSION < OS.VERSION(3, 8, 0)) {
			GTK.gtk_scrolled_window_add_with_viewport (scrolledHandle, handle);
		} else {
			GTK.gtk_container_add(scrolledHandle, handle);
		}
		int /*long*/ viewport = GTK.gtk_bin_get_child (scrolledHandle);
		GTK.gtk_viewport_set_shadow_type (viewport, GTK.GTK_SHADOW_NONE);
	} else {
		GTK.gtk_container_add (fixedHandle, handle);
	}
	GTK.gtk_container_set_border_width (handle, 0);
	// In GTK 3 font description is inherited from parent widget which is not how SWT has always worked,
	// reset to default font to get the usual behavior
	if (GTK.GTK3) {
		setFontDescription(defaultFont().handle);
	}
}

void createItem (ExpandItem item, int style, int index) {
	if (!(0 <= index && index <= itemCount)) error (SWT.ERROR_INVALID_RANGE);
	if (itemCount == items.length) {
		ExpandItem [] newItems = new ExpandItem [itemCount + 4];
		System.arraycopy (items, 0, newItems, 0, items.length);
		items = newItems;
	}
	System.arraycopy (items, index, items, index + 1, itemCount - index);
	items [index] = item;
	itemCount++;
	item.width = Math.max (0, getClientAreaInPixels ().width - spacing * 2);
	layoutItems (index, true);
}

@Override
void createWidget (int index) {
	super.createWidget (index);
	items = new ExpandItem [4];
}

void destroyItem (ExpandItem item) {
	int index = 0;
	while (index < itemCount) {
		if (items [index] == item) break;
		index++;
	}
	if (index == itemCount) return;
	System.arraycopy (items, index + 1, items, index, --itemCount - index);
	items [itemCount] = null;
	item.redraw ();
	layoutItems (index, true);
}

@Override
int /*long*/ eventHandle () {
	return fixedHandle;
}

@Override
boolean forceFocus (int /*long*/ focusHandle) {
	if (lastFocus != null && lastFocus.setFocus ()) return true;
	for (int i = 0; i < itemCount; i++) {
		ExpandItem item = items [i];
		if (item.setFocus ()) return true;
	}
	return super.forceFocus (focusHandle);
}

@Override
boolean hasFocus () {
	for (int i=0; i<itemCount; i++) {
		ExpandItem item = items [i];
		if (item.hasFocus ()) return true;
	}
	return super.hasFocus();
}

@Override
void hookEvents () {
	super.hookEvents ();
	if (scrolledHandle != 0) {
		OS.g_signal_connect_closure (scrolledHandle, OS.size_allocate, display.getClosure (SIZE_ALLOCATE), true);
	}
}

int getBandHeight () {
	if (font == null) return ExpandItem.CHEVRON_SIZE;
	GC gc = new GC (this);
	FontMetrics metrics = gc.getFontMetrics ();
	gc.dispose ();
	return Math.max (ExpandItem.CHEVRON_SIZE, metrics.getHeight ());
}

/**
 * Returns the item at the given, zero-relative index in the
 * receiver. Throws an exception if the index is out of range.
 *
 * @param index the index of the item to return
 * @return the item at the given index
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public ExpandItem getItem (int index) {
	checkWidget();
	if (!(0 <= index && index < itemCount)) error (SWT.ERROR_INVALID_RANGE);
	return items [index];
}

/**
 * Returns the number of items contained in the receiver.
 *
 * @return the number of items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getItemCount () {
	checkWidget();
	return itemCount;
}

/**
 * Returns an array of <code>ExpandItem</code>s which are the items
 * in the receiver.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver.
 * </p>
 *
 * @return the items in the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public ExpandItem [] getItems () {
	checkWidget ();
	ExpandItem [] result = new ExpandItem [itemCount];
	System.arraycopy (items, 0, result, 0, itemCount);
	return result;
}

/**
 * Returns the receiver's spacing.
 *
 * @return the spacing
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getSpacing () {
	checkWidget ();
	return DPIUtil.autoScaleDown(spacing);
}

int getSpacingInPixels () {
	checkWidget ();
	return spacing;
}

@Override
int /*long*/ gtk_key_press_event (int /*long*/ widget, int /*long*/ event) {
	if (!hasFocus ()) return 0;
	int /*long*/ result = super.gtk_key_press_event (widget, event);
	if (result != 0) return result;
	int index = 0;
	while (index < itemCount) {
		if (items [index].hasFocus ()) break;
		index++;
	}
	GdkEventKey gdkEvent = new GdkEventKey ();
	OS.memmove (gdkEvent, event, GdkEventKey.sizeof);
	boolean next = false;
	switch (gdkEvent.keyval) {
		case GDK.GDK_Up:
		case GDK.GDK_Left: next = false; break;
		case GDK.GDK_Down:
		case GDK.GDK_Right: next = true; break;
		default: return result;
	}
	int start = index, offset = next ? 1 : -1;
	while ((index = (index + offset + itemCount) % itemCount) != start) {
		ExpandItem item = items [index];
		if (item.setFocus ()) return result;
	}
	return result;
}

/**
 * Searches the receiver's list starting at the first item
 * (index 0) until an item is found that is equal to the
 * argument, and returns the index of that item. If no item
 * is found, returns -1.
 *
 * @param item the search item
 * @return the index of the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int indexOf (ExpandItem item) {
	checkWidget();
	if (item == null) error (SWT.ERROR_NULL_ARGUMENT);
	for (int i = 0; i < itemCount; i++) {
		if (items [i] == item) return i;
	}
	return -1;
}

void layoutItems (int index, boolean setScrollbar) {
	for (int i = 0; i < itemCount; i++) {
		ExpandItem item = items [i];
		if (item != null) item.resizeControl (yCurrentScroll);
	}
}

@Override
int /*long*/ gtk_size_allocate (int /*long*/ widget, int /*long*/ allocation) {
	int /*long*/ result = super.gtk_size_allocate (widget, allocation);
	layoutItems (0, false);
	return result;
}

@Override
int /*long*/ parentingHandle () {
	return fixedHandle;
}

@Override
void releaseChildren (boolean destroy) {
	for (int i = 0; i < itemCount; i++) {
		ExpandItem item = items [i];
		if (item != null && !item.isDisposed ()) {
			item.release (false);
		}
	}
	super.releaseChildren (destroy);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when items in the receiver are expanded or collapsed.
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
 * @see ExpandListener
 * @see #addExpandListener
 */
public void removeExpandListener (ExpandListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Expand, listener);
	eventTable.unhook (SWT.Collapse, listener);
}

@Override
void reskinChildren (int flags) {
	if (items != null) {
		for (int i=0; i<items.length; i++) {
			ExpandItem item = items [i];
			if (item != null ) item.reskin (flags);
		}
	}
	super.reskinChildren (flags);
}

@Override
void setWidgetBackground  () {
	if (GTK.GTK_VERSION >= OS.VERSION(3, 14, 0)) {
		GdkRGBA rgba = (state & BACKGROUND) != 0 ? getBackgroundGdkRGBA () : null;
		super.setBackgroundGdkRGBA (handle, rgba);
	} else {
		super.setWidgetBackground();
	}
}

@Override
void setFontDescription (int /*long*/ font) {
	super.setFontDescription (font);
	for (int i = 0; i < itemCount; i++) {
		items[i].setFontDescription (font);
	}
	layoutItems (0, true);
}

@Override
void setForegroundGdkColor (GdkColor color) {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	super.setForegroundGdkColor (color);
	for (int i = 0; i < itemCount; i++) {
		items[i].setForegroundColor (color);
	}
}

@Override
void setForegroundGdkRGBA (GdkRGBA rgba) {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	super.setForegroundGdkRGBA(rgba);
	for (int i = 0; i < itemCount; i++) {
		items[i].setForegroundRGBA (rgba);
	}
}

@Override
void setOrientation (boolean create) {
	super.setOrientation (create);
	if (items != null) {
		for (int i=0; i<items.length; i++) {
			if (items[i] != null) items[i].setOrientation (create);
		}
	}
}

void setScrollbar () {
	if (itemCount == 0) return;
	if ((style & SWT.V_SCROLL) == 0) return;
	int height = getClientAreaInPixels ().height;
	ExpandItem item = items [itemCount - 1];
	int maxHeight = item.y + getBandHeight () + spacing;
	if (item.expanded) maxHeight += item.height;
	int /*long*/ adjustmentHandle = GTK.gtk_scrolled_window_get_vadjustment (scrolledHandle);
	GtkAdjustment adjustment = new GtkAdjustment ();
	gtk_adjustment_get (adjustmentHandle, adjustment);
	yCurrentScroll = (int)adjustment.value;

	//claim bottom free space
	if (yCurrentScroll > 0 && height > maxHeight) {
		yCurrentScroll = Math.max (0, yCurrentScroll + maxHeight - height);
		layoutItems (0, false);
	}
	maxHeight += yCurrentScroll;
	adjustment.value = Math.min (yCurrentScroll, maxHeight);
	adjustment.upper = maxHeight;
	adjustment.page_size = height;
	GTK.gtk_adjustment_configure(adjustmentHandle, adjustment.value, adjustment.lower, adjustment.upper,
		adjustment.step_increment, adjustment.page_increment, adjustment.page_size);
	int policy = maxHeight > height ? GTK.GTK_POLICY_ALWAYS : GTK.GTK_POLICY_NEVER;
	GTK.gtk_scrolled_window_set_policy (scrolledHandle, GTK.GTK_POLICY_NEVER, policy);
	GtkAllocation allocation = new GtkAllocation ();
	GTK.gtk_widget_get_allocation (fixedHandle, allocation);
	int width = allocation.width - spacing * 2;
	if (policy == GTK.GTK_POLICY_ALWAYS) {
		int /*long*/ vHandle = 0;
		vHandle = GTK.gtk_scrolled_window_get_vscrollbar (scrolledHandle);
		GtkRequisition requisition = new GtkRequisition ();
		gtk_widget_get_preferred_size (vHandle, requisition);
		width -= requisition.width;
	}
	width = Math.max (0,  width);
	for (int i = 0; i < itemCount; i++) {
		ExpandItem item2 = items[i];
		item2.setBounds (0, 0, width, item2.height, false, true);
	}
}

/**
 * Sets the receiver's spacing. Spacing specifies the number of pixels allocated around
 * each item.
 *
 * @param spacing the spacing around each item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setSpacing (int spacing) {
	checkWidget ();
	setSpacingInPixels(DPIUtil.autoScaleUp(spacing));
}

void setSpacingInPixels (int spacing) {
	checkWidget ();
	if (spacing < 0) return;
	if (spacing == this.spacing) return;
	this.spacing = spacing;
	GTK.gtk_box_set_spacing (handle, spacing);
	GTK.gtk_container_set_border_width (handle, spacing);
}

void showItem (ExpandItem item) {
	Control control = item.control;
	if (control != null && !control.isDisposed ()) {
		control.setVisible (item.expanded);
	}
	item.redraw ();
	int index = indexOf (item);
	layoutItems (index + 1, true);
}

@Override
void updateScrollBarValue (ScrollBar bar) {
	yCurrentScroll = bar.getSelection();
	layoutItems (0, false);
}
}
