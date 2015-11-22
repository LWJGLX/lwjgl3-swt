package org.lwjgl.opengl.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLContextAttributes;
import org.lwjgl.opengl.OpenGLContextException;

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

    public GLCanvas(Composite parent, int style, GLData data) {
        super(parent, checkStyle(parent, style));
        Canvas dummycanvas = new Canvas(parent, checkStyle(parent, style));
        parent.getDisplay().setData(USE_OWNDC_KEY, new Boolean(false));
        GLContextAttributes contextAttribs = data.toContextAttributes();
        try {
            context = GLContext.create(handle, dummycanvas.handle, contextAttribs);
        } catch (OpenGLContextException e) {
            /* There are no SWT errors which can handle all error cases, so just issue ERROR_IO. */
            SWT.error(SWT.ERROR_IO, e);
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
        // Somehow we need to temporarily set 'org.eclipse.swt.internal.win32.useOwnDC'
        // to true or else context creation on Windows fails...
        if (parent != null) {
            if (!OS.IsWinCE && OS.WIN32_VERSION >= OS.VERSION(6, 0)) {
                parent.getDisplay().setData(USE_OWNDC_KEY, new Boolean(true));
            }
        }
        return style;
    }

    public boolean isCurrent() {
        checkWidget();
        return GLContext.isCurrent(context);
    }

    public void setCurrent() {
        checkWidget();
        if (GLContext.isCurrent(context))
            return;
        GLContext.makeCurrent(handle, context);
    }

    public void swapBuffers() {
        checkWidget();
        GLContext.swapBuffers(handle);
    }

}
