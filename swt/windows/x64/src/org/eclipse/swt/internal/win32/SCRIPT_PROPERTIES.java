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

public class SCRIPT_PROPERTIES {
	public short langid;
	public boolean fNumeric;
	public boolean fComplex;
	public boolean fNeedsWordBreaking;
	public boolean fNeedsCaretInfo;
	public byte bCharSet;
	public boolean fControl;
	public boolean fPrivateUseArea;
	public boolean fNeedsCharacterJustify;
	public boolean fInvalidGlyph;
	public boolean fInvalidLogAttr;
	public boolean fCDM;
	public boolean fAmbiguousCharSet;
	public boolean fClusterSizeVaries;
	public boolean fRejectInvalid;
	public static final int sizeof = OS.SCRIPT_PROPERTIES_sizeof ();
}
