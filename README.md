## What is it?

Revised drop-in implementation of SWT's GLCanvas.

## What does it get me?

Support for creating:
- OpenGL 3.0 and 3.2 core/compatibility contexts
- Multisampled framebuffers

## Why does it exist?

The above features have been lacking for some years in the SWT-provided GLCanvas implementation.
The purpose of the new implementation on top of LWJGL 3 is to have full support for those features in an OpenGL SWT application.

## How to use it?

In your existing SWT application just replace all imports of `org.eclipse.swt.opengl.*` with `org.lwjgl.opengl.swt.*`.
The new implementation is a drop-in replacement, which means that your current SWT code should work like before.
To use the new features you can use the new fields in the `GLData` class, such as using real multisampled framebuffers on Windows
or creating a OpenGL 3.2 core context.

If your current OpenGL SWT setup looks like this:
```Java
		final Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		GLData data = new GLData();
		GLCanvas canvas = new GLCanvas(shell, 0, data);
```
then adding multisampling and using a OpenGL 3.2 core context is as easy as doing:
```Java
		final Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		GLData data = new GLData();
		data.core = true;
		data.majorVersion = 3;
		data.minorVersion = 2;
		data.samples = 4; // 4x multisampling
		GLCanvas canvas = new GLCanvas(shell, 0, data);
```
