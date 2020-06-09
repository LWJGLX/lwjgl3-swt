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

public class PRINTDLG {
	public int lStructSize; // DWORD
	/** @field cast=(HWND) */
	public int /*long*/ hwndOwner; // HWND
	/** @field cast=(HGLOBAL) */
	public int /*long*/ hDevMode; // HGLOBAL
	/** @field cast=(HGLOBAL) */
	public int /*long*/ hDevNames; // HGLOBAL
	/** @field cast=(HDC) */
	public int /*long*/ hDC; // HDC
	public int Flags; // DWORD
	public short nFromPage; // WORD
	public short nToPage; // WORD
	public short nMinPage; // WORD
	public short nMaxPage; // WORD
	public short nCopies; // WORD
	/** @field cast=(HINSTANCE) */
	public int /*long*/ hInstance; // HINSTANCE
	public int /*long*/ lCustData; // LPARAM
	/** @field cast=(LPPRINTHOOKPROC) */
	public int /*long*/ lpfnPrintHook; // LPPRINTHOOKPROC
	/** @field cast=(LPPRINTHOOKPROC) */
	public int /*long*/ lpfnSetupHook; // LPSETUPHOOKPROC
	/** @field cast=(LPCTSTR) */
	public int /*long*/ lpPrintTemplateName; // LPCTSTR
	/** @field cast=(LPCTSTR) */
	public int /*long*/ lpSetupTemplateName; // LPCTSTR
	/** @field cast=(HGLOBAL) */
	public int /*long*/ hPrintTemplate; // HGLOBAL
	/** @field cast=(HGLOBAL) */
	public int /*long*/ hSetupTemplate; // HGLOBAL
	public static final int sizeof = OS.PRINTDLG_sizeof ();
}
