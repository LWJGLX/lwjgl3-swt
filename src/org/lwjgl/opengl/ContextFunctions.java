package org.lwjgl.opengl;

/**
 * Abstract interface to encapsulate interfacing with platform functions to managed OpenGL contexts.
 * 
 * @author Kai Burjack
 */
public interface ContextFunctions {

    long create(long windowHandle, long dummyWindowHandle, GLContextAttributes attribs) throws OpenGLContextException;

    boolean isCurrent(long context);

    boolean makeCurrent(long windowHandle, long context);

    boolean deleteContext(long context);

    boolean swapBuffers(long windowHandle);

    boolean delayBeforeSwapNV(long windowHandle, float seconds);

}
