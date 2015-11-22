package org.lwjgl.opengl.swt;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

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
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

/**
 * SWT with OpenGL 3.3 core and multisampling.
 * <p>
 * You can verify that a 3.3 core context is being created by removing/commenting the call to glBindVertexArray and see that nothing gets drawn, because with
 * 3.3 core, VAOs MUST be used to draw something.
 * 
 * @author Kai Burjack
 */
public class Swt33CoreMsDemo {
    public static void main(String[] args) {
        int minClientWidth = 640;
        int minClientHeight = 480;
        final Display display = new Display();
        final Shell shell = new Shell(display, SWT.SHELL_TRIM | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
        shell.setLayout(new FillLayout());
        int dw = shell.getSize().x - shell.getClientArea().width;
        int dh = shell.getSize().y - shell.getClientArea().height;
        shell.setMinimumSize(minClientWidth + dw, minClientHeight + dh);
        GLData data = new GLData();
        data.majorVersion = 3;
        data.minorVersion = 3;
        data.core = true;
        data.samples = 4;
        final GLCanvas canvas = new GLCanvas(shell, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE, data);
        canvas.setCurrent();
        GL.createCapabilities();

        final Rectangle rect = new Rectangle(0, 0, 0, 0);
        canvas.addListener(SWT.Resize, new Listener() {
            public void handleEvent(Event event) {
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
        glShaderSource(vs, "uniform float rot;" + "uniform float aspect;" + "void main(void) {" + "  vec4 v = gl_Vertex * 0.5;"
                + "  vec4 v_ = vec4(0.0, 0.0, 0.0, 1.0);" + "  v_.x = v.x * cos(rot) - v.y * sin(rot);" + "  v_.y = v.y * cos(rot) + v.x * sin(rot);"
                + "  v_.x /= aspect;" + "  gl_Position = v_;" + "}");
        glCompileShader(vs);
        glAttachShader(program, vs);
        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, "void main(void) {" + "  gl_FragColor = vec4(0.1, 0.3, 0.5, 1.0);" + "}");
        glCompileShader(fs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        glUseProgram(program);
        final int rotLocation = glGetUniformLocation(program, "rot");
        final int aspectLocation = glGetUniformLocation(program, "aspect");

        // Create a simple quad
        int vbo = glGenBuffers();
        int ibo = glGenBuffers();
        int vao = glGenVertexArrays();
        float[] vertices = { -1, -1, 0, 1, -1, 0, 1, 1, 0, -1, 1, 0 };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        glBindVertexArray(vao);
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