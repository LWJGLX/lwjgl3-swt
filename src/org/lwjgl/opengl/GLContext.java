package org.lwjgl.opengl;

import org.lwjgl.system.Platform;

/**
 * Represents a platform OpenGL context.
 * 
 * @author Kai Burjack
 */
public class GLContext {

    private static ContextFunctions funcs;
    static {
        funcs = createFunctions();
    }

    private static ContextFunctions createFunctions() {
        switch (Platform.get()) {
        case WINDOWS:
            return new Win32ContextFunctions();
        case LINUX:
        case MACOSX:
            throw new UnsupportedOperationException("NYI");
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * Create a GL context and return the opaque handle to be used by other methods such as {@link #makeCurrent(long, long)}, {@link #isCurrent(long)} or
     * {@link #deleteContext(long)}.
     * 
     * @see #makeCurrent(long, long)
     * @see #isCurrent(long)
     * @see #deleteContext(long)
     * 
     * @param windowHandle
     *            the window handle on which the final GL context will be created
     * @param dummyWindowHandle
     *            a dummy window handle that is used to query supported pixel formats on platforms where this is necessary, such as Windows
     * @param attribs
     *            the {@link GLContextAttributes}
     * @return the opaque handle of the new context
     * @throws OpenGLContextException
     */
    public static long create(long windowHandle, long dummyWindowHandle, GLContextAttributes attribs) throws OpenGLContextException {
        return funcs.create(windowHandle, dummyWindowHandle, attribs);
    }

    /**
     * Determine whether the given GL context is current in the calling thread.
     * 
     * @param context
     *            the handle of a GL context
     * @return <code>true</code> if the given context is current; <code>false</code> otherwise
     */
    public static boolean isCurrent(long context) {
        return funcs.isCurrent(context);
    }

    public static boolean makeCurrent(long windowHandle, long context) {
        return funcs.makeCurrent(windowHandle, context);
    }

    public static boolean deleteContext(long context) {
        return funcs.deleteContext(context);
    }

    public static boolean swapBuffers(long windowHandle) {
        return funcs.swapBuffers(windowHandle);
    }

}
