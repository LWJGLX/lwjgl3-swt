package org.lwjgl.opengl;

@SuppressWarnings("serial")
public class OpenGLContextException extends Exception {

    public OpenGLContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenGLContextException(String message) {
        super(message);
    }

}
