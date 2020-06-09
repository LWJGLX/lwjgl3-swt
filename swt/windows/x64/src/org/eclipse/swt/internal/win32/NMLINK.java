/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.swt.internal.win32;

public class NMLINK extends NMHDR {
//	LITEM item;
	/** @field accessor=item.mask */
	public int mask;
	/** @field accessor=item.iLink */
	public int iLink;
	/** @field accessor=item.state */
	public int state;
	/** @field accessor=item.stateMask */
	public int stateMask;
	/** @field accessor=item.szID */
	public char[] szID = new char[OS.MAX_LINKID_TEXT];
	/** @field accessor=item.szUrl */
	public char[] szUrl = new char[OS.L_MAX_URL_LENGTH];
	public static final int sizeof = OS.NMLINK_sizeof ();
}
