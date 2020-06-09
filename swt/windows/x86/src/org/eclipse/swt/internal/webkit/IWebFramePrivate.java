/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.swt.internal.webkit;


import org.eclipse.swt.internal.ole.win32.*;
import org.eclipse.swt.internal.win32.*;

public class IWebFramePrivate extends IUnknown {

public IWebFramePrivate (int /*long*/ address) {
	super (address);
}

public int setInPrintingMode (int value, int /*long*/ printDC) {
	return OS.VtblCall (8, getAddress (), value, printDC);
}

public int getPrintedPageCount (int /*long*/ printDC, int[] pageCount) {
	return OS.VtblCall (9, getAddress (), printDC, pageCount);
}

public int spoolPages (int /*long*/ printDC, int startPage, int endPage, int /*long*/[] ctx) {
	return OS.VtblCall (10, getAddress (), printDC, startPage, endPage, ctx);
}

}
