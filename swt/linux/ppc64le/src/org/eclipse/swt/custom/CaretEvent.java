/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.swt.custom;


import org.eclipse.swt.events.*;

/**
 * This event is sent when the caret offset changes.
 *
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 *
 * @since 3.5
 */
public class CaretEvent extends TypedEvent {

	/**
	 * caret offset
	 */
	public int caretOffset;

	static final long serialVersionUID = 3257846571587545489L;

CaretEvent(StyledTextEvent e) {
	super(e);
	caretOffset = e.end;
}
}
