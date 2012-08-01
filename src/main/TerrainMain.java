/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import static opengl.GL.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import opengl.GL;
import opengl.OpenCL;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import util.*;

/**
 *
 * @author nico3000
 * test
 */
public class TerrainMain {
    // current configurations
    private static boolean bContinue = true;
    private static boolean culling = true;
    private static boolean wireframe = true;
    
    // control
    private static final Vector3f moveDir = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final Camera cam = new Camera(); 
    
    // animation params
    private static float ingameTime = 0;
    private static float ingameTimePerSecond = 1.0f;
    
    //tone mapping
    private static int exposureLoc;
    private static float exposure = 1.0f;
    
    private static ShaderProgram fboSP; 
    
    public static void main(String[] argv) {
        try {
            init();
            OpenCL.init();
            glEnable(GL_CULL_FACE);
            glFrontFace(GL_CCW);
            glCullFace(GL_BACK);
            glEnable(GL_DEPTH_TEST);
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR);
            
            render();
            OpenCL.destroy();
            destroy();
        } catch (LWJGLException ex) {
            Logger.getLogger(TerrainMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void render() throws LWJGLException {
        glClearColor(0.1f, 0.0f, 0.0f, 1.0f); // background color: dark red
        
        long last = System.currentTimeMillis();
        long now, millis;
        long frameTimeDelta = 0;
        int frames = 0;
        
        fboSP = new ShaderProgram("./shader/Main_VS.glsl", "./shader/Main_FS.glsl");
        
        DeferredShader shader = new DeferredShader();
        shader.init();
        shader.registerShaderProgram(fboSP);
        Texture tex = Texture.generateTexture("asteroid.jpg", 0);
        
        Geometry testCube = GeometryFactory.createCube();
        Geometry screenQuad = GeometryFactory.createScreenQuad();
        
        ShaderProgram phongSP = new ShaderProgram("./shader/ScreenQuad_VS.glsl", "./shader/PhongLighting_FS.glsl");
        ShaderProgram toneSP = new ShaderProgram("./shader/ScreenQuad_VS.glsl", "./shader/ToneMapping_FS.glsl");
        FrameBuffer enlightened = new FrameBuffer();
        enlightened.init(false, GL.WIDTH, GL.HEIGHT);
        enlightened.addTexture(new Texture(GL_TEXTURE_2D, 0), GL30.GL_RGBA16F, GL_RGBA);
        
        while(bContinue && !Display.isCloseRequested()) {
            // time handling
            now = System.currentTimeMillis();
            millis = now - last;
            last = now;     
            frameTimeDelta += millis;
            ++frames;
            if(frameTimeDelta > 1000) {
                System.out.println(1e3f * (float)frames / (float)frameTimeDelta + " FPS");
                frameTimeDelta -= 1000;
                frames = 0;
            }
            
            // input and animation
            handleInput(millis);
            animate(millis);
            
            // clear screen
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            
            fboSP.use();
        	Matrix4f modelMatrix = Util.mul(null, Util.rotationX(1.0f, null), Util.rotationZ(1.0f, null));
        	Matrix4f modelIT = Util.transposeInverse(modelMatrix, null);
        	fboSP.setUniform("model", 	 modelMatrix);
        	fboSP.setUniform("modelIT",  modelIT);
        	fboSP.setUniform("viewProj", Util.mul(null, cam.getProjection(), cam.getView()));
            fboSP.setUniform("camPos",   cam.getCamPos());
            
            shader.bind();
            shader.clear();
        	
            testCube.draw();

        	shader.finish();
        	
        	// blinn-phong lighting
        	enlightened.bind();
        	phongSP.use();
        	phongSP.setUniform("normalTex",  shader.getNormalTexture());
        	phongSP.setUniform("worldTex",   shader.getWorldTexture());
        	phongSP.setUniform("diffuseTex", shader.getDiffuseTexture());
        	phongSP.setUniform("camPos",     cam.getCamPos());
        	
        	screenQuad.draw();
        	
        	enlightened.unbind();
        	
        	// tone mapping
        	toneSP.use();
        	toneSP.setUniform("diffuseTex", shader.getDiffuseTexture());
        	toneSP.setUniform("exposure", exposure);
        	screenQuad.draw();
        	
        	
        	//shader.DrawTexture(enlightened.getTexture(0));
            
            // TODO: postfx
            
            // present screen
            Display.update();
            Display.sync(60);
        }
        shader.delete();
        tex.delete();
    }
    
    /**
     * Behandelt Input und setzt die Kamera entsprechend.
     * @param millis Millisekunden seit dem letzten Aufruf
     */
    public static void handleInput(long millis) {
        float moveSpeed = 2e-3f*(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 2.0f : 1.0f)*(float)millis;
        float camSpeed = 5e-3f;
        
        while(Keyboard.next()) {
            if(Keyboard.getEventKeyState()) {
                switch(Keyboard.getEventKey()) {
                    case Keyboard.KEY_W: moveDir.z += 1.0f; break;
                    case Keyboard.KEY_S: moveDir.z -= 1.0f; break;
                    case Keyboard.KEY_A: moveDir.x += 1.0f; break;
                    case Keyboard.KEY_D: moveDir.x -= 1.0f; break;
                    case Keyboard.KEY_SPACE: moveDir.y += 1.0f; break;
                    case Keyboard.KEY_C: moveDir.y -= 1.0f; break;
                    case Keyboard.KEY_ESCAPE: bContinue = false; break;
                }
            } else {
                switch(Keyboard.getEventKey()) {
                    case Keyboard.KEY_W: moveDir.z -= 1.0f; break;
                    case Keyboard.KEY_S: moveDir.z += 1.0f; break;
                    case Keyboard.KEY_A: moveDir.x -= 1.0f; break;
                    case Keyboard.KEY_D: moveDir.x += 1.0f; break;
                    case Keyboard.KEY_SPACE: moveDir.y -= 1.0f; break;
                    case Keyboard.KEY_C: moveDir.y += 1.0f; break;
                    case Keyboard.KEY_F1: cam.changeProjection(); break;
                    case Keyboard.KEY_LEFT:
                        if(Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                            ingameTimePerSecond = 0.0f;
                        } else {
                            ingameTimePerSecond = Math.max(1.0f / 64.0f, 0.5f * ingameTimePerSecond);
                        }
                        break;
                    case Keyboard.KEY_RIGHT:
                        if(Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                            ingameTimePerSecond = 1.0f;
                        } else {
                            ingameTimePerSecond = Math.min(64.0f, 2.0f * ingameTimePerSecond);
                        }
                        break;
                    case Keyboard.KEY_F2: glPolygonMode(GL_FRONT_AND_BACK, (wireframe ^= true) ? GL_FILL : GL_LINE); break;
                    case Keyboard.KEY_F3: if(culling ^= true) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE); break;
                    case Keyboard.KEY_NUMPAD8: if (exposure <  19) exposure += 1.0f; ; break;
                    case Keyboard.KEY_NUMPAD2:
                    	if (exposure > 1.0)
                    		exposure -= 1.0f;
                    	else if (exposure <= 1.0 && exposure > 0)
                    		exposure -= 0.1f ; break;
                }
            }
        }
        
        cam.move(moveSpeed * moveDir.z, moveSpeed * moveDir.x, moveSpeed * moveDir.y);
        
        while(Mouse.next()) {
            if(Mouse.getEventButton() == 0) {
                Mouse.setGrabbed(Mouse.getEventButtonState());
            }
            if(Mouse.isGrabbed()) {
                cam.rotate(-camSpeed*Mouse.getEventDX(), -camSpeed*Mouse.getEventDY());
            }
        }
        
        //if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) bContinue = false;
    }
    
    /**
     * Aktualisiert Model Matrizen der Erde und des Mondes.
     * @param millis Millisekunden, die seit dem letzten Aufruf vergangen sind.
     */
    private static void animate(long millis) {

    }
}
