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

public abstract class NOTIFYICONDATA {
	public int cbSize;
	/** @field cast=(HWND) */
	public int /*long*/ hWnd;
	public int uID;
	public int uFlags;
	public int uCallbackMessage;
	/** @field cast=(HICON) */
	public int /*long*/ hIcon;
	public int dwState;
	public int dwStateMask;
	public int uVersion;
	public int dwInfoFlags;
	public static final int sizeof = OS.NOTIFYICONDATA_V2_SIZE;
}
