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

public class BROWSEINFO {
	/** @field cast=(HWND) */
	public int /*long*/ hwndOwner;
	/** @field cast=(LPCITEMIDLIST) */
	public int /*long*/ pidlRoot;
	/** @field cast=(LPTSTR) */
	public int /*long*/ pszDisplayName;
	/** @field cast=(LPCTSTR) */
	public int /*long*/ lpszTitle;
	public int ulFlags;
	/** @field cast=(BFFCALLBACK) */
	public int /*long*/ lpfn;
	public int /*long*/ lParam;
	public int iImage;
	public static final int sizeof = OS.BROWSEINFO_sizeof ();
}
