/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.swt.events;


import org.eclipse.swt.widgets.*;

/**
 * Instances of this class are sent as a result of
 * widgets being selected.
 * <p>
 * Note: The fields that are filled in depend on the widget.
 * </p>
 *
 * @see SelectionListener
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 */

public class SelectionEvent extends TypedEvent {

	/**
	 * The item that was selected.
	 */
	public Widget item;

	/**
	 * Extra detail information about the selection, depending on the widget.
	 *
	 * <p><b>Sash</b></p>
	 * <ul>
	 * <li>{@link org.eclipse.swt.SWT#DRAG}</li>
	 * </ul>
	 * <p><b>ScrollBar and Slider</b></p>
	 * <ul>
	 * <li>{@link org.eclipse.swt.SWT#DRAG}</li>
	 * <li>{@link org.eclipse.swt.SWT#HOME}</li>
	 * <li>{@link org.eclipse.swt.SWT#END}</li>
	 * <li>{@link org.eclipse.swt.SWT#ARROW_DOWN}</li>
	 * <li>{@link org.eclipse.swt.SWT#ARROW_UP}</li>
	 * <li>{@link org.eclipse.swt.SWT#PAGE_DOWN}</li>
	 * <li>{@link org.eclipse.swt.SWT#PAGE_UP}</li>
	 * </ul>
	 * <p><b>Table and Tree</b></p>
	 * <ul>
	 * <li>{@link org.eclipse.swt.SWT#CHECK}</li>
	 * </ul>
	 * <p><b>Text</b></p>
	 * <ul>
	 * <li>{@link org.eclipse.swt.SWT#CANCEL}</li>
	 * </ul>
	 * <p><b>CoolItem and ToolItem</b></p>
	 * <ul>
	 * <li>{@link org.eclipse.swt.SWT#ARROW}</li>
	 * </ul>
	 */
	public int detail;

	/**
	 * The x location of the selected area.
	 */
	public int x;

	/**
	 * The y location of selected area.
	 */
	public int y;

	/**
	 * The width of selected area.
	 */
	public int width;

	/**
	 * The height of selected area.
	 */
	public int height;

	/**
	 * The state of the keyboard modifier keys and mouse masks
	 * at the time the event was generated.
	 * <p>
	 * <b>Note:</b> Mouse button states are currently not included consistently
	 * for all widgets on all platforms. Clients should be prepared to receive
	 * button states, but should not rely on getting them everywhere.
	 * </p>
	 *
	 * @see org.eclipse.swt.SWT#MODIFIER_MASK
	 * @see org.eclipse.swt.SWT#BUTTON_MASK
	 */
	public int stateMask;

	/**
	 * The text of the hyperlink that was selected.
	 * This will be either the text of the hyperlink or the value of its HREF,
	 * if one was specified.
	 *
	 * @see org.eclipse.swt.widgets.Link#setText(String)
	 * @since 3.1
	 */
	public String text;

	/**
	 * A flag indicating whether the operation should be allowed.
	 * Setting this field to <code>false</code> will cancel the
	 * operation, depending on the widget.
	 */
	public boolean doit;

	static final long serialVersionUID = 3976735856884987953L;

/**
 * Constructs a new instance of this class based on the
 * information in the given untyped event.
 *
 * @param e the untyped event containing the information
 */
public SelectionEvent(Event e) {
	super(e);
	this.item = e.item;
	this.x = e.x;
	this.y = e.y;
	this.width = e.width;
	this.height = e.height;
	this.detail = e.detail;
	this.stateMask = e.stateMask;
	this.text = e.text;
	this.doit = e.doit;
}

/**
 * Returns a string containing a concise, human-readable
 * description of the receiver.
 *
 * @return a string representation of the event
 */
@Override
public String toString() {
	String string = super.toString ();
	return string.substring (0, string.length() - 1) // remove trailing '}'
		+ " item=" + item
		+ " detail=" + detail
		+ " x=" + x
		+ " y=" + y
		+ " width=" + width
		+ " height=" + height
		+ " stateMask=0x" + Integer.toHexString(stateMask)
		+ " text=" + text
		+ " doit=" + doit
		+ "}";
}
}

