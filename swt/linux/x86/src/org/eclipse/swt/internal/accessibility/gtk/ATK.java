/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others. All rights reserved.
 * The contents of this file are made available under the terms
 * of the GNU Lesser General Public License (LGPL) Version 2.1 that
 * accompanies this distribution (lgpl-v21.txt).  The LGPL is also
 * available at http://www.gnu.org/licenses/lgpl.html.  If the version
 * of the LGPL at http://www.gnu.org is different to the version of
 * the LGPL accompanying this distribution and there is any conflict
 * between the two license versions, the terms of the LGPL accompanying
 * this distribution shall govern.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.accessibility.gtk;


import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;

public class ATK extends OS {
	static {
		Library.loadLibrary("swt-atk");
	}

	/** Constants */
	public static final int ATK_RELATION_NULL = 0;
	public static final int ATK_RELATION_CONTROLLED_BY = 1;
	public static final int ATK_RELATION_CONTROLLER_FOR = 2;
	public static final int ATK_RELATION_LABEL_FOR = 3;
	public static final int ATK_RELATION_LABELLED_BY = 4;
	public static final int ATK_RELATION_MEMBER_OF = 5;
	public static final int ATK_RELATION_NODE_CHILD_OF = 6;
	public static final int ATK_RELATION_FLOWS_TO = 7;
	public static final int ATK_RELATION_FLOWS_FROM = 8;
	public static final int ATK_RELATION_SUBWINDOW_OF = 9;
	public static final int ATK_RELATION_EMBEDS = 10;
	public static final int ATK_RELATION_EMBEDDED_BY = 11;
	public static final int ATK_RELATION_POPUP_FOR = 12;
	public static final int ATK_RELATION_PARENT_WINDOW_OF = 13;
	public static final int ATK_RELATION_DESCRIBED_BY = 14;
	public static final int ATK_RELATION_DESCRIPTION_FOR = 15;
	public static final int ATK_ROLE_ALERT = 2;
	public static final int ATK_ROLE_ANIMATION = 3;
	public static final int ATK_ROLE_CALENDAR = 4;
	public static final int ATK_ROLE_CANVAS = 6;
	public static final int ATK_ROLE_CHECK_BOX = 7;
	public static final int ATK_ROLE_CHECK_MENU_ITEM = 8;
	public static final int ATK_ROLE_COMBO_BOX = 11;
	public static final int ATK_ROLE_DATE_EDITOR = 12;
	public static final int ATK_ROLE_DIALOG = 16;
	public static final int ATK_ROLE_DRAWING_AREA = 18;
	public static final int ATK_ROLE_WINDOW = 68;
	public static final int ATK_ROLE_LABEL = 28;
	public static final int ATK_ROLE_LIST = 30;
	public static final int ATK_ROLE_LIST_ITEM = 31;
	public static final int ATK_ROLE_MENU = 32;
	public static final int ATK_ROLE_MENU_BAR = 33;
	public static final int ATK_ROLE_MENU_ITEM = 34;
	public static final int ATK_ROLE_PAGE_TAB = 36;
	public static final int ATK_ROLE_PAGE_TAB_LIST = 37;
	public static final int ATK_ROLE_PANEL = 38;
	public static final int ATK_ROLE_PROGRESS_BAR = 41;
	public static final int ATK_ROLE_PUSH_BUTTON = 42;
	public static final int ATK_ROLE_RADIO_BUTTON = 43;
	public static final int ATK_ROLE_RADIO_MENU_ITEM = 44;
	public static final int ATK_ROLE_SCROLL_BAR = 47;
	public static final int ATK_ROLE_SEPARATOR = 49;
	public static final int ATK_ROLE_SLIDER = 50;
	public static final int ATK_ROLE_SPIN_BUTTON = 52;
	public static final int ATK_ROLE_STATUSBAR = 53;
	public static final int ATK_ROLE_TABLE = 54;
	public static final int ATK_ROLE_TABLE_CELL = 55;
	public static final int ATK_ROLE_TABLE_COLUMN_HEADER = 56;
	public static final int ATK_ROLE_TABLE_ROW_HEADER = 57;
	public static final int ATK_ROLE_TEXT = 60;
	public static final int ATK_ROLE_TOOL_BAR = 62;
	public static final int ATK_ROLE_TOOL_TIP = 63;
	public static final int ATK_ROLE_TREE = 64;
	public static final int ATK_ROLE_HEADER = 69;
	public static final int ATK_ROLE_FOOTER = 70;
	public static final int ATK_ROLE_PARAGRAPH = 71;
	public static final int ATK_ROLE_FORM = 85;
	public static final int ATK_ROLE_HEADING = 81;
	public static final int ATK_ROLE_DOCUMENT_FRAME = 80;
	public static final int ATK_ROLE_TABLE_ROW = 88; //since atk 2.1.0
	public static final int ATK_ROLE_IMAGE = 26;
	public static final int ATK_ROLE_PAGE = 82;
	public static final int ATK_ROLE_SECTION = 83;
	public static final int ATK_ROLE_UNKNOWN = 66;
	public static final int ATK_STATE_ACTIVE = 1;
	public static final int ATK_STATE_ARMED = 2;
	public static final int ATK_STATE_BUSY = 3;
	public static final int ATK_STATE_CHECKED = 4;
	public static final int ATK_STATE_DEFUNCT = 5;
	public static final int ATK_STATE_EDITABLE = 6;
	public static final int ATK_STATE_ENABLED = 7;
	public static final int ATK_STATE_EXPANDED = 9;
	public static final int ATK_STATE_FOCUSABLE = 10;
	public static final int ATK_STATE_FOCUSED = 11;
	public static final int ATK_STATE_MULTI_LINE = 15;
	public static final int ATK_STATE_MULTISELECTABLE = 16;
	public static final int ATK_STATE_PRESSED = 18;
	public static final int ATK_STATE_RESIZABLE = 19;
	public static final int ATK_STATE_SELECTABLE = 20;
	public static final int ATK_STATE_SELECTED = 21;
	public static final int ATK_STATE_SHOWING = 23;
	public static final int ATK_STATE_SINGLE_LINE = 24;
	public static final int ATK_STATE_TRANSIENT = 26;
	public static final int ATK_STATE_REQUIRED = 32;
	public static final int ATK_STATE_INVALID_ENTRY = 33;
	public static final int ATK_STATE_SUPPORTS_AUTOCOMPLETION = 34;
	public static final int ATK_STATE_VISIBLE = 28;
	public static final int ATK_TEXT_BOUNDARY_CHAR = 0;
	public static final int ATK_TEXT_BOUNDARY_WORD_START = 1;
	public static final int ATK_TEXT_BOUNDARY_WORD_END = 2;
	public static final int ATK_TEXT_BOUNDARY_SENTENCE_START = 3;
	public static final int ATK_TEXT_BOUNDARY_SENTENCE_END = 4;
	public static final int ATK_TEXT_BOUNDARY_LINE_START = 5;
	public static final int ATK_TEXT_BOUNDARY_LINE_END = 6;
	/* TODO_a11y: use the ATK_TEXT_GRANULARITY_* constants once
	 * Orca is updated to use the new API.
	 */
	public static final int ATK_TEXT_GRANULARITY_CHAR = 0;
	public static final int ATK_TEXT_GRANULARITY_WORD = 1;
	public static final int ATK_TEXT_GRANULARITY_SENTENCE = 2;
	public static final int ATK_TEXT_GRANULARITY_LINE = 3;
	public static final int ATK_TEXT_GRANULARITY_PARAGRAPH = 4;
	public static final int ATK_TEXT_CLIP_NONE = 0;
	public static final int ATK_TEXT_CLIP_MIN = 1;
	public static final int ATK_TEXT_CLIP_MAX = 2;
	public static final int ATK_TEXT_CLIP_BOTH = 3;
	public static final int ATK_TEXT_ATTR_LEFT_MARGIN = 1;
	public static final int ATK_TEXT_ATTR_RIGHT_MARGIN = 2;
	public static final int ATK_TEXT_ATTR_INDENT = 3;
	public static final int ATK_TEXT_ATTR_INVISIBLE = 4;
	public static final int ATK_TEXT_ATTR_EDITABLE = 5;
	public static final int ATK_TEXT_ATTR_PIXELS_ABOVE_LINES = 6;
	public static final int ATK_TEXT_ATTR_PIXELS_BELOW_LINES = 7;
	public static final int ATK_TEXT_ATTR_PIXELS_INSIDE_WRAP = 8;
	public static final int ATK_TEXT_ATTR_BG_FULL_HEIGHT = 9;
	public static final int ATK_TEXT_ATTR_RISE = 10;
	public static final int ATK_TEXT_ATTR_UNDERLINE = 11;
	public static final int ATK_TEXT_ATTR_STRIKETHROUGH = 12;
	public static final int ATK_TEXT_ATTR_SIZE = 13;
	public static final int ATK_TEXT_ATTR_SCALE = 14;
	public static final int ATK_TEXT_ATTR_WEIGHT = 15;
	public static final int ATK_TEXT_ATTR_LANGUAGE = 16;
	public static final int ATK_TEXT_ATTR_FAMILY_NAME = 17;
	public static final int ATK_TEXT_ATTR_BG_COLOR = 18;
	public static final int ATK_TEXT_ATTR_FG_COLOR = 19;
	public static final int ATK_TEXT_ATTR_BG_STIPPLE = 20;
	public static final int ATK_TEXT_ATTR_FG_STIPPLE = 21;
	public static final int ATK_TEXT_ATTR_WRAP_MODE = 22;
	public static final int ATK__TEXT_ATTR_DIRECTION = 23;
	public static final int ATK_TEXT_ATTR_JUSTIFICATION = 24;
	public static final int ATK_TEXT_ATTR_STRETCH = 25;
	public static final int ATK_TEXT_ATTR_VARIANT = 26;
	public static final int ATK_TEXT_ATTR_STYLE = 27;
	public static final int ATK_XY_WINDOW = 1;

	/** Signals */
	public static final byte[] selection_changed = OS.ascii ("selection_changed");
	public static final byte[] active_descendant_changed = OS.ascii ("active_descendant_changed");
	public static final byte[] text_changed_insert = OS.ascii ("text_changed::insert");
	public static final byte[] text_changed_delete = OS.ascii ("text_changed::delete");
	public static final byte[] text_caret_moved = OS.ascii ("text_caret_moved");
	public static final byte[] text_selection_changed = OS.ascii ("text_selection_changed");
	public static final byte[] load_complete = OS.ascii ("load-complete");
	public static final byte[] load_stopped = OS.ascii ("load-stopped");
	public static final byte[] reload = OS.ascii ("reload");
	public static final byte[] state_change = OS.ascii ("state-change");
	public static final byte[] bounds_changed = OS.ascii ("bounds-changed");
	public static final byte[] link_activated = OS.ascii ("link-activated");
	public static final byte[] link_selected = OS.ascii ("link-selected");
	public static final byte[] attributes_changed = OS.ascii ("attributes-changed");
	public static final byte[] text_attributes_changed = OS.ascii ("text-attributes-changed");
	public static final byte[] column_deleted = OS.ascii ("column-deleted");
	public static final byte[] column_inserted = OS.ascii ("column-inserted");
	public static final byte[] row_deleted = OS.ascii ("row-deleted");
	public static final byte[] row_inserted = OS.ascii ("row-inserted");
	public static final byte[] focus_event = OS.ascii ("focus-event");

	/** Properties */
	public static final byte[] accessible_name = OS.ascii ("accessible-name");
	public static final byte[] accessible_description = OS.ascii ("accessible-description");
	public static final byte[] accessible_value = OS.ascii ("accessible-value");
	public static final byte[] end_index = OS.ascii ("end-index");
	public static final byte[] number_of_anchors = OS.ascii ("number-of-anchors");
	public static final byte[] selected_link = OS.ascii ("selected-link");
	public static final byte[] start_index = OS.ascii ("start-index");
	public static final byte[] accessible_hypertext_nlinks = OS.ascii ("accessible-hypertext-nlinks");
	public static final byte[] accessible_table_caption_object = OS.ascii ("accessible-table-caption-object");
	public static final byte[] accessible_table_column_description = OS.ascii ("accessible-table-column-description");
	public static final byte[] accessible_table_column_header = OS.ascii ("accessible-table-column-header");
	public static final byte[] accessible_table_row_description = OS.ascii ("accessible-table-row-description");
	public static final byte[] accessible_table_row_header = OS.ascii ("accessible-table-row-header");
	public static final byte[] accessible_table_summary = OS.ascii ("accessible-table-summary");

/** 64 bit */
public static final native int AtkObjectFactory_sizeof ();
public static final native int AtkObjectFactoryClass_sizeof ();
public static final native int AtkAttribute_sizeof ();
public static final native int AtkTextRange_sizeof ();
public static final native int AtkTextRectangle_sizeof ();

/** Natives */

/** @method flags=const */
public static final native int /*long*/ ATK_TYPE_ACTION ();
/** @method flags=const */
public static final native int /*long*/ ATK_TYPE_COMPONENT ();
/** @method flags=const */
public static final native int /*long*/ ATK_TYPE_EDITABLE_TEXT ();
/** @method flags=const */
public static final native int /*long*/ ATK_TYPE_HYPERTEXT ();
/** @method flags=const */
public static final native int /*long*/ ATK_TYPE_SELECTION ();
/** @method flags=const */
public static final native int /*long*/ ATK_TYPE_TABLE ();
/** @method flags=const */
public static final native int /*long*/ ATK_TYPE_TEXT ();
/** @method flags=const */
public static final native int /*long*/ ATK_TYPE_VALUE ();
/** @method flags=const */
public static final native int /*long*/ ATK_TYPE_OBJECT_FACTORY ();
public static final native boolean ATK_IS_NO_OP_OBJECT_FACTORY (int /*long*/ obj);
public static final native int /*long*/ _ATK_ACTION_GET_IFACE (int /*long*/ obj);
public static final int /*long*/ ATK_ACTION_GET_IFACE (int /*long*/ obj) {
	lock.lock();
	try {
		return _ATK_ACTION_GET_IFACE(obj);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _ATK_COMPONENT_GET_IFACE(int /*long*/ atkHandle);
public static final int /*long*/ ATK_COMPONENT_GET_IFACE(int /*long*/ atkHandle) {
	lock.lock();
	try {
		return _ATK_COMPONENT_GET_IFACE(atkHandle);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _ATK_OBJECT_FACTORY_CLASS (int /*long*/ klass);
public static final native int /*long*/ _ATK_SELECTION_GET_IFACE (int /*long*/ obj);
public static final int /*long*/ ATK_SELECTION_GET_IFACE (int /*long*/ obj) {
	lock.lock();
	try {
		return _ATK_SELECTION_GET_IFACE(obj);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _ATK_EDITABLE_TEXT_GET_IFACE (int /*long*/ handle);
public static final int /*long*/ ATK_EDITABLE_TEXT_GET_IFACE (int /*long*/ handle) {
	lock.lock();
	try {
		return _ATK_EDITABLE_TEXT_GET_IFACE(handle);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _ATK_HYPERTEXT_GET_IFACE (int /*long*/ handle);
public static final int /*long*/ ATK_HYPERTEXT_GET_IFACE (int /*long*/ handle) {
	lock.lock();
	try {
		return _ATK_HYPERTEXT_GET_IFACE(handle);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _ATK_TABLE_GET_IFACE (int /*long*/ handle);
public static final int /*long*/ ATK_TABLE_GET_IFACE (int /*long*/ handle) {
	lock.lock();
	try {
		return _ATK_TABLE_GET_IFACE(handle);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _ATK_TEXT_GET_IFACE (int /*long*/ handle);
public static final int /*long*/ ATK_TEXT_GET_IFACE (int /*long*/ handle) {
	lock.lock();
	try {
		return _ATK_TEXT_GET_IFACE(handle);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _ATK_VALUE_GET_IFACE (int /*long*/ handle);
public static final int /*long*/ ATK_VALUE_GET_IFACE (int /*long*/ handle) {
	lock.lock();
	try {
		return _ATK_VALUE_GET_IFACE(handle);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _atk_get_default_registry ();
public static final int /*long*/ atk_get_default_registry () {
	lock.lock();
	try {
		return _atk_get_default_registry();
	} finally {
		lock.unlock();
	}
}
/** @param factory cast=(AtkObjectFactory *) */
public static final native int /*long*/ _atk_object_factory_get_accessible_type (int /*long*/ factory);
public static final int /*long*/ atk_object_factory_get_accessible_type (int /*long*/ factory) {
	lock.lock();
	try {
		return _atk_object_factory_get_accessible_type(factory);
	} finally {
		lock.unlock();
	}
}
/**
 * @param accessible cast=(AtkObject *)
 * @param data cast=(gpointer)
 */
public static final native void _atk_object_initialize (int /*long*/ accessible, int /*long*/ data);
public static final void atk_object_initialize (int /*long*/ accessible, int /*long*/ data) {
	lock.lock();
	try {
		_atk_object_initialize(accessible, data);
	} finally {
		lock.unlock();
	}
}
/** @param accessible cast=(AtkObject *) */
public static final native void _atk_object_notify_state_change (int /*long*/ accessible, int state, boolean value);
public static final void atk_object_notify_state_change (int /*long*/ accessible, int state, boolean value) {
	lock.lock();
	try {
		_atk_object_notify_state_change(accessible, state, value);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(AtkObject *)
 * @param relationship cast=(AtkRelationType)
 * @param target cast=(AtkObject *)
 */
public static final native boolean _atk_object_add_relationship (int /*long*/ object, int relationship, int /*long*/ target);
public static final boolean atk_object_add_relationship (int /*long*/ object, int relationship, int /*long*/ target) {
	lock.lock();
	try {
		return _atk_object_add_relationship(object, relationship, target);
	} finally {
		lock.unlock();
	}
}
/**
 * @param object cast=(AtkObject *)
 * @param relationship cast=(AtkRelationType)
 * @param target cast=(AtkObject *)
 */
public static final native boolean _atk_object_remove_relationship (int /*long*/ object, int relationship, int /*long*/ target);
public static final boolean atk_object_remove_relationship (int /*long*/ object, int relationship, int /*long*/ target) {
	lock.lock();
	try {
		return _atk_object_remove_relationship(object, relationship, target);
	} finally {
		lock.unlock();
	}
}
/**
 * @method flags=dynamic
 * @param name cast=(const gchar *)
 */
public static final native int _atk_role_register (byte[] name); //Note, deprecated, do not use. See atk docu.
public static final int atk_role_register (byte[] name) {
	lock.lock();
	try {
		return _atk_role_register(name);
	} finally {
		lock.unlock();
	}
}
/**
 * @param registry cast=(AtkRegistry *)
 * @param type cast=(GType)
 */
public static final native int /*long*/ _atk_registry_get_factory (int /*long*/ registry, int /*long*/ type);
public static final int /*long*/ atk_registry_get_factory (int /*long*/ registry, int /*long*/ type) {
	lock.lock();
	try {
		return _atk_registry_get_factory(registry, type);
	} finally {
		lock.unlock();
	}
}
/**
 * @param registry cast=(AtkRegistry *)
 * @param type cast=(GType)
 * @param factory_type cast=(GType)
 */
public static final native void _atk_registry_set_factory_type (int /*long*/ registry, int /*long*/ type, int /*long*/ factory_type);
public static final void atk_registry_set_factory_type (int /*long*/ registry, int /*long*/ type, int /*long*/ factory_type) {
	lock.lock();
	try {
		_atk_registry_set_factory_type(registry, type, factory_type);
	} finally {
		lock.unlock();
	}
}
/**
 * @param set cast=(AtkStateSet *)
 * @param type cast=(AtkStateType)
 */
public static final native boolean _atk_state_set_add_state (int /*long*/ set, int type);
public static final boolean atk_state_set_add_state (int /*long*/ set, int type) {
	lock.lock();
	try {
		return _atk_state_set_add_state(set, type);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _atk_text_attribute_get_name (int attr);
public static final int /*long*/ atk_text_attribute_get_name (int attr) {
	lock.lock();
	try {
		return _atk_text_attribute_get_name(attr);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _atk_text_attribute_get_value (int attr, int index);
public static final int /*long*/ atk_text_attribute_get_value (int attr, int index) {
	lock.lock();
	try {
		return _atk_text_attribute_get_value(attr, index);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _call (int /*long*/ function, int /*long*/ arg0);
public static final int /*long*/ call (int /*long*/ function, int /*long*/ arg0) {
	lock.lock();
	try {
		return _call(function, arg0);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1);
public static final int /*long*/ call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1) {
	lock.lock();
	try {
		return _call(function, arg0, arg1);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2);
public static final int /*long*/ call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2) {
	lock.lock();
	try {
		return _call(function, arg0, arg1, arg2);
	} finally {
		lock.unlock();
	}
}
public static final native int /*long*/ _call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ arg4);
public static final int /*long*/ call (int /*long*/ function, int /*long*/ arg0, int /*long*/ arg1, int /*long*/ arg2, int /*long*/ arg3, int /*long*/ arg4) {
	lock.lock();
	try {
		return _call(function, arg0, arg1, arg2, arg3, arg4);
	} finally {
		lock.unlock();
	}
}
public static final native void memmove (AtkActionIface dest, int /*long*/ src);
public static final native void memmove (AtkComponentIface dest, int /*long*/ src);
public static final native void memmove (AtkEditableTextIface dest, int /*long*/ src);
public static final native void memmove (AtkHypertextIface dest, int /*long*/ src);
public static final native void memmove (AtkObjectClass dest, int /*long*/ src);
public static final native void memmove (AtkObjectFactoryClass  dest, int /*long*/ src);
public static final native void memmove (AtkSelectionIface dest, int /*long*/ src);
public static final native void memmove (AtkTableIface dest, int /*long*/ src);
public static final native void memmove (AtkTextIface dest, int /*long*/ src);
public static final native void memmove (AtkValueIface dest, int /*long*/ src);
public static final native void memmove (int /*long*/ dest, AtkActionIface src);
public static final native void memmove (int /*long*/ dest, AtkComponentIface src);
public static final native void memmove (int /*long*/ dest, AtkEditableTextIface src);
public static final native void memmove (int /*long*/ dest, AtkHypertextIface src);
public static final native void memmove (int /*long*/ dest, AtkObjectClass src);
public static final native void memmove (int /*long*/ dest, AtkObjectFactoryClass src);
public static final native void memmove (int /*long*/ dest, AtkSelectionIface src);
public static final native void memmove (int /*long*/ dest, AtkTableIface src);
public static final native void memmove (int /*long*/ dest, AtkTextIface src);
public static final native void memmove (int /*long*/ dest, AtkValueIface src);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove (int /*long*/ dest, AtkTextRectangle src, int size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove (AtkTextRectangle dest, int /*long*/ src, int size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove (int /*long*/ dest, AtkTextRange src, int size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove (AtkTextRange dest, int /*long*/ src, int size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove (int /*long*/ dest, AtkAttribute src, int size);
/**
 * @param dest cast=(void *)
 * @param src cast=(const void *),flags=no_out
 * @param size cast=(size_t)
 */
public static final native void memmove (AtkAttribute dest, int /*long*/ src, int size);
}
