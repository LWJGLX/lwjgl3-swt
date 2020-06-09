/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 * Instances of this class represent a selectable user interface object
 * that issues notification when pressed and released.
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>CHECK, CASCADE, PUSH, RADIO, SEPARATOR</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Arm, Help, Selection</dd>
 * </dl>
 * <p>
 * Note: Only one of the styles CHECK, CASCADE, PUSH, RADIO and SEPARATOR
 * may be specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 *
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 * @noextend This class is not intended to be subclassed by clients.
 */
public class MenuItem extends Item {
	Menu parent, menu;
	long groupHandle, labelHandle, imageHandle;

	/** Feature in Gtk: as of Gtk version 3.10 GtkImageMenuItem is deprecated,
	 * meaning that MenuItems in SWT with images can no longer be GtkImageMenuItems
	 * after Gtk3.10. The solution to this is to create a GtkMenuItem, add a GtkBox
	 * as its child, and pack that box with a GtkLabel and GtkImage. This reproduces
	 * the functionality of a GtkImageMenuItem and allows SWT to retain image support
	 * for MenuItems.
	 *
	 * For more information see:
	 * https://developer.gnome.org/gtk3/stable/GtkImageMenuItem.html#GtkImageMenuItem.description
	 * Bug 470298
	 */
	long boxHandle;

	int accelerator, userId;
	String toolTipText;

/**
 * Constructs a new instance of this class given its parent
 * (which must be a <code>Menu</code>) and a style value
 * describing its behavior and appearance. The item is added
 * to the end of the items maintained by its parent.
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
 * @param parent a menu control which will be the parent of the new instance (cannot be null)
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
 * @see SWT#CHECK
 * @see SWT#CASCADE
 * @see SWT#PUSH
 * @see SWT#RADIO
 * @see SWT#SEPARATOR
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public MenuItem (Menu parent, int style) {
	super (parent, checkStyle (style));
	this.parent = parent;
	createWidget (parent.getItemCount ());
}

/**
 * Constructs a new instance of this class given its parent
 * (which must be a <code>Menu</code>), a style value
 * describing its behavior and appearance, and the index
 * at which to place it in the items maintained by its parent.
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
 * @param parent a menu control which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 * @param index the zero-relative index to store the receiver in its parent
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the parent (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT#CHECK
 * @see SWT#CASCADE
 * @see SWT#PUSH
 * @see SWT#RADIO
 * @see SWT#SEPARATOR
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public MenuItem (Menu parent, int style, int index) {
	super (parent, checkStyle (style));
	this.parent = parent;
	int count = parent.getItemCount ();
	if (!(0 <= index && index <= count)) {
		error (SWT.ERROR_INVALID_RANGE);
	}
	createWidget (index);
}

void addAccelerator (long accelGroup) {
	updateAccelerator (accelGroup, true);
}

void addAccelerators (long accelGroup) {
	addAccelerator (accelGroup);
	if (menu != null) menu.addAccelerators (accelGroup);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the arm events are generated for the control, by sending
 * it one of the messages defined in the <code>ArmListener</code>
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
 * @see ArmListener
 * @see #removeArmListener
 */
public void addArmListener (ArmListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Arm, typedListener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when the help events are generated for the control, by sending
 * it one of the messages defined in the <code>HelpListener</code>
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
 * be notified when the menu item is selected by the user, by sending
 * it one of the messages defined in the <code>SelectionListener</code>
 * interface.
 * <p>
 * When <code>widgetSelected</code> is called, the stateMask field of the event object is valid.
 * <code>widgetDefaultSelected</code> is not called.
 * </p>
 * <p>
 * When the <code>SWT.RADIO</code> style bit is set, the <code>widgetSelected</code> method is
 * also called when the receiver loses selection because another item in the same radio group
 * was selected by the user. During <code>widgetSelected</code> the application can use
 * <code>getSelection()</code> to determine the current selected state of the receiver.
 * </p>
 *
 * @param listener the listener which should be notified when the menu item is selected by the user
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
public void addSelectionListener (SelectionListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener(listener);
	addListener (SWT.Selection,typedListener);
	addListener (SWT.DefaultSelection,typedListener);
}

static int checkStyle (int style) {
	return checkBits (style, SWT.PUSH, SWT.CHECK, SWT.RADIO, SWT.SEPARATOR, SWT.CASCADE, 0);
}

@Override
protected void checkSubclass () {
	if (!isValidSubclass ()) error (SWT.ERROR_INVALID_SUBCLASS);
}

@Override
void createHandle (int index) {
	state |= HANDLE;
	byte [] buffer = new byte [1];
	int bits = SWT.CHECK | SWT.RADIO | SWT.PUSH | SWT.SEPARATOR | SWT.CASCADE;
	switch (style & bits) {
		case SWT.SEPARATOR:
			handle = GTK.gtk_separator_menu_item_new ();
			if (handle == 0) error (SWT.ERROR_NO_HANDLES);
			break;
		case SWT.RADIO:
			/*
			* Feature in GTK.  In GTK, radio button must always be part of
			* a radio button group.  In a GTK radio group, one button is always
			* selected.  This means that it is not possible to have a single
			* radio button that is unselected.  This is necessary to allow
			* applications to implement their own radio behavior or use radio
			* buttons outside of radio groups.  The fix is to create a hidden
			* radio button for each radio button we create and add them
			* to the same group.  This allows the visible button to be
			* unselected.
			*/
			groupHandle = GTK.gtk_radio_menu_item_new (0);
			if (groupHandle == 0) error (SWT.ERROR_NO_HANDLES);
			OS.g_object_ref_sink (groupHandle);
			long group = GTK.gtk_radio_menu_item_get_group (groupHandle);
			handle = GTK.gtk_radio_menu_item_new (group);
			if (handle == 0) error (SWT.ERROR_NO_HANDLES);

			labelHandle = GTK.gtk_accel_label_new (buffer);
			if (labelHandle == 0) error (SWT.ERROR_NO_HANDLES);

			boxHandle = gtk_box_new (GTK.GTK_ORIENTATION_HORIZONTAL, false, 6);
			if (boxHandle == 0) error (SWT.ERROR_NO_HANDLES);

			if (OS.SWT_PADDED_MENU_ITEMS) {
				imageHandle = GTK.gtk_image_new ();
				if (imageHandle == 0) error (SWT.ERROR_NO_HANDLES);
			}
			break;
		case SWT.CHECK:
			handle = GTK.gtk_check_menu_item_new ();
			if (handle == 0) error (SWT.ERROR_NO_HANDLES);

			labelHandle = GTK.gtk_accel_label_new (buffer);
			if (labelHandle == 0) error (SWT.ERROR_NO_HANDLES);

			boxHandle = gtk_box_new (GTK.GTK_ORIENTATION_HORIZONTAL, false, 6);
			if (boxHandle == 0) error (SWT.ERROR_NO_HANDLES);

			if (OS.SWT_PADDED_MENU_ITEMS) {
				imageHandle = GTK.gtk_image_new ();
				if (imageHandle == 0) error (SWT.ERROR_NO_HANDLES);
			}
			break;
		// This case now needs to be handled due to double padding. When double padded
		// menus are used, the "head" menu item (such as File, Edit, Help, etc.) should
		// not be padded. We only care about this in Gtk3.
		case SWT.CASCADE:
			handle = GTK.gtk_menu_item_new ();
			if (handle == 0) error (SWT.ERROR_NO_HANDLES);

			labelHandle = GTK.gtk_accel_label_new (buffer);
			if (labelHandle == 0) error (SWT.ERROR_NO_HANDLES);

			boxHandle = gtk_box_new (GTK.GTK_ORIENTATION_HORIZONTAL, false, 6);
			if (boxHandle == 0) error (SWT.ERROR_NO_HANDLES);
			if ((parent.style & bits) == SWT.BAR) {
				break;
			}
			if (OS.SWT_PADDED_MENU_ITEMS) {
				imageHandle = GTK.gtk_image_new ();
				if (imageHandle == 0) error (SWT.ERROR_NO_HANDLES);
			}
			break;
		case SWT.PUSH:
		default:
			handle = GTK.gtk_menu_item_new ();
			if (handle == 0) error (SWT.ERROR_NO_HANDLES);

			labelHandle = GTK.gtk_accel_label_new (buffer);
			if (labelHandle == 0) error (SWT.ERROR_NO_HANDLES);

			boxHandle = gtk_box_new (GTK.GTK_ORIENTATION_HORIZONTAL, false, 6);
			if (boxHandle == 0) error (SWT.ERROR_NO_HANDLES);

			if (OS.SWT_PADDED_MENU_ITEMS) {
				imageHandle = GTK.gtk_image_new ();
				if (imageHandle == 0) error (SWT.ERROR_NO_HANDLES);
			}
			break;
	}
	if (imageHandle != 0) {
		if (OS.SWT_PADDED_MENU_ITEMS) {
			GTK.gtk_image_set_pixel_size (imageHandle, 16);
		}
		GTK.gtk_container_add (boxHandle, imageHandle);
		GTK.gtk_widget_show (imageHandle);
	}
	if (labelHandle != 0) {
		if (GTK.GTK_VERSION >= OS.VERSION (3, 16, 0)) {
			GTK.gtk_label_set_xalign (labelHandle, 0);
			GTK.gtk_widget_set_halign (labelHandle, GTK.GTK_ALIGN_FILL);
		} else {
			GTK.gtk_misc_set_alignment(labelHandle, 0, 0);
		}
		gtk_box_pack_end (boxHandle, labelHandle, true, true, 0);
		GTK.gtk_widget_show (labelHandle);
	}
	if (boxHandle != 0) {
		GTK.gtk_container_add (handle, boxHandle);
		GTK.gtk_widget_show (boxHandle);
	}
	if ((style & SWT.SEPARATOR) == 0) {
		if (boxHandle == 0) {
			labelHandle = GTK.gtk_bin_get_child (handle);
		}
		GTK.gtk_accel_label_set_accel_widget (labelHandle, 0);
	}
	long parentHandle = parent.handle;
	boolean enabled = GTK.gtk_widget_get_sensitive (parentHandle);
	if (!enabled) GTK.gtk_widget_set_sensitive (parentHandle, true);
	GTK.gtk_menu_shell_insert (parentHandle, handle, index);
	if (!enabled) GTK.gtk_widget_set_sensitive (parentHandle, false);
	GTK.gtk_widget_show (handle);
}

void fixMenus (Decorations newParent) {
	if (menu != null && !menu.isDisposed() && !newParent.isDisposed()) menu.fixMenus (newParent);
}

/**
 * Returns the widget accelerator.  An accelerator is the bit-wise
 * OR of zero or more modifier masks and a key. Examples:
 * <code>SWT.CONTROL | SWT.SHIFT | 'T', SWT.ALT | SWT.F2</code>.
 * The default value is zero, indicating that the menu item does
 * not have an accelerator.
 *
 * @return the accelerator or 0
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getAccelerator () {
	checkWidget();
	return accelerator;
}

long getAccelGroup () {
	Menu menu = parent;
	while (menu != null && menu.cascade != null) {
		menu = menu.cascade.parent;
	}
	if (menu == null) return 0;
	Decorations shell = menu.parent;
	if (shell == null) return 0;
	return shell.menuBar == menu ? shell.accelGroup : 0;
}

/*public*/ Rectangle getBounds () {
	checkWidget();
	if (!GTK.gtk_widget_get_mapped (handle)) {
		return new Rectangle (0, 0, 0, 0);
	}
	GtkAllocation allocation = new GtkAllocation ();
	GTK.gtk_widget_get_allocation (handle, allocation);
	int x = allocation.x;
	int y = allocation.y;
	int width = allocation.width;
	int height = allocation.height;
	return new Rectangle (x, y, width, height);
}

/**
 * Returns <code>true</code> if the receiver is enabled, and
 * <code>false</code> otherwise. A disabled menu item is typically
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
	checkWidget();
	return GTK.gtk_widget_get_sensitive (handle);
}

/**
 * Gets the identifier associated with the receiver.
 *
 * @return the receiver's identifier
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.7
 */
public int getID () {
	checkWidget();
	return userId;
}

/**
 * Returns the receiver's cascade menu if it has one or null
 * if it does not. Only <code>CASCADE</code> menu items can have
 * a pull down menu. The sequence of key strokes, button presses
 * and/or button releases that are used to request a pull down
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

@Override
String getNameText () {
	if ((style & SWT.SEPARATOR) != 0) return "|";
	return super.getNameText ();
}

/**
 * Returns the receiver's parent, which must be a <code>Menu</code>.
 *
 * @return the receiver's parent
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Menu getParent () {
	checkWidget();
	return parent;
}

/**
 * Returns <code>true</code> if the receiver is selected,
 * and false otherwise.
 * <p>
 * When the receiver is of type <code>CHECK</code> or <code>RADIO</code>,
 * it is selected when it is checked.
 *
 * @return the selection state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean getSelection () {
	checkWidget();
	if ((style & (SWT.CHECK | SWT.RADIO)) == 0) return false;
	return GTK.gtk_check_menu_item_get_active(handle);
}

/**
 * Returns the receiver's tool tip text, or null if it has not been set.
 *
 * @return the receiver's tool tip text
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.104
 */
public String getToolTipText () {
	checkWidget();
	return toolTipText;
}

@Override
long gtk_activate (long widget) {
	if ((style & SWT.CASCADE) != 0 && menu != null) return 0;
	/*
	* Bug in GTK.  When an ancestor menu is disabled and
	* the user types an accelerator key, GTK delivers the
	* the activate signal even though the menu item cannot
	* be invoked using the mouse.  The fix is to ignore
	* activate signals when an ancestor menu is disabled.
	*/
	if (!isEnabled ()) return 0;
	if ((style & SWT.RADIO) != 0) {
		if ((parent.getStyle () & SWT.NO_RADIO_GROUP) == 0) {
			selectRadio ();
		}
	}
	sendSelectionEvent (SWT.Selection);
	return 0;
}

@Override
long gtk_select (long item) {
	parent.selectedItem = this;
	sendEvent (SWT.Arm);
	return 0;
}

@Override
long gtk_show_help (long widget, long helpType) {
	boolean handled = hooks (SWT.Help);
	if (handled) {
		postEvent (SWT.Help);
	} else {
		handled = parent.sendHelpEvent (helpType);
	}
	if (handled) {
		GTK.gtk_menu_shell_deactivate (parent.handle);
		return 1;
	}
	return 0;
}

@Override
void hookEvents () {
	super.hookEvents ();
	OS.g_signal_connect_closure (handle, OS.activate, display.getClosure (ACTIVATE), false);
	OS.g_signal_connect_closure (handle, OS.select, display.getClosure (SELECT), false);
	OS.g_signal_connect_closure_by_id (handle, display.signalIds [SHOW_HELP], 0, display.getClosure (SHOW_HELP), false);
}

/**
 * Returns <code>true</code> if the receiver is enabled and all
 * of the receiver's ancestors are enabled, and <code>false</code>
 * otherwise. A disabled menu item is typically not selectable from the
 * user interface and draws with an inactive or "grayed" look.
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
	return getEnabled () && parent.isEnabled ();
}

@Override
void releaseChildren (boolean destroy) {
	if (menu != null) {
		menu.release (false);
		menu = null;
	}
	super.releaseChildren (destroy);
}

@Override
void releaseParent () {
	super.releaseParent ();
	if (menu != null) {
		if (menu.selectedItem == this) menu.selectedItem = null;
		menu.dispose ();
	}
	menu = null;
}

@Override
void releaseWidget () {
	super.releaseWidget ();
	long accelGroup = getAccelGroup ();
	if (accelGroup != 0) removeAccelerator (accelGroup);
	if (groupHandle != 0) OS.g_object_unref (groupHandle);
	groupHandle = 0;
	accelerator = 0;
	parent = null;
}

void removeAccelerator (long accelGroup) {
	updateAccelerator (accelGroup, false);
}

void removeAccelerators (long accelGroup) {
	removeAccelerator (accelGroup);
	if (menu != null) menu.removeAccelerators (accelGroup);
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when the arm events are generated for the control.
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
 * @see ArmListener
 * @see #addArmListener
 */
public void removeArmListener (ArmListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Arm, listener);
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
public void removeSelectionListener (SelectionListener listener) {
	checkWidget();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Selection, listener);
	eventTable.unhook (SWT.DefaultSelection,listener);
}
@Override
void reskinChildren (int flags) {
	if (menu != null) {
		menu.reskin (flags);
	}
	super.reskinChildren (flags);
}
void selectRadio () {
	int index = 0;
	MenuItem [] items = parent.getItems ();
	while (index < items.length && items [index] != this) index++;
	int i = index - 1;
	while (i >= 0 && items [i].setRadioSelection (false)) --i;
	int j = index + 1;
	while (j < items.length && items [j].setRadioSelection (false)) j++;
	setSelection (true);
}
/**
 * Sets the widget accelerator.  An accelerator is the bit-wise
 * OR of zero or more modifier masks and a key. Examples:
 * <code>SWT.MOD1 | SWT.MOD2 | 'T', SWT.MOD3 | SWT.F2</code>.
 * <code>SWT.CONTROL | SWT.SHIFT | 'T', SWT.ALT | SWT.F2</code>.
 * The default value is zero, indicating that the menu item does
 * not have an accelerator.
 *
 * @param accelerator an integer that is the bit-wise OR of masks and a key
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setAccelerator (int accelerator) {
	checkWidget();
	if (this.accelerator == accelerator) return;
	long accelGroup = getAccelGroup ();
	if (accelGroup != 0) removeAccelerator (accelGroup);
	this.accelerator = accelerator;
	if (accelGroup != 0) addAccelerator (accelGroup);
}

/**
 * Enables the receiver if the argument is <code>true</code>,
 * and disables it otherwise. A disabled menu item is typically
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
	if (GTK.gtk_widget_get_sensitive (handle) == enabled) return;
	long accelGroup = getAccelGroup ();
	if (accelGroup != 0) removeAccelerator (accelGroup);
	GTK.gtk_widget_set_sensitive (handle, enabled);
	if (accelGroup != 0) addAccelerator (accelGroup);
}

/**
 * Sets the identifier associated with the receiver to the argument.
 *
 * @param id the new identifier. This must be a non-negative value. System-defined identifiers are negative values.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_INVALID_ARGUMENT - if called with an negative-valued argument.</li>
 * </ul>
 *
 * @since 3.7
 */
public void setID (int id) {
	checkWidget();
	if (id < 0) error(SWT.ERROR_INVALID_ARGUMENT);
	userId = id;
}

/**
 * Sets the receiver's image to the argument, which may be
 * null indicating that no image should be displayed.
 * <p>
 * Note: This operation is a <em>HINT</em> and is not supported on
 * platforms that do not have this concept (for example, Windows NT).
 * Furthermore, some platforms (such as GTK2), cannot display both
 * a check box and an image at the same time.  Instead, they hide
 * the image and display the check box. Some platforms (such as GTK3)
 * support images alongside check boxes.
 * </p>
 *
 * @param image the image to display on the receiver (may be null)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
@Override
public void setImage (Image image) {
	checkWidget();
	if ((style & SWT.SEPARATOR) != 0) return;
	super.setImage (image);
	if (image != null) {
		ImageList imageList = parent.imageList;
		if (imageList == null) imageList = parent.imageList = new ImageList ();
		int imageIndex = imageList.indexOf (image);
		long pixbuf = 0;
		if (imageIndex == -1) {
			imageIndex = imageList.add (image);
			pixbuf = imageList.getPixbuf (imageIndex);
			/*
			 * Bug in GTK the default renderer does scale again on pixbuf.
			 * Need to scaledown here and no need to scaledown id device scale is 1
			 *
			 * We should resize pixbuf and update pixbuf array in the imagelist only if
			 * the image is added to the imagelist in this call. If the image is already
			 * add imageList.getPixbuf returns resized pixbuf.
			 */
			if (DPIUtil.useCairoAutoScale()) {
				Rectangle imgSize = image.getBounds();
				long scaledPixbuf = GDK.gdk_pixbuf_scale_simple(pixbuf, imgSize.width, imgSize.height, GDK.GDK_INTERP_BILINEAR);
				if (scaledPixbuf !=0) {
					pixbuf = scaledPixbuf;
				}
				imageList.replacePixbuf(imageIndex, pixbuf);
			}
		} else {
			imageList.put (imageIndex, image);
			pixbuf = imageList.getPixbuf (imageIndex);
		}

		if (!GTK.GTK_IS_MENU_ITEM (handle)) return;
		if (OS.SWT_PADDED_MENU_ITEMS && imageHandle != 0) {
			GTK.gtk_image_set_from_pixbuf(imageHandle, pixbuf);
		} else {
			if (imageHandle == 0 && boxHandle != 0) {
				imageHandle = GTK.gtk_image_new_from_pixbuf (pixbuf);
				GTK.gtk_container_add (boxHandle, imageHandle);
				GTK.gtk_box_reorder_child (boxHandle, imageHandle, 0);
			} else {
				GTK.gtk_image_set_from_pixbuf(imageHandle, pixbuf);
			}
			if (boxHandle == 0) error (SWT.ERROR_NO_HANDLES);
		}
		if (imageHandle == 0) error (SWT.ERROR_NO_HANDLES);
		GTK.gtk_widget_show (imageHandle);
	} else {
		if (imageHandle != 0 && boxHandle != 0) {
			if (OS.SWT_PADDED_MENU_ITEMS) {
				GTK.gtk_container_remove(boxHandle, imageHandle);
				imageHandle = GTK.gtk_image_new ();
				if (imageHandle == 0) error (SWT.ERROR_NO_HANDLES);
				GTK.gtk_image_set_pixel_size (imageHandle, 16);
				GTK.gtk_container_add (boxHandle, imageHandle);
				GTK.gtk_widget_show (imageHandle);
			} else {
				GTK.gtk_container_remove(boxHandle, imageHandle);
				imageHandle = 0;
			}
		}
	}
}

/**
 * Sets the receiver's pull down menu to the argument.
 * Only <code>CASCADE</code> menu items can have a
 * pull down menu. The sequence of key strokes, button presses
 * and/or button releases that are used to request a pull down
 * menu is platform specific.
 * <p>
 * Note: Disposing of a menu item that has a pull down menu
 * will dispose of the menu.  To avoid this behavior, set the
 * menu to null before the menu item is disposed.
 * </p>
 *
 * @param menu the new pull down menu
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_MENU_NOT_DROP_DOWN - if the menu is not a drop down menu</li>
 *    <li>ERROR_MENUITEM_NOT_CASCADE - if the menu item is not a <code>CASCADE</code></li>
 *    <li>ERROR_INVALID_ARGUMENT - if the menu has been disposed</li>
 *    <li>ERROR_INVALID_PARENT - if the menu is not in the same widget tree</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setMenu (Menu menu) {
	checkWidget ();

	/* Check to make sure the new menu is valid */
	if ((style & SWT.CASCADE) == 0) {
		error (SWT.ERROR_MENUITEM_NOT_CASCADE);
	}
	if (menu != null) {
		if ((menu.style & SWT.DROP_DOWN) == 0) {
			error (SWT.ERROR_MENU_NOT_DROP_DOWN);
		}
		if (menu.parent != parent.parent) {
			error (SWT.ERROR_INVALID_PARENT);
		}
	}

	/* Assign the new menu */
	Menu oldMenu = this.menu;
	if (oldMenu == menu) return;
	long accelGroup = getAccelGroup ();
	if (accelGroup != 0) removeAccelerators (accelGroup);
	if (oldMenu != null) {
		oldMenu.cascade = null;
		/*
		* Add a reference to the menu we are about
		* to replace or GTK will destroy it.
		*/
		OS.g_object_ref (oldMenu.handle);
		GTK.gtk_menu_item_set_submenu (handle, 0);
	}
	if ((this.menu = menu) != null) {
		menu.cascade = this;
		GTK.gtk_menu_item_set_submenu (handle, menu.handle);
	}
	if (accelGroup != 0) addAccelerators (accelGroup);
}

@Override
void setOrientation (boolean create) {
	super.setOrientation (create);
	if ((parent.style & SWT.RIGHT_TO_LEFT) != 0 || !create) {
		int dir = (parent.style & SWT.RIGHT_TO_LEFT) != 0 ? GTK.GTK_TEXT_DIR_RTL : GTK.GTK_TEXT_DIR_LTR;
		GTK.gtk_widget_set_direction (handle, dir);
		GTK.gtk_container_forall (handle, display.setDirectionProc, dir);
		if (menu != null) menu._setOrientation (parent.style & (SWT.RIGHT_TO_LEFT | SWT.LEFT_TO_RIGHT));
	}
}

boolean setRadioSelection (boolean value) {
	if ((style & SWT.RADIO) == 0) return false;
	if (getSelection () != value) {
		setSelection (value);
		sendSelectionEvent (SWT.Selection);
	}
	return true;
}

/**
 * Sets the selection state of the receiver.
 * <p>
 * When the receiver is of type <code>CHECK</code> or <code>RADIO</code>,
 * it is selected when it is checked.
 *
 * @param selected the new selection state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setSelection (boolean selected) {
	checkWidget();
	if ((style & (SWT.CHECK | SWT.RADIO)) == 0) return;
	OS.g_signal_handlers_block_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, ACTIVATE);
	GTK.gtk_check_menu_item_set_active (handle, selected);
	if ((style & SWT.RADIO) != 0) GTK.gtk_check_menu_item_set_active (groupHandle, !selected);
	OS.g_signal_handlers_unblock_matched (handle, OS.G_SIGNAL_MATCH_DATA, 0, 0, 0, 0, ACTIVATE);
}

/**
 * Sets the receiver's text. The string may include
 * the mnemonic character and accelerator text.
 * <p>
 * Mnemonics are indicated by an '&amp;' that causes the next
 * character to be the mnemonic.  When the user presses a
 * key sequence that matches the mnemonic, a selection
 * event occurs. On most platforms, the mnemonic appears
 * underlined but may be emphasised in a platform specific
 * manner.  The mnemonic indicator character '&amp;' can be
 * escaped by doubling it in the string, causing a single
 * '&amp;' to be displayed.
 * </p>
 * <p>
 * Accelerator text is indicated by the '\t' character.
 * On platforms that support accelerator text, the text
 * that follows the '\t' character is displayed to the user,
 * typically indicating the key stroke that will cause
 * the item to become selected.  On most platforms, the
 * accelerator text appears right aligned in the menu.
 * Setting the accelerator text does not install the
 * accelerator key sequence. The accelerator key sequence
 * is installed using #setAccelerator.
 * </p>
 *
 * @param string the new text
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the text is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #setAccelerator
 */
@Override
public void setText (String string) {
	checkWidget();
	if (string == null) error (SWT.ERROR_NULL_ARGUMENT);
	if ((style & SWT.SEPARATOR) != 0) return;
	if (text.equals (string)) return;
	super.setText (string);
	int index = string.indexOf ('\t');
	if (index != -1) {
		string = string.substring (0, index);
	}
	char [] chars = fixMnemonic (string);
	byte [] buffer = Converter.wcsToMbcs (chars, true);
	if (labelHandle != 0 && GTK.GTK_IS_LABEL (labelHandle)) {
		GTK.gtk_label_set_text_with_mnemonic (labelHandle, buffer);
		if (GTK.GTK_IS_ACCEL_LABEL (labelHandle)) {
			MaskKeysym maskKeysym = getMaskKeysym();
			if (maskKeysym != null) {
				GTK.gtk_accel_label_set_accel_widget (labelHandle, handle);
				GTK.gtk_accel_label_set_accel (labelHandle,
						maskKeysym.keysym, maskKeysym.mask);
			}
			// A workaround for Ubuntu Unity global menu
			OS.g_signal_emit_by_name(handle, OS.accel_closures_changed);
		}
	}
}

/**
 * Sets the receiver's tool tip text to the argument, which
 * may be null indicating that the default tool tip for the
 * control will be shown. For a menu item that has a default
 * tool tip, setting
 * the tool tip text to an empty string replaces the default,
 * causing no tool tip text to be shown.
 * <p>
 * The mnemonic indicator (character '&amp;') is not displayed in a tool tip.
 * To display a single '&amp;' in the tool tip, the character '&amp;' can be
 * escaped by doubling it in the string.
 * </p>
 * <p>
 * NOTE: Tooltips are currently not shown for top-level menu items in the
 * {@link Shell#setMenuBar(Menu) shell menubar} on Windows, Mac, and Ubuntu Unity desktop.
 * </p>
 * <p>
 * NOTE: This operation is a hint and behavior is platform specific, on Windows
 * for CJK-style mnemonics of the form " (&amp;C)" at the end of the tooltip text
 * are not shown in tooltip.
 * </p>
 * @param toolTip the new tool tip text (or null)
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.104
 */
public void setToolTipText (String toolTip) {
	checkWidget();
	if (toolTip != null && (toolTip.trim().length() == 0 || toolTip.equals (toolTipText))) return;

	this.parent.getShell().setToolTipText (handle, (toolTipText = toolTip));
}

void updateAccelerator (long accelGroup, boolean add) {
	if (accelerator == 0 || !getEnabled ()) return;
	if ((accelerator & SWT.COMMAND) != 0) return;
	int mask = 0;
	if ((accelerator & SWT.ALT) != 0) mask |= GDK.GDK_MOD1_MASK;
	if ((accelerator & SWT.SHIFT) != 0) mask |= GDK.GDK_SHIFT_MASK;
	if ((accelerator & SWT.CONTROL) != 0) mask |= GDK.GDK_CONTROL_MASK;
	int keysym = accelerator & SWT.KEY_MASK;
	int newKey = Display.untranslateKey (keysym);
	if (newKey != 0) {
		keysym = newKey;
	} else {
		switch (keysym) {
			case '\r': keysym = GDK.GDK_Return; break;
			default: keysym = Converter.wcsToMbcs ((char) keysym);
		}
	}
	/* When accel_key is zero, it causes GTK warnings */
	if (keysym != 0) {
		if (add) {
			GTK.gtk_widget_add_accelerator (handle, OS.activate, accelGroup, keysym, mask, GTK.GTK_ACCEL_VISIBLE);
		} else {
			GTK.gtk_widget_remove_accelerator (handle, accelGroup, keysym, mask);
		}
	}
}

private class MaskKeysym {
	int mask = 0;
	int keysym = 0;
}

private MaskKeysym getMaskKeysym() {
	if (text == null) return null;
	MaskKeysym maskKeysym = new MaskKeysym();
	int accelIndex = text.indexOf ('\t');
	if (accelIndex == -1) return null;
	int start = accelIndex + 1;
	int plusIndex = text.indexOf('+', start);
	while (plusIndex != -1) {
		String maskStr = text.substring(start, plusIndex);
		if (maskStr.equals("Ctrl")) maskKeysym.mask |= GDK.GDK_CONTROL_MASK;
		if (maskStr.equals("Shift")) maskKeysym.mask |= GDK.GDK_SHIFT_MASK;
		if (maskStr.equals("Alt")) maskKeysym.mask |= GDK.GDK_MOD1_MASK;
		start = plusIndex + 1;
		plusIndex = text.indexOf('+', start);
	}
	if ("Enter".equals(text.substring(start))) {
		maskKeysym.keysym = GDK.GDK_ISO_Enter;
	}
	switch (text.length() - start) {
		case 1:
			maskKeysym.keysym = text.charAt(start);
			maskKeysym.keysym = Converter.wcsToMbcs ((char) maskKeysym.keysym);
			break;
		case 2:
			if (text.charAt(start) == 'F') {
				switch (text.charAt(start + 1)) {
					case '1': maskKeysym.keysym = GDK.GDK_F1; break;
					case '2': maskKeysym.keysym = GDK.GDK_F2; break;
					case '3': maskKeysym.keysym = GDK.GDK_F3; break;
					case '4': maskKeysym.keysym = GDK.GDK_F4; break;
					case '5': maskKeysym.keysym = GDK.GDK_F5; break;
					case '6': maskKeysym.keysym = GDK.GDK_F6; break;
					case '7': maskKeysym.keysym = GDK.GDK_F7; break;
					case '8': maskKeysym.keysym = GDK.GDK_F8; break;
					case '9': maskKeysym.keysym = GDK.GDK_F9; break;
				}
			}
			break;
		case 3:
			if (text.charAt(start) == 'F' && text.charAt(start + 1) == '1') {
				switch (text.charAt(start + 2)) {
					case '0': maskKeysym.keysym = GDK.GDK_F10; break;
					case '1': maskKeysym.keysym = GDK.GDK_F11; break;
					case '2': maskKeysym.keysym = GDK.GDK_F12; break;
					case '3': maskKeysym.keysym = GDK.GDK_F13; break;
					case '4': maskKeysym.keysym = GDK.GDK_F14; break;
					case '5': maskKeysym.keysym = GDK.GDK_F15; break;
				}
			}
			break;
	}
	return maskKeysym;
}
boolean updateAcceleratorText (boolean show) {
	if (accelerator != 0) return false;
	MaskKeysym maskKeysym = null;
	if (show) {
		maskKeysym = getMaskKeysym();
	}
	if (maskKeysym == null) return true;
	if (maskKeysym.keysym != 0) {
		long accelGroup = getAccelGroup ();
		if (show) {
			GTK.gtk_widget_add_accelerator (handle, OS.activate, accelGroup, maskKeysym.keysym, maskKeysym.mask, GTK.GTK_ACCEL_VISIBLE);
		} else {
			GTK.gtk_widget_remove_accelerator (handle, accelGroup, maskKeysym.keysym, maskKeysym.mask);
		}
	}
	return maskKeysym.keysym != 0;
}

}
