package util;

import static opengl.GL.GL_ARRAY_BUFFER;
import static opengl.GL.GL_FLOAT;
import static opengl.GL.GL_STATIC_DRAW;
import static opengl.GL.GL_POINTS;
import static opengl.GL.glBindBuffer;
import static opengl.GL.glBindVertexArray;
import static opengl.GL.glBufferData;
import static opengl.GL.glEnableVertexAttribArray;
import static opengl.GL.glGenBuffers;
import static opengl.GL.glGenVertexArrays;
import static opengl.GL.glGetUniformLocation;
import static opengl.GL.glUniform1f;
import static opengl.GL.glUniform3f;
import static opengl.GL.glVertexAttribPointer;
import static opengl.OpenCL.CL_MEM_COPY_HOST_PTR;
import static opengl.OpenCL.CL_MEM_READ_WRITE;
import static opengl.OpenCL.clBuildProgram;
import static opengl.OpenCL.clCreateBuffer;
import static opengl.OpenCL.clCreateCommandQueue;
import static opengl.OpenCL.clCreateFromGLBuffer;
import static opengl.OpenCL.clCreateKernel;
import static opengl.OpenCL.clCreateProgramWithSource;
import static opengl.OpenCL.clEnqueueAcquireGLObjects;
import static opengl.OpenCL.clEnqueueNDRangeKernel;
import static opengl.OpenCL.clEnqueueReleaseGLObjects;
import static opengl.OpenCL.clFinish;
import static opengl.OpenCL.clReleaseCommandQueue;
import static opengl.OpenCL.clReleaseContext;
import static opengl.OpenCL.clReleaseKernel;
import static opengl.OpenCL.clReleaseMemObject;
import static opengl.OpenCL.clReleaseProgram;
import static opengl.OpenCL.create;

import java.nio.FloatBuffer;

import opengl.GL;
import opengl.OpenCL.Device_Type;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;


public class Particle {
    
    //opencl pointer
    private CLContext context;
    private CLProgram program;
    private CLDevice device;
    private CLCommandQueue queue;
    private CLKernel kernel0;
    private CLKernel kernel1;
	
	private int MAX_PARTICLES;
    
    private FloatBuffer particles, veloBuffer;
    
    //opencl buffer
    private CLMem old_pos, new_pos, old_velos, new_velos;
    
    //shader
    private int diffuseTexture;
    private int specularTexture;
    private ShaderProgram shaderProgram;
    private int viewProjLoc;
    private int eyeLoc;
    private int diffTexLoc;
    private int specTexLoc;
    private int kaLoc;
    private int kdLoc;
    private int ksLoc;
    private int esLoc;
    private int caLoc;
    
    private final Matrix4f viewProj = new Matrix4f();
    
    //kernel settings
    private boolean swap = true;
    private int localWorkSize = 32;
    private final PointerBuffer gwz = BufferUtils.createPointerBuffer(1);
    private final PointerBuffer lwz = BufferUtils.createPointerBuffer(1);
    
    // construct variables
    private int vaid = -1;                  // vertex array id
    private int vbid;                       // vertex buffer id
   
    
    public Particle(int amount, Device_Type device_type, Drawable drawable) throws LWJGLException {
    	MAX_PARTICLES = amount;
    	
        this.createCLContext(device_type, Util.getFileContents("./shader/particle_sim.cl"), drawable);
        this.createData();
        this.createBuffer();
        this.createKernels();
        this.createShaderProgram();
        this.gwz.put(0, this.MAX_PARTICLES);
        this.lwz.put(0, this.localWorkSize);   
    }
    
    public void addParticle(Vector4f position){
    	
    	particles.put(position.x);
    	particles.put(position.y);
    	particles.put(position.z);
    	particles.put(position.w);
    	
    }
    
    private void createCLContext(Device_Type type, String source, Drawable drawable) throws LWJGLException {
        
        int device_type;
        
        switch(type) {
        case CPU: device_type = CL10.CL_DEVICE_TYPE_CPU; break;
        case GPU: device_type = CL10.CL_DEVICE_TYPE_GPU; break;
        default: throw new IllegalArgumentException("Wrong device type!");
        }
        
        CLPlatform platform = null;
        
        for(CLPlatform plf : CLPlatform.getPlatforms()) {
            if(plf.getDevices(device_type) != null) {
                this.device = plf.getDevices(device_type).get(0);
                platform = plf;
                if(this.device != null) {
                    break;
                }
            }
        }
        
        this.context = create(platform, platform.getDevices(device_type), null, drawable);
        
        this.queue = clCreateCommandQueue(this.context, this.device, 0);
        
        this.program = clCreateProgramWithSource(this.context, source);

        clBuildProgram(this.program, this.device, "", null);
    }
    
    public ShaderProgram getShaderProgram() {
        return this.shaderProgram;
    }
    
    /**
     * Erzeugt das OpenGL Shaderprogram zum Rendern der Partikel
     */
    private void createShaderProgram() {
        
        shaderProgram = new ShaderProgram("./shader/Particle_VS.glsl", "./shader/Particle_FS.glsl");
        viewProjLoc = glGetUniformLocation(shaderProgram.getID(), "viewProj");
        diffTexLoc = glGetUniformLocation(shaderProgram.getID(), "diffuseTex");
        specTexLoc = glGetUniformLocation(shaderProgram.getID(), "specularTex");
        eyeLoc = glGetUniformLocation(shaderProgram.getID(), "eyePosition");
        kaLoc = glGetUniformLocation(shaderProgram.getID(), "k_a");
        kdLoc = glGetUniformLocation(shaderProgram.getID(), "k_dif");
        ksLoc = glGetUniformLocation(shaderProgram.getID(), "k_spec");
        esLoc = glGetUniformLocation(shaderProgram.getID(), "es");
        caLoc = glGetUniformLocation(shaderProgram.getID(), "c_a");
        shaderProgram.use();
        //GL.glUseProgram(shaderProgram);
        glUniform1f(kaLoc, 0.05f);
        glUniform1f(kdLoc, 0.6f);
        glUniform1f(ksLoc, 0.3f);
        glUniform1f(esLoc, 16.0f);
        glUniform3f(caLoc, 1.0f, 1.0f, 1.0f);
        
        //diffuseTexture = Util.generateTexture("asteroid.jpg");
        //specularTexture = Util.generateTexture("asteroid_spec.jpg");
    }
    
    public void createData(){
    	
    	particles = BufferUtils.createFloatBuffer(MAX_PARTICLES*4);
    	particles.position(0);
    	for(int i=0; i<MAX_PARTICLES; i++){
    		particles.put((float)(Math.sin(i) + Math.random()));
    		particles.put((float)(3+ Math.random()));
    		particles.put((float)(Math.cos(i) + Math.random()));
    		particles.put(1000);
    	}
    	particles.position(0);
    	
    	veloBuffer = BufferUtils.createFloatBuffer(MAX_PARTICLES*4);
    	veloBuffer.position(0);
    	for(int i=0; i<MAX_PARTICLES*4; i++){
    		veloBuffer.put(0.001f* (float)Math.random());
    	}
    	veloBuffer.position(0);
    }
    
    public void updateSimulation(long deltaTime) {
        
        clEnqueueAcquireGLObjects(this.queue, this.new_pos, null, null);
        clEnqueueAcquireGLObjects(this.queue, this.old_pos, null, null);
        
        if(this.swap) {
            
            this.kernel0.setArg(5, 1e-3f*deltaTime);
            clEnqueueNDRangeKernel(this.queue, kernel0, 1, null, gwz, lwz, null, null);
            
        } else {
            
            this.kernel1.setArg(5, 1e-3f*deltaTime);
            clEnqueueNDRangeKernel(this.queue, kernel1, 1, null, gwz, lwz, null, null);
           
        }
        clEnqueueReleaseGLObjects(this.queue, this.new_pos, null, null);
        clEnqueueReleaseGLObjects(this.queue, this.old_pos, null, null);
        clFinish(this.queue);
        this.swap = !this.swap;
    }
    
    /**
     * Rendert das Partikelsystem
     * @param cam
     */
    public void draw(Camera cam) {
        
    	shaderProgram.use();
        //GL.glUseProgram(shaderProgram);
        
        glUniform3f(this.eyeLoc, cam.getCamPos().x, cam.getCamPos().y, cam.getCamPos().z);       
        Matrix4f.mul(cam.getProjection(), cam.getView(), viewProj);  
        viewProj.store(Util.MAT_BUFFER);
        Util.MAT_BUFFER.position(0);
        GL.glUniformMatrix4(viewProjLoc, false, Util.MAT_BUFFER);
        
        glBindVertexArray(vaid);
        
        GL11.glPointSize(5);
        GL11.glDrawArrays(GL_POINTS, 0, MAX_PARTICLES); 
        
        clEnqueueAcquireGLObjects(this.queue, this.old_pos, null, null);
        
        this.kernel0.setArg(5, 1e-3f);
        clEnqueueNDRangeKernel(this.queue, kernel0, 1, null, gwz, lwz, null, null);
        
        clEnqueueReleaseGLObjects(this.queue, this.old_pos, null, null);
        clFinish(this.queue);
        
        /**
        GL.glActiveTexture(GL.GL_TEXTURE0 + 0);
        GL.glBindTexture(GL.GL_TEXTURE_2D, this.diffuseTexture);
        GL.glUniform1i(this.diffTexLoc, 0);
        
        GL.glActiveTexture(GL.GL_TEXTURE0 + 1);
        GL.glBindTexture(GL.GL_TEXTURE_2D, this.specularTexture);
        GL.glUniform1i(this.specTexLoc, 1);
        */
    }
    
    /**
     * Erstellt alle notwendigen OpenCL Buffer
     */
    private void createBuffer() {
        this.construct();
        
        this.old_velos = clCreateBuffer(this.context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, this.veloBuffer);
        this.new_velos = clCreateBuffer(this.context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, this.veloBuffer);
        //this.new_pos = clCreateFromGLBuffer(this.context, CL_MEM_READ_WRITE, vbid);
        this.old_pos = clCreateFromGLBuffer(this.context, CL_MEM_READ_WRITE, vbid);
        
        //this.particles = null;
        //this.veloBuffer = null;
    }
    
    /**
     * Erstellt zwei OpenCL Kernel
     */
    private void createKernels() {
    	
    	this.kernel0 = clCreateKernel(this.program, "particle_sim");
    	this.kernel0.setArg(0, this.old_pos);
    	this.kernel0.setArg(1, this.old_velos);
    	
    	/**
        this.kernel0 = clCreateKernel(this.program, "asteroid_sim");
        this.kernel0.setArg(0, this.old_pos);
        this.kernel0.setArg(1, this.new_pos);
        this.kernel0.setArg(2, this.old_velos);
        this.kernel0.setArg(3, this.new_velos);
        this.kernel0.setArg(4, this.MAX_PARTICLES);
        this.kernel0.setArg(5, 0f);
        
        this.kernel1 = clCreateKernel(this.program, "asteroid_sim");
        this.kernel1.setArg(0, this.new_pos);
        this.kernel1.setArg(1, this.old_pos);
        this.kernel1.setArg(2, this.new_velos);
        this.kernel1.setArg(3, this.old_velos);
        this.kernel1.setArg(4, this.MAX_PARTICLES);
        this.kernel1.setArg(5, 0f);
    	*/
    
    }
    
    /**
     * free memory
     */
    public void destroy() {
        clReleaseMemObject(this.new_pos);
        clReleaseMemObject(this.old_pos);
        clReleaseMemObject(this.new_velos);
        clReleaseMemObject(this.old_velos);
        clReleaseKernel(this.kernel0);
        clReleaseKernel(this.kernel1);
        clReleaseCommandQueue(this.queue);
        clReleaseProgram(this.program);
        clReleaseContext(this.context);
        particles.clear();
    }
    
    public FloatBuffer getBuffer(){
    	return particles;
    }
    
    public void setBuffer(FloatBuffer buffer){
    	this.particles = buffer;
    }
    
    public void draw() {
        
        if(vaid == -1) {
            construct();
        }
        glBindVertexArray(vaid);

        GL11.glDrawArrays(GL_POINTS, 0, MAX_PARTICLES); 

    }
    
    /**
     * Erzeugt aus den gesetzten Vertex- und Indexdaten ein Vertexarray Object,
     * das die zugoerige Topologie beinhaltet.
     */
    public void construct() {
    	
    	this.particles.position(0);
    	
        this.vaid = glGenVertexArrays();
        glBindVertexArray(this.vaid);
        
        this.vbid = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.vbid);
        glBufferData(GL_ARRAY_BUFFER, this.particles, GL_STATIC_DRAW);
        
        glEnableVertexAttribArray(ShaderProgram.ATTR_POS);

        glVertexAttribPointer(ShaderProgram.ATTR_POS, 4, GL_FLOAT, false, 16, 0);

        glBindVertexArray(0);
    }
	
}