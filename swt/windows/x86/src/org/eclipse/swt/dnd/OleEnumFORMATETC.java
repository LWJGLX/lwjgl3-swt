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
package org.eclipse.swt.dnd;

import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.ole.win32.*;
import org.eclipse.swt.internal.win32.*;

final class OleEnumFORMATETC {

	private COMObject iUnknown;
	private COMObject iEnumFORMATETC;

	private int refCount;
	private int index;

	private FORMATETC[] formats;

OleEnumFORMATETC() {

	createCOMInterfaces();

}
int AddRef() {
	refCount++;
	return refCount;
}
private void createCOMInterfaces() {
	// register each of the interfaces that this object implements
	iUnknown = new COMObject(new int[] {2, 0, 0}){
		@Override
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		@Override
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		@Override
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
	};
	iEnumFORMATETC = new COMObject(new int[] {2, 0, 0, 3, 1, 0, 1}){
		@Override
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		@Override
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		@Override
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		@Override
		public int /*long*/ method3(int /*long*/[] args) {return Next((int)/*64*/args[0], args[1], args[2]);}
		@Override
		public int /*long*/ method4(int /*long*/[] args) {return Skip((int)/*64*/args[0]);}
		@Override
		public int /*long*/ method5(int /*long*/[] args) {return Reset();}
		// method6 Clone - not implemented
	};
}
private void disposeCOMInterfaces() {

	if (iUnknown != null)
		iUnknown.dispose();
	iUnknown = null;

	if (iEnumFORMATETC != null)
		iEnumFORMATETC.dispose();
	iEnumFORMATETC = null;
}
int /*long*/ getAddress() {
	return iEnumFORMATETC.getAddress();
}
private FORMATETC[] getNextItems(int numItems){

	if (formats == null || numItems < 1) return null;

	int endIndex = index + numItems - 1;
	if (endIndex > (formats.length - 1)) endIndex = formats.length - 1;
	if (index > endIndex) return null;

	FORMATETC[] items =  new FORMATETC[endIndex - index + 1];
	for (int i = 0; i < items.length; i++){
		items[i] = formats[index];
		index++;
	}

	return items;
}
private int Next(int celt, int /*long*/ rgelt, int /*long*/ pceltFetched) {
	/* Retrieves the next celt items in the enumeration sequence.
	   If there are fewer than the requested number of elements left in the sequence,
	   it retrieves the remaining elements.
	   The number of elements actually retrieved is returned through pceltFetched
	   (unless the caller passed in NULL for that parameter).
	*/

	if (rgelt == 0)	return COM.E_INVALIDARG;
	if (pceltFetched == 0 && celt != 1) return COM.E_INVALIDARG;

	FORMATETC[] nextItems = getNextItems(celt);
	if (nextItems != null) {
		for (int i = 0; i < nextItems.length; i++) {
			COM.MoveMemory(rgelt + i*FORMATETC.sizeof, nextItems[i], FORMATETC.sizeof);
		}

		if (pceltFetched != 0)
			OS.MoveMemory(pceltFetched, new int[] {nextItems.length}, 4);

		if (nextItems.length == celt) return COM.S_OK;

	} else {
		if (pceltFetched != 0)
			OS.MoveMemory(pceltFetched, new int[] {0}, 4);
		COM.MoveMemory(rgelt, new FORMATETC(), FORMATETC.sizeof);

	}
	return COM.S_FALSE;
}
private int QueryInterface(int /*long*/ riid, int /*long*/ ppvObject) {

	if (riid == 0 || ppvObject == 0) return COM.E_NOINTERFACE;

	GUID guid = new GUID();
	COM.MoveMemory(guid, riid, GUID.sizeof);

	if (COM.IsEqualGUID(guid, COM.IIDIUnknown)) {
		OS.MoveMemory(ppvObject, new int /*long*/[] {iUnknown.getAddress()}, C.PTR_SIZEOF);
		AddRef();
		return COM.S_OK;
	}
	if (COM.IsEqualGUID(guid, COM.IIDIEnumFORMATETC)) {
		OS.MoveMemory(ppvObject, new int /*long*/[] {iEnumFORMATETC.getAddress()}, C.PTR_SIZEOF);
		AddRef();
		return COM.S_OK;
	}
	OS.MoveMemory(ppvObject, new int /*long*/[] {0}, C.PTR_SIZEOF);
	return COM.E_NOINTERFACE;
}
int Release() {
	refCount--;

	if (refCount == 0) {
		disposeCOMInterfaces();
		if (COM.FreeUnusedLibraries) {
			COM.CoFreeUnusedLibraries();
		}
	}

	return refCount;
}
private int Reset() {
	//Resets the enumeration sequence to the beginning.
	index = 0;
	return COM.S_OK;
}
void setFormats(FORMATETC[] newFormats) {
	formats = newFormats;
	index = 0;
}
private int Skip(int celt) {
	//Skips over the next specified number of elements in the enumeration sequence.
	if (celt < 1 ) return COM.E_INVALIDARG;

	index += celt;
	if (index > (formats.length - 1)){
		index = formats.length - 1;
		return COM.S_FALSE;
	}
	return COM.S_OK;
}
}
