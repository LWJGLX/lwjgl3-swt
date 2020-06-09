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

public class NSOutlineView extends NSTableView {

public NSOutlineView() {
	super();
}

public NSOutlineView(long id) {
	super(id);
}

public NSOutlineView(id id) {
	super(id);
}

public void collapseItem(id item) {
	OS.objc_msgSend(this.id, OS.sel_collapseItem_, item != null ? item.id : 0);
}

public void collapseItem(id item, boolean collapseChildren) {
	OS.objc_msgSend(this.id, OS.sel_collapseItem_collapseChildren_, item != null ? item.id : 0, collapseChildren);
}

public void expandItem(id item) {
	OS.objc_msgSend(this.id, OS.sel_expandItem_, item != null ? item.id : 0);
}

public void expandItem(id item, boolean expandChildren) {
	OS.objc_msgSend(this.id, OS.sel_expandItem_expandChildren_, item != null ? item.id : 0, expandChildren);
}

public NSRect frameOfOutlineCellAtRow(long row) {
	NSRect result = new NSRect();
	OS.objc_msgSend_stret(result, this.id, OS.sel_frameOfOutlineCellAtRow_, row);
	return result;
}

public double indentationPerLevel() {
	return OS.objc_msgSend_fpret(this.id, OS.sel_indentationPerLevel);
}

public boolean isItemExpanded(id item) {
	return OS.objc_msgSend_bool(this.id, OS.sel_isItemExpanded_, item != null ? item.id : 0);
}

public id itemAtRow(long row) {
	long result = OS.objc_msgSend(this.id, OS.sel_itemAtRow_, row);
	return result != 0 ? new id(result) : null;
}

public long levelForItem(id item) {
	return OS.objc_msgSend(this.id, OS.sel_levelForItem_, item != null ? item.id : 0);
}

public void reloadItem(id item, boolean reloadChildren) {
	OS.objc_msgSend(this.id, OS.sel_reloadItem_reloadChildren_, item != null ? item.id : 0, reloadChildren);
}

public long rowForItem(id item) {
	return OS.objc_msgSend(this.id, OS.sel_rowForItem_, item != null ? item.id : 0);
}

public void setAutoresizesOutlineColumn(boolean autoresizesOutlineColumn) {
	OS.objc_msgSend(this.id, OS.sel_setAutoresizesOutlineColumn_, autoresizesOutlineColumn);
}

public void setAutosaveExpandedItems(boolean autosaveExpandedItems) {
	OS.objc_msgSend(this.id, OS.sel_setAutosaveExpandedItems_, autosaveExpandedItems);
}

public void setDropItem(id item, long index) {
	OS.objc_msgSend(this.id, OS.sel_setDropItem_dropChildIndex_, item != null ? item.id : 0, index);
}

public void setOutlineTableColumn(NSTableColumn outlineTableColumn) {
	OS.objc_msgSend(this.id, OS.sel_setOutlineTableColumn_, outlineTableColumn != null ? outlineTableColumn.id : 0);
}

public static long cellClass() {
	return OS.objc_msgSend(OS.class_NSOutlineView, OS.sel_cellClass);
}

public static void setCellClass(long factoryId) {
	OS.objc_msgSend(OS.class_NSOutlineView, OS.sel_setCellClass_, factoryId);
}

}
