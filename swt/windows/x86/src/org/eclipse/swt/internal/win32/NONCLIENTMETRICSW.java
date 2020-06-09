/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

public class NONCLIENTMETRICSW extends NONCLIENTMETRICS {
	public LOGFONTW lfCaptionFont = new LOGFONTW ();
	public LOGFONTW lfSmCaptionFont = new LOGFONTW ();
	public LOGFONTW lfMenuFont = new LOGFONTW ();
	public LOGFONTW lfStatusFont = new LOGFONTW ();
	public LOGFONTW lfMessageFont = new LOGFONTW ();
	public static final int sizeof = OS.NONCLIENTMETRICSW_sizeof ();
}

