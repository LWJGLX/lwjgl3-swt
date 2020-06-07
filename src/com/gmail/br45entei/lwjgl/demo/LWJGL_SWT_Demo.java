package com.gmail.br45entei.lwjgl.demo;

import com.gmail.br45entei.util.CodeUtil;
import com.gmail.br45entei.util.FrequencyTimer;
import com.gmail.br45entei.util.FrequencyTimer.TimerCallback;
import com.gmail.br45entei.util.SWTUtil;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.swt.GLCanvas;
import org.lwjgl.opengl.swt.GLData;

/** Class used to test basic LWJGL functionality with SWT.
 *
 * @author Brian_Entei */
public class LWJGL_SWT_Demo {
	
	private static final void createMenuBar(Shell shell, Function<Void, Boolean> swtLoop, Function<Boolean, Void> setFullScreen, boolean[] state, boolean[] vsync, double[] frequency, MenuItem[] verticalSyncMenuItem, MenuItem[] framerateMenuItem, MenuItem[] fullscreenMenuItem) {
		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);
		
		MenuItem mntmfile = new MenuItem(menu, SWT.CASCADE);
		mntmfile.setText("&File");
		
		Menu menu_1 = new Menu(mntmfile);
		mntmfile.setMenu(menu_1);
		
		MenuItem mntmExit = new MenuItem(menu_1, SWT.NONE);
		mntmExit.setToolTipText("Closes this application");
		mntmExit.setText("E&xit\tAlt+F4");
		mntmExit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				state[0] = false;
			}
		});
		mntmExit.setAccelerator(SWT.ALT | SWT.F4);
		
		MenuItem mntmsettings = new MenuItem(menu, SWT.CASCADE);
		mntmsettings.setToolTipText("Contains various program settings");
		mntmsettings.setText("&Settings");
		
		Menu menu_2 = new Menu(mntmsettings);
		mntmsettings.setMenu(menu_2);
		
		MenuItem mntmVerticalSync = new MenuItem(menu_2, SWT.CHECK);
		mntmVerticalSync.setToolTipText("Toggles vertical sync on or off");
		mntmVerticalSync.setText("&Vertical Sync\tV");
		//mntmVerticalSync.setAccelerator('V');
		
		MenuItem mntmadjustFramerateBy = new MenuItem(menu_2, SWT.NONE);
		mntmadjustFramerateBy.setToolTipText("Opens a dialog with frequency settings");
		mntmadjustFramerateBy.setText("Adjust &Framerate By Frequency...\tCtrl+F");
		mntmVerticalSync.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				vsync[0] = mntmVerticalSync.getSelection();
				mntmadjustFramerateBy.setEnabled(!vsync[0]);
			}
		});
		mntmadjustFramerateBy.setAccelerator(SWT.CTRL | 'F');
		mntmadjustFramerateBy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Shell dialog = new Shell(shell, SWT.DIALOG_TRIM);
				dialog.setText("Framerate Frequency Adjustment");
				dialog.setSize(450, 320);
				dialog.setImages(shell.getImages());
				SWTUtil.centerShell2OnShell1(shell, dialog);
				
				Label lblFrequency = new Label(dialog, SWT.NONE);
				lblFrequency.setBounds(10, 22, 60, 15);
				lblFrequency.setText("Frequency:");
				
				Scale sldrFrequency = new Scale(dialog, SWT.HORIZONTAL);
				sldrFrequency.setToolTipText("Adjusts the FPS (\"Frames Per Second\")");
				sldrFrequency.setLocation(lblFrequency.getLocation().x + lblFrequency.getSize().x + 6, 10);
				sldrFrequency.setSize(dialog.getSize().x - (sldrFrequency.getLocation().x + 6), 45);
				sldrFrequency.setMaximum(240);
				sldrFrequency.setMinimum(0);
				sldrFrequency.setIncrement(1);
				//sldrFrequency.setThumb(5);// Whyyyy is this not a thing for Scales, but it is for Sliders?!
				sldrFrequency.setPageIncrement(10);
				sldrFrequency.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(MouseEvent e) {
						int increment = 1;//5 minus the built-in thumb increment that you can't change for some dumb reason which is 4
						int multiplier = e.count / 3;
						int amount = increment * multiplier;
						sldrFrequency.setSelection(sldrFrequency.getSelection() + amount);
					}
				});
				int fps = Long.valueOf(Math.round(frequency[0])).intValue();
				sldrFrequency.setSelection(fps);
				double mpf = 1000.0 / (fps + 0.0);
				
				Label lblDisplayFrequency = new Label(dialog, SWT.CENTER);
				lblDisplayFrequency.setToolTipText("The framerate frequency ('MPF' is \"Milliseconds Per Frame\", or the amount of time each frame spends on screen)");
				lblDisplayFrequency.setText(fps == 0 ? "Infinity <No Limit>" : String.format("Frequency: %s FPS (%s MPF)", Integer.toString(fps), CodeUtil.limitDecimalNoRounding(mpf, 8, true)));
				lblDisplayFrequency.setSize(215, 15);
				lblDisplayFrequency.setLocation(sldrFrequency.getLocation().x + (sldrFrequency.getSize().x / 2) - (lblDisplayFrequency.getSize().x / 2), sldrFrequency.getLocation().y + sldrFrequency.getSize().y + 6);
				
				Button btnResetFrequency = new Button(dialog, SWT.PUSH);
				btnResetFrequency.setToolTipText("Sets the frequency back to the refresh rate of the primary monitor");
				btnResetFrequency.setText("Reset Frequency");
				btnResetFrequency.setSize(lblDisplayFrequency.getSize().x, 25);
				btnResetFrequency.setLocation(lblDisplayFrequency.getLocation().x, lblDisplayFrequency.getLocation().y + lblDisplayFrequency.getSize().y + 6);
				btnResetFrequency.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						sldrFrequency.setSelection(Long.valueOf(Math.round(frequency[0] = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getRefreshRate())).intValue());
					}
				});
				
				Button btnDone = new Button(dialog, SWT.PUSH);
				btnDone.setToolTipText("Closes this dialog window");
				btnDone.setText("Done");
				btnDone.setBounds(10, dialog.getClientArea().height - 35, dialog.getClientArea().width - 20, 25);
				btnDone.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						dialog.close();
					}
				});
				dialog.setDefaultButton(btnDone);
				
				dialog.open();
				
				while(!vsync[0] && swtLoop.apply(null).booleanValue() && !dialog.isDisposed()) {
					MenuItem mntmadjustFramerateBy = framerateMenuItem[0];
					if(mntmadjustFramerateBy != null) {
						SWTUtil.setEnabled(mntmadjustFramerateBy, false);
					}
					
					fps = sldrFrequency.getSelection();
					frequency[0] = fps;
					mpf = 1000.0 / (fps + 0.0);
					SWTUtil.setText(lblDisplayFrequency, fps == 0 ? "Frequency: Infinity <No Limit>" : String.format("Frequency: %s FPS (%s MPF)", Integer.toString(fps), CodeUtil.limitDecimalNoRounding(mpf, 8, true)));
				}
				dialog.dispose();
			}
		});
		
		new MenuItem(menu_2, SWT.SEPARATOR);
		
		MenuItem mntmFullscreen = new MenuItem(menu_2, SWT.CHECK);
		mntmFullscreen.setToolTipText("Toggles fullscreen mode on or off");
		mntmFullscreen.setText("Fullscreen\tF11");
		mntmFullscreen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setFullScreen.apply(Boolean.valueOf(mntmFullscreen.getSelection()));
			}
		});
		//mntmFullscreen.setAccelerator(SWT.F11);
		
		verticalSyncMenuItem[0] = mntmVerticalSync;
		framerateMenuItem[0] = mntmadjustFramerateBy;
		fullscreenMenuItem[0] = mntmFullscreen;
	}
	
	/** Runs a simple OpenGL demo program.
	 * 
	 * @param args Program command line arguments */
	public static final void main(String[] args) {
		GraphicsDevice screenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		final Robot robot;
		try {
			robot = new Robot(screenDevice);
		} catch(AWTException ex) {
			throw new RuntimeException("Failed to create java.awt.Robot for mouse control!", ex);
		}
		
		final boolean[] state = {true};
		final int[] viewport = {0, 0, 800, 600};
		final boolean[] vsync = {false};
		final double[] frequency = {screenDevice.getDisplayMode().getRefreshRate() + 0.0D};
		final boolean[] mouseCaptured = {false};
		final int[] mouseCaptureLoc = {0, 0};
		final int[] mouseDeltaXY = {0, 0};
		final FrequencyTimer timer = new FrequencyTimer(frequency[0]);
		final ConcurrentLinkedDeque<String> fpsLog = new ConcurrentLinkedDeque<>();
		timer.setCallback(new TimerCallback() {
			@Override
			public void onTick() {
			}
			
			@Override
			public void onSecond() {
				fpsLog.addLast(String.format("FPS: %s; Average FPS: %s; Last MPF: %s; Average MPF: %s; Last MPFPS: %s;", Double.toString(timer.getLastFrameCount()), Double.toString(timer.getTargetPeriodInMilliseconds() / timer.getLastAverageMillisecondsPerFrame()), Double.toString(timer.getLastMillisecondsPerFrame()), Double.toString(timer.getLastAverageMillisecondsPerFrame()), Long.toString(timer.getLastMillisecondsPerFramePerPeriod())));
			}
		});
		final MenuItem[] mntmVerticalSync = new MenuItem[1],
				mntmadjustFramerateBy = new MenuItem[1],
				mntmFullscreen = new MenuItem[1];
		
		Display display = Display.getDefault();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setSize(800, 600);
		shell.setText("LWJGL-SWT Demo");
		shell.setImages(SWTUtil.getTitleImages());
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				state[0] = false;
			}
		});
		
		//builds a transparent cursor 
		//ImageData cursor = new ImageData(1, 1, 32, new PaletteData(0, 0, 0));
		//invisibleCursor = new Cursor(mouse.control.getDisplay(), cursor, 0, 0);
		Color white = display.getSystemColor(SWT.COLOR_WHITE);
		Color black = display.getSystemColor(SWT.COLOR_BLACK);
		PaletteData palette = new PaletteData(new RGB[] {white.getRGB(), black.getRGB()});
		ImageData sourceData = new ImageData(16, 16, 1, palette);
		sourceData.transparentPixel = 0;
		final Cursor invisibleCursor = new Cursor(display, sourceData, 0, 0);
		
		GLData data = new GLData();
		data.doubleBuffer = true;
		data.forwardCompatible = true;
		data.swapInterval = Integer.valueOf(0);
		data.majorVersion = 3;
		data.minorVersion = 3;
		GLCanvas glCanvas = new GLCanvas(shell, SWT.DOUBLE_BUFFERED, data);
		shell.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				glCanvas.setBounds(shell.getClientArea());
				Point location = glCanvas.getLocation();
				Point size = glCanvas.getSize();
				viewport[0] = location.x;
				viewport[1] = location.y;
				viewport[2] = size.x;
				viewport[3] = size.y;
			}
		});
		
		final Function<Void, Boolean> swtLoop = (v) -> {
			if(!display.readAndDispatch()) {
				CodeUtil.sleep(10L);
			}
			String log;
			while(state[0] && !shell.isDisposed() && (log = fpsLog.pollFirst()) != null) {
				System.out.println(log);
				display.readAndDispatch();
			}
			
			if(timer.getTargetFrequency() != frequency[0]) {
				timer.setFrequency(frequency[0]);
			}
			MenuItem verticalSyncMenuItem = mntmVerticalSync[0];
			if(verticalSyncMenuItem != null && !verticalSyncMenuItem.isDisposed()) {
				SWTUtil.setSelection(verticalSyncMenuItem, vsync[0]);
			}
			MenuItem fullscreenMenuItem = mntmFullscreen[0];
			if(fullscreenMenuItem != null && !fullscreenMenuItem.isDisposed()) {
				SWTUtil.setSelection(fullscreenMenuItem, shell.getFullScreen());// This will always be false (unless you don't have the program remove the menubar while in fullscreen mode)
			}
			if(display.getActiveShell() == shell) {
				glCanvas.setFocus();
			}
			return Boolean.valueOf(state[0] && !shell.isDisposed());
		};
		final Function<Void, Point> getCenterOfCanvas = (v) -> {
			Point halfSize = glCanvas.getSize();
			halfSize.x /= 2;
			halfSize.y /= 2;
			//return glCanvas.toDisplay(halfSize);
			return display.map(glCanvas, null, halfSize);
		};
		final Function<Boolean, Void>[] setFullScreen = new Function[1];
		setFullScreen[0] = (b) -> {
			if(b == null ? shell.getFullScreen() : !b.booleanValue()) {
				shell.setFullScreen(false);
				if(shell.getMenuBar() == null) {
					createMenuBar(shell, swtLoop, setFullScreen[0], state, vsync, frequency, mntmVerticalSync, mntmadjustFramerateBy, mntmFullscreen);
				}
			} else {
				Menu menu = shell.getMenuBar();
				if(menu != null) {
					mntmVerticalSync[0] = mntmadjustFramerateBy[0] = null;
					shell.setMenu(null);
					menu.dispose();
				}
				shell.setFullScreen(true);
			}
			shell.redraw();
			shell.update();
			display.readAndDispatch();
			mouseDeltaXY[0] = mouseDeltaXY[1] = 0;
			if(mouseCaptured[0]) {
				Point center = getCenterOfCanvas.apply(null);
				robot.mouseMove(center.x, center.y);
				mouseDeltaXY[0] = mouseDeltaXY[1] = 0;
			}
			return null;
		};
		final Function<Void, Void> onMouseMoved = (v) -> {
			int dx = mouseDeltaXY[0], dy = mouseDeltaXY[1];
			System.out.println(String.format("DX: %s; DY: %s;", Integer.toString(dx), Integer.toString(dy)));
			
			return null;
		};
		glCanvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == 'v' && (e.stateMask & SWT.CTRL) == 0 && (e.stateMask & SWT.SHIFT) == 0 && (e.stateMask & SWT.ALT) == 0) {
					vsync[0] = !vsync[0];
				}
				if(e.keyCode == SWT.F11 && (e.stateMask & SWT.CTRL) == 0 && (e.stateMask & SWT.SHIFT) == 0 && (e.stateMask & SWT.ALT) == 0) {
					setFullScreen[0].apply(null);
				}
			}
		});
		glCanvas.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.keyCode == SWT.ESC) {
					if(mouseCaptured[0]) {
						mouseCaptured[0] = false;
						mouseDeltaXY[0] = mouseDeltaXY[1] = 0;
						robot.mouseMove(mouseCaptureLoc[0], mouseCaptureLoc[1]);
						glCanvas.setCursor(display.getSystemCursor(SWT.CURSOR_ARROW));
					} else if(shell.getFullScreen()) {
						setFullScreen[0].apply(Boolean.FALSE);
					} else {
						MessageBox box = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
						box.setText("Exit ".concat(shell.getText()).concat("?"));
						box.setMessage("Are you sure you wish to exit the program?");
						
						switch(box.open()) {
						case SWT.YES:
							e.doit = true;
							state[0] = false;
							break;
						case SWT.NO:
						default:
							e.doit = false;
							break;
						}
					}
				}
			}
		});
		
		glCanvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if(e.button == 1) {
					if(mouseCaptured[0] == false) {
						mouseDeltaXY[0] = mouseDeltaXY[1] = 0;
						
						java.awt.Point mLoc = MouseInfo.getPointerInfo().getLocation();
						mouseCaptureLoc[0] = mLoc.x;
						mouseCaptureLoc[1] = mLoc.y;
						
						glCanvas.setCursor(invisibleCursor);
						Point center = getCenterOfCanvas.apply(null);
						robot.mouseMove(center.x, center.y);
						
						mouseCaptured[0] = true;
					}
				}
			}
		});
		glCanvas.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if(mouseCaptured[0]) {
					java.awt.Point mLoc = MouseInfo.getPointerInfo().getLocation();
					Point center = getCenterOfCanvas.apply(null);
					if(mLoc.x != center.x || mLoc.y != center.y) {
						robot.mouseMove(center.x, center.y);
						mouseDeltaXY[0] = mLoc.x - center.x;
						mouseDeltaXY[1] = mLoc.y - center.y;
						onMouseMoved.apply(null);
					}
				}
			}
		});
		createMenuBar(shell, swtLoop, setFullScreen[0], state, vsync, frequency, mntmVerticalSync, mntmadjustFramerateBy, mntmFullscreen);
		
		shell.open();
		Point size = shell.getSize();
		Rectangle clientArea = shell.getClientArea();
		shell.setVisible(false);
		int xDiff = size.x - clientArea.width,
				yDiff = size.y - clientArea.height;
		shell.setSize(size.x + xDiff, size.y + yDiff);
		glCanvas.setBounds(shell.getClientArea());
		
		shell.open();
		shell.layout();
		shell.forceActive();
		shell.forceFocus();
		glCanvas.forceFocus();
		
		Thread glThread = new Thread(() -> {// XXX GLThread
			try {
				//Set the OpenGL context current on the current thread:
				glCanvas.setCurrent();
				//Initialize the OpenGL library and get the capabilities:
				GLCapabilities glCaps = GL.createCapabilities(true);
				
				//Set up some variables for our demo:
				final SecureRandom random = new SecureRandom();// A random source of data to use for our changing canvas color
				final float maxIncrement = 0.025f;// Each color channel will be changed by a random float value between 0 and this number
				float increment = Math.min(maxIncrement, random.nextFloat());// Go ahead and initialize the increment for the first frame
				float r = 0.0f, g = random.nextFloat(), b = 1.0f;// The three color channels that we'll use to make our GLCanvas change color
				boolean rUp = true, gUp = random.nextBoolean(), bUp = false;// The three booleans that will tell us what each color channel's direction of change is (up/down)
				
				boolean lastVsync = glCanvas.glGetSwapInterval() == 1;// Flag that we'll use to check if someone's changed the vertical sync so that we can update it
				
				while(state[0]) {// Begin our OpenGL loop:
					GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);// Set the viewport to match the glCanvas' size (and optional offset)
					GL11.glClearColor(r, g, b, 1);// Set the clear color to a random color that changes a bit every frame
					GL11.glClear(GL11.GL_COLOR_BUFFER_BIT/* | GL11.GL_DEPTH_BUFFER_BIT*/);// Clear the color buffer, setting it to the clear color above
					
					//Update our r/g/b variables for the next frame:
					if(rUp) {// Check if we're increasing or decreasing the color channel
						r += increment;// Add the increment to the color channel
						if(r >= 1.0f) {// Check if the color channel has reached (or passed) 1.0f
							rUp = false;// Set the direction to decreasing
							r = 1.0f;// Cap the color channel to the maximum (1.0f) just in case it overshot
						}
					} else {
						r -= increment;// Subtract the increment from the color channel
						if(r <= 0.0f) {// Check if the color channel has reached (or gone below) 0.0f
							rUp = true;// Set the direction to increasing
							r = 0.0f;// Cap the color channel to the minimum (0.0f) just in case it undershot
						}
					}
					//increment = Math.min(maxIncrement, random.nextFloat());// Set the increment to a random float value between 0 and the maxIncrement for the next color
					if(gUp) {// ...above steps repeated for the green and blue color channels:
						g += increment;
						if(g >= 1.0f) {
							gUp = false;
							g = 1.0f;
						}
					} else {
						g -= increment;
						if(g <= 0.0f) {
							gUp = true;
							g = 0.0f;
						}
					}
					//increment = Math.min(maxIncrement, random.nextFloat());
					if(bUp) {
						b += increment;
						if(b >= 1.0f) {
							bUp = false;
							b = 1.0f;
						}
					} else {
						b -= increment;
						if(b <= 0.0f) {
							bUp = true;
							b = 0.0f;
						}
					}
					increment = Math.min(maxIncrement, random.nextFloat());
					
					//Update the GL_SWAP_INTERVAL (set vertical sync):
					if(vsync[0] != lastVsync) {
						lastVsync = vsync[0];
						glCanvas.glSwapInterval(lastVsync ? 1 : 0);
					}
					//Swap the front and back buffers:
					glCanvas.swapBuffers();
					
					//Sleep for the desired amount of time if the swap interval is 0 (if vertical sync is off):
					if(!lastVsync) {
						timer.frequencySleep();
					}
				}
				
				//The program is shutting down, let's clean up:
				glCanvas.deleteContext();
				GL.destroy();
			} finally {
				state[0] = false;
			}
		}, "GLThread");
		//Mark the GLThread as a daemon thread just in case the main thread crashes without setting the state[0] flag to false:
		glThread.setDaemon(true);// (this prevents the program from continuing to run without a window that the user can close)
		//Start the GLThread:
		glThread.start();
		
		//Begin our SWT window loop:
		MenuItem framerateMenuItem;
		while(swtLoop.apply(null).booleanValue()) {
			framerateMenuItem = mntmadjustFramerateBy[0];
			if(framerateMenuItem != null && !framerateMenuItem.isDisposed()) {
				SWTUtil.setEnabled(framerateMenuItem, !vsync[0]);
			}
		}
		
		//Wait for the GLThread to shut down and clean up:
		while(glThread.isAlive()) {
			state[0] = false;
			CodeUtil.sleep(10L);
		}
		
		//The program is shutting down, let's clean up:
		shell.dispose();
		SWTResourceManager.dispose();
		display.dispose();
	}
}
