package org.lwjgl.opengl.swt;

import static org.lwjgl.opengles.GLES20.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.swt.GLData.API;
import org.lwjgl.opengles.GLES;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Platform;

/**
 * Shows how to use OpenGL ES with SWT.
 * 
 * @author Kai Burjack
 */
public class NvGLESDemo {
	public static void main(String[] args) {
		final Display display = new Display();
		final Shell shell = new Shell(display, SWT.SHELL_TRIM | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
		shell.setLayout(new FillLayout());
		GLData data = new GLData();
		data.api = API.GLES;
		data.majorVersion = 2;
		data.minorVersion = 0;
		data.samples = 4;
		data.swapInterval = 1;
		final GLCanvas canvas = new GLCanvas(shell, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE, data);
		canvas.setCurrent();

		// Enable LWJGL3 to use GLES via GL library
		// From: http://bedroomcoders.co.uk/gles2-0-everywhere-thanks-to-lwjgl3/
		if (Platform.get() == Platform.WINDOWS ) {
		    // pretend we're using GLES in windows, instead use a subset of OpenGL 2.0 as GLES 2.0
		    Configuration.LIBRARY_NAME_OPENGLES.set("OpenGL32");
		    Configuration.EXPLICIT_INIT_OPENGLES.set(true);
		    org.lwjgl.opengles.GLES.create(org.lwjgl.opengl.GL.getFunctionProvider()); // omg?!
		}

		GLES.createCapabilities();

		final Rectangle rect = new Rectangle(0, 0, 0, 0);
		canvas.addListener(SWT.Resize, new Listener() {
			public void handleEvent(@SuppressWarnings("unused") Event event) {
				Rectangle bounds = canvas.getBounds();
				rect.width = bounds.width;
				rect.height = bounds.height;
			}
		});
		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				switch (event.detail) {
				case SWT.TRAVERSE_ESCAPE:
					shell.close();
					event.detail = SWT.TRAVERSE_NONE;
					event.doit = false;
					break;
				}
			}
		});

		glClearColor(0.3f, 0.5f, 0.8f, 1.0f);

		// Create a simple shader program
		int program = glCreateProgram();
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs,
				"#version 100\n" +
				"attribute vec3 vertex;" +
				"uniform float rot;" +
				"uniform float aspect;" +
				"void main(void) {" + 
				"  vec3 v = vertex * 0.5;" +
				"  vec3 v_ = vec3(0.0, 0.0, 0.0);" +
				"  v_.x = v.x * cos(rot) - v.y * sin(rot);" +
				"  v_.y = v.y * cos(rot) + v.x * sin(rot);" +
				"  v_.x /= aspect;" +
				"  gl_Position = vec4(v_, 1.0);" +
				"}");
		glCompileShader(vs);
		glAttachShader(program, vs);
		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs,
				"#version 100\n" +
				"precision mediump float;" +
				"void main(void) {" +
				"  gl_FragColor = vec4(0.1, 0.3, 0.5, 1.0);" + 
				"}");
		glCompileShader(fs);
		glAttachShader(program, fs);
		glBindAttribLocation(program, 0, "vertex");
		glLinkProgram(program);
		glUseProgram(program);
		final int rotLocation = glGetUniformLocation(program, "rot");
		final int aspectLocation = glGetUniformLocation(program, "aspect");

		// Create a simple quad
		int vbo = glGenBuffers();
		int ibo = glGenBuffers();
		float[] vertices = { 
			-1, -1, 0,
			 1, -1, 0,
			 1,  1, 0,
			-1,  1, 0
		};
		int[] indices = {
			0, 1, 2,
			2, 3, 0
		};
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) BufferUtils.createFloatBuffer(vertices.length).put(vertices).flip(), GL_STATIC_DRAW);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
		glEnableVertexAttribArray(0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, (IntBuffer) BufferUtils.createIntBuffer(indices.length).put(indices).flip(), GL_STATIC_DRAW);

		shell.setSize(800, 600);
		shell.open();

		display.asyncExec(new Runnable() {
			float rot;
			long lastTime = System.nanoTime();
			public void run() {
				if (!canvas.isDisposed()) {
					canvas.setCurrent();
					glClear(GL_COLOR_BUFFER_BIT);
					glViewport(0, 0, rect.width, rect.height);

					float aspect = (float) rect.width / rect.height;
					glUniform1f(aspectLocation, aspect);
					glUniform1f(rotLocation, rot);
					glDrawElements(GL11.GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

					canvas.swapBuffers();
					display.asyncExec(this);

					long thisTime = System.nanoTime();
					float delta = (thisTime - lastTime) / 1E9f;
					rot += delta * 0.1f;
					if (rot > 2.0 * Math.PI) {
						rot -= 2.0f * (float) Math.PI;
					}
					lastTime = thisTime;
				}
			}
		});

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}