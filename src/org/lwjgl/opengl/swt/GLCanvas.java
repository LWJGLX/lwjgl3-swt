package org.lwjgl.opengl.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.lwjgl.system.Platform;

/** Drop-in replacement for SWT's {@link org.eclipse.swt.opengl.GLCanvas} class.
 * <p>
 * It supports creating OpenGL 3.0 and 3.2 core/compatibility contexts as well
 * as multisampled framebuffers.
 * 
 * @author Kai Burjack */
public class GLCanvas extends Canvas {
	GLData effective;
	long context;
	long xWindow;
	long glWindow;
	
	private static PlatformGLCanvas platformCanvas;
	static {
		String platformClassName;
		switch(Platform.get()) {
		case WINDOWS:
			platformClassName = "org.lwjgl.opengl.swt.PlatformWin32GLCanvas";
			break;
		case LINUX:
			platformClassName = "org.lwjgl.opengl.swt.PlatformLinuxGLCanvas";
			break;
		case MACOSX:
			platformClassName = "org.lwjgl.opengl.swt.PlatformMacOSXGLCanvas";
			break;
		default:
			throw new AssertionError("NYI");
		}
		try {
			@SuppressWarnings("unchecked")
			Class<? extends PlatformGLCanvas> clazz = (Class<? extends PlatformGLCanvas>) GLCanvas.class.getClassLoader().loadClass(platformClassName);
			platformCanvas = clazz.newInstance();
		} catch(ClassNotFoundException e) {
			throw new AssertionError("Platform-specific GLCanvas class not found: " + platformClassName);
		} catch(InstantiationException e) {
			throw new AssertionError("Could not instantiate platform-specific GLCanvas class: " + platformClassName);
		} catch(IllegalAccessException e) {
			throw new AssertionError("Could not instantiate platform-specific GLCanvas class: " + platformClassName);
		}
	}
	
	/** Create a GLCanvas widget using the attributes described in the GLData
	 * object provided.
	 *
	 * @param parent a composite widget
	 * @param style the bitwise OR'ing of widget styles
	 * @param data the requested attributes of the GLCanvas
	 *
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT when the data is null
	 *                <li>ERROR_UNSUPPORTED_DEPTH when the requested attributes
	 *                cannot be provided
	 *                </ul>
	 */
	public GLCanvas(Composite parent, int style, GLData data) {
		super(parent, platformCanvas.checkStyle(parent, style));
		if(Platform.get() == Platform.WINDOWS) {
			platformCanvas.resetStyle(parent);
		}
		if(data == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		effective = new GLData();
		context = platformCanvas.create(this, data, effective);
	}
	
	/** Returns a GLData object describing the created context.
	 * 
	 * @return GLData description of the OpenGL context attributes
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public GLData getGLData() {
		//checkWidget();
		return effective;
	}
	
	/** Returns a boolean indicating whether the receiver's OpenGL context
	 * is the current context.
	 * 
	 * @return true if the receiver holds the current OpenGL context,
	 *         false otherwise
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public boolean isCurrent() {
		//checkWidget();
		return platformCanvas.isCurrent(context);
	}
	
	/** Sets the OpenGL context associated with this GLCanvas to be the
	 * current GL context.
	 * 
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public void setCurrent() {
		//checkWidget();
		if(platformCanvas.isCurrent(context)) return;
		platformCanvas.makeCurrent(this, context);
	}
	
	/** Swaps the front and back color buffers.
	 * 
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public void swapBuffers() {
		//checkWidget();
		platformCanvas.swapBuffers(this);
	}
	
	/** Returns the current swap interval for the window associated with the
	 * current context.
	 * 
	 * @return The current swap interval for the window associated with the
	 *         current context. */
	public int glGetSwapInterval() {
		//checkWidget();
		return platformCanvas.glGetSwapInterval(this);
	}
	
	/** Specifies the minimum number of video frame periods per buffer swap for
	 * the window associated with the current context. The interval takes effect
	 * when
	 * {@link #swapBuffers()} or glSwapLayerBuffer is first called subsequent to
	 * the {@code glSwapInterval} call.
	 * 
	 * <p>
	 * A video frame period is the time required by the monitor to display a
	 * full frame of video data. In the case of an interlaced monitor, this is
	 * typically
	 * the time required to display both the even and odd fields of a frame of
	 * video data. An interval set to a value of 2 means that the color buffers
	 * will
	 * be swapped at most every other video frame.
	 * </p>
	 * 
	 * <p>
	 * If {@code interval} is set to a value of 0, buffer swaps are not
	 * synchronized to a video frame. The {@code interval} value is silently
	 * clamped to the
	 * maximum implementation-dependent value supported before being stored.
	 * </p>
	 * 
	 * <p>
	 * The swap interval is not part of the render context state. It cannot be
	 * pushed or popped. The default swap interval is 1.
	 * </p>
	 *
	 * @param interval the minimum number of video frames that are displayed
	 *            before a buffer swap will occur
	 * @return <code>true</code> if the implementation was able to set the swap
	 *         interval */
	public boolean glSwapInterval(int interval) {
		//checkWidget();
		return platformCanvas.glSwapInterval(this, interval);
	}
	
	/** Blocks until <code>seconds</code> seconds before a synchronized swap
	 * would occur.
	 * 
	 * @param seconds
	 *            the seconds to wait until a synchronized swap would occur
	 * @return <code>true</code> if the implementation had to wait for the
	 *         synchronized swap; <code>false</code> otherwise */
	public boolean delayBeforeSwapNV(float seconds) {
		//checkWidget();
		return platformCanvas.delayBeforeSwapNV(this, seconds);
	}
	
	public void deleteContext() {
		platformCanvas.deleteContext(this, this.context);
	}
	
}
