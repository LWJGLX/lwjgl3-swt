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

public class IWebOpenPanelResultListener extends IUnknown {

public IWebOpenPanelResultListener (int /*long*/ address) {
	super (address);
}

public int chooseFilename (int /*long*/ fileName) {
	return OS.VtblCall (3, getAddress (), fileName);
}

public int cancel () {
	return OS.VtblCall (4, getAddress ());
}

}
