package org.lwjgl.opengl.swt;

import org.eclipse.swt.widgets.Composite;

/**
 * Interface of platform-specific GLCanvas delegate classes.
 * 
 * @author Kai Burjack
 */
interface PlatformGLCanvas {

    long create(GLCanvas canvas, GLData attribs, GLData effective);

    boolean isCurrent(long context);

    boolean makeCurrent(GLCanvas canvas, long context);

    boolean deleteContext(GLCanvas canvas, long context);

    boolean swapBuffers(GLCanvas canvas);

    public int glGetSwapInterval(GLCanvas canvas);

    public boolean glSwapInterval(GLCanvas canvas, int interval);

    boolean delayBeforeSwapNV(GLCanvas canvas, float seconds);

    int checkStyle(Composite parent, int style);

    void resetStyle(Composite parent);

}
