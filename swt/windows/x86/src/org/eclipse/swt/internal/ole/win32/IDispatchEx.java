/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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

public class IDispatchEx extends IDispatch {

public IDispatchEx (int /*long*/ address) {
	super (address);
}

public int GetDispID (int /*long*/ bstrName, int grfdex, int[] pid) {
	return COM.VtblCall (7, address, bstrName, grfdex, pid);
}

public int InvokeEx (int id, int lcid, int wFlags, DISPPARAMS pdp, int /*long*/ pvarRes, EXCEPINFO pei, int /*long*/ pspCaller) {
	return COM.VtblCall (8, address, id, lcid, wFlags, pdp, pvarRes, pei, pspCaller);
}

public int DeleteMemberByName (int /*long*/ bstrName, int grfdex) {
	return OS.VtblCall (9, address, bstrName, grfdex);
}

public int DeleteMemberByDispID (int id) {
	return OS.VtblCall (10, address, id);
}

public int GetMemberProperties (int id, int grfdexFetch, int[] pgrfdex) {
	return COM.VtblCall (11, address, id, grfdexFetch, pgrfdex);
}

public int GetMemberName (int id, int /*long*/[] pbstrName) {
	return OS.VtblCall (12, address, id, pbstrName);
}

public int GetNextDispID (int grfdex, int id, int[] pid) {
	return COM.VtblCall (13, address, grfdex, id, pid);
}

public int GetNameSpaceParent (int /*long*/[] ppunk) {
	return OS.VtblCall (14, address, ppunk);
}
}
