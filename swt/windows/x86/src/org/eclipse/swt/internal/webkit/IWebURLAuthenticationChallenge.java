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

public class IWebURLAuthenticationChallenge extends IUnknown {

public IWebURLAuthenticationChallenge (int /*long*/ address) {
	super (address);
}

public int previousFailureCount (int[] result) {
	return OS.VtblCall (7, getAddress (), result);
}

public int proposedCredential (int /*long*/[] result) {
	return OS.VtblCall (8, getAddress (), result);
}

public int protectionSpace (int /*long*/[] result) {
	return OS.VtblCall (9, getAddress (), result);
}

public int sender (int /*long*/[] sender) {
	return OS.VtblCall (10, getAddress (), sender);
}

}
