/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import static opengl.GL.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import opengl.OpenCL;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import util.*;

/**
 *
 * @author nico3000
 */
public class TerrainMain {
	//Test2 Commit
    // current configurations
    private static boolean bContinue = true;
    private static boolean culling = true;
    private static boolean wireframe = true;

    //Lights
    private static final Vector3f lightPosition1 = new Vector3f(0.0f, 1.0f, 2.0f);
    private static final Vector3f lightPosition2 = new Vector3f(0.0f, 0.1f, -4.0f);
    private static boolean holdLight =false;
    
    // textures
    private static Texture normalQuaderTexture;
    private static Texture quaderTexture;
    private static Texture diffuseQuaderTexture;
    private static Texture specularQuaderTexture;
    private static Texture bumpQuaderTexture;
    
    // geometries
    private static Geometry quaderGeo = GeometryFactory.createQuad();
    private static Geometry sphere1 = GeometryFactory.createSphere(0.2f, 10, 10);
    
    // shader programs
    private static ShaderProgram normalMappingSP;
    private static ShaderProgram BlurSP ;
    private static ShaderProgram GodRaysSP;
    private static ShaderProgram LightningSP;
    
    //private static float specCoeff = 0.5f;
    
    // control
    private static final Vector3f moveDir = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final Camera cam = new Camera(); 
    
    // animation params
    private static float ingameTime = 0;
    private static float ingameTimePerSecond = 1.0f;
    
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
            
            //textures
            normalQuaderTexture   = Texture.generateTexture("./stone_wall_normal_map.jpg",0 );
            quaderTexture         = Texture.generateTexture("./stone_wall.jpg",1 );
            diffuseQuaderTexture  = Texture.generateTexture("./stone_wall.jpg",2 );
            specularQuaderTexture = Texture.generateTexture("./stone_wall_specular.jpg",3 );
            bumpQuaderTexture     = Texture.generateTexture("./stone_wall_bump.jpg",4 );
           

            
            //blurPosteffect
            BlurSP = new ShaderProgram("./shader/ScreenQuad_VS.glsl","./shader/Blur_FS.glsl");
            
            //godRaysPosteffect
			GodRaysSP = new ShaderProgram("./shader/ScreenQuad_VS.glsl", "./shader/GodRayFS.glsl");            
            
            //Lightning
            LightningSP = new ShaderProgram("./shader/ScreenQuad_VS.glsl", "./shader/Normal_FS.glsl");   
           
            
            render();
            OpenCL.destroy();
            destroy();
        } catch (LWJGLException ex) {
            Logger.getLogger(TerrainMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void render() throws LWJGLException {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // background color: dark red
        
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
        
        Geometry geo = GeometryFactory.createScreenQuad();
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
        	Matrix4f modelMatrix = new Matrix4f();
        	Matrix4f modelIT = Util.transposeInverse(modelMatrix, null);
        	fboSP.setUniform("model", 	 modelMatrix);
        	fboSP.setUniform("modelIT",  modelIT);
        	fboSP.setUniform("viewProj", Util.mul(null, cam.getProjection(), cam.getView()));
            fboSP.setUniform("camPos",   cam.getCamPos());
            fboSP.setUniform("normalTexture", normalQuaderTexture);
            fboSP.setUniform("specularTexture", specularQuaderTexture);
            fboSP.setUniform("textureImage", quaderTexture);
            
            shader.bind();
            shader.clear();

            testCube.draw();

        	shader.finish();

            shader.DrawTexture(shader.getSkyTexture());
            

            
            // TODO: postfx
            
			//  geo.draw();
//			LightningSP.use();
//            LightningSP.setUniform("normalTexture",  shader.getNormalTexture());
//            LightningSP.setUniform("diffuseTexture",  shader.getDiffuseTexture());
//            LightningSP.setUniform("specularTexture", shader.getSpecTexture());
//            LightningSP.setUniform("bumpTexture", bumpQuaderTexture);
//            LightningSP.setUniform("lightPosition1", lightPosition1);
            
            
//			GodRaysSP.use();
//			GodRaysSP.setUniform("worldTexture", shader.getSkyTexture());
//			GodRaysSP.setUniform("diffuseTexture", shader.getDiffuseTexture());
//			GodRaysSP.setUniform("model", 	 modelMatrix);
//        	GodRaysSP.setUniform("viewProj", Util.mul(null, cam.getProjection(), cam.getView()));
//			GodRaysSP.setUniform("lightPosition", lightPosition1);
//			
//			BlurSP.use();
//            BlurSP.setUniform("worldTexture", shader.getWorldTexture());
//            BlurSP.setUniform("deltaBlur", 0.001f);
            geo.draw();
            
            // present screen
            Display.update();
            Display.sync(60);
        }
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
                    case Keyboard.KEY_P: 
                    	if (holdLight==false)
                    		{holdLight = true;}
                    	else
                    		{holdLight = false;}
                    	break;
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
    	if (!holdLight)
    	{
    		Util.transformCoord(Util.rotationY(10e-4f * (float)millis, null), lightPosition1, lightPosition1);
    	}
    }
    
    
   
    private static void matrix2uniform(Matrix4f matrix, int uniform){
    	matrix.store(Util.MAT_BUFFER);
    	Util.MAT_BUFFER.position(0);
    	glUniformMatrix4(uniform, false, Util.MAT_BUFFER);
    }
}
