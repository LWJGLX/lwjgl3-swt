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
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.cocoa;

public class NSInputManager extends NSObject {

public NSInputManager() {
	super();
}

public NSInputManager(long id) {
	super(id);
}

public NSInputManager(id id) {
	super(id);
}

public static NSInputManager currentInputManager() {
	long result = OS.objc_msgSend(OS.class_NSInputManager, OS.sel_currentInputManager);
	return result != 0 ? new NSInputManager(result) : null;
}

public boolean handleMouseEvent(NSEvent theMouseEvent) {
	return OS.objc_msgSend_bool(this.id, OS.sel_handleMouseEvent_, theMouseEvent != null ? theMouseEvent.id : 0);
}

public boolean wantsToHandleMouseEvents() {
	return OS.objc_msgSend_bool(this.id, OS.sel_wantsToHandleMouseEvents);
}

}
