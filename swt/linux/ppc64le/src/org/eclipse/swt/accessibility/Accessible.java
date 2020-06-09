/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.swt.accessibility;


import java.util.*;
import java.util.List;

import org.eclipse.swt.*;
import org.eclipse.swt.internal.gtk.*;
import org.eclipse.swt.widgets.*;

/**
 * Instances of this class provide a bridge between application
 * code and assistive technology clients. Many platforms provide
 * default accessible behavior for most widgets, and this class
 * allows that default behavior to be overridden. Applications
 * can get the default Accessible object for a control by sending
 * it <code>getAccessible</code>, and then add an accessible listener
 * to override simple items like the name and help string, or they
 * can add an accessible control listener to override complex items.
 * As a rule of thumb, an application would only want to use the
 * accessible control listener to implement accessibility for a
 * custom control.
 *
 * @see Control#getAccessible
 * @see AccessibleListener
 * @see AccessibleEvent
 * @see AccessibleControlListener
 * @see AccessibleControlEvent
 * @see <a href="http://www.eclipse.org/swt/snippets/#accessibility">Accessibility snippets</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 *
 * @since 2.0
 */
public class Accessible {
	List<AccessibleListener> accessibleListeners;
	List<AccessibleControlListener> accessibleControlListeners;
	List<AccessibleTextListener> accessibleTextListeners;
	List<AccessibleActionListener> accessibleActionListeners;
	List<AccessibleEditableTextListener> accessibleEditableTextListeners;
	List<AccessibleHyperlinkListener> accessibleHyperlinkListeners;
	List<AccessibleTableListener> accessibleTableListeners;
	List<AccessibleTableCellListener> accessibleTableCellListeners;
	List<AccessibleTextExtendedListener> accessibleTextExtendedListeners;
	List<AccessibleValueListener> accessibleValueListeners;
	List<AccessibleAttributeListener> accessibleAttributeListeners;
	Accessible parent;
	AccessibleObject accessibleObject;
	Control control;
	List<Relation> relations;
	List<Accessible> children;

	static class Relation {
		int type;
		Accessible target;

		public Relation(int type, Accessible target) {
			this.type = type;
			this.target = target;
		}

		@Override
		public boolean equals(Object object) {
			if (object == this) return true;
			if (!(object instanceof Relation)) return false;
			Relation relation = (Relation)object;
			return (relation.type == this.type) && (relation.target == this.target);
		}
	}

	/**
	 * Constructs a new instance of this class given its parent.
	 *
	 * @param parent the Accessible parent, which must not be null
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
	 * </ul>
	 *
	 * @see #dispose
	 * @see Control#getAccessible
	 *
	 * @since 3.6
	 */
	public Accessible(Accessible parent) {
		this.parent = checkNull(parent);
		this.control = parent.control;
		if (parent.children == null) parent.children = new ArrayList<>();
		parent.children.add(this);
	}

	/**
	 * @since 3.5
	 * @deprecated
	 */
	@Deprecated
	protected Accessible() {
	}

	static Accessible checkNull (Accessible parent) {
		if (parent == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		return parent;
	}

	Accessible (Control control) {
		super ();
		this.control = control;
		long type = OS.G_OBJECT_TYPE (getControlHandle());
		accessibleObject = new AccessibleObject (type, getControlHandle(), this, false);
		addRelations();
	}

	/**
	 * Adds the listener to the collection of listeners who will
	 * be notified when an accessible client asks for certain strings,
	 * such as name, description, help, or keyboard shortcut. The
	 * listener is notified by sending it one of the messages defined
	 * in the <code>AccessibleListener</code> interface.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for a name, description, help, or keyboard shortcut string
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleListener
	 * @see #removeAccessibleListener
	 */
	public void addAccessibleListener (AccessibleListener listener) {
		checkWidget ();
		if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		if (accessibleListeners == null) accessibleListeners = new ArrayList<>();
		accessibleListeners.add (listener);
	}

	/**
	 * Adds the listener to the collection of listeners who will
	 * be notified when an accessible client asks for custom control
	 * specific information. The listener is notified by sending it
	 * one of the messages defined in the <code>AccessibleControlListener</code>
	 * interface.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for custom control specific information
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleControlListener
	 * @see #removeAccessibleControlListener
	 */
	public void addAccessibleControlListener (AccessibleControlListener listener) {
		checkWidget ();
		if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		if (accessibleControlListeners == null) accessibleControlListeners = new ArrayList<>();
		accessibleControlListeners.add (listener);
	}

	/**
	 * Adds the listener to the collection of listeners who will
	 * be notified when an accessible client asks for custom text control
	 * specific information. The listener is notified by sending it
	 * one of the messages defined in the <code>AccessibleTextListener</code>
	 * and <code>AccessibleTextExtendedListener</code> interfaces.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for custom text control specific information
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleTextListener
	 * @see AccessibleTextExtendedListener
	 * @see #removeAccessibleTextListener
	 *
	 * @since 3.0
	 */
	public void addAccessibleTextListener (AccessibleTextListener listener) {
		checkWidget ();
		if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		if (listener instanceof AccessibleTextExtendedListener) {
			if (accessibleTextExtendedListeners == null) accessibleTextExtendedListeners = new ArrayList<>();
			accessibleTextExtendedListeners.add ((AccessibleTextExtendedListener)listener);
		} else {
			if (accessibleTextListeners == null) accessibleTextListeners = new ArrayList<>();
			accessibleTextListeners.add (listener);
		}
	}

	/**
	 * Adds the listener to the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleActionListener</code> interface.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for <code>AccessibleActionListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleActionListener
	 * @see #removeAccessibleActionListener
	 *
	 * @since 3.6
	 */
	public void addAccessibleActionListener(AccessibleActionListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleActionListeners == null) accessibleActionListeners = new ArrayList<>();
		accessibleActionListeners.add(listener);
	}

	/**
	 * Adds the listener to the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleEditableTextListener</code> interface.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for <code>AccessibleEditableTextListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleEditableTextListener
	 * @see #removeAccessibleEditableTextListener
	 *
	 * @since 3.7
	 */
	public void addAccessibleEditableTextListener(AccessibleEditableTextListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleEditableTextListeners == null) accessibleEditableTextListeners = new ArrayList<>();
		accessibleEditableTextListeners.add(listener);
	}

	/**
	 * Adds the listener to the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleHyperlinkListener</code> interface.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for <code>AccessibleHyperlinkListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleHyperlinkListener
	 * @see #removeAccessibleHyperlinkListener
	 *
	 * @since 3.6
	 */
	public void addAccessibleHyperlinkListener(AccessibleHyperlinkListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleHyperlinkListeners == null) accessibleHyperlinkListeners = new ArrayList<>();
		accessibleHyperlinkListeners.add(listener);
	}

	/**
	 * Adds the listener to the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleTableListener</code> interface.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for <code>AccessibleTableListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleTableListener
	 * @see #removeAccessibleTableListener
	 *
	 * @since 3.6
	 */
	public void addAccessibleTableListener(AccessibleTableListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleTableListeners == null) accessibleTableListeners = new ArrayList<>();
		accessibleTableListeners.add(listener);
	}

	/**
	 * Adds the listener to the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleTableCellListener</code> interface.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for <code>AccessibleTableCellListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleTableCellListener
	 * @see #removeAccessibleTableCellListener
	 *
	 * @since 3.6
	 */
	public void addAccessibleTableCellListener(AccessibleTableCellListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleTableCellListeners == null) accessibleTableCellListeners = new ArrayList<>();
		accessibleTableCellListeners.add(listener);
	}

	/**
	 * Adds the listener to the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleValueListener</code> interface.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for <code>AccessibleValueListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleValueListener
	 * @see #removeAccessibleValueListener
	 *
	 * @since 3.6
	 */
	public void addAccessibleValueListener(AccessibleValueListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleValueListeners == null) accessibleValueListeners = new ArrayList<>();
		accessibleValueListeners.add(listener);
	}

	/**
	 * Adds the listener to the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleAttributeListener</code> interface.
	 *
	 * @param listener the listener that should be notified when the receiver
	 * is asked for <code>AccessibleAttributeListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleAttributeListener
	 * @see #removeAccessibleAttributeListener
	 *
	 * @since 3.6
	 */
	public void addAccessibleAttributeListener(AccessibleAttributeListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleAttributeListeners == null) accessibleAttributeListeners = new ArrayList<>();
		accessibleAttributeListeners.add(listener);
	}

	/**
	 * Adds a relation with the specified type and target
	 * to the receiver's set of relations.
	 *
	 * @param type an <code>ACC</code> constant beginning with RELATION_* indicating the type of relation
	 * @param target the accessible that is the target for this relation
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the Accessible target is null
	 * @since 3.6
	 */
	public void addRelation(int type, Accessible target) {
		checkWidget();
		if (target == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		if (relations == null) relations = new ArrayList<>();
		Relation relation = new Relation(type, target);
		if (relations.indexOf(relation) != -1) return;
		relations.add(relation);
		if (accessibleObject != null) accessibleObject.addRelation(type, target);
	}

	void addRelations () {
		if (relations == null) return;
		if (accessibleObject == null) return;
		for (int i = 0; i < relations.size(); i++) {
			Relation relation = relations.get(i);
			accessibleObject.addRelation(relation.type, relation.target);
		}
	}

	/**
	 * Disposes of the operating system resources associated with
	 * the receiver, and removes the receiver from its parent's
	 * list of children.
	 * <p>
	 * This method should be called when an accessible that was created
	 * with the public constructor <code>Accessible(Accessible parent)</code>
	 * is no longer needed. You do not need to call this when the receiver's
	 * control is disposed, because all <code>Accessible</code> instances
	 * associated with a control are released when the control is disposed.
	 * It is also not necessary to call this for instances of <code>Accessible</code>
	 * that were retrieved with <code>Control.getAccessible()</code>.
	 * </p>
	 *
	 * @since 3.6
	 */
	public void dispose () {
		if (parent == null) return;
		release();
		parent.children.remove(this);
		parent = null;
	}

	/**
	 * Returns the control for this Accessible object.
	 *
	 * @return the receiver's control
	 * @since 3.0
	 */
	public Control getControl() {
		return control;
	}

	/* checkWidget was copied from Widget, and rewritten to work in this package */
	void checkWidget () {
		if (!isValidThread ()) SWT.error (SWT.ERROR_THREAD_INVALID_ACCESS);
		if (control.isDisposed ()) SWT.error (SWT.ERROR_WIDGET_DISPOSED);
	}

	AccessibleObject getAccessibleObject () {
		if (accessibleObject == null) {
			long widget = this.getControlHandle();
			long type = OS.G_OBJECT_TYPE (widget);
			if (parent == null) {
				accessibleObject = new AccessibleObject (type, widget, this, false);
			} else {
				accessibleObject = new AccessibleObject (type, 0, this, true);
				accessibleObject.parent = parent.getAccessibleObject();
			}
		}
		return accessibleObject;
	}

	long getControlHandle () {
		long result = control.handle;
		if (control instanceof Label) {
			long list = GTK.gtk_container_get_children (result);
			if (list != 0) {
				long temp = list;
				while (temp != 0) {
					long widget = OS.g_list_data( temp);
					if (GTK.gtk_widget_get_visible (widget)) {
						result = widget;
						break;
					}
					temp = OS.g_list_next (temp);
				}
				OS.g_list_free (list);
			}
		}
		return result;
	}

	/**
	 * Invokes platform specific functionality to dispose an accessible object.
	 * <p>
	 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
	 * API for <code>Accessible</code>. It is marked public only so that it
	 * can be shared within the packages provided by SWT. It is not
	 * available on all platforms, and should never be called from
	 * application code.
	 * </p>
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void internal_dispose_Accessible() {
		release ();
	}

	/**
	 * Invokes platform specific functionality to allocate a new accessible object.
	 * <p>
	 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
	 * API for <code>Accessible</code>. It is marked public only so that it
	 * can be shared within the packages provided by SWT. It is not
	 * available on all platforms, and should never be called from
	 * application code.
	 * </p>
	 *
	 * @param control the control to get the accessible object for
	 * @return the platform specific accessible object
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static Accessible internal_new_Accessible (Control control) {
		return new Accessible (control);
	}

	/* isValidThread was copied from Widget, and rewritten to work in this package */
	boolean isValidThread () {
		return control.getDisplay ().getThread () == Thread.currentThread ();
	}

	void release () {
		if (children != null) {
			List<Accessible> temp = new ArrayList<>(children);
			for (int i = 0; i < temp.size(); i++) {
				Accessible child = temp.get(i);
				child.dispose();
			}
		}
		if (accessibleObject != null) {
			accessibleObject.release ();
			accessibleObject = null;
		}
	}

	/**
	 * Removes the listener from the collection of listeners who will
	 * be notified when an accessible client asks for custom control
	 * specific information.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for custom control specific information
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleControlListener
	 * @see #addAccessibleControlListener
	 */
	public void removeAccessibleControlListener (AccessibleControlListener listener) {
		checkWidget ();
		if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		if (accessibleControlListeners != null) {
			accessibleControlListeners.remove(listener);
			if (accessibleControlListeners.isEmpty()) accessibleControlListeners = null;
		}
	}

	/**
	 * Removes the listener from the collection of listeners who will
	 * be notified when an accessible client asks for certain strings,
	 * such as name, description, help, or keyboard shortcut.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for a name, description, help, or keyboard shortcut string
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleListener
	 * @see #addAccessibleListener
	 */
	public void removeAccessibleListener (AccessibleListener listener) {
		checkWidget ();
		if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		if (accessibleListeners != null) {
			accessibleListeners.remove(listener);
			if (accessibleListeners.isEmpty()) accessibleListeners = null;
		}
	}

	/**
	 * Removes the listener from the collection of listeners who will
	 * be notified when an accessible client asks for custom text control
	 * specific information.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for custom text control specific information
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleTextListener
	 * @see AccessibleTextExtendedListener
	 * @see #addAccessibleTextListener
	 *
	 * @since 3.0
	 */
	public void removeAccessibleTextListener (AccessibleTextListener listener) {
		checkWidget ();
		if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		if (listener instanceof AccessibleTextExtendedListener) {
			if (accessibleTextExtendedListeners != null) {
				accessibleTextExtendedListeners.remove (listener);
				if (accessibleTextExtendedListeners.isEmpty()) accessibleTextExtendedListeners = null;
			}
		} else {
			if (accessibleTextListeners != null) {
				accessibleTextListeners.remove (listener);
				if (accessibleTextListeners.isEmpty()) accessibleTextListeners = null;
			}
		}
	}

	/**
	 * Removes the listener from the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleActionListener</code> interface.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for <code>AccessibleActionListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleActionListener
	 * @see #addAccessibleActionListener
	 *
	 * @since 3.6
	 */
	public void removeAccessibleActionListener(AccessibleActionListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleActionListeners != null) {
			accessibleActionListeners.remove(listener);
			if (accessibleActionListeners.isEmpty()) accessibleActionListeners = null;
		}
	}

	/**
	 * Removes the listener from the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleEditableTextListener</code> interface.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for <code>AccessibleEditableTextListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleEditableTextListener
	 * @see #addAccessibleEditableTextListener
	 *
	 * @since 3.7
	 */
	public void removeAccessibleEditableTextListener(AccessibleEditableTextListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleEditableTextListeners != null) {
			accessibleEditableTextListeners.remove(listener);
			if (accessibleEditableTextListeners.isEmpty()) accessibleEditableTextListeners = null;
		}
	}

	/**
	 * Removes the listener from the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleHyperlinkListener</code> interface.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for <code>AccessibleHyperlinkListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleHyperlinkListener
	 * @see #addAccessibleHyperlinkListener
	 *
	 * @since 3.6
	 */
	public void removeAccessibleHyperlinkListener(AccessibleHyperlinkListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleHyperlinkListeners != null) {
			accessibleHyperlinkListeners.remove(listener);
			if (accessibleHyperlinkListeners.isEmpty()) accessibleHyperlinkListeners = null;
		}
	}

	/**
	 * Removes the listener from the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleTableListener</code> interface.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for <code>AccessibleTableListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleTableListener
	 * @see #addAccessibleTableListener
	 *
	 * @since 3.6
	 */
	public void removeAccessibleTableListener(AccessibleTableListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleTableListeners != null) {
			accessibleTableListeners.remove(listener);
			if (accessibleTableListeners.isEmpty()) accessibleTableListeners = null;
		}
	}

	/**
	 * Removes the listener from the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleTableCellListener</code> interface.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for <code>AccessibleTableCellListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleTableCellListener
	 * @see #addAccessibleTableCellListener
	 *
	 * @since 3.6
	 */
	public void removeAccessibleTableCellListener(AccessibleTableCellListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleTableCellListeners != null) {
			accessibleTableCellListeners.remove(listener);
			if (accessibleTableCellListeners.isEmpty()) accessibleTableCellListeners = null;
		}
	}

	/**
	 * Removes the listener from the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleValueListener</code> interface.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for <code>AccessibleValueListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleValueListener
	 * @see #addAccessibleValueListener
	 *
	 * @since 3.6
	 */
	public void removeAccessibleValueListener(AccessibleValueListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleValueListeners != null) {
			accessibleValueListeners.remove(listener);
			if (accessibleValueListeners.isEmpty()) accessibleValueListeners = null;
		}
	}

	/**
	 * Removes the listener from the collection of listeners that will be
	 * notified when an accessible client asks for any of the properties
	 * defined in the <code>AccessibleAttributeListener</code> interface.
	 *
	 * @param listener the listener that should no longer be notified when the receiver
	 * is asked for <code>AccessibleAttributeListener</code> interface properties
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see AccessibleAttributeListener
	 * @see #addAccessibleAttributeListener
	 *
	 * @since 3.6
	 */
	public void removeAccessibleAttributeListener(AccessibleAttributeListener listener) {
		checkWidget();
		if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (accessibleAttributeListeners != null) {
			accessibleAttributeListeners.remove(listener);
			if (accessibleAttributeListeners.isEmpty()) accessibleAttributeListeners = null;
		}
	}

	/**
	 * Removes the relation with the specified type and target
	 * from the receiver's set of relations.
	 *
	 * @param type an <code>ACC</code> constant beginning with RELATION_* indicating the type of relation
	 * @param target the accessible that is the target for this relation
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the Accessible target is null
	 * @since 3.6
	 */
	public void removeRelation(int type, Accessible target) {
		checkWidget();
		if (relations == null) return;
		if (target == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		Relation relation = new Relation(type, target);
		int index = relations.indexOf(relation);
		if (index == -1) return;
		relations.remove(index);
		if (accessibleObject != null) accessibleObject.removeRelation(type, target);
	}

	/**
	 * Sends a message with event-specific data to accessible clients
	 * indicating that something has changed within a custom control.
	 *
	 * @param event an <code>ACC</code> constant beginning with EVENT_* indicating the message to send
	 * @param eventData an object containing event-specific data, or null if there is no event-specific data
	 * (eventData is specified in the documentation for individual ACC.EVENT_* constants)
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see ACC#EVENT_ACTION_CHANGED
	 * @see ACC#EVENT_ATTRIBUTE_CHANGED
	 * @see ACC#EVENT_DESCRIPTION_CHANGED
	 * @see ACC#EVENT_DOCUMENT_LOAD_COMPLETE
	 * @see ACC#EVENT_DOCUMENT_LOAD_STOPPED
	 * @see ACC#EVENT_DOCUMENT_RELOAD
	 * @see ACC#EVENT_HYPERLINK_ACTIVATED
	 * @see ACC#EVENT_HYPERLINK_ANCHOR_COUNT_CHANGED
	 * @see ACC#EVENT_HYPERLINK_END_INDEX_CHANGED
	 * @see ACC#EVENT_HYPERLINK_SELECTED_LINK_CHANGED
	 * @see ACC#EVENT_HYPERLINK_START_INDEX_CHANGED
	 * @see ACC#EVENT_HYPERTEXT_LINK_COUNT_CHANGED
	 * @see ACC#EVENT_HYPERTEXT_LINK_SELECTED
	 * @see ACC#EVENT_LOCATION_CHANGED
	 * @see ACC#EVENT_NAME_CHANGED
	 * @see ACC#EVENT_PAGE_CHANGED
	 * @see ACC#EVENT_SECTION_CHANGED
	 * @see ACC#EVENT_SELECTION_CHANGED
	 * @see ACC#EVENT_STATE_CHANGED
	 * @see ACC#EVENT_TABLE_CAPTION_CHANGED
	 * @see ACC#EVENT_TABLE_CHANGED
	 * @see ACC#EVENT_TABLE_COLUMN_DESCRIPTION_CHANGED
	 * @see ACC#EVENT_TABLE_COLUMN_HEADER_CHANGED
	 * @see ACC#EVENT_TABLE_ROW_DESCRIPTION_CHANGED
	 * @see ACC#EVENT_TABLE_ROW_HEADER_CHANGED
	 * @see ACC#EVENT_TABLE_SUMMARY_CHANGED
	 * @see ACC#EVENT_TEXT_ATTRIBUTE_CHANGED
	 * @see ACC#EVENT_TEXT_CARET_MOVED
	 * @see ACC#EVENT_TEXT_CHANGED
	 * @see ACC#EVENT_TEXT_COLUMN_CHANGED
	 * @see ACC#EVENT_TEXT_SELECTION_CHANGED
	 * @see ACC#EVENT_VALUE_CHANGED
	 *
	 * @since 3.6
	 */
	public void sendEvent(int event, Object eventData) {
		checkWidget();
		if (accessibleObject != null) {
			accessibleObject.sendEvent(event, eventData);
		}
	}

	/**
	 * Sends a message with event-specific data and a childID
	 * to accessible clients, indicating that something has changed
	 * within a custom control.
	 *
	 * NOTE: This API is intended for applications that are still using childIDs.
	 * Moving forward, applications should use accessible objects instead of childIDs.
	 *
	 * @param event an <code>ACC</code> constant beginning with EVENT_* indicating the message to send
	 * @param eventData an object containing event-specific data, or null if there is no event-specific data
	 * (eventData is specified in the documentation for individual ACC.EVENT_* constants)
	 * @param childID an identifier specifying a child of the control
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see ACC#EVENT_DESCRIPTION_CHANGED
	 * @see ACC#EVENT_LOCATION_CHANGED
	 * @see ACC#EVENT_NAME_CHANGED
	 * @see ACC#EVENT_SELECTION_CHANGED
	 * @see ACC#EVENT_STATE_CHANGED
	 * @see ACC#EVENT_TEXT_SELECTION_CHANGED
	 * @see ACC#EVENT_VALUE_CHANGED
	 *
	 * @since 3.8
	 */
	public void sendEvent(int event, Object eventData, int childID) {
		checkWidget();
		if (accessibleObject != null) {
			switch (event) {
				case ACC.EVENT_STATE_CHANGED:
				case ACC.EVENT_NAME_CHANGED:
				case ACC.EVENT_VALUE_CHANGED:
				case ACC.EVENT_LOCATION_CHANGED:
				case ACC.EVENT_SELECTION_CHANGED:
				case ACC.EVENT_TEXT_SELECTION_CHANGED:
				case ACC.EVENT_DESCRIPTION_CHANGED:
					accessibleObject.sendEvent(event, eventData, childID);
			}
		}
	}

	/**
	 * Sends a message to accessible clients that the child selection
	 * within a custom container control has changed.
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @since 3.0
	 */
	public void selectionChanged () {
		checkWidget ();
		if (accessibleObject != null) {
			accessibleObject.selectionChanged ();
		}
	}

	/**
	 * Sends a message to accessible clients indicating that the focus
	 * has changed within a custom control.
	 *
	 * @param childID an identifier specifying a child of the control
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 */
	public void setFocus (int childID) {
		checkWidget ();
		if (accessibleObject != null) {
			accessibleObject.setFocus (childID);
		}
	}

	/**
	 * Sends a message to accessible clients that the text
	 * caret has moved within a custom control.
	 *
	 * @param index the new caret index within the control
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @since 3.0
	 */
	public void textCaretMoved (int index) {
		checkWidget ();
		if (accessibleObject != null) {
			accessibleObject.textCaretMoved (index);
		}
	}

	/**
	 * Sends a message to accessible clients that the text
	 * within a custom control has changed.
	 *
	 * @param type the type of change, one of <code>ACC.TEXT_INSERT</code>
	 * or <code>ACC.TEXT_DELETE</code>
	 * @param startIndex the text index within the control where the insertion or deletion begins
	 * @param length the non-negative length in characters of the insertion or deletion
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @see ACC#TEXT_INSERT
	 * @see ACC#TEXT_DELETE
	 *
	 * @since 3.0
	 */
	public void textChanged (int type, int startIndex, int length) {
		checkWidget ();
		if (accessibleObject != null) {
			accessibleObject.textChanged (type, startIndex, length);
		}
	}

	/**
	 * Sends a message to accessible clients that the text
	 * selection has changed within a custom control.
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver's control has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver's control</li>
	 * </ul>
	 *
	 * @since 3.0
	 */
	public void textSelectionChanged () {
		checkWidget ();
		if (accessibleObject != null) {
			accessibleObject.textSelectionChanged ();
		}
	}
}
