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

public class IWebFramePrivate extends IUnknown {

public IWebFramePrivate (long address) {
	super (address);
}

public int setInPrintingMode (int value, long printDC) {
	return COM.VtblCall (8, getAddress (), value, printDC);
}

public int getPrintedPageCount (long printDC, int[] pageCount) {
	return COM.VtblCall (9, getAddress (), printDC, pageCount);
}

public int spoolPages (long printDC, int startPage, int endPage, long[] ctx) {
	return COM.VtblCall (10, getAddress (), printDC, startPage, endPage, ctx);
}

}
