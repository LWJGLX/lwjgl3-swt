/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
 * Instances of this class are sent as a result of accessibility clients
 * sending AccessibleValue messages to an accessible object.
 *
 * @see AccessibleValueListener
 * @see AccessibleValueAdapter
 *
 * @since 3.6
 */
public class AccessibleValueEvent extends EventObject {

	public Number value;

	static final long serialVersionUID = -465979079760740668L;

/**
 * Constructs a new instance of this class.
 *
 * @param source the object that fired the event
 */
public AccessibleValueEvent(Object source) {
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
	return "AccessibleValueEvent {" //$NON-NLS-1$
		+ "value=" + value   //$NON-NLS-1$
		+ "}";  //$NON-NLS-1$
}
}
