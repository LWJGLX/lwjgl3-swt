package org.lwjgl.opengl.swt;

import org.eclipse.swt.widgets.Composite;

/**
 * Interface of platform-specific GLCanvas delegate classes.
 * 
 * @author Kai Burjack
 */
interface PlatformGLCanvas {

    long create(GLCanvas canvas, Composite parent, int style, GLData attribs, GLData effective);

    boolean isCurrent(long context);

    boolean makeCurrent(long windowHandle, long context);

    boolean deleteContext(long context);

    boolean swapBuffers(long windowHandle);

    boolean delayBeforeSwapNV(long windowHandle, float seconds);

    int checkStyle(Composite parent, int style);

    void resetStyle(Composite parent);

}
