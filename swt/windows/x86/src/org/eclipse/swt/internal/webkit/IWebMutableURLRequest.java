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


import org.eclipse.swt.internal.win32.*;

public class IWebMutableURLRequest extends IWebURLRequest {

public IWebMutableURLRequest (int /*long*/ address) {
	super (address);
}

//public int setHTTPBody (int /*long*/ data) {
//	return COM.VtblCall (21, getAddress (), data);
//}

//public int setHTTPBodyStream (int /*long*/ data) {
//	return COM.VtblCall (22, getAddress (), data);
//}

public int setHTTPMethod (int post) {
	return OS.VtblCall (23, getAddress (), post);
}

public int setURL (int /*long*/ theUrl) {
	return OS.VtblCall (27, getAddress (), theUrl);
}

public int setValue (int /*long*/ value, int /*long*/ field) {
	return OS.VtblCall (28, getAddress (), value, field);
}

public int setAllowsAnyHTTPSCertificate () {
	return OS.VtblCall (29, getAddress ());
}

}
