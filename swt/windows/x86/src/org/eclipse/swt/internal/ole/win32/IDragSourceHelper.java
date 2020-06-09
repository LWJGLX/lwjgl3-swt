/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
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
package org.eclipse.swt.internal.ole.win32;

import org.eclipse.swt.internal.win32.*;

public class IDragSourceHelper extends IUnknown {
public IDragSourceHelper(int /*long*/ address) {
	super(address);
}
public int InitializeFromBitmap(SHDRAGIMAGE pshdi, int /*long*/ pDataObject) {
	return COM.VtblCall(3, address, pshdi, pDataObject);
}
public int InitializeFromWindow(int /*long*/ hwnd, POINT ppt, int /*long*/ pDataObject) {
	return COM.VtblCall(4, address, hwnd, ppt, pDataObject);
}
}
