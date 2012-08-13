package main;

import java.util.logging.Level;
import java.util.logging.Logger;
import static opengl.GL.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import util.Camera;
import util.Geometry;
import util.GeometryFactory;
import util.Util;

/**
 *
 * @author NMARNIOK
 */
public class MainTest {
    // shader programs
    private static int terrainProgram;
    
    // terrain
    private static util.Terrain terra;
    
    // uniform locations
    private static int terrainViewProjLoc;
    private static int terrainModelLoc;
    private static int terrainModelITLoc;
    private static int terrainParamLoc;
    
    // geometries
    private static Geometry terrainGeometry;
    
    // model matrices
    private static final Matrix4f terrainModelMatrix = new Matrix4f();    
    private static final Matrix4f terrainModelITMatrix = new Matrix4f();
    
    // configs
    private static boolean bContinue = true;
    private static boolean wireframe = false;
    private static boolean culling = true;
    private static float param = 0.0f;
    
    // controls
    private static final Camera cam = new Camera();
    private static final Vector3f moveDir = new Vector3f();
    private static final Matrix4f viewProjMatrix = new Matrix4f();
    
    
    
    public static void main(String argv[]) {
        try {
            init();
            
            terrainProgram = Util.createShaderProgram("shader/Terrain_VS.glsl", "shader/Terrain_FS.glsl");
            terrainViewProjLoc = glGetUniformLocation(terrainProgram, "viewProj");
            terrainModelLoc = glGetUniformLocation(terrainProgram, "model");
            terrainModelITLoc = glGetUniformLocation(terrainProgram, "modelIT");
            terrainParamLoc = glGetUniformLocation(terrainProgram, "param");
                   
                      	
            terra = new util.Terrain(0f, 512, 512, 4);
            terra.genTerrain(10);
            
//            TerrainView tv = new TerrainView(cam);
            // terrainGeometry = GeometryFactory.genTerrain(tv.getArray());
            
            terrainGeometry = GeometryFactory.genTerrain(terra.getTerra());
            
            glEnable(GL_DEPTH_TEST);
            glPointSize(2.0f);
            
            render();
            destroy();
        } catch (LWJGLException ex) {
            Logger.getLogger(MainTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void render() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        long now, millis;
        long last = System.currentTimeMillis();
        
        while(bContinue && !Display.isCloseRequested()) {
            now = System.currentTimeMillis();
            millis = now - last;
            last = now;
            handleInput(millis);
            param += 5e-3f * (float)millis*0.25f;
            
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            glUseProgram(terrainProgram);
            matrix2uniform(viewProjMatrix, terrainViewProjLoc);
            
            matrix2uniform(terrainModelMatrix, terrainModelLoc);
            matrix2uniform(terrainModelITMatrix, terrainModelITLoc);
            glUniform1f(terrainParamLoc, param);
            terrainGeometry.draw();
            
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
        
        if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) bContinue = false;
        
        Matrix4f.mul(cam.getProjection(), cam.getView(), viewProjMatrix);        
    }
    
    private static void matrix2uniform(Matrix4f mat, int location) {
        Util.MAT_BUFFER.position(0);
        mat.store(Util.MAT_BUFFER);
        Util.MAT_BUFFER.position(0);
        glUniformMatrix4(location, false, Util.MAT_BUFFER);
        Util.MAT_BUFFER.position(0);
    }
}
