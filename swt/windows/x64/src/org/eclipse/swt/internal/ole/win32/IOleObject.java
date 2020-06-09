/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

public class IOleObject extends IUnknown
{
public IOleObject(long address) {
	super(address);
}
public int Advise(long pAdvSink, int[] pdwConnection) {
	return COM.VtblCall(19, address, pAdvSink, pdwConnection);
}
public int Close(int dwSaveOption) {
	return COM.VtblCall(6, address, dwSaveOption);
}
public int DoVerb(int iVerb, MSG lpmsg, long pActiveSite, int lindex, long hwndParent, RECT lprcPosRect) {
	return COM.VtblCall(11, address, iVerb, lpmsg, pActiveSite, lindex, hwndParent, lprcPosRect);
}
public int GetClientSite(long[] ppvClientSite) {
	return COM.VtblCall(4, address, ppvClientSite);
}
public int GetExtent(int dwDrawAspect, SIZE pSizel) {
	return COM.VtblCall(18, address, dwDrawAspect, pSizel);
}
public int SetClientSite(long pClientSite) {
	return COM.VtblCall(3, address, pClientSite);
}
public int SetExtent(int dwDrawAspect, SIZE pSizel) {
	return COM.VtblCall(17, address, dwDrawAspect, pSizel);
}
public int SetHostNames(String szContainerApp, String szContainerObj) {

	// create a null terminated array of char
	char[] buffer1 = null;
	if (szContainerApp != null) {
		int count1 = szContainerApp.length();
		buffer1 = new char[count1 + 1];
		szContainerApp.getChars(0, count1, buffer1, 0);
	}

	// create a null terminated array of char
	char[] buffer2 = null;
	if (szContainerObj != null) {
		int count2 = szContainerObj.length();
		buffer2 = new char[count2 + 1];
		szContainerObj.getChars(0, count2, buffer2, 0);
	}
	return COM.VtblCall(5, address, buffer1, buffer2);
}
public int Unadvise(int token) {
	return COM.VtblCall(20, address, token);
}
public int Update() {
	return COM.VtblCall(13, address);
}
}
