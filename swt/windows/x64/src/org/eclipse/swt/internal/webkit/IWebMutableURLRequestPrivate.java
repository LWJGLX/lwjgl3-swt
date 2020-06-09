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

public class IWebMutableURLRequestPrivate extends IUnknown {

public IWebMutableURLRequestPrivate (long address) {
	super (address);
}

//public int setClientCertificate (long cert) {
//return COM.VtblCall (3, getAddress (), cert);
//}

public int cfRequest () {
	return COM.VtblCall (4, getAddress ());
}

}
