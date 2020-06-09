/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.swt.accessibility;


import java.util.*;

/**
 * Instances of this class are sent as a result of
 * accessibility clients sending messages to controls
 * asking for detailed information about the implementation
 * of the control instance. Typically, only implementors
 * of custom controls need to listen for this event.
 * <p>
 * Note: The meaning of each field depends on the
 * message that was sent.
 * </p>
 *
 * @see AccessibleControlListener
 * @see AccessibleControlAdapter
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 *
 * @since 2.0
 */
public class AccessibleControlEvent extends EventObject {
	public int childID;			// IN/OUT
	public Accessible accessible;	// OUT
	public int x, y;				// IN/OUT
	public int width, height;		// OUT
	public int detail;			// IN/OUT
	public String result;			// OUT
	public Object children[];		// [OUT]

	static final long serialVersionUID = 3257281444169529141L;

/**
 * Constructs a new instance of this class.
 *
 * @param source the object that fired the event
 */
public AccessibleControlEvent(Object source) {
	super(source);
}

/**
 * Returns a string containing a concise, human-readable
 * description of the receiver.
 *
 * @return a string representation of the event
 */
@Override
public String toString () {
	return "AccessibleControlEvent {childID=" + childID +  //$NON-NLS-1$
		" accessible=" + accessible +  //$NON-NLS-1$
		" x=" + x +  //$NON-NLS-1$
		" y=" + y +  //$NON-NLS-1$
		" width=" + width +  //$NON-NLS-1$
		" height=" + height +  //$NON-NLS-1$
		" detail=" + detail +  //$NON-NLS-1$
		" result=" + result +  //$NON-NLS-1$
		"}"; //$NON-NLS-1$
}
}
