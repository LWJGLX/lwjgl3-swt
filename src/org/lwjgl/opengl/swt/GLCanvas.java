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
    static final String USE_OWNDC_KEY = "org.eclipse.swt.internal.win32.useOwnDC";

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
        parent.getDisplay().setData(USE_OWNDC_KEY, Boolean.FALSE);
        if (data == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
        Canvas dummycanvas = new Canvas(parent, checkStyle(parent, style));
        GLContextAttributes contextAttribs = data.toContextAttributes();
        try {
            context = GLContext.create(handle, dummycanvas.handle, contextAttribs);
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

    private static int checkStyleWin32(Composite parent, int style) {
        // Somehow we need to temporarily set 'org.eclipse.swt.internal.win32.useOwnDC'
        // to true or else context creation on Windows fails...
        if (parent != null) {
            if (!org.eclipse.swt.internal.win32.OS.IsWinCE
                    && org.eclipse.swt.internal.win32.OS.WIN32_VERSION >= org.eclipse.swt.internal.win32.OS.VERSION(6, 0)) {
                parent.getDisplay().setData(USE_OWNDC_KEY, Boolean.TRUE);
            }
        }
        return style;
    }

    private static int checkStyle(Composite parent, int style) {
        if (Platform.get() == Platform.WINDOWS) {
            return checkStyleWin32(parent, style);
        }
        return style;
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
    public GLData getGLData () {
        throw new UnsupportedOperationException("NYI");
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
