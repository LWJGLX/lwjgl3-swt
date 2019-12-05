package org.lwjgl.opengl.swt;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.swt.GLCanvas;
import org.lwjgl.opengl.swt.GLData;
import org.lwjgl.system.Platform;

/**
 * Showcases context sharing.
 * 
 * @author Kai Burjack
 */
public class SharedContextsDemo {
    public static void main(String[] args) {
        int minClientWidth = 600;
        int minClientHeight = 300;
        final Display display = new Display();
        final Shell shell = new Shell(display, SWT.SHELL_TRIM);
        shell.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.stateMask == SWT.ALT && (e.keyCode == SWT.KEYPAD_CR || e.keyCode == SWT.CR)) {
                    if (Platform.get() == Platform.WINDOWS) {
                        // Fix crappy/buggy fullscreen mode in SWT
                    	SwtHelperWin32.properFullscreen(shell);
                    } else {
                        shell.setFullScreen(!shell.getFullScreen());
                    }
                }
            }
        });
        GridLayout layout = new GridLayout(2, false);
        shell.setLayout(layout);
        int dw = shell.getSize().x - shell.getClientArea().width;
        int dh = shell.getSize().y - shell.getClientArea().height;
        shell.setMinimumSize(minClientWidth + dw, minClientHeight + dh);
        GLData data = new GLData();
        data.doubleBuffer = true;
        data.swapInterval = 1;
        data.samples = 2;
        final GLCanvas canvas0 = new GLCanvas(shell, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE, data);
        canvas0.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        data.shareContext = canvas0;
        final GLCanvas canvas1 = new GLCanvas(shell, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE, data);
        canvas1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        final GLCanvas[] canvases = { canvas0, canvas1 };

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

        shell.setSize(600, 300);
        shell.open();
        
        // Create GLCapabilities in the first context
        canvas0.setCurrent();
        GL.createCapabilities();
        // Create resources in the first context
        // Create a simple shader program
        final int program = glCreateProgram();
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs,
        		"uniform float rot;" +
                "uniform float aspect;" +
                "void main(void) {" + 
                "  vec4 v = gl_Vertex * 0.5;" +
                "  vec4 v_ = vec4(0.0, 0.0, 0.0, 1.0);" +
                "  v_.x = v.x * cos(rot) - v.y * sin(rot);" +
                "  v_.y = v.y * cos(rot) + v.x * sin(rot);" +
                "  v_.x /= aspect;" +
                "  gl_Position = v_;" +
                "}");
        glCompileShader(vs);
        glAttachShader(program, vs);
        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs,
                "void main(void) {" +
                "  gl_FragColor = vec4(0.1, 0.3, 0.5, 1.0);" + 
                "}");
        glCompileShader(fs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        glUseProgram(program);
        final int rotLocation = glGetUniformLocation(program, "rot");
        final int aspectLocation = glGetUniformLocation(program, "aspect");

        // Create a simple quad
        final int vbo = glGenBuffers();
        final int ibo = glGenBuffers();
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
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (IntBuffer) BufferUtils.createIntBuffer(indices.length).put(indices).flip(), GL_STATIC_DRAW);

        // Bind objects in each context and set state
        for (GLCanvas canvas : canvases) {
            canvas.setCurrent();
            glClearColor(0.4f, 0.6f, 0.9f, 1.0f);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glVertexPointer(3, GL_FLOAT, 0, 0L);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
            glEnableClientState(GL_VERTEX_ARRAY);
            glUseProgram(program);
        }
        
        display.asyncExec(new Runnable() {
            float rot;
            long lastTime = System.nanoTime();

            public void run() {
                // Render in each context
                for (int i = 0; i < canvases.length; i++) {
                    GLCanvas canvas = canvases[i];
                    if (canvas.isDisposed()) {
                    	return;
                    }
                    canvas.setCurrent();
                    glClear(GL_COLOR_BUFFER_BIT);
                    glViewport(0, 0, canvas.getSize().x, canvas.getSize().y);
                    float aspect = (float) canvas.getSize().x / canvas.getSize().y;
                    glUniform1f(aspectLocation, aspect);
                    glUniform1f(rotLocation, rot);
                    glDrawElements(GL11.GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
                    canvas.swapBuffers();
                }
                display.asyncExec(this);

                long thisTime = System.nanoTime();
                float delta = (thisTime - lastTime) / 1E9f;
                rot += delta * 0.1f;
                if (rot > 2.0 * Math.PI) {
                    rot -= 2.0f * (float) Math.PI;
                }
                lastTime = thisTime;
            }
        });

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
    }
}