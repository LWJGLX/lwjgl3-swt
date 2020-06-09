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

public class CREATESTRUCT {
	/** @field cast=(LPVOID) */
	public int /*long*/ lpCreateParams;
	/** @field cast=(HINSTANCE) */
	public int /*long*/ hInstance;
	/** @field cast=(HMENU) */
	public int /*long*/ hMenu;
	/** @field cast=(HWND) */
	public int /*long*/ hwndParent;
	public int cy;
	public int cx;
	public int y;
	public int x;
	public int style;
	/** @field cast=(LPCTSTR) */
	public int /*long*/ lpszName;
	/** @field cast=(LPCTSTR) */
	public int /*long*/ lpszClass;
	public int dwExStyle;
	public static final int sizeof = OS.CREATESTRUCT_sizeof ();
}
