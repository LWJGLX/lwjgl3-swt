package org.lwjgl.opengl.swt;

import org.eclipse.swt.widgets.Composite;

/**
 * Linux-specific implementation of methods for GLCanvas.
 * 
 * @author Joshua Slack
 */
class PlatformLinuxGLCanvas extends AbstractPlatformGLCanvas {

	@Override
	public long create(GLCanvas canvas, GLData attribs, GLData effective) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isCurrent(long context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean makeCurrent(GLCanvas canvas, long context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteContext(long context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean swapBuffers(GLCanvas canvas) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delayBeforeSwapNV(GLCanvas canvas, float seconds) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int checkStyle(Composite parent, int style) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void resetStyle(Composite parent) {
		// TODO Auto-generated method stub
		
	}

}
