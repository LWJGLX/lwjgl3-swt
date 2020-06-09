/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.swt.graphics;


import org.eclipse.swt.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * Instances of this class manage the operating system resources that
 * implement SWT's RGB color model. To create a color you can either
 * specify the individual color components as integers in the range
 * 0 to 255 or provide an instance of an <code>RGB</code> or <code>RGBA</code>.
 * <p>
 * Application code must explicitly invoke the <code>Color.dispose()</code>
 * method to release the operating system resources managed by each instance
 * when those instances are no longer required.
 * </p>
 *
 * @see RGB
 * @see RGBA
 * @see Device#getSystemColor
 * @see <a href="http://www.eclipse.org/swt/snippets/#color">Color and RGB snippets</a>
 * @see <a href="http://www.eclipse.org/swt/examples.php">SWT Example: PaintExample</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 */
public final class Color extends Resource {
	/**
	 * the handle to the OS color resource
	 * (Warning: This field is platform dependent)
	 * <p>
	 * <b>IMPORTANT:</b> This field is <em>not</em> part of the SWT
	 * public API. It is marked public only so that it can be shared
	 * within the packages provided by SWT. It is not available on all
	 * platforms and should never be accessed from application code.
	 * </p>
	 *
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public GdkRGBA handle;
	int alpha = 0;

Color(Device device) {
	super(device);
}

/**
 * Constructs a new instance of this class given a device and the
 * desired red, green and blue values expressed as ints in the range
 * 0 to 255 (where 0 is black and 255 is full brightness). On limited
 * color devices, the color instance created by this call may not have
 * the same RGB values as the ones specified by the arguments. The
 * RGB values on the returned instance will be the color values of
 * the operating system color.
 * <p>
 * You must dispose the color when it is no longer required.
 * </p>
 *
 * @param device the device on which to allocate the color
 * @param red the amount of red in the color
 * @param green the amount of green in the color
 * @param blue the amount of blue in the color
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if device is null and there is no current device</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the red, green or blue argument is not between 0 and 255</li>
 * </ul>
 *
 * @see #dispose
 */
public Color(Device device, int red, int green, int blue) {
	super(device);
	init(red, green, blue, 255);
	init();
}

/**
 * Constructs a new instance of this class given a device and the
 * desired red, green, blue &amp; alpha values expressed as ints in the range
 * 0 to 255 (where 0 is black and 255 is full brightness). On limited
 * color devices, the color instance created by this call may not have
 * the same RGB values as the ones specified by the arguments. The
 * RGB values on the returned instance will be the color values of
 * the operating system color.
 * <p>
 * You must dispose the color when it is no longer required.
 * </p>
 *
 * @param device the device on which to allocate the color
 * @param red the amount of red in the color
 * @param green the amount of green in the color
 * @param blue the amount of blue in the color
 * @param alpha the amount of alpha in the color. Currently, SWT only honors extreme values for alpha i.e. 0 (transparent) or 255 (opaque).
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if device is null and there is no current device</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the red, green, blue or alpha argument is not between 0 and 255</li>
 * </ul>
 *
 * @see #dispose
 * @since 3.104
 */
public Color(Device device, int red, int green, int blue, int alpha) {
	super(device);
	init(red, green, blue, alpha);
	init();
}

/**
 * Constructs a new instance of this class given a device and an
 * <code>RGB</code> describing the desired red, green and blue values.
 * On limited color devices, the color instance created by this call
 * may not have the same RGB values as the ones specified by the
 * argument. The RGB values on the returned instance will be the color
 * values of the operating system color.
 * <p>
 * You must dispose the color when it is no longer required.
 * </p>
 *
 * @param device the device on which to allocate the color
 * @param rgb the RGB values of the desired color
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if device is null and there is no current device</li>
 *    <li>ERROR_NULL_ARGUMENT - if the rgb argument is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the red, green or blue components of the argument are not between 0 and 255</li>
 * </ul>
 *
 * @see #dispose
 */
public Color(Device device, RGB rgb) {
	super(device);
	if (rgb == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	init(rgb.red, rgb.green, rgb.blue, 255);
	init();
}

/**
 * Constructs a new instance of this class given a device and an
 * <code>RGBA</code> describing the desired red, green, blue &amp; alpha values.
 * On limited color devices, the color instance created by this call
 * may not have the same RGBA values as the ones specified by the
 * argument. The RGBA values on the returned instance will be the color
 * values of the operating system color + alpha.
 * <p>
 * You must dispose the color when it is no longer required.
 * </p>
 *
 * @param device the device on which to allocate the color
 * @param rgba the RGBA values of the desired color. Currently, SWT only honors extreme values for alpha i.e. 0 (transparent) or 255 (opaque).
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if device is null and there is no current device</li>
 *    <li>ERROR_NULL_ARGUMENT - if the rgba argument is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the red, green, blue or alpha components of the argument are not between 0 and 255</li>
 * </ul>
 *
 * @see #dispose
 * @since 3.104
 */
public Color(Device device, RGBA rgba) {
	super(device);
	if (rgba == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	init(rgba.rgb.red, rgba.rgb.green, rgba.rgb.blue, rgba.alpha);
	init();
}

/**
 * Constructs a new instance of this class given a device, an
 * <code>RGB</code> describing the desired red, green and blue values,
 * alpha specifying the level of transparency.
 * On limited color devices, the color instance created by this call
 * may not have the same RGB values as the ones specified by the
 * argument. The RGB values on the returned instance will be the color
 * values of the operating system color.
 * <p>
 * You must dispose the color when it is no longer required.
 * </p>
 *
 * @param device the device on which to allocate the color
 * @param rgb the RGB values of the desired color
 * @param alpha the alpha value of the desired color. Currently, SWT only honors extreme values for alpha i.e. 0 (transparent) or 255 (opaque).
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if device is null and there is no current device</li>
 *    <li>ERROR_NULL_ARGUMENT - if the rgb argument is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the red, green, blue or alpha components of the argument are not between 0 and 255</li>
 * </ul>
 *
 * @see #dispose
 * @since 3.104
 */
public Color(Device device, RGB rgb, int alpha) {
	super(device);
	if (rgb == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	init(rgb.red, rgb.green, rgb.blue, alpha);
	init();
}

@Override
void destroy() {
	handle = null;
}

/**
 * Compares the argument to the receiver, and returns true
 * if they represent the <em>same</em> object using a class
 * specific comparison.
 *
 * @param object the object to compare with this object
 * @return <code>true</code> if the object is the same as this object and <code>false</code> otherwise
 *
 * @see #hashCode
 */
@Override
public boolean equals(Object object) {
	if (object == this) return true;
	if (!(object instanceof Color)) return false;
	Color color = (Color)object;
	GdkRGBA gdkRGBA = color.handle;
	if (handle == gdkRGBA) return true;
	if (this.getRed() != color.getRed()) return false;
	if (this.getGreen() != color.getGreen()) return false;
	if (this.getBlue() != color.getBlue()) return false;
	if (this.getAlpha() != color.getAlpha()) return false;
	return device == color.device;
}

/**
 * Returns the amount of alpha in the color, from 0 (transparent) to 255 (opaque).
 *
 * @return the alpha component of the color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_GRAPHIC_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * @since 3.104
 */
public int getAlpha() {
	if (isDisposed()) SWT.error(SWT.ERROR_GRAPHIC_DISPOSED);
	return alpha;
}

/**
 * Returns the amount of blue in the color, from 0 to 255.
 *
 * @return the blue component of the color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_GRAPHIC_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public int getBlue() {
	if (isDisposed()) SWT.error(SWT.ERROR_GRAPHIC_DISPOSED);
	/*
	 * Conversion formula comes from Cairo's _cairo_color_double_to_short()
	 * and color_to_pixel() functions. See bug 549181 and 549101 for more info.
	 */
	int b = (((int)(handle.blue * 65535.0 + 0.5)) >> 8);
	return Math.min(b, 255);
}

/**
 * Returns the amount of green in the color, from 0 to 255.
 *
 * @return the green component of the color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_GRAPHIC_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public int getGreen() {
	if (isDisposed()) SWT.error(SWT.ERROR_GRAPHIC_DISPOSED);
	/*
	 * Conversion formula comes from Cairo's _cairo_color_double_to_short()
	 * and color_to_pixel() functions. See bug 549181 and 549101 for more info.
	 */
	int g = (((int)(handle.green * 65535.0 + 0.5)) >> 8);
	return Math.min(g, 255);
}

/**
 * Returns the amount of red in the color, from 0 to 255.
 *
 * @return the red component of the color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_GRAPHIC_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public int getRed() {
	if (isDisposed()) SWT.error(SWT.ERROR_GRAPHIC_DISPOSED);
	/*
	 * Conversion formula comes from Cairo's _cairo_color_double_to_short()
	 * and color_to_pixel() functions. See bug 549181 and 549101 for more info.
	 */
	int r = (((int)(handle.red * 65535.0 + 0.5)) >> 8);
	return Math.min(r, 255);
}

/**
 * Returns an integer hash code for the receiver. Any two
 * objects that return <code>true</code> when passed to
 * <code>equals</code> must return the same value for this
 * method.
 *
 * @return the receiver's hash
 *
 * @see #equals
 */
@Override
public int hashCode() {
	if (isDisposed()) return 0;
	return (((this.getAlpha() * 31) + this.getGreen()) * 31 + this.getBlue()) * 31 + this.getRed();
}

/**
 * Returns an <code>RGB</code> representing the receiver.
 *
 * @return the RGB for the color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_GRAPHIC_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public RGB getRGB () {
	if (isDisposed()) SWT.error(SWT.ERROR_GRAPHIC_DISPOSED);
	return new RGB(getRed(), getGreen(), getBlue());
}

/**
 * Returns an <code>RGBA</code> representing the receiver.
 *
 * @return the RGBA for the color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_GRAPHIC_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * @since 3.104
 */
public RGBA getRGBA () {
	if (isDisposed()) SWT.error(SWT.ERROR_GRAPHIC_DISPOSED);
	return new RGBA(getRed(), getGreen(), getBlue(), getAlpha());
}

/**
 * Invokes platform specific functionality to allocate a new color.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Color</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param device the device on which to allocate the color
 * @param gdkRGBA the handle for the color
 *
 * @noreference This method is not intended to be referenced by clients.
 */
public static Color gtk_new(Device device, GdkRGBA gdkRGBA) {
	Color color = new Color(device);
	color.handle = gdkRGBA;
	color.alpha = (int) (gdkRGBA.alpha * 255);
	return color;
}

/**
 * Invokes platform specific functionality to allocate a new color.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Color</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param device the device on which to allocate the color
 * @param gdkRGBA the handle for the color
 * @param alpha the int for the alpha content in the color(Currently SWT honors extreme values for alpha ie. 0 or 255)
 *
 * @noreference This method is not intended to be referenced by clients.
 */
public static Color gtk_new(Device device, GdkRGBA gdkRGBA, int alpha) {
	Color color = new Color(device);
	color.handle = gdkRGBA;
	color.alpha = alpha;
	return color;
}

/**
 * Allocates the operating system resources associated
 * with the receiver.
 *
 * @param device the device on which to allocate the color
 * @param red the amount of red in the color
 * @param green the amount of green in the color
 * @param blue the amount of blue in the color
 * @param alpha the amount of alpha in the color
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the red, green, blue or alpha argument is not between 0 and 255</li>
 * </ul>
 *
 * @see #dispose
 */
void init(int red, int green, int blue, int alpha) {
	if ((red > 255) || (red < 0) ||
		(green > 255) || (green < 0) ||
		(blue > 255) || (blue < 0) ||
		(alpha > 255) || (alpha < 0)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}
	GdkRGBA rgba = new GdkRGBA ();
	rgba.red = (double) red / 255;
	rgba.green = (double) green / 255;
	rgba.blue = (double) blue / 255;
	rgba.alpha = (double) alpha / 255;
	this.alpha = alpha;
	handle = rgba;
}

/**
 * Returns <code>true</code> if the color has been disposed,
 * and <code>false</code> otherwise.
 * <p>
 * This method gets the dispose state for the color.
 * When a color has been disposed, it is an error to
 * invoke any other method (except {@link #dispose()}) using the color.
 *
 * @return <code>true</code> when the color is disposed and <code>false</code> otherwise
 */
@Override
public boolean isDisposed() {
	return handle == null;
}

/**
 * Returns a string containing a concise, human-readable
 * description of the receiver.
 *
 * @return a string representation of the receiver
 */
@Override
public String toString () {
	if (isDisposed()) return "Color {*DISPOSED*}";
	return "Color {" + getRed() + ", " + getGreen() + ", " + getBlue() + ", " + getAlpha() +"}";
}

}
