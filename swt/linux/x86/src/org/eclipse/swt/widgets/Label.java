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
package org.eclipse.swt.widgets;


import org.eclipse.swt.*;
import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * Instances of this class represent a non-selectable
 * user interface object that displays a string or image.
 * When SEPARATOR is specified, displays a single
 * vertical or horizontal line.
 * <p>
 * Shadow styles are hints and may not be honored
 * by the platform.  To create a separator label
 * with the default shadow style for the platform,
 * do not specify a shadow style.
 * </p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SEPARATOR, HORIZONTAL, VERTICAL</dd>
 * <dd>SHADOW_IN, SHADOW_OUT, SHADOW_NONE</dd>
 * <dd>CENTER, LEFT, RIGHT, WRAP</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * <p>
 * Note: Only one of SHADOW_IN, SHADOW_OUT and SHADOW_NONE may be specified.
 * SHADOW_NONE is a HINT. Only one of HORIZONTAL and VERTICAL may be specified.
 * Only one of CENTER, LEFT and RIGHT may be specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 *
 * @see <a href="http://www.eclipse.org/swt/snippets/#label">Label snippets</a>
 * @see <a href="http://www.eclipse.org/swt/examples.php">SWT Example: ControlExample</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 * @noextend This class is not intended to be subclassed by clients.
 */
public class Label extends Control {
	int /*long*/ frameHandle, labelHandle, imageHandle, boxHandle;
	ImageList imageList;
	Image image;
	String text;

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a composite control which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT#SEPARATOR
 * @see SWT#HORIZONTAL
 * @see SWT#VERTICAL
 * @see SWT#SHADOW_IN
 * @see SWT#SHADOW_OUT
 * @see SWT#SHADOW_NONE
 * @see SWT#CENTER
 * @see SWT#LEFT
 * @see SWT#RIGHT
 * @see SWT#WRAP
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
public Label (Composite parent, int style) {
	super (parent, checkStyle (style));
}

static int checkStyle (int style) {
	style |= SWT.NO_FOCUS;
	if ((style & SWT.SEPARATOR) != 0) {
		style = checkBits (style, SWT.VERTICAL, SWT.HORIZONTAL, 0, 0, 0, 0);
		return checkBits (style, SWT.SHADOW_OUT, SWT.SHADOW_IN, SWT.SHADOW_NONE, 0, 0, 0);
	}
	return checkBits (style, SWT.LEFT, SWT.CENTER, SWT.RIGHT, 0, 0, 0);
}

@Override
void addRelation (Control control) {
	if (!control.isDescribedByLabel ()) return;
	if (labelHandle == 0) return;
	control._getAccessible().addRelation(ACC.RELATION_LABELLED_BY, _getAccessible());
	control.labelRelation = this;
}

@Override
Point computeNativeSize (int /*long*/ h, int wHint, int hHint, boolean changed) {
	int width = wHint, height = hHint;
	/*
	 * Feature in GTK3: Labels with long text have an extremely large natural width.
	 * As a result, such Labels created with SWT.WRAP end up being too big. The fix
	 * is to use the minimum width instead of the natural width when SWT.WRAP is specified.
	 * In all other cases, use the natural width. See bug 534768.
	 */
	boolean wrapGTK3 = GTK.GTK3 && labelHandle != 0 && (style & SWT.WRAP) != 0 && GTK.gtk_widget_get_visible (labelHandle);
	if (wrapGTK3 && wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
		GtkRequisition naturalSize = new GtkRequisition ();
		GtkRequisition minimumSize = new GtkRequisition ();
		GTK.gtk_widget_get_preferred_size(h, minimumSize, naturalSize);
		width = minimumSize.width;
		height = naturalSize.height;
		return new Point(width, height);
	} else {
		return super.computeNativeSize(h, wHint, hHint, changed);
	}
}
@Override
Point computeSizeInPixels (int wHint, int hHint, boolean changed) {
	checkWidget ();
	if (wHint != SWT.DEFAULT && wHint < 0) wHint = 0;
	if (hHint != SWT.DEFAULT && hHint < 0) hHint = 0;
	if ((style & SWT.SEPARATOR) != 0) {
		if ((style & SWT.HORIZONTAL) != 0) {
			if (wHint == SWT.DEFAULT) wHint = DEFAULT_WIDTH;
		} else {
			if (hHint == SWT.DEFAULT) hHint = DEFAULT_HEIGHT;
		}
	}
	Point size;
	/*
	* Feature in GTK. GTK has a predetermined maximum width for wrapping text.
	* The fix is to use pango layout directly instead of the label size request
	* to calculate its preferred size.
	*/
	boolean fixWrap = labelHandle != 0 && (style & SWT.WRAP) != 0 && GTK.gtk_widget_get_visible (labelHandle);
	if (fixWrap || frameHandle != 0) forceResize ();
	if (fixWrap) {
		int /*long*/ labelLayout = GTK.gtk_label_get_layout (labelHandle);
		int pangoWidth = OS.pango_layout_get_width (labelLayout);
		if (wHint != SWT.DEFAULT) {
			OS.pango_layout_set_width (labelLayout, wHint * OS.PANGO_SCALE);
		} else {
			OS.pango_layout_set_width (labelLayout, -1);
		}
		int [] w = new int [1], h = new int [1];
		OS.pango_layout_get_pixel_size (labelLayout, w, h);
		OS.pango_layout_set_width (labelLayout, pangoWidth);
		if (frameHandle != 0) {
			int [] labelWidth = new int [1], labelHeight = new int [1];
			GTK.gtk_widget_get_size_request (labelHandle, labelWidth, labelHeight);
			GTK.gtk_widget_set_size_request (labelHandle, 1, 1);
			size = computeNativeSize (frameHandle, -1, -1, changed);
			GTK.gtk_widget_set_size_request (labelHandle, labelWidth [0], labelHeight [0]);
			size.x = size.x - 1;
			size.y = size.y - 1;
		} else {
			size = new Point (0,0);
		}
		size.x += wHint == SWT.DEFAULT ? w [0] : wHint;
		size.y += hHint == SWT.DEFAULT ? h [0] : hHint;
	} else {
		if (frameHandle != 0) {
			int [] reqWidth = new int [1], reqHeight = new int [1];
			GTK.gtk_widget_get_size_request (handle, reqWidth, reqHeight);
			GTK.gtk_widget_set_size_request (handle, wHint, hHint);
			size = computeNativeSize (frameHandle, -1, -1, changed);
			GTK.gtk_widget_set_size_request (handle, reqWidth [0], reqHeight [0]);
		} else {
			size = computeNativeSize (handle, wHint, hHint, changed);
		}
	}
	/*
	* Feature in GTK.  Instead of using the font height to determine
	* the preferred height of the widget, GTK uses the text metrics.
	* The fix is to ensure that the preferred height is at least as
	* tall as the font height.
	*
	* NOTE: This work around does not fix the case when there are
	* muliple lines of text.
	*/
	if (hHint == SWT.DEFAULT && labelHandle != 0) {
		int /*long*/ layout = GTK.gtk_label_get_layout (labelHandle);
		int /*long*/ context = OS.pango_layout_get_context (layout);
		int /*long*/ lang = OS.pango_context_get_language (context);
		int /*long*/ font = getFontDescription ();
		int /*long*/ metrics = OS.pango_context_get_metrics (context, font, lang);
		int ascent = OS.PANGO_PIXELS (OS.pango_font_metrics_get_ascent (metrics));
		int descent = OS.PANGO_PIXELS (OS.pango_font_metrics_get_descent (metrics));
		OS.pango_font_metrics_unref (metrics);
		int fontHeight = ascent + descent;
		if (GTK.GTK3) {
			int [] bufferBottom = new int [1];
			int [] bufferTop = new int [1];
			OS.g_object_get(labelHandle, OS.margin_bottom, bufferBottom, 0);
			OS.g_object_get(labelHandle, OS.margin_top, bufferTop, 0);
			fontHeight += bufferBottom [0] + bufferTop [0];
		} else {
			int [] bufferYpad = new int[1];
			OS.g_object_get (labelHandle, OS.ypad, bufferYpad, 0);
			fontHeight += 2 * bufferYpad [0];
		}
		if (frameHandle != 0) {
			fontHeight += 2 * getThickness (frameHandle).y;
			fontHeight += 2 * GTK.gtk_container_get_border_width (frameHandle);
		}
		size.y = Math.max (size.y, fontHeight);
	}
	return size;
}

@Override
void createHandle (int index) {
	state |= HANDLE | THEME_BACKGROUND;
	fixedHandle = OS.g_object_new (display.gtk_fixed_get_type (), 0);
	if (fixedHandle == 0) error (SWT.ERROR_NO_HANDLES);
	GTK.gtk_widget_set_has_window (fixedHandle, true);
	if ((style & SWT.SEPARATOR) != 0) {
		if ((style & SWT.HORIZONTAL)!= 0) {
			handle = gtk_separator_new (GTK.GTK_ORIENTATION_HORIZONTAL);
			if (handle != 0 && GTK.GTK_VERSION >= OS.VERSION(3, 20, 0)) {
				GTK.gtk_widget_set_valign(handle, GTK.GTK_ALIGN_CENTER);
			}
		} else {
			handle = gtk_separator_new (GTK.GTK_ORIENTATION_VERTICAL);
			if (handle != 0 && GTK.GTK_VERSION >= OS.VERSION(3, 20, 0)) {
				GTK.gtk_widget_set_halign(handle, GTK.GTK_ALIGN_CENTER);
			}
		}
		if (handle == 0) error (SWT.ERROR_NO_HANDLES);
	} else {
		handle = GTK.gtk_event_box_new();
		if (handle == 0) error (SWT.ERROR_NO_HANDLES);
		boxHandle = gtk_box_new (GTK.GTK_ORIENTATION_HORIZONTAL, false, 0);
		if (boxHandle == 0) error (SWT.ERROR_NO_HANDLES);
		labelHandle = GTK.gtk_label_new_with_mnemonic (null);
		if (labelHandle == 0) error (SWT.ERROR_NO_HANDLES);
		imageHandle = GTK.gtk_image_new ();
		if (imageHandle == 0) error (SWT.ERROR_NO_HANDLES);
		GTK.gtk_container_add (handle, boxHandle);
		GTK.gtk_container_add (boxHandle, labelHandle);
		GTK.gtk_container_add (boxHandle, imageHandle);
		GTK.gtk_box_set_child_packing(boxHandle, labelHandle, true, true, 0, GTK.GTK_PACK_START);
		GTK.gtk_box_set_child_packing(boxHandle, imageHandle, true, true, 0, GTK.GTK_PACK_START);
	}
	if ((style & SWT.BORDER) != 0) {
		frameHandle = GTK.gtk_frame_new (null);
		if (frameHandle == 0) error (SWT.ERROR_NO_HANDLES);
		GTK.gtk_container_add (fixedHandle, frameHandle);
		GTK.gtk_container_add (frameHandle, handle);
		GTK.gtk_frame_set_shadow_type (frameHandle, GTK.GTK_SHADOW_ETCHED_IN);
	} else {
		GTK.gtk_container_add (fixedHandle, handle);
	}
	if ((style & SWT.SEPARATOR) != 0) return;
	if ((style & SWT.WRAP) != 0) {
		GTK.gtk_label_set_line_wrap (labelHandle, true);
		GTK.gtk_label_set_line_wrap_mode (labelHandle, OS.PANGO_WRAP_WORD_CHAR);
	}
	// In GTK 3 font description is inherited from parent widget which is not how SWT has always worked,
	// reset to default font to get the usual behavior
	if (GTK.GTK3) {
		setFontDescription(defaultFont ().handle);
	}
	setAlignment ();
}

@Override
void createWidget (int index) {
	super.createWidget (index);
	text = "";
}

@Override
void deregister () {
	super.deregister ();
	if (frameHandle != 0) display.removeWidget (frameHandle);
	if (labelHandle != 0) display.removeWidget (labelHandle);
	if (imageHandle != 0) display.removeWidget (imageHandle);
	if (boxHandle != 0) display.removeWidget (boxHandle);
}

@Override
int /*long*/ eventHandle () {
	return fixedHandle;
}

@Override
int /*long*/ cssHandle () {
	if ((style & SWT.SEPARATOR) == 0) {
		return labelHandle;
	}
	return handle;
}

/**
 * Returns a value which describes the position of the
 * text or image in the receiver. The value will be one of
 * <code>LEFT</code>, <code>RIGHT</code> or <code>CENTER</code>
 * unless the receiver is a <code>SEPARATOR</code> label, in
 * which case, <code>NONE</code> is returned.
 *
 * @return the alignment
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getAlignment () {
	checkWidget ();
	if ((style & SWT.SEPARATOR) != 0) return 0;
	if ((style & SWT.LEFT) != 0) return SWT.LEFT;
	if ((style & SWT.CENTER) != 0) return SWT.CENTER;
	if ((style & SWT.RIGHT) != 0) return SWT.RIGHT;
	return SWT.LEFT;
}

@Override
int getBorderWidthInPixels () {
	checkWidget();
	if (frameHandle != 0) {
		return getThickness (frameHandle).x;
	}
	return 0;
}

/**
 * Returns the receiver's image if it has one, or null
 * if it does not.
 *
 * @return the receiver's image
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Image getImage () {
	checkWidget ();
	return image;
}

@Override
String getNameText () {
	return getText ();
}

/**
 * Returns the receiver's text, which will be an empty
 * string if it has never been set or if the receiver is
 * a <code>SEPARATOR</code> label.
 *
 * @return the receiver's text
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public String getText () {
	checkWidget ();
	if ((style & SWT.SEPARATOR) != 0) return "";
	return text;
}

@Override
void hookEvents () {
	super.hookEvents();
	if (labelHandle != 0) {
		OS.g_signal_connect_closure_by_id (labelHandle, display.signalIds [MNEMONIC_ACTIVATE], 0, display.getClosure (MNEMONIC_ACTIVATE), false);
	}
}

@Override
boolean isDescribedByLabel () {
	return false;
}

@Override
boolean mnemonicHit (char key) {
	if (labelHandle == 0) return false;
	boolean result = super.mnemonicHit (labelHandle, key);
	if (result) {
		Control control = this;
		while (control.parent != null) {
			Control [] children = control.parent._getChildren ();
			int index = 0;
			while (index < children.length) {
				if (children [index] == control) break;
				index++;
			}
			index++;
			if (index < children.length) {
				if (children [index].setFocus ()) return result;
			}
			control = control.parent;
		}
	}
	return result;
}

@Override
boolean mnemonicMatch (char key) {
	if (labelHandle == 0) return false;
	return mnemonicMatch (labelHandle, key);
}

@Override
void register () {
	super.register ();
	if (boxHandle != 0) display.addWidget (boxHandle, this);
	if (frameHandle != 0) display.addWidget (frameHandle, this);
	if (labelHandle != 0) display.addWidget (labelHandle, this);
	if (imageHandle != 0) display.addWidget (imageHandle, this);
}

@Override
void releaseHandle () {
	super.releaseHandle ();
	frameHandle = imageHandle = labelHandle = boxHandle = 0;
}

@Override
void releaseWidget () {
	super.releaseWidget ();
	if (imageList != null) imageList.dispose ();
	imageList = null;
	image = null;
	text = null;
}

@Override
void resizeHandle (int width, int height) {
	if (GTK.GTK3) {
		OS.swt_fixed_resize (GTK.gtk_widget_get_parent (fixedHandle), fixedHandle, width, height);
		int /*long*/ child = frameHandle != 0 ? frameHandle : handle;
		Point sizes = resizeCalculationsGTK3 (child, width, height);
		width = sizes.x;
		height = sizes.y;
		OS.swt_fixed_resize (GTK.gtk_widget_get_parent (child), child, width, height);
	} else {
		GTK.gtk_widget_set_size_request (fixedHandle, width, height);
		GTK.gtk_widget_set_size_request (frameHandle != 0 ? frameHandle : handle, width, height);
	}
}

/**
 * Controls how text and images will be displayed in the receiver.
 * The argument should be one of <code>LEFT</code>, <code>RIGHT</code>
 * or <code>CENTER</code>.  If the receiver is a <code>SEPARATOR</code>
 * label, the argument is ignored and the alignment is not changed.
 *
 * @param alignment the new alignment
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setAlignment (int alignment) {
	checkWidget ();
	if ((style & SWT.SEPARATOR) != 0) return;
	if ((alignment & (SWT.LEFT | SWT.RIGHT | SWT.CENTER)) == 0) return;
	style &= ~(SWT.LEFT | SWT.RIGHT | SWT.CENTER);
	style |= alignment & (SWT.LEFT | SWT.RIGHT | SWT.CENTER);
	setAlignment ();
}

void setAlignment () {
	if ((style & SWT.LEFT) != 0) {
		if (GTK.GTK_VERSION >= OS.VERSION(3, 16, 0)) {
			gtk_widget_set_align(labelHandle,GTK.GTK_ALIGN_START, GTK.GTK_ALIGN_START); //Aligns widget
			gtk_label_set_align (0.0f, 0.0f); //Aligns text inside the widget.
			gtk_widget_set_align(imageHandle, GTK.GTK_ALIGN_START, GTK.GTK_ALIGN_CENTER);
		} else {
			GTK.gtk_misc_set_alignment (labelHandle, 0.0f, 0.0f);
			GTK.gtk_misc_set_alignment (imageHandle, 0.0f, 0.5f);
		}
		GTK.gtk_label_set_justify (labelHandle, GTK.GTK_JUSTIFY_LEFT);
		return;
	}
	if ((style & SWT.CENTER) != 0) {
		if (GTK.GTK_VERSION >= OS.VERSION(3, 16, 0)) {
			gtk_widget_set_align(labelHandle,GTK.GTK_ALIGN_CENTER, GTK.GTK_ALIGN_START); //Aligns widget
			gtk_label_set_align (0.5f, 0.0f); //Aligns text inside the widget.
			gtk_widget_set_align(imageHandle, GTK.GTK_ALIGN_CENTER, GTK.GTK_ALIGN_CENTER);
		} else {
			GTK.gtk_misc_set_alignment (labelHandle, 0.5f, 0.0f);
			GTK.gtk_misc_set_alignment (imageHandle, 0.5f, 0.5f);
		}

		GTK.gtk_label_set_justify (labelHandle, GTK.GTK_JUSTIFY_CENTER);
		return;
	}
	if ((style & SWT.RIGHT) != 0) {
		if (GTK.GTK_VERSION >= OS.VERSION(3, 16, 0)) {
			gtk_widget_set_align(labelHandle,GTK.GTK_ALIGN_END, GTK.GTK_ALIGN_START); //Aligns widget.
			gtk_label_set_align (1.0f, 0.0f); //Aligns text inside the widget.
			gtk_widget_set_align(imageHandle, GTK.GTK_ALIGN_END, GTK.GTK_ALIGN_CENTER);
		} else  {
			GTK.gtk_misc_set_alignment (labelHandle, 1.0f, 0.0f);
			GTK.gtk_misc_set_alignment (imageHandle, 1.0f, 0.5f);
		}
		GTK.gtk_label_set_justify (labelHandle, GTK.GTK_JUSTIFY_RIGHT);
		return;
	}
}

private void gtk_label_set_align (float xalign, float yalign) {
	GTK.gtk_label_set_xalign (labelHandle, xalign);
	GTK.gtk_label_set_yalign (labelHandle, yalign);
}

@Override
void setBackgroundGdkColor (GdkColor color) {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	super.setBackgroundGdkColor (color);
	setBackgroundGdkColor(fixedHandle, color);
	if (labelHandle != 0) setBackgroundGdkColor(labelHandle, color);
	if (imageHandle != 0) setBackgroundGdkColor(imageHandle, color);
}

@Override
int setBounds (int x, int y, int width, int height, boolean move, boolean resize) {
	/*
	* Bug in GTK.  For some reason, when the label is
	* wrappable and its container is resized, it does not
	* cause the label to be wrapped.  The fix is to
	* determine the size that will wrap the label
	* and expilictly set that size to force the label
	* to wrap.
	*
	* This part of the fix causes the label to be
	* resized to the preferred size but it still
	* won't draw properly.
	*/
	boolean fixWrap = resize && labelHandle != 0 && (style & SWT.WRAP) != 0;
	if (fixWrap) GTK.gtk_widget_set_size_request (labelHandle, -1, -1);
	int result = super.setBounds (x, y, width, height, move, resize);
	/*
	* Bug in GTK.  For some reason, when the label is
	* wrappable and its container is resized, it does not
	* cause the label to be wrapped.  The fix is to
	* determine the size that will wrap the label
	* and expilictly set that size to force the label
	* to wrap.
	*
	* This part of the fix forces the label to be
	* resized so that it will draw wrapped.
	*/
	if (fixWrap) {
		GtkAllocation allocation = new GtkAllocation();
		GTK.gtk_widget_get_allocation (handle, allocation);
		int labelWidth = allocation.width;
		int labelHeight = allocation.height;
		GTK.gtk_widget_set_size_request (labelHandle, labelWidth, labelHeight);
		/*
		* Bug in GTK.  Setting the size request should invalidate the label's
		* layout, but it does not.  The fix is to resize the label directly.
		*/
		GtkRequisition requisition = new GtkRequisition ();
		gtk_widget_get_preferred_size (labelHandle, requisition);
		GTK.gtk_widget_get_allocation(labelHandle, allocation);
		allocation.width = labelWidth;
		allocation.height = labelHeight;
		GTK.gtk_widget_size_allocate (labelHandle, allocation);
	}
	return result;
}

@Override
void setFontDescription (int /*long*/ font) {
	super.setFontDescription (font);
	if (labelHandle != 0) setFontDescription (labelHandle, font);
	if (imageHandle != 0) setFontDescription (imageHandle, font);

	// Bug 445801: Work around for computeSize not returning a different value after
	// changing font, see https://bugzilla.gnome.org/show_bug.cgi?id=753116
	if (GTK.GTK3) {
		// This updates the pango context and also clears the size request cache on the GTK side.
		int originalDirection = (style & SWT.RIGHT_TO_LEFT) != 0 ? GTK.GTK_TEXT_DIR_RTL : GTK.GTK_TEXT_DIR_LTR;
		int tempDirection = (style & SWT.RIGHT_TO_LEFT) != 0 ? GTK.GTK_TEXT_DIR_LTR : GTK.GTK_TEXT_DIR_RTL;
		GTK.gtk_widget_set_direction (labelHandle, tempDirection);
		GTK.gtk_widget_set_direction (labelHandle, originalDirection);
	}
}

@Override
void setForegroundGdkColor (GdkColor color) {
	assert !GTK.GTK3 : "GTK2 code was run by GTK3";
	super.setForegroundGdkColor(color);
	setForegroundColor (fixedHandle, color);
	if (labelHandle != 0) setForegroundColor (labelHandle, color);
	if (imageHandle != 0) setForegroundColor (imageHandle, color);
}

@Override
void setForegroundGdkRGBA (GdkRGBA rgba) {
	assert GTK.GTK3 : "GTK3 code was run by GTK2";
	super.setForegroundGdkRGBA (rgba);
	setForegroundGdkRGBA (fixedHandle, rgba);
	if (labelHandle != 0) setForegroundGdkRGBA (labelHandle, rgba);
	if (imageHandle != 0) setForegroundGdkRGBA (imageHandle, rgba);
}

@Override
void setOrientation (boolean create) {
	super.setOrientation (create);
	if ((style & SWT.RIGHT_TO_LEFT) != 0 || !create) {
		int dir = (style & SWT.RIGHT_TO_LEFT) != 0 ? GTK.GTK_TEXT_DIR_RTL : GTK.GTK_TEXT_DIR_LTR;
		if (labelHandle != 0) GTK.gtk_widget_set_direction (labelHandle, dir);
		if (imageHandle != 0) GTK.gtk_widget_set_direction (imageHandle, dir);
	}
}

/**
 * Sets the receiver's image to the argument, which may be
 * null indicating that no image should be displayed.
 *
 * @param image the image to display on the receiver (may be null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the image has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setImage (Image image) {
	checkWidget ();
	if (image != null && image.isDisposed ()) {
		error(SWT.ERROR_INVALID_ARGUMENT);
	}
	if ((style & SWT.SEPARATOR) != 0) return;
	this.image = image;
	if (imageList != null) imageList.dispose ();
	imageList = null;
	if (image != null) {
		imageList = new ImageList ();
		int imageIndex = imageList.add (image);
		int /*long*/ pixbuf = imageList.getPixbuf (imageIndex);
		gtk_image_set_from_pixbuf (imageHandle, pixbuf);
		GTK.gtk_widget_hide (labelHandle);
		GTK.gtk_widget_show (imageHandle);
	} else {
		gtk_image_set_from_pixbuf (imageHandle, 0);
		GTK.gtk_widget_show (labelHandle);
		GTK.gtk_widget_hide (imageHandle);
	}
}

/**
 * Sets the receiver's text.
 * <p>
 * This method sets the widget label.  The label may include
 * the mnemonic character and line delimiters.
 * </p>
 * <p>
 * Mnemonics are indicated by an '&amp;' that causes the next
 * character to be the mnemonic.  When the user presses a
 * key sequence that matches the mnemonic, focus is assigned
 * to the control that follows the label. On most platforms,
 * the mnemonic appears underlined but may be emphasised in a
 * platform specific manner.  The mnemonic indicator character
 * '&amp;' can be escaped by doubling it in the string, causing
 * a single '&amp;' to be displayed.
 * </p>
 * <p>
 * Note: If control characters like '\n', '\t' etc. are used
 * in the string, then the behavior is platform dependent.
 * </p>
 *
 * @param string the new text
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the text is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setText (String string) {
	checkWidget ();
	if (string == null) error (SWT.ERROR_NULL_ARGUMENT);
	if ((style & SWT.SEPARATOR) != 0) return;
	text = string;
	char [] chars = fixMnemonic (string);
	byte [] buffer = Converter.wcsToMbcs (chars, true);
	GTK.gtk_label_set_text_with_mnemonic (labelHandle, buffer);
	GTK.gtk_widget_hide (imageHandle);
	GTK.gtk_widget_show (labelHandle);
}

@Override
void setWidgetBackground  () {
	if (GTK.GTK_VERSION >= OS.VERSION(3, 14, 0)) {
		GdkRGBA rgba = (state & BACKGROUND) != 0 ? getBackgroundGdkRGBA () : null;
		super.setBackgroundGdkRGBA (handle, rgba);
	} else {
		super.setWidgetBackground();
	}
}

@Override
void showWidget () {
	super.showWidget ();
	if (frameHandle != 0) GTK.gtk_widget_show (frameHandle);
	if (labelHandle != 0) GTK.gtk_widget_show (labelHandle);
	if (boxHandle != 0) GTK.gtk_widget_show (boxHandle);
}

int /*long*/ gtk_separator_new (int orientation) {
	int /*long*/ separator = 0;
	if (GTK.GTK3) {
		separator = GTK.gtk_separator_new (orientation);
	} else {
		if (orientation == GTK.GTK_ORIENTATION_HORIZONTAL) {
			separator = GTK.gtk_hseparator_new ();
		} else {
			separator = GTK.gtk_vseparator_new ();
		}
	}
	return separator;
}

@Override
int /*long*/ windowProc (int /*long*/ handle, int /*long*/ arg0, int /*long*/ user_data) {
	/*
	 * For Labels/Buttons, the first widget in the tree with a GdkWindow is SwtFixed.
	 * Unfortunately this fails the check in !GTK_IS_CONTAINER check Widget.windowProc().
	 * Instead lets override windowProc() here and check for paintHandle() compatibility.
	 * Fixes bug 481485 without re-introducing bug 483791.
	 */
	switch ((int)/*64*/user_data) {
		case DRAW: {
			if (GTK.GTK_VERSION >= OS.VERSION(3, 9, 0) && paintHandle() == handle) {
				return gtk_draw(handle, arg0);
			}
		}
	}
	return super.windowProc(handle, arg0, user_data);
}
}
