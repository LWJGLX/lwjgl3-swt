/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

public class TOOLINFO {
	public int cbSize;
	public int uFlags;
	/** @field cast=(HWND) */
	public int /*long*/ hwnd;
	public int /*long*/ uId;
//	public RECT rect;
	/** @field accessor=rect.left */
	public int left;
	/** @field accessor=rect.top */
	public int top;
	/** @field accessor=rect.right */
	public int right;
	/** @field accessor=rect.bottom */
	public int bottom;
	/** @field cast=(HINSTANCE) */
	public int /*long*/ hinst;
	/** @field cast=(LPTSTR) */
	public int /*long*/ lpszText;
	public int /*long*/ lParam;
	/** @field cast=(void *) */
	public int /*long*/ lpReserved;
	public static int sizeof = OS.TOOLINFO_sizeof ();
}
