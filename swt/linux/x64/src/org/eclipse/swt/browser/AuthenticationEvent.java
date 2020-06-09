/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
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
package org.eclipse.swt.browser;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;

/**
 * An <code>AuthenticationEvent</code> is sent by a {@link Browser}
 * to {@link AuthenticationListener}'s when the <code>Browser</code>
 * navigates to a page that requires authentication. This event allows
 * a client to either supply authentication credentials, cancel the
 * authentication, or do nothing (which causes an authentication prompter
 * to be shown to the user).
 *
 * @since 3.5
 */
public class AuthenticationEvent extends TypedEvent {
	/** The location that triggered the authentication challenge */
	public String location;

	/** The user name to authenticate with */
	public String user;

	/** The password to authenticate with */
	public String password;

	/**
	 * A flag indicating whether the authentication should proceed.
	 * Setting this field to <code>false</code> will cancel the operation.
	 */
	public boolean doit = true;

	static final long serialVersionUID = -8322331206780057921L;

/**
 * Constructs a new instance of this class.
 *
 * @param widget the widget that fired the event
 *
 * @since 3.5
 */
public AuthenticationEvent(Widget widget) {
	super(widget);
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
		+ " name=" + user
		+ " password=" + password
		+ " location=" + location
		+ "}";
}
}
