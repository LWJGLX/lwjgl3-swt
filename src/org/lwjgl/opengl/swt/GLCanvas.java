package org.lwjgl.opengl.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLContextAttributes;
import org.lwjgl.opengl.OpenGLContextException;
import org.lwjgl.system.Platform;

/**
 * Drop-in replacement for SWT's {@link org.eclipse.swt.opengl.GLCanvas} class.
 * <p>
 * It supports creating OpenGL 3.0 and 3.2 core/compatibility contexts as well as multisampled framebuffers.
 * 
 * @author Kai Burjack
 */
public class GLCanvas extends Canvas {
    long context;
    int pixelFormat;
    GLData effective;

    private static PlatformGLCanvas platformCanvas;
    static {
        String platformClassName;
        switch (Platform.get()) {
        case WINDOWS:
            platformClassName = "org.lwjgl.opengl.swt.PlatformWin32GLCanvas";
            break;
        default:
            throw new AssertionError("NYI");
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends PlatformGLCanvas> clazz = (Class<? extends PlatformGLCanvas>) GLCanvas.class.getClassLoader().loadClass(platformClassName);
            platformCanvas = clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Platform-specific GLCanvas class not found: " + platformClassName);
        } catch (InstantiationException e) {
            throw new AssertionError("Could not instantiate platform-specific GLCanvas class: " + platformClassName);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not instantiate platform-specific GLCanvas class: " + platformClassName);
        }
    }

    /**
     * Create a GLCanvas widget using the attributes described in the GLData
     * object provided.
     *
     * @param parent a composite widget
     * @param style the bitwise OR'ing of widget styles
     * @param data the requested attributes of the GLCanvas
     *
     * @exception IllegalArgumentException
     * <ul><li>ERROR_NULL_ARGUMENT when the data is null
     *     <li>ERROR_UNSUPPORTED_DEPTH when the requested attributes cannot be provided</ul> 
     * </ul>
     */
    public GLCanvas(Composite parent, int style, GLData data) {
        super(parent, checkStyle(parent, style));
        if (Platform.get() == Platform.WINDOWS) {
            platformCanvas.resetStyle(parent);
        }
        if (data == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
        Canvas dummycanvas = new Canvas(parent, checkStyle(parent, style));
        GLContextAttributes contextAttribs = data.toContextAttributes();
        GLContextAttributes effectiveAttribs = new GLContextAttributes();
        try {
            context = GLContext.create(handle, dummycanvas.handle, contextAttribs, effectiveAttribs);
            effective = new GLData();
            effective.fromContextAttributes(effectiveAttribs);
        } catch (OpenGLContextException e) {
            SWT.error(SWT.ERROR_UNSUPPORTED_DEPTH, e);
        }
        dummycanvas.dispose();
        Listener listener = new Listener() {
            public void handleEvent(Event event) {
                switch (event.type) {
                case SWT.Dispose:
                    GLContext.deleteContext(context);
                    break;
                }
            }
        };
        addListener(SWT.Dispose, listener);
    }

    private static int checkStyle(Composite parent, int style) {
        return platformCanvas.checkStyle(parent, style);
    }

    /**
     * Returns a GLData object describing the created context.
     *  
     * @return GLData description of the OpenGL context attributes
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     */
    public GLData getGLData() {
        return effective;
    }

    /**
     * Returns a boolean indicating whether the receiver's OpenGL context
     * is the current context.
     *  
     * @return true if the receiver holds the current OpenGL context,
     * false otherwise
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     */
    public boolean isCurrent() {
        checkWidget();
        return GLContext.isCurrent(context);
    }

    /**
     * Sets the OpenGL context associated with this GLCanvas to be the
     * current GL context.
     * 
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     */
    public void setCurrent() {
        checkWidget();
        if (GLContext.isCurrent(context))
            return;
        GLContext.makeCurrent(handle, context);
    }

    /**
     * Swaps the front and back color buffers.
     * 
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     */
    public void swapBuffers() {
        checkWidget();
        GLContext.swapBuffers(handle);
    }

    /**
     * Blocks until <code>seconds</code> seconds before a synchronized swap would occur.
     * 
     * @return <code>true</code> if the implementation had to wait for the synchronized swap; <code>false</code> otherwise
     */
    public boolean delayBeforeSwapNV(float seconds) {
        return GLContext.delayBeforeSwapNV(handle, seconds);
    }

}
