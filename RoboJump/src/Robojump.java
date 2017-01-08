import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.glu.*;

import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class Robojump implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {
	public static final boolean TRACE = false;

	public static final String WINDOW_TITLE = "A4: [Liam Pimlott]";
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 640;

	private static final GLU glu = new GLU();

	private static final String TEXTURE_PATH = "resources/";
	public static final String OBJ_PATH_NAME = "resources/";
	
	// TODO: change this
	public static final String[] TEXTURE_FILES = { "circle.png", "cobble.jpg", "walls.jpg",
													"sand.jpg", "box.jpg", "pyramidblock.jpg",
													"palmbark.jpg", "cactus.jpg", "sahara_ft.jpg",
													"sahara_bk.jpg", "sahara_lf.jpg", "sahara_rt.jpg",
													"sahara_up.jpg", "bender.jpg"};

	public static void main(String[] args) {
		final JFrame frame = new JFrame(WINDOW_TITLE);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (TRACE)
					System.out.println("closing window '" + ((JFrame)e.getWindow()).getTitle() + "'");
				System.exit(0);
			}
		});

		final GLProfile profile = GLProfile.get(GLProfile.GL2);
		final GLCapabilities capabilities = new GLCapabilities(profile);
		final GLCanvas canvas = new GLCanvas(capabilities);
		try {
			Object self = self().getConstructor().newInstance();
			self.getClass().getMethod("setup", new Class[] { GLCanvas.class }).invoke(self, canvas);
			canvas.addGLEventListener((GLEventListener)self);
			canvas.addKeyListener((KeyListener)self);
			canvas.addMouseListener((MouseListener)self);
			canvas.addMouseMotionListener((MouseMotionListener)self);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);

		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);

		canvas.requestFocusInWindow();

		if (TRACE)
			System.out.println("-> end of main().");
	}

	private static Class<?> self() {
		// This ugly hack gives us the containing class of a static method 
		return new Object() { }.getClass().getEnclosingClass();
	}

	/*** Instance variables and methods ***/

	private String direction;
	private Texture[] textures;

	// TODO: Add instance variables here
	
	private boolean freeview;
	private float xangle;
	private float yangle;
	
	private float tpCamHeight;
	private float fpCamHeight;
	
	private float charRotate;
	private float charHeight;
	
	private float xposit;
	private float yposit;
	private float zposit;
	
	private int[] lastDragPos;
	private char keyheld = '\0';
	
	private boolean jump = false;
	private boolean onSurface = true;
	private float jumpt;
	
	private boolean jReverse = false;
	
	private boolean crouched = false;
	
	float[] fogcolour = new float[] { 0.2f, 0.2f, 0.2f, 0.0f };
	float fogstart = 0f;
	float fogend = 30f;
	
	private Robot robot;
	private ArrayList<Structure>  worldObjects = new ArrayList<Structure>();
	
	float t = 0.0f;
	
	public void setup(final GLCanvas canvas) {
		// Called for one-time setup
		if (TRACE)
			System.out.println("-> executing setup()");

		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				canvas.repaint();
			}
		}, 1000, 1000/60);

		// TODO: Add code here
		
		freeview = true;
		xangle = 0f;
		yangle = 0f;
		charRotate = 0f;
		xposit = 0f;
		yposit = -1.5f;
		zposit = 0f;
		
		tpCamHeight = -1.5f;
		fpCamHeight = -1.5f;
		
		jumpt = 0.0f;
		charHeight = 0.9f;
		
		
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// Called when the canvas is (re-)created - use it for initial GL setup
		if (TRACE)
			System.out.println("-> executing init()");

		final GL2 gl = drawable.getGL().getGL2();

		textures = new Texture[TEXTURE_FILES.length];
		try {
			for (int i = 0; i < TEXTURE_FILES.length; i++) {
				File infile = new File(TEXTURE_PATH + TEXTURE_FILES[i]); 
				BufferedImage image = ImageIO.read(infile);
				ImageUtil.flipImageVertically(image);
				textures[i] = TextureIO.newTexture(AWTTextureIO.newTextureData(gl.getGLProfile(), image, false));
				textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT); // GL_REPEAT or GL_CLAMP
				textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		

		// TODO: Add code here
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		//gl.glEnable(GL2.GL_CULL_FACE);
		
		robot = new Robot(new Shape[] {new Shape(OBJ_PATH_NAME + "testBody.obj"),
										new Shape(OBJ_PATH_NAME + "testUpperArm.obj"), //left arm 
										new Shape(OBJ_PATH_NAME + "testUpperArm.obj"), // right arm
										new Shape(OBJ_PATH_NAME + "testHand.obj"), //left hand
										new Shape(OBJ_PATH_NAME + "testHand.obj"), // right hand
										new Shape(OBJ_PATH_NAME + "testUpperArm.obj"),  //left thigh
										new Shape(OBJ_PATH_NAME + "testUpperArm.obj"), // right thigh
										new Shape(OBJ_PATH_NAME + "testUpperArm.obj"), // left shin
										new Shape(OBJ_PATH_NAME + "testUpperArm.obj"), // right shin
										new Shape(OBJ_PATH_NAME + "testFoot.obj"),// left foot
										new Shape(OBJ_PATH_NAME + "testFoot.obj"), // right foot
										new Shape(OBJ_PATH_NAME + "testUpperArm.obj"), // left forearm
										new Shape(OBJ_PATH_NAME + "testUpperArm.obj"),
										new RoboHead(textures[13], glu)},       // head
						new float[][] {{0.0f, 0.0f, 0.0f}, {0.2f, 0.0f, 0.0f}, 
										{-0.2f, 0.0f, 0.0f},{0.0f, -0.275f, 0.0f},
										{0.0f, -0.275f, 0.0f}, {0.1f, -0.350f, 0.0f},
										{-0.1f, -0.350f, 0.0f}, {0.1f, -0.550f, 0f}, {-0.1f, -0.550f, 0.0f},
										{0.0f, -0.275f, 0f}, {0.0f, -0.275f, 0.0f},
										{0.2f, -0.175f, 0f}, {-0.2f, -0.175f, 0.0f},
										{0.0f, 0.0f, 0.0f}},
						new float[] {0.25f,-0.25f,0.35f,-0.825f,0.25f,-0.25f});
		
		//Course 1
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{3f, 0.5f, 0f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{6f, 0.5f, 0f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{10f, 1f, 0f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{15f, 1f, -4f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{15f, 1f, -8f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{20f, 1f, -8f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{20f, 1f, -14f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new Cactuar(textures[7],glu)}, new float[][] {{30f, 3f, -16.6f}},
				new float[]{2f,-2f,2.5f,-1f, 0.5f, -0.5f }, -50f, 0, 0.01f));

		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{20f, 1f, -19f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new Tree(textures[6],glu)}, new float[][] {{25f, 0f, -20f}},
				new float[]{0.5f,-0.5f,5f,0f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{15f, 1f, -22f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{12f, 2f, -22f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{12f, 2.5f, -25f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{12f, 3f, -28f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{12f, 1.5f, -33f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{12f, 1f, -38f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{8f, 1.5f, -38f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{4f, 2f, -38f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{0f, 2.5f, -38f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new Cactuar(textures[7],glu)}, new float[][] {{0f, 8f, -42}},
				new float[]{2f,-2f,2.5f,-1f, 0.5f, -0.5f }, 5f, 1, 0.02f));
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{0f, 1.5f, -44f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-4f, 2f, -44f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-7f, 2f, -41f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-10f, 2f, -38f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-14f, 1f, -35f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-17f, 1f, -33f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-11f, 1f, -33f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new Tree(textures[6],glu)}, new float[][] {{-14, 0f, -33}},
				new float[]{0.5f,-0.5f,5f,0f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-14f, 1f, -31f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-14f, 1f, -27f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-16f, 1.5f, -26f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-16f, 2f, -24f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-17f, 2.5f, -22f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-16f, 3f, -20f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-15f, 3.5f, -18f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-16f, 4f, -16f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-17f, 4.5f, -14f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-16f,5f, -12f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-13f,5f, -12f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new Tree(textures[6],glu)}, new float[][] {{-13, 5f, -12}},
				new float[]{0.5f,-0.5f,5f,0f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-16f, 1f, -6f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-13f, 1f, -4f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-10f, 1f, -2f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-7f, 1f, 0f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		
		//Random
		
		worldObjects.add(new Structure(new Shape[] { new LargeBox(textures[4])}, new float[][] {{-3f, 1f, 0f}},
				new float[]{1f,-1f,1f,-1f,1f,-1f }));
		
		worldObjects.add(new Structure(new Shape[] { new Pyramid(textures[5])}, new float[][] {{0f, 0f, -20f}},
				new float[]{10f,-10f,20f,0f,10f,-10f }));
		
		worldObjects.add(new Structure(new Shape[] { new Pyramid(textures[5])}, new float[][] {{30f, 0f, 20f}},
				new float[]{10f,-10f,20f,0f,10f,-10f }));
		
		worldObjects.add(new Structure(new Shape[] { new Pyramid(textures[5])}, new float[][] {{-40f, 0f, 40f}},
				new float[]{10f,-10f,20f,0f,10f,-10f }));
		
		worldObjects.add(new Structure(new Shape[] { new Pyramid(textures[5])}, new float[][] {{-40f, 0f, 0f}},
				new float[]{10f,-10f,20f,0f,10f,-10f }));
		
		worldObjects.add(new Structure(new Shape[] { new Tree(textures[6],glu)}, new float[][] {{-5f, 0f, -5f}},
				new float[]{0.5f,-0.5f,5f,0f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{-5f, 0f, -5f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new Tree(textures[6],glu)}, new float[][] {{5f, 0f, -5f}},
				new float[]{0.5f,-0.5f,5f,0f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new SmallBox(textures[4])}, new float[][] {{5f, 0f, -5f}},
				new float[]{0.5f,-0.5f,0.5f, -0.5f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new Tree(textures[6],glu)}, new float[][] {{15f, 0f, -2.5f}},
				new float[]{0.5f,-0.5f,5f,0f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new Tree(textures[6],glu)}, new float[][] {{-4f, 0f, 5f}},
				new float[]{0.5f,-0.5f,5f,0f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new Tree(textures[6],glu)}, new float[][] {{0f, 0f, 25f}},
				new float[]{0.5f,-0.5f,5f,0f, 0.5f, -0.5f }));
		
		worldObjects.add(new Structure(new Shape[] { new Cactuar(textures[7],glu)}, new float[][] {{10f, 1.6f, -8f}},
				new float[]{2f,-2f,2.5f,-1f, 0.5f, -0.5f }, -20f, 0, 0.005f));
	
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		// Draws the display
		if (TRACE)
			System.out.println("-> executing display()");

		final GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		if(keyheld != '\0'){walk(keyheld);}
		
		if(jump){
			jump();
		}else{
			fall();
		}
		
		gl.glEnable(GL2.GL_FOG);
		System.out.printf("Fog colour is %.1f, %.1f, %.1f, %.1f\n", fogcolour[0], fogcolour[1], fogcolour[2], fogcolour[3]);
		gl.glFogfv(GL2.GL_FOG_COLOR, fogcolour, 0);
		
			System.out.println("Using linear afog");
			System.out.println("Fog start is " + fogstart + " and end is " + fogend);
			gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
			gl.glFogf(GL2.GL_FOG_START, fogstart);
			gl.glFogf(GL2.GL_FOG_END, fogend);
		
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		
		if(freeview){
			gl.glFrustumf(-0.25f, 0.25f, -0.25f, 0.25f, 0.25f, 250.0f);
		} else {
			gl.glFrustumf(-1.0f, 1.0f, -1.0f, 1.0f, 1f, 250.0f);
		}
			
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		
		// view tranforms
		if(freeview){
			gl.glRotatef(xangle, 1, 0, 0);
			gl.glRotatef(yangle, 0, 1, 0);
			gl.glTranslatef(xposit, fpCamHeight, zposit );
		} else {
			gl.glTranslatef(xposit, tpCamHeight, zposit-3 );
			gl.glTranslatef(-xposit, charHeight, -zposit);
			gl.glRotatef(xangle, 1, 0, 0);
			gl.glRotatef(yangle, 0, 1, 0);
			gl.glTranslatef(xposit, -charHeight, zposit);
			
		}
		
		// Draw Robot
		gl.glPushMatrix();
		gl.glTranslatef(-xposit, charHeight, -zposit);
		gl.glRotatef(180+charRotate, 0, 1, 0);
		if(!freeview){
			robot.draw(gl);
		}
		gl.glPopMatrix();
		
		//draw world objects
		for(int i = 0; i< worldObjects.size(); i++){
			gl.glPushMatrix();
			worldObjects.get(i).draw(gl);
			gl.glPopMatrix();
		}
		
		
		// floor
		gl.glPushMatrix();
		textures[3].enable(gl);
		textures[3].bind(gl);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0f, 0f);
		gl.glVertex3f(-50, 0,50);
		gl.glTexCoord2f(50f, 0f);
		gl.glVertex3f(50, 0, 50);
		gl.glTexCoord2f(50f, 50f);
		gl.glVertex3f(50, 0, -50);
		gl.glTexCoord2f(0f, 50f);
		gl.glVertex3f(-50, 0, -50);
		gl.glEnd();
		textures[3].disable(gl);
		gl.glPopMatrix();
		
		//walls
		drawWalls(gl);
		
		/*axis
		gl.glPushMatrix();
		gl.glTranslatef(0, 0.5f, 0);
		gl.glScalef(0.5f, 0.5f, 0.5f);
		gl.glBegin(GL2.GL_LINES);
		gl.glColor3f(1, 0, 0);
		gl.glVertex3f(0, 0, 0);
		gl.glVertex3f(1, 0, 0);
		gl.glColor3f(0, 1, 0);
		gl.glVertex3f(0, 0, 0);
		gl.glVertex3f(0, 1, 0);
		gl.glColor3f(0, 0, 1);
		gl.glVertex3f(0, 0, 0);
		gl.glVertex3f(0, 0, 1);
		gl.glEnd();
		gl.glPopMatrix();
		gl.glColor3f(1, 1, 1); */
		
		//draw skybox
		gl.glDisable(GL2.GL_FOG);
		gl.glPushMatrix();
		gl.glTranslatef(-xposit, 0, -zposit);
		drawSkyBox(gl);
		gl.glPopMatrix();
		
	}
	
	public void drawSkyBox(GL2 gl){
		//front
		gl.glPushMatrix();
		textures[8].enable(gl);
		textures[8].bind(gl);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0f, 0f);
		gl.glVertex3f(110, -80,-110);
		gl.glTexCoord2f(1f, 0f);
		gl.glVertex3f(-110, -80f, -110);
		gl.glTexCoord2f(1f, 1f);
		gl.glVertex3f(-110, 140, -110);
		gl.glTexCoord2f(0f, 1f);
		gl.glVertex3f(110, 140, -110);
		gl.glEnd();
		textures[8].disable(gl);
		gl.glPopMatrix();
		//back
		gl.glPushMatrix();
		textures[9].enable(gl);
		textures[9].bind(gl);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0f, 0f);
		gl.glVertex3f(-110, -80, 110);
		gl.glTexCoord2f(1f, 0f);
		gl.glVertex3f(110, -80, 110);
		gl.glTexCoord2f(1f, 1f);
		gl.glVertex3f(110, 140, 110);
		gl.glTexCoord2f(0f, 1f);
		gl.glVertex3f(-110, 140, 110);
		gl.glEnd();
		textures[9].disable(gl);
		gl.glPopMatrix();
		//left
		gl.glPushMatrix();
		textures[10].enable(gl);
		textures[10].bind(gl);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0f, 0f);
		gl.glVertex3f(-110, -80,-110);
		gl.glTexCoord2f(1, 0f);
		gl.glVertex3f(-110, -80, 110);
		gl.glTexCoord2f(1, 1f);
		gl.glVertex3f(-110, 140, 110);
		gl.glTexCoord2f(0f, 1f);
		gl.glVertex3f(-110, 140, -110);
		gl.glEnd();
		textures[10].disable(gl);
		gl.glPopMatrix();
		//right
		gl.glPushMatrix();
		textures[11].enable(gl);
		textures[11].bind(gl);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0f, 0f);
		gl.glVertex3f(110, -80, 110);
		gl.glTexCoord2f(1f, 0f);
		gl.glVertex3f(110, -80, -110);
		gl.glTexCoord2f(1f, 1f);
		gl.glVertex3f(110, 140, -110);
		gl.glTexCoord2f(0f, 1f);
		gl.glVertex3f(110, 140, 110);
		gl.glEnd();
		textures[11].disable(gl);
		gl.glPopMatrix();
		//top
		gl.glPushMatrix();
		textures[12].enable(gl);
		textures[12].bind(gl);
		gl.glRotatef(90, 0, 1, 0);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0f, 0f);
		gl.glVertex3f(-110, 140, 110);
		gl.glTexCoord2f(1f, 0f);
		gl.glVertex3f(110, 140, 110);
		gl.glTexCoord2f(1f, 1f);
		gl.glVertex3f(110, 140, -110);
		gl.glTexCoord2f(0f, 1f);
		gl.glVertex3f(-110, 140, -110);
		gl.glEnd();
		textures[12].disable(gl);
		gl.glPopMatrix();
	}
	
	public void drawWalls(GL2 gl){
				gl.glPushMatrix();
				textures[2].enable(gl);
				textures[2].bind(gl);
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(0f, 0f);
				gl.glVertex3f(-50, 0,-50);
				gl.glTexCoord2f(25f, 0f);
				gl.glVertex3f(50, 0, -50);
				gl.glTexCoord2f(25f, 1f);
				gl.glVertex3f(50, 5, -50);
				gl.glTexCoord2f(0f, 1f);
				gl.glVertex3f(-50, 5, -50);
				gl.glEnd();
				textures[2].disable(gl);
				gl.glPopMatrix();
				gl.glPushMatrix();
				textures[2].enable(gl);
				textures[2].bind(gl);
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(0f, 0f);
				gl.glVertex3f(-50, 0,50);
				gl.glTexCoord2f(25f, 0f);
				gl.glVertex3f(50, 0, 50);
				gl.glTexCoord2f(25f, 1f);
				gl.glVertex3f(50, 5, 50);
				gl.glTexCoord2f(0f, 1f);
				gl.glVertex3f(-50, 5, 50);
				gl.glEnd();
				textures[2].disable(gl);
				gl.glPopMatrix();
				gl.glPushMatrix();
				textures[2].enable(gl);
				textures[2].bind(gl);
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(0f, 0f);
				gl.glVertex3f(-50, 0,-50);
				gl.glTexCoord2f(25f, 0f);
				gl.glVertex3f(-50, 0, 50);
				gl.glTexCoord2f(25f, 1f);
				gl.glVertex3f(-50, 5, 50);
				gl.glTexCoord2f(0f, 1f);
				gl.glVertex3f(-50, 5, -50);
				gl.glEnd();
				textures[2].disable(gl);
				gl.glPopMatrix();
				gl.glPushMatrix();
				textures[2].enable(gl);
				textures[2].bind(gl);
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(0f, 0f);
				gl.glVertex3f(50, 0,50);
				gl.glTexCoord2f(25f, 0f);
				gl.glVertex3f(50, 0, -50);
				gl.glTexCoord2f(25f, 1f);
				gl.glVertex3f(50, 5, -50);
				gl.glTexCoord2f(0f, 1f);
				gl.glVertex3f(50, 5, 50);
				gl.glEnd();
				textures[2].disable(gl);
				gl.glPopMatrix();
	}
	
	
	public boolean collision(Structure a){
		boolean collision = false;
		Structure b;
		for(int i = 0; i<worldObjects.size(); i++){
			b = worldObjects.get(i);
			if(a.getClass() ==  Robot.class){
				if((a.xMin-xposit <= b.xMax+b.positions[0][0] && a.xMax-xposit >= b.xMin+b.positions[0][0]) &&
				    (a.yMin+charHeight <= b.yMax+b.positions[0][1] && a.yMax+charHeight >= b.yMin+b.positions[0][1]) &&
				    (a.zMin-zposit <= b.zMax+b.positions[0][2] && a.zMax-zposit >= b.zMin+b.positions[0][2]) )
					{ collision = true; }
			}else {
				if((a.xMin+a.positions[0][0] <= b.xMax+b.positions[0][0] && a.xMax+a.positions[0][0] >= b.xMin+b.positions[0][0]) &&
				    (a.yMin+a.positions[0][1] <= b.yMax+b.positions[0][1] && a.yMax+a.positions[0][1] >= b.yMin+b.positions[0][1]) &&
				    (a.zMin+a.positions[0][2] <= b.zMax+b.positions[0][2] && a.zMax+a.positions[0][2] >= b.zMin+b.positions[0][2]) )
					{ collision = true; }
			}
		}
		return collision;
	}// End collision method.
	
	public void walk(char key){
		
		System.out.println(key);
		
		if(!freeview){
			robot.walkAnimation();
		}
		
		if (key == 'a'){	
				xposit += (float)Math.cos(Math.toRadians(yangle))*0.1f;
				zposit += (float)Math.sin(Math.toRadians(yangle))*0.1f;
				
				charRotate = -yangle + 90;
				
				if(collision(robot) || (-xposit >= 49.5f || -xposit <= -49.5f) || (-zposit >= 49.5f || -zposit <= -49.5f)){
					xposit -= (float)Math.cos(Math.toRadians(yangle))*0.1f;
					zposit -= (float)Math.sin(Math.toRadians(yangle))*0.1f;
				}
				
		}
		else if (key == 'd'){
				xposit -= (float)Math.cos(Math.toRadians(yangle))*0.1f;
				zposit -= (float)Math.sin(Math.toRadians(yangle))*0.1f;
				
				charRotate = -yangle - 90;
				
				if(collision(robot) || (-xposit >= 49.5f || -xposit <= -49.5f) || (-zposit >= 49.5f || -zposit <= -49.5f)){
					xposit += (float)Math.cos(Math.toRadians(yangle))*0.1f;
					zposit += (float)Math.sin(Math.toRadians(yangle))*0.1f;
				}
		}
		else if (key == 'w'){
				zposit += (float)Math.cos(Math.toRadians(yangle))*0.1f;
				xposit -= (float)Math.sin(Math.toRadians(yangle))*0.1f;
				
				charRotate = -yangle;
				
				if(collision(robot) || (-xposit >= 49.5f || -xposit <= -49.5f) || (-zposit >= 49.5f || -zposit <= -49.5f)){
					zposit -= (float)Math.cos(Math.toRadians(yangle))*0.1f;
					xposit += (float)Math.sin(Math.toRadians(yangle))*0.1f;
				}
		}
		else if (key == 's'){
				zposit -= (float)Math.cos(Math.toRadians(yangle))*0.1f;
				xposit += (float)Math.sin(Math.toRadians(yangle))*0.1f;
				
				charRotate = -yangle + 180;
				
				if(collision(robot) || (-xposit >= 49.5f || -xposit <= -49.5f) || (-zposit >= 49.5f || -zposit <= -49.5f)){
					zposit += (float)Math.cos(Math.toRadians(yangle))*0.1f;
					xposit -= (float)Math.sin(Math.toRadians(yangle))*0.1f;
				}
		}
	}// End Method jump
	
	public void jump(){	
		jumpt+=0.05f;
		charHeight += 0.05f;
		tpCamHeight -= 0.05f;
		fpCamHeight -= 0.05f;
		
		if(jumpt > 1f || collision(robot)){
			jump = false;
			jumpt = 0;
			charHeight -= 0.05f;
			tpCamHeight += 0.05f;
			fpCamHeight += 0.05f;
		} 
		
	}// End Method jump
	
	public void fall(){
		
		
		charHeight-=0.05f;
		tpCamHeight += 0.05f;
		fpCamHeight += 0.05f;
		onSurface = false;
		
		if(charHeight < 0.9f || collision(robot)){
			charHeight+=0.05f;
			tpCamHeight -= 0.05f;
			fpCamHeight -= 0.05f;
			onSurface = true;
		}
	}
		
	public float lerp(float t, float a, float b) {
		return (1 - t) * a + t * b;	
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// Called when the canvas is destroyed (reverse anything from init) 
		if (TRACE)
			System.out.println("-> executing dispose()");
	}
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// Called when the canvas has been resized
		// Note: glViewport(x, y, width, height) has already been called so don't bother if that's what you want
		if (TRACE)
			System.out.println("-> executing reshape(" + x + ", " + y + ", " + width + ", " + height + ")");

		final GL2 gl = drawable.getGL().getGL2();
		float ar = (float)width / (height == 0 ? 1 : height);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		
		// TODO: use a perspective projection instead
		gl.glOrthof(ar < 1 ? -1.0f : -ar, ar < 1 ? 1.0f : ar, ar > 1 ? -1.0f : -1/ar, ar > 1 ? 1.0f : 1/ar, 0.5f, 2.5f);
	}
	@Override
	public void keyTyped(KeyEvent e) {
	}
	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyChar() == 'a'){
			keyheld = '\0';
		}else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyChar() == 'd'){
			keyheld = '\0';
		}else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyChar() == 'w'){
			keyheld = '\0';
		}else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyChar() == 's'){
			keyheld = '\0';
		} 
	}
	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Change this however you like
	
		if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyChar() == 'a'){
			keyheld = 'a';
		}
		if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyChar() == 'd'){
			keyheld = 'd';
		}
		if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyChar() == 'w'){
			keyheld = 'w';
		}
		if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyChar() == 's'){
			keyheld = 's';
		}
		
		if (e.getKeyChar() == ' ') {
			System.out.println("Space bar: jump!");
			if(onSurface){
				jump = true;
			}
		} 
		if(e.getKeyCode() == KeyEvent.VK_CONTROL) {
			if(crouched) yposit -= 0.5f;
			else if (!crouched) yposit += 0.5f;
			crouched = !crouched;
		}
		if (e.getKeyChar() == '\n') {
			System.out.println("Enter: switch view");
			freeview = !freeview;
			if(xangle < 0){
				xangle = 0;
			}
		}
		// TODO: add more keys as necessary
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO: use this or mouse moved for free look
		System.out.println("drag: (" + e.getX() + "," + e.getY() + ") at " + e.getWhen());
		
		int x = e.getX();
		int y = ((GLCanvas)(e.getSource())).getHeight() - e.getY() - 1;
		System.out.printf(" [viewport (%d, %d)]\n", x, y);
		
		if (lastDragPos != null) {
			// calculate the change in mouse position
			int deltaX = x - lastDragPos[0];
			int deltaY = y - lastDragPos[1];
			System.out.printf("dragging, delta is (%d, %d)\n", deltaX, deltaY);
			
			if(freeview){
				if(deltaY > 0 && xangle > -90){
					xangle-=2;
				} else if(deltaY < 0 &&  xangle < 90){
					xangle+=2;
				}
			} else{
				if(deltaY > 0 && xangle > 0){
					xangle-=2;
				} else if(deltaY < 0 &&  xangle < 90){
					xangle+=2;
				}
			}
			
			if(deltaX > 0){
				yangle+=2;
			} else if(deltaX < 0 ) {
				yangle-=2;
			}
			
			// remember the last mouse position
			lastDragPos[0] = x;
			lastDragPos[1] = y;
		} 
	}
	@Override
	public void mouseMoved(MouseEvent e) {
	}
	@Override
	public void mouseClicked(MouseEvent arg0) {
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
	}
	@Override
	public void mousePressed(MouseEvent e) {
		// TODO: you may need this
		System.out.println("press: (" + e.getX() + "," + e.getY() + ") at " + e.getWhen());
		
		int x = e.getX();
		int y = ((GLCanvas)(e.getSource())).getHeight() - e.getY() - 1;
		
		lastDragPos = new int[] { x, y };
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		if (lastDragPos != null) {
			System.out.println("end drag");
			lastDragPos = null;
		}
	}
	
	// bring in shape & structure (or whatever you used) from A3Q2 here
	// include the OBJ reading code if you are using it
}

class Face {
	public int[] indices;
	public float[] colour;
	
	public Face(int[] indices, float[] colour) {
		this.indices = new int[indices.length];
		this.colour = new float[colour.length];
		System.arraycopy(indices, 0, this.indices, 0, indices.length);
		System.arraycopy(colour, 0, this.colour, 0, colour.length);
	}
	
	//draw with texture
	public void draw(GL2 gl, ArrayList<float[]> vertices, Texture tex) {
		
		tex.bind(gl);
		tex.enable(gl);
		
		if (indices.length == 1) {
			gl.glBegin(GL2.GL_POINTS);
		} else if (indices.length == 2) {
			gl.glBegin(GL2.GL_LINES);
		} else if (indices.length == 3) {
			gl.glBegin(GL2.GL_TRIANGLES);
		} else if (indices.length == 4) {
			gl.glBegin(GL2.GL_QUADS);
				
			gl.glTexCoord2f(0f, 0f);
			gl.glVertex3f(vertices.get(indices[0])[0], vertices.get(indices[0])[1], vertices.get(indices[0])[2]);
			gl.glTexCoord2f(1f, 0f);
			gl.glVertex3f(vertices.get(indices[1])[0], vertices.get(indices[1])[1], vertices.get(indices[1])[2]);
			gl.glTexCoord2f(1f, 1f);
			gl.glVertex3f(vertices.get(indices[2])[0], vertices.get(indices[2])[1], vertices.get(indices[2])[2]);
			gl.glTexCoord2f(0f, 1f);
			gl.glVertex3f(vertices.get(indices[3])[0], vertices.get(indices[3])[1], vertices.get(indices[3])[2]);
		
		} else {
			gl.glBegin(GL2.GL_POLYGON);
		}

		gl.glEnd();
		tex.disable(gl);
	}

	// draw w/o texture
	public void draw(GL2 gl, ArrayList<float[]> vertices, boolean useColour) {
		if (useColour) {
			if (colour.length == 3)
				gl.glColor3f(colour[0], colour[1], colour[2]);
			else
				gl.glColor4f(colour[0], colour[1], colour[2], colour[3]);
		}	
		if (indices.length == 1) {
			gl.glBegin(GL2.GL_POINTS);
		} else if (indices.length == 2) {
			gl.glBegin(GL2.GL_LINES);
		} else if (indices.length == 3) {
			gl.glBegin(GL2.GL_TRIANGLES);
		} else if (indices.length == 4) {
			gl.glBegin(GL2.GL_QUADS);
		} else {
			gl.glBegin(GL2.GL_POLYGON);
		}
		
		for (int i: indices) {
			gl.glVertex3f(vertices.get(i)[0], vertices.get(i)[1], vertices.get(i)[2]);		
		}
		gl.glEnd();
	}
}

// TODO: rewrite the following as you like
class Shape {
	// set this to NULL if you don't want outlines
	public float[] line_colour;

	protected ArrayList<float[]> vertices;
	protected ArrayList<Face> faces;
	Texture tex;

	public Shape() {
		// you could subclass Shape and override this with your own
		init();
		
		// default shape: cube
		vertices.add(new float[] { -1.0f, -1.0f, 1.0f });
		vertices.add(new float[] { 1.0f, -1.0f, 1.0f });
		vertices.add(new float[] { 1.0f, 1.0f, 1.0f });
		vertices.add(new float[] { -1.0f, 1.0f, 1.0f });
		vertices.add(new float[] { -1.0f, -1.0f, -1.0f });
		vertices.add(new float[] { 1.0f, -1.0f, -1.0f });
		vertices.add(new float[] { 1.0f, 1.0f, -1.0f });
		vertices.add(new float[] { -1.0f, 1.0f, -1.0f });
		
		faces.add(new Face(new int[] { 0, 1, 2, 3 }, new float[] { 1.0f, 0.0f, 0.0f } ));
		faces.add(new Face(new int[] { 0, 3, 7, 4 }, new float[] { 1.0f, 1.0f, 0.0f } ));
		faces.add(new Face(new int[] { 7, 6, 5, 4 }, new float[] { 1.0f, 1.0f, 1.0f } ));
		faces.add(new Face(new int[] { 2, 1, 5, 6 }, new float[] { 0.0f, 1.0f, 0.0f } ));
		faces.add(new Face(new int[] { 3, 2, 6, 7 }, new float[] { 0.0f, 0.0f, 1.0f } ));
		faces.add(new Face(new int[] { 1, 0, 4, 5 }, new float[] { 0.0f, 1.0f, 1.0f } ));
		
	}

	public Shape(String filename) {
		init();

		// TODO Use as you like
		// NOTE that there is limited error checking, to make this as flexible as possible
		BufferedReader input;
		String line;
		String[] tokens;

		float[] vertex;
		float[] colour;
		String specifyingMaterial = null;
		String selectedMaterial;
		int[] face;
		
		HashMap<String, float[]> materials = new HashMap<String, float[]>();
		materials.put("default", new float[] {1,1,1});
		selectedMaterial = "default";
		
		// vertex positions start at 1
		vertices.add(new float[] {0,0,0});
		
		int currentColourIndex = 0;

		// these are for error checking (which you don't need to do)
		int lineCount = 0;
		int vertexCount = 0, colourCount = 0, faceCount = 0;

		try {
			input = new BufferedReader(new FileReader(filename));

			line = input.readLine();
			while (line != null) {
				lineCount++;
				tokens = line.split("\\s+");

				if (tokens[0].equals("v")) {
					assert tokens.length == 4 : "Invalid vertex specification (line " + lineCount + "): " + line;

					vertex = new float[3];
					try {
						vertex[0] = Float.parseFloat(tokens[1]);
						vertex[1] = Float.parseFloat(tokens[2]);
						vertex[2] = Float.parseFloat(tokens[3]);
					} catch (NumberFormatException nfe) {
						assert false : "Invalid vertex coordinate (line " + lineCount + "): " + line;
					}

					System.out.printf("vertex %d: (%f, %f, %f)\n", vertexCount + 1, vertex[0], vertex[1], vertex[2]);
					vertices.add(vertex);

					vertexCount++;
				} else if (tokens[0].equals("newmtl")) {
					assert tokens.length == 2 : "Invalid material name (line " + lineCount + "): " + line;
					specifyingMaterial = tokens[1];
				} else if (tokens[0].equals("Kd")) {
					assert tokens.length == 4 : "Invalid colour specification (line " + lineCount + "): " + line;
					assert faceCount == 0 && currentColourIndex == 0 : "Unexpected (late) colour (line " + lineCount + "): " + line;

					colour = new float[3];
					try {
						colour[0] = Float.parseFloat(tokens[1]);
						colour[1] = Float.parseFloat(tokens[2]);
						colour[2] = Float.parseFloat(tokens[3]);
					} catch (NumberFormatException nfe) {
						assert false : "Invalid colour value (line " + lineCount + "): " + line;
					}
					for (float colourValue: colour) {
						assert colourValue >= 0.0f && colourValue <= 1.0f : "Colour value out of range (line " + lineCount + "): " + line;
					}

					if (specifyingMaterial == null) {
						System.out.printf("Error: no material name for colour %d: (%f %f %f)\n", colourCount + 1, colour[0], colour[1], colour[2]);
					} else {
						System.out.printf("material %s: (%f %f %f)\n", specifyingMaterial, colour[0], colour[1], colour[2]);
						materials.put(specifyingMaterial, colour);
					}

					colourCount++;
				} else if (tokens[0].equals("usemtl")) {
					assert tokens.length == 2 : "Invalid material selection (line " + lineCount + "): " + line;

					selectedMaterial = tokens[1];
				} else if (tokens[0].equals("f")) {
					assert tokens.length > 1 : "Invalid face specification (line " + lineCount + "): " + line;

					face = new int[tokens.length - 1];
					try {
						for (int i = 1; i < tokens.length; i++) {
							face[i - 1] = Integer.parseInt(tokens[i].split("/")[0]);
						}
					} catch (NumberFormatException nfe) {
						assert false : "Invalid vertex index (line " + lineCount + "): " + line;
					}

					System.out.printf("face %d: [ ", faceCount + 1);
					for (int index: face) {
						System.out.printf("%d ", index);
					}
					System.out.printf("] using material %s\n", selectedMaterial);
					
					colour = materials.get(selectedMaterial);
					if (colour == null) {
						System.out.println("Error: material " + selectedMaterial + " not found, using default.");
						colour = materials.get("default");
					}
					faces.add(new Face(face, colour));

					faceCount++;
				} else {
					System.out.println("Ignoring: " + line);
				}

				line = input.readLine();
			}
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
			assert false : "Error reading input file " + filename;
		}
	}

	protected void init() {
		vertices = new ArrayList<float[]>();
		faces = new ArrayList<Face>();
		
		line_colour = new float[] { 0,0,0 };
	}
	
	public void draw(GL2 gl, boolean lines) {

		for (Face f: faces) {
			if (line_colour == null) {
				f.draw(gl, vertices, true);
			} else {
				gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
				gl.glPolygonOffset(1.0f, 1.0f);
				
				if(tex != null){
					f.draw(gl, vertices, tex);
				} else{
					f.draw(gl, vertices, true);
				}
			
				gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
				
				if(lines){
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
					gl.glLineWidth(2.0f);
					gl.glColor3f(line_colour[0], line_colour[1], line_colour[2]);
					f.draw(gl, vertices, false);
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
					gl.glColor3f(1, 1, 1);
				}
				
			}
		}
	}

} // End Class Shape


class SmallBox extends Shape{
	
	private Texture tex;

	public SmallBox(Texture tex){
		super.init();
		
		//shape: SmallCrate.
		vertices.add(new float[] { -0.5f, -0.5f, 0.5f });
		vertices.add(new float[] { 0.5f, -0.5f, 0.5f });
		vertices.add(new float[] { 0.5f, 0.5f, 0.5f });
		vertices.add(new float[] { -0.5f, 0.5f, 0.5f });
		vertices.add(new float[] { -0.5f, -0.5f, -0.5f });
		vertices.add(new float[] { 0.5f, -0.5f, -0.5f });
		vertices.add(new float[] { 0.5f, 0.5f, -0.5f });
		vertices.add(new float[] { -0.5f, 0.5f, -0.5f });
		
		faces.add(new Face(new int[] { 0, 1, 2, 3 }, new float[] { 1.0f, 0.0f, 0.0f } ));
		faces.add(new Face(new int[] { 0, 3, 7, 4 }, new float[] { 1.0f, 1.0f, 0.0f } ));
		faces.add(new Face(new int[] { 7, 6, 5, 4 }, new float[] { 1.0f, 1.0f, 1.0f } ));
		faces.add(new Face(new int[] { 2, 1, 5, 6 }, new float[] { 0.0f, 1.0f, 0.0f } ));
		faces.add(new Face(new int[] { 3, 2, 6, 7 }, new float[] { 0.0f, 0.0f, 1.0f } ));
		faces.add(new Face(new int[] { 1, 0, 4, 5 }, new float[] { 0.0f, 1.0f, 1.0f } ));
		
		this.tex = tex;
	}
	
	public void draw(GL2 gl, boolean lines){
		
		for (Face f: faces) {
			if (line_colour == null) {
				f.draw(gl, vertices, true);
			} else {
				gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
				gl.glPolygonOffset(1.0f, 1.0f);
				
				tex.bind(gl);
				tex.enable(gl);
				gl.glBegin(GL2.GL_QUADS);	
				gl.glTexCoord2f(0f, 0f);
				gl.glVertex3f(vertices.get(f.indices[0])[0], vertices.get(f.indices[0])[1], vertices.get(f.indices[0])[2]);
				gl.glTexCoord2f(1f, 0f);
				gl.glVertex3f(vertices.get(f.indices[1])[0], vertices.get(f.indices[1])[1], vertices.get(f.indices[1])[2]);
				gl.glTexCoord2f(1f, 1f);
				gl.glVertex3f(vertices.get(f.indices[2])[0], vertices.get(f.indices[2])[1], vertices.get(f.indices[2])[2]);
				gl.glTexCoord2f(0f, 1f);
				gl.glVertex3f(vertices.get(f.indices[3])[0], vertices.get(f.indices[3])[1], vertices.get(f.indices[3])[2]);
				gl.glEnd();
				tex.disable(gl);
				
				gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
				
				if(lines){
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
					gl.glLineWidth(2.0f);
					gl.glColor3f(line_colour[0], line_colour[1], line_colour[2]);
					f.draw(gl, vertices, false);
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
				}		
			}
		}
	}// draw end
	
} // Class smallBox end.

class LargeBox extends Shape{
	
	private Texture tex;
	
	public LargeBox(Texture tex){
		super.init();
		
		//shape: SmallCrate.
		vertices.add(new float[] { -1f, -1f, 1f });
		vertices.add(new float[] { 1f, -1f, 1f });
		vertices.add(new float[] { 1f, 1f, 1f });
		vertices.add(new float[] { -1f, 1f, 1f });
		vertices.add(new float[] { -1f, -1f, -1f });
		vertices.add(new float[] { 1f, -1f, -1f });
		vertices.add(new float[] { 1f, 1f, -1f });
		vertices.add(new float[] { -1f, 1f, -1f });
		
		faces.add(new Face(new int[] { 0, 1, 2, 3 }, new float[] { 1.0f, 0.0f, 0.0f } ));
		faces.add(new Face(new int[] { 0, 3, 7, 4 }, new float[] { 1.0f, 1.0f, 0.0f } ));
		faces.add(new Face(new int[] { 7, 6, 5, 4 }, new float[] { 1.0f, 1.0f, 1.0f } ));
		faces.add(new Face(new int[] { 2, 1, 5, 6 }, new float[] { 0.0f, 1.0f, 0.0f } ));
		faces.add(new Face(new int[] { 3, 2, 6, 7 }, new float[] { 0.0f, 0.0f, 1.0f } ));
		faces.add(new Face(new int[] { 1, 0, 4, 5 }, new float[] { 0.0f, 1.0f, 1.0f } ));
		
		this.tex = tex;
	}
	
	public void draw(GL2 gl, boolean lines){
		
		for (Face f: faces) {
			if (line_colour == null) {
				f.draw(gl, vertices, true);
			} else {
				gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
				gl.glPolygonOffset(1.0f, 1.0f);
				
				
				tex.bind(gl);
				tex.enable(gl);
				
				gl.glBegin(GL2.GL_QUADS);	
				gl.glTexCoord2f(0f, 0f);
				gl.glVertex3f(vertices.get(f.indices[0])[0], vertices.get(f.indices[0])[1], vertices.get(f.indices[0])[2]);
				gl.glTexCoord2f(1f, 0f);
				gl.glVertex3f(vertices.get(f.indices[1])[0], vertices.get(f.indices[1])[1], vertices.get(f.indices[1])[2]);
				gl.glTexCoord2f(1f, 1f);
				gl.glVertex3f(vertices.get(f.indices[2])[0], vertices.get(f.indices[2])[1], vertices.get(f.indices[2])[2]);
				gl.glTexCoord2f(0f, 1f);
				gl.glVertex3f(vertices.get(f.indices[3])[0], vertices.get(f.indices[3])[1], vertices.get(f.indices[3])[2]);
				
				gl.glEnd();
				tex.disable(gl);
				
				gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
				
				if(lines){
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
					gl.glLineWidth(2.0f);
					gl.glColor3f(line_colour[0], line_colour[1], line_colour[2]);
					f.draw(gl, vertices, false);
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
				}		
			}
		}
	}// draw end
	
} // Class largeBox end.

class Pyramid extends Shape{
	
	private Texture tex;
	
	public Pyramid(Texture tex){
		super.init();
		
		//shape: pyramid.
		vertices.add(new float[] { -10f, 0f, -10f });
		vertices.add(new float[] { 10f, 0f, -10f });
		vertices.add(new float[] { 10f, 0f, 10f });
		vertices.add(new float[] { -10f, 0f, 10f });
		vertices.add(new float[] { 0f, 20f, 0f });

		
		faces.add(new Face(new int[] { 0, 1, 4}, new float[] { 1.0f, 0.0f, 0.0f } ));
		faces.add(new Face(new int[] { 1, 2, 4 }, new float[] { 1.0f, 1.0f, 0.0f } ));
		faces.add(new Face(new int[] { 2, 3, 4 }, new float[] { 1.0f, 1.0f, 1.0f } ));
		faces.add(new Face(new int[] { 3, 0, 4  }, new float[] { 0.0f, 1.0f, 0.0f } ));
		this.tex = tex;
	}
	
	public void draw(GL2 gl, boolean lines){
		
		for (Face f: faces) {
			if (line_colour == null) {
				f.draw(gl, vertices, true);
			} else {
				gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
				gl.glPolygonOffset(1.0f, 1.0f);
				
				
				tex.bind(gl);
				tex.enable(gl);
				
				gl.glBegin(GL2.GL_TRIANGLES);	
				gl.glTexCoord2f(0f, 0f);
				gl.glVertex3f(vertices.get(f.indices[0])[0], vertices.get(f.indices[0])[1], vertices.get(f.indices[0])[2]);
				gl.glTexCoord2f(1f, 0f);
				gl.glVertex3f(vertices.get(f.indices[1])[0], vertices.get(f.indices[1])[1], vertices.get(f.indices[1])[2]);
				gl.glTexCoord2f(0.5f, 1f);
				gl.glVertex3f(vertices.get(f.indices[2])[0], vertices.get(f.indices[2])[1], vertices.get(f.indices[2])[2]);
				gl.glEnd();
				tex.disable(gl);
				
				gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
				
				if(lines){
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
					gl.glLineWidth(2.0f);
					gl.glColor3f(line_colour[0], line_colour[1], line_colour[2]);
					f.draw(gl, vertices, false);
					gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
				}		
			}
		}
	}// draw end
	
} // Class Pyramid end.


class Tree extends Shape{
	
	private Texture tex;
	private GLU glu;
	
	public Tree(Texture tex, GLU glu){
		super.init();
		this.tex = tex;
		this.glu = glu;
	}

	public void draw(GL2 gl, boolean lines){
		GLUquadric cylinder = glu.gluNewQuadric();
		glu.gluQuadricDrawStyle(cylinder, GLU.GLU_FILL);
		glu.gluQuadricTexture(cylinder, true);
		glu.gluQuadricNormals(cylinder, GLU.GLU_SMOOTH);
		
		tex.bind(gl);
		tex.enable(gl);
		gl.glPushMatrix();
		gl.glRotatef(-90, 1, 0, 0);
		//glu.gluDisk(cylinder, 0, flip?0.1:0.12, 9, 1);
		glu.gluCylinder(cylinder, .5, .1, 5, 8, 8);
	
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glTranslatef(0f,4.5f,0f);
		gl.glRotatef(-90, 1, 0, 0);
		glu.gluSphere(cylinder, 0.5, 5, 5);
		gl.glPopMatrix();
		
		tex.disable(gl);
		
		gl.glColor3f(0f, 0.5f, 0f);
		for(int i = 0; i<360; i+=36){
			gl.glPushMatrix();
			gl.glRotatef(i, 0, 1, 0);
			gl.glBegin(GL2.GL_QUADS);
			gl.glVertex3f(0, 5, 0);
			gl.glVertex3f(-0.25f, 4.75f, 1f);
			gl.glVertex3f(0f, 4f, 2.5f);
			gl.glVertex3f(0.25f, 4.75f, 1f);
			gl.glEnd();
			gl.glPopMatrix();
		}
		gl.glColor3f(1, 1, 1);
	}
	
} // Tree class end.

class Cactuar extends Shape{
	private Texture tex;
	private GLU glu;
	
	private float armT;
	private boolean reverseArms;
	
	public Cactuar(Texture tex, GLU glu){
		super.init();
		this.tex = tex;
		this.glu = glu;
		armT = 0.0f;
		reverseArms = false;
	}

	public void draw(GL2 gl, boolean lines){
		GLUquadric cylinder = glu.gluNewQuadric();
		glu.gluQuadricDrawStyle(cylinder, GLU.GLU_FILL);
		glu.gluQuadricTexture(cylinder, true);
		glu.gluQuadricNormals(cylinder, GLU.GLU_SMOOTH);
			
		float arms;
		
		armT += 0.025f;
		
		if(armT >= 1){
			reverseArms = !reverseArms;
			armT = 0.0f;
		}
		
		arms = lerp(armT, 0f, 180f);
		if (reverseArms) arms = 180f - arms;
	
		
		tex.bind(gl);
		tex.enable(gl);
		
		gl.glPushMatrix();

		gl.glRotatef(-35, 0, 0, 1);
		
		gl.glPushMatrix();
		gl.glTranslatef(0, 0, 0.01f);
		
		//face
		gl.glColor3f(0, 0, 0);
		gl.glBegin(GL2.GL_QUADS);
		gl.glVertex3f(-0.3f, 1.45f, 0.5f);
		gl.glVertex3f(-0.2f, 1.45f, 0.5f);
		gl.glVertex3f(-0.2f, 1.55f, 0.5f);
		gl.glVertex3f(-0.3f, 1.55f, 0.5f);
		
		gl.glVertex3f(0.3f, 1.45f, 0.5f);
		gl.glVertex3f(0.2f, 1.45f, 0.5f);
		gl.glVertex3f(0.2f, 1.55f, 0.5f);
		gl.glVertex3f(0.3f, 1.55f, 0.5f);

		gl.glVertex3f(-0.1f, 0.75f, 0.5f);
		gl.glVertex3f(0.1f, 0.75f, 0.5f);
		gl.glVertex3f(0.1f, 1.25f, 0.5f);
		gl.glVertex3f(-0.1f, 1.25f, 0.5f);
		
		gl.glEnd();
		
		gl.glPopMatrix();
		gl.glColor3f(1, 1, 1);
		
		for(int i = 0; i<9; i++){
			
	
			gl.glPushMatrix();
			
			if(i == 0){
				gl.glTranslatef(0, 0, 0);// do nothing
			}else if(i==1){ //right shoulder
				gl.glTranslatef(1, 1, 0);
				gl.glRotatef(arms, 1, 0, 0);
				gl.glRotated(90, 0, 0, 1);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			} else if(i == 2){ // right forearm
				gl.glTranslatef(1, 1, 0);
				gl.glRotatef(arms, 1, 0, 0);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}else if(i == 3){// left shoulder
				gl.glTranslatef(-1, 1, 0);
				gl.glRotatef(-arms, 1, 0, 0);
				gl.glRotated(-90, 0, 0, 1);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}else if(i == 4){// left forearm
				gl.glTranslatef(-1f, 1f, 0);
				gl.glRotatef((-arms)+180, 1, 0, 0);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}else if(i == 5){// left thigh
				gl.glTranslatef(-0.25f, -1, 0);
				gl.glRotated(90, 0, 0, 1);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}else if(i == 6){//left shin
				gl.glTranslatef(-0.25f, -1, 0);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}else if(i == 7){//right thigh
				gl.glTranslatef(1, 0, 0);
				gl.glRotated(90, 0, 0, 1);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}else if(i == 8){//right shin
				gl.glTranslatef(1, -1, 0);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}
			
			gl.glPushMatrix();
			gl.glRotatef(-90, 1, 0, 0);
			//glu.gluDisk(cylinder, 0, flip?0.1:0.12, 9, 1);
			glu.gluCylinder(cylinder, .5, .5, 2, 8, 8);
			gl.glPopMatrix();
			gl.glPushMatrix();
			gl.glTranslatef(0f,2f,0f);
			gl.glRotatef(-90, 1, 0, 0);
			glu.gluSphere(cylinder, 0.5, 8, 8);
			gl.glPopMatrix();
			
			gl.glPushMatrix();
			gl.glTranslatef(0f,0f,0f);
			gl.glRotatef(-90, 1, 0, 0);
			glu.gluSphere(cylinder, 0.5, 8, 8);
			gl.glPopMatrix();
			
			gl.glPopMatrix();
		}
		
		gl.glPopMatrix();
	
		tex.disable(gl);
	}
	
	public float lerp(float t, float a, float b) {
		return (1 - t) * a + t * b;
	}

} // cactuar class end.

class RoboHead extends Shape{
	private Texture tex;
	private GLU glu;
	
	public RoboHead(Texture tex, GLU glu){
		super.init();
		this.tex = tex;
		this.glu = glu;
	}
	
	public void draw(GL2 gl, boolean lines){
		GLUquadric cylinder = glu.gluNewQuadric();
		glu.gluQuadricDrawStyle(cylinder, GLU.GLU_FILL);
		glu.gluQuadricTexture(cylinder, true);
		glu.gluQuadricNormals(cylinder, GLU.GLU_SMOOTH);
		
		tex.bind(gl);
		tex.enable(gl);
		gl.glPushMatrix();
		gl.glTranslatef(0f, 0.13f, 0f);
		gl.glRotatef(-90, 1, 0, 0);
		glu.gluCylinder(cylinder, .12, .12, .35, 8, 8);
		gl.glPopMatrix();
		tex.disable(gl);
		gl.glColor3f(.68f,.75f ,0.79f);
		gl.glPushMatrix();
		gl.glTranslatef(0f,0.45f,0f);
		gl.glRotatef(-90, 1, 0, 0);
		glu.gluSphere(cylinder, 0.12, 8, 8);
		gl.glPopMatrix();
	
		gl.glColor3f(1, 1, 1);
	}
}

class Structure extends Shape {
	// this array can include other structures...
	protected Shape[] contents; // shapes array
	public float[][] positions;
	
	protected float tOne;
	protected float tTwo;
	protected float rotDirOne;
	protected float rotDirTwo;
	
	protected float[] boundingBox;
	
	protected float[][][] shapesMM;
	
	public float xMax;
	public float yMax;
	public float zMax;
	public float xMin;
	public float yMin;
	public float zMin;
	
	float moveDist;
	int moveDir;
	float moveSpd;
	float moveT;
	boolean move;
	boolean reverse;
	
	
	protected final float STEP = 0.0001f;
	
	public Structure(Shape[] contents, float[][] positions, float[] boundingBox,float moveDist, int moveDir, float moveSpd) {
		super();
		init(contents, positions);
		tOne = 1.0f;
		tTwo = 1.0f;
		rotDirOne = -1f;
		rotDirTwo = -1;
		this.boundingBox = boundingBox;
		initMaxMin();
		this.moveDist = moveDist;
		this.moveDir = moveDir;
		this.moveSpd = moveSpd;
		moveT = 0;
		reverse = false;
		move = true;
	}
	
	public Structure(Shape[] contents, float[][] positions, float[] boundingBox) {
		super();
		init(contents, positions);
		tOne = 1.0f;
		tTwo = 1.0f;
		rotDirOne = -1f;
		rotDirTwo = -1;
		this.boundingBox = boundingBox;
		initMaxMin();
		move = false;
	}
	
	public Structure(String filename, Shape[] contents, float[][] positions) {
		super(filename);
		init(contents, positions);
		tOne = 1.0f;
		tTwo = 1.0f;
		rotDirOne = -1f;
		rotDirTwo = -1f;
		shapesMM = new float[contents.length][3][2];
	
	}
	
	private void init(Shape[] contents, float[][] positions) {
		this.contents = new Shape[contents.length];
		this.positions = new float[positions.length][3];
		System.arraycopy(contents, 0, this.contents, 0, contents.length);
		for (int i = 0; i < positions.length; i++) {
			System.arraycopy(positions[i], 0, this.positions[i], 0, 3);
		}
	}

	private void initMaxMin() {	
		xMax = boundingBox[0];
		xMin = boundingBox[1];
		yMax = boundingBox[2];
		yMin = boundingBox[3];
		zMax = boundingBox[4];
		zMin = boundingBox[5];
	}
	
	
	public void draw(GL2 gl) {
		gl.glPushMatrix();
		gl.glTranslatef(positions[0][0], positions[0][1], positions[0][2]);
		
		if(move){
			float move;
			moveT += moveSpd;
			if(moveT >= 1){
				reverse = !reverse;
				moveT = 0.0f;
			}
			move = lerp(moveT, 0f, -20f);
			if (reverse) move = -20f - move;
			
			if(moveDir == 0){
				gl.glTranslatef(move, 0, 0);
				xMax = boundingBox[0]+ move;
				xMin = boundingBox[1]+ move;
			} else if(moveDir == 1){
				gl.glTranslatef(0, move, 0);
				yMax = boundingBox[2]+ move;
				yMin = boundingBox[3]+ move;
			} else if(moveDir == 2){
				gl.glTranslatef(0, 0, move);
				zMax = boundingBox[4]+ move;
				zMin = boundingBox[5]+ move;
			}
		}
		
		for(int i = 0; i<contents.length; i++){
			contents[i].draw(gl,false);
		}
		
		//drawBoundingBox(gl);
		gl.glPopMatrix();
		
	}
	
	public void drawBoundingBox(GL2 gl){
		gl.glPolygonMode( GL2.GL_FRONT_AND_BACK, GL2.GL_LINE );
		gl.glBegin(GL2.GL_QUADS);
		gl.glColor3f(1f, 1f, 1f);
		
		gl.glVertex3f(boundingBox[1],boundingBox[3],boundingBox[5]);
		gl.glVertex3f(boundingBox[0],boundingBox[3],boundingBox[5]);
		gl.glVertex3f(boundingBox[0],boundingBox[2],boundingBox[5]);
		gl.glVertex3f(boundingBox[1],boundingBox[2],boundingBox[5]);
		
		gl.glVertex3f(boundingBox[0],boundingBox[3],boundingBox[5]);
		gl.glVertex3f(boundingBox[0],boundingBox[3],boundingBox[4]);
		gl.glVertex3f(boundingBox[0],boundingBox[2],boundingBox[4]);
		gl.glVertex3f(boundingBox[0],boundingBox[2],boundingBox[5]);
		
		gl.glVertex3f(boundingBox[0],boundingBox[3],boundingBox[4]);
		gl.glVertex3f(boundingBox[1],boundingBox[3],boundingBox[4]);
		gl.glVertex3f(boundingBox[1],boundingBox[2],boundingBox[4]);
		gl.glVertex3f(boundingBox[0],boundingBox[2],boundingBox[4]);
		
		
		gl.glVertex3f(boundingBox[1],boundingBox[3],boundingBox[4]);
		gl.glVertex3f(boundingBox[1],boundingBox[3],boundingBox[5]);
		gl.glVertex3f(boundingBox[1],boundingBox[2],boundingBox[5]);
		gl.glVertex3f(boundingBox[1],boundingBox[2],boundingBox[4]);
		
		
		gl.glEnd();
		gl.glPolygonMode( GL2.GL_FRONT_AND_BACK, GL2.GL_FILL );
	} 
	
	
	public float lerp(float t, float a, float b) {
		return (1 - t) * a + t * b;
	}
		
} // End Class Structure.

class Robot extends Structure{
	
	
	public Robot(Shape[] contents, float[][] positions, float[] boundingBox){
		super(contents, positions, boundingBox);
	}
	

	
	public void draw(GL2 gl){
		
		boolean lines = true; 
		//Head
		gl.glPushMatrix();
		gl.glTranslatef(positions[13][0], positions[13][1], positions[13][2]);
		//gl.glScalef(0.0075f, 0.0075f, 0.0075f);
		contents[13].draw(gl,true);
		gl.glPopMatrix();
		
		//Body 
		gl.glPushMatrix();
		gl.glTranslatef(positions[0][0], positions[0][1], positions[0][2]);
		gl.glScalef(0.30f, 0.30f, 0.3f);
		contents[0].draw(gl,lines);
		gl.glPopMatrix();
		
		//Left Arm
		gl.glPushMatrix();
		gl.glRotatef((rotDirOne)*45*Math.abs(tOne),1f,0f,0f);
		
		gl.glPushMatrix();
		gl.glTranslatef(positions[1][0], positions[1][1], positions[1][2]);
		gl.glScalef(0.3f, 0.15f, 1);
		contents[1].draw(gl,lines);
		gl.glPopMatrix();
		
		//Left forearm
		gl.glTranslatef(positions[11][0], positions[11][1], positions[11][2]);
		gl.glRotatef(-45*Math.abs(tOne),1f,0f,0f);
		
		gl.glPushMatrix();
		gl.glScalef(0.3f, 0.2f, 1);
		contents[11].draw(gl,lines);
		gl.glPopMatrix();
		
		//LeftHand
		gl.glTranslatef(positions[3][0], positions[3][1], positions[3][2]);
		gl.glRotatef((rotDirOne)*-45*Math.abs(tOne),0f,1f,0f);
		gl.glScalef(0.3f, 0.3f, 1);
		contents[3].draw(gl,lines);
		gl.glPopMatrix();
		
		//Right Arm
		gl.glPushMatrix();
		gl.glRotatef((rotDirTwo)*-45*Math.abs(tTwo),1f,0f,0f);
		
		gl.glPushMatrix();
		gl.glTranslatef(positions[2][0], positions[2][1], positions[2][2]);
		gl.glScalef(0.3f, 0.15f, 1);
		contents[2].draw(gl,lines);
		gl.glPopMatrix();
		
		//Right forearm
		gl.glTranslatef(positions[12][0], positions[12][1], positions[12][2]);
		gl.glRotatef(-45*Math.abs(tTwo),1f,0f,0f);
		
		gl.glPushMatrix();
		gl.glScalef(0.3f, 0.2f, 1);
		contents[12].draw(gl,lines);
		gl.glPopMatrix();
		
		//Right Hand
		gl.glTranslatef(positions[4][0], positions[4][1], positions[4][2]);
		gl.glRotatef((rotDirTwo)*-45*Math.abs(tTwo),0f,1f,0f);
		gl.glScalef(0.3f, 0.3f, 1);
		contents[4].draw(gl,lines);
		gl.glPopMatrix();
		
		//Left Leg
		gl.glPushMatrix();
		gl.glRotatef((rotDirTwo)*-15*Math.abs(tTwo),1f,0f,0f);

		gl.glPushMatrix();
		gl.glTranslatef(positions[5][0], positions[5][1], positions[5][2]);
		//gl.glRotatef(20*Math.abs(tTwo),1f,0f,0f);
		gl.glScalef(0.3f, 0.225f, 1);
		contents[5].draw(gl,lines);
		gl.glPopMatrix();
		
		//Left Shin (7)	
		gl.glTranslatef(positions[7][0], positions[7][1], positions[7][2]);
		gl.glRotatef(25*Math.abs(tTwo),1f,0f,0f);
		
		gl.glPushMatrix();
		gl.glScalef(0.3f, 0.225f, 1);
		contents[7].draw(gl,lines);
		gl.glPopMatrix();
		
		//LeftFoot (9)
		gl.glTranslatef(positions[9][0], positions[9][1], positions[9][2]);
		gl.glRotatef((rotDirTwo)*-25*Math.abs(tTwo),1f,0f,0f);
		gl.glScalef(0.3f, 0.2f, 1);
		contents[9].draw(gl,lines);
		gl.glPopMatrix();
		
		//Right Leg (6)
		gl.glPushMatrix();
		gl.glRotatef((rotDirOne)*15*Math.abs(tOne),1f,0f,0f);

		gl.glPushMatrix();
		gl.glTranslatef(positions[6][0], positions[6][1], positions[6][2]);
		//gl.glRotatef(20*Math.abs(tTwo),1f,0f,0f);
		gl.glScalef(0.3f, 0.225f, 1);
		contents[5].draw(gl,lines);
		gl.glPopMatrix();
			
		//Right Shin (8)	
		gl.glTranslatef(positions[8][0], positions[8][1], positions[8][2]);
		gl.glRotatef(25*Math.abs(tOne),1f,0f,0f);
		
		gl.glPushMatrix();;
		gl.glScalef(0.3f, 0.225f, 1);
		contents[7].draw(gl,lines);
		gl.glPopMatrix();
				
		// Right Foot (10)	
		gl.glTranslatef(positions[10][0], positions[10][1], positions[10][2]);
		gl.glRotatef((rotDirOne)*25*Math.abs(tOne),1f,0f,0f);
		gl.glScalef(0.3f, 0.2f, 1);
		contents[9].draw(gl,lines);
		gl.glPopMatrix();
		
		//drawBoundingBox(gl);
		
		gl.glPopMatrix();
		
	}// DRAW ROBOT END
	
	
	public void walkAnimation(){	
		if (tOne >= 1) {
			tOne = -1*tOne;
		} 
		if (tTwo >= 1){
			tTwo = -1*tTwo;
		}
		if(tTwo < 0f && ((tTwo+0.1f) > 0) ){
			rotDirTwo = -1*rotDirTwo;
		}
		if(tOne < 0f && ((tOne+0.1f) > 0)){
			rotDirOne = -1*rotDirOne;
		}
		tOne += 0.1f;
		tTwo += 0.1f;		
	}// Walk animation end.
}

