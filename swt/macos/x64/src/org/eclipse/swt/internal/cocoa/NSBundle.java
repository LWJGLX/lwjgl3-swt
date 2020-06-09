/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

public class NSBundle extends NSObject {

public NSBundle() {
	super();
}

public NSBundle(long id) {
	super(id);
}

public NSBundle(id id) {
	super(id);
}

public static boolean loadNibFile(NSString fileName, NSDictionary context, long zone) {
	return OS.objc_msgSend_bool(OS.class_NSBundle, OS.sel_loadNibFile_externalNameTable_withZone_, fileName != null ? fileName.id : 0, context != null ? context.id : 0, zone);
}

public NSString bundleIdentifier() {
	long result = OS.objc_msgSend(this.id, OS.sel_bundleIdentifier);
	return result != 0 ? new NSString(result) : null;
}

public NSString bundlePath() {
	long result = OS.objc_msgSend(this.id, OS.sel_bundlePath);
	return result != 0 ? new NSString(result) : null;
}

public static NSBundle bundleWithIdentifier(NSString identifier) {
	long result = OS.objc_msgSend(OS.class_NSBundle, OS.sel_bundleWithIdentifier_, identifier != null ? identifier.id : 0);
	return result != 0 ? new NSBundle(result) : null;
}

public static NSBundle bundleWithPath(NSString path) {
	long result = OS.objc_msgSend(OS.class_NSBundle, OS.sel_bundleWithPath_, path != null ? path.id : 0);
	return result != 0 ? new NSBundle(result) : null;
}

public NSDictionary infoDictionary() {
	long result = OS.objc_msgSend(this.id, OS.sel_infoDictionary);
	return result != 0 ? new NSDictionary(result) : null;
}

public static NSBundle mainBundle() {
	long result = OS.objc_msgSend(OS.class_NSBundle, OS.sel_mainBundle);
	return result != 0 ? new NSBundle(result) : null;
}

public id objectForInfoDictionaryKey(NSString key) {
	long result = OS.objc_msgSend(this.id, OS.sel_objectForInfoDictionaryKey_, key != null ? key.id : 0);
	return result != 0 ? new id(result) : null;
}

public NSString pathForResource(NSString name, NSString ext) {
	long result = OS.objc_msgSend(this.id, OS.sel_pathForResource_ofType_, name != null ? name.id : 0, ext != null ? ext.id : 0);
	return result != 0 ? new NSString(result) : null;
}

public NSString pathForResource(NSString name, NSString ext, NSString subpath, NSString localizationName) {
	long result = OS.objc_msgSend(this.id, OS.sel_pathForResource_ofType_inDirectory_forLocalization_, name != null ? name.id : 0, ext != null ? ext.id : 0, subpath != null ? subpath.id : 0, localizationName != null ? localizationName.id : 0);
	return result != 0 ? new NSString(result) : null;
}

}
