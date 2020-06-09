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
package org.eclipse.swt.internal.ole.win32;

import org.eclipse.swt.internal.win32.*;

public class IViewObject2 extends IUnknown
{
public IViewObject2(long address) {
	super(address);
}
public int GetExtent(int dwAspect, int lindex, long ptd, SIZE lpsizel) {
	return COM.VtblCall(9, address, dwAspect, lindex, ptd, lpsizel);
}
public int SetAdvise(int dwAspects, int dwAdvf, long pIAdviseSink) {
	return COM.VtblCall(7, address, dwAspects, dwAdvf, pIAdviseSink);
}
}
