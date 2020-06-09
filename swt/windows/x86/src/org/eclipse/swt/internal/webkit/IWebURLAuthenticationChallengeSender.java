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

public class IWebURLAuthenticationChallengeSender extends IUnknown {

public IWebURLAuthenticationChallengeSender (int /*long*/ address) {
	super (address);
}

public int cancelAuthenticationChallenge (int /*long*/ challenge) {
	return OS.VtblCall (3, getAddress (), challenge);
}

public int useCredential (int /*long*/ credential, int /*long*/ challenge) {
	return OS.VtblCall (5, getAddress (), credential, challenge);
}

}
