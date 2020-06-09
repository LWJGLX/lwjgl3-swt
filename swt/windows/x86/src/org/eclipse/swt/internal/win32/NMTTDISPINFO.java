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

public abstract class NMTTDISPINFO extends NMHDR {
	/** @field cast=(void *) */
	public int /*long*/ lpszText;
	/** @field cast=(HINSTANCE) */
	public int /*long*/ hinst;
	public int uFlags;
	public int /*long*/ lParam;
	public static final int sizeof = OS.IsUnicode ? OS.NMTTDISPINFOW_sizeof () : OS.NMTTDISPINFOA_sizeof ();
}
