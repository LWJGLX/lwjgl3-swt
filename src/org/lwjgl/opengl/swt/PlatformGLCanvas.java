package org.lwjgl.opengl.swt;

import org.eclipse.swt.widgets.Composite;

/**
 * Interface of platform-specific methods for GLCanvas.
 * 
 * @author Kai Burjack
 */
public abstract class PlatformGLCanvas {

    abstract int checkStyle(Composite parent, int style);

    abstract void resetStyle(Composite parent);

}
