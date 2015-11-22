package org.lwjgl.opengl.swt;

import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Abstract base class for all platform-specific GLCanvas implementations.
 * 
 * @author Kai Burjack
 */
public abstract class AbstractGLCanvas extends Canvas {

    /**
     * The GL context.
     */
    protected long context;

    protected AbstractGLCanvas(Composite paramComposite, int paramInt) {
        super(paramComposite, paramInt);
    }

}
