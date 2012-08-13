package util;

import static opengl.GL.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import java.io.*;
import java.util.*;
import opengl.GL;

/**
 * Stellt Methoden zur Erzeugung von Geometrie bereit.
 * @author Sascha Kolodzey, Nico Marniok
 */
public class GeometryFactory {
    public static List<ModelPart> importFromBlender(String objFile, String mtlFile, String texturePath) {
        List<Material> materialList = getMaterialListFromMTL(mtlFile, texturePath);
        List<ModelPart> modelPartList = getModelPartListFromOBJ(objFile, materialList);

/*        Iterator<ModelPart> modelPartListIterator = modelPartList.listIterator();
        while(modelPartListIterator.hasNext()) {
            System.out.println(modelPartListIterator.next());
        }*/
        
        return modelPartList;
    }
    
    public static List<Material> getMaterialListFromMTL(String mtlPath, String texturePath) {
        System.out.println("Reading MTL-file and creating material objects...");
        List<Material> materialList = new LinkedList<Material>();
        
       //Open File
        try (BufferedReader objBufferedReader = new BufferedReader(new FileReader(mtlPath))) {
            //Loop through file line by line
            String line = "";
            
            String materialLibrary = "default";
            Material material = null;
            while((line = objBufferedReader.readLine()) != null) {
                //add previos used material object to list if one exists
                //generate a new material object
                //parse material name
                if(line.startsWith("newmtl ")) {
                    if(material!=null) {
                        material.loadTextures(texturePath);
                        materialList.add(material);
                    }
                    material = new Material();
                    material.materialName = line.split(" {1,}")[1];
                } else if (line.startsWith("Ns ")) {
                    material.specularEx = Float.valueOf(line.split(" {1,}")[1]);
                } else if (line.startsWith("Ka ")) {
                    material.ambientRef = new Vector3f(Float.valueOf(line.split(" {1,}")[1]),
                                                   Float.valueOf(line.split(" {1,}")[2]),
                                                   Float.valueOf(line.split(" {1,}")[3]));
                } else if (line.startsWith("Kd ")) {
                    material.diffuseRef = new Vector3f(Float.valueOf(line.split(" {1,}")[1]),
                                                   Float.valueOf(line.split(" {1,}")[2]),
                                                   Float.valueOf(line.split(" {1,}")[3]));                    
                } else if (line.startsWith("Ks ")) {
                    material.specularRef = new Vector3f(Float.valueOf(line.split(" {1,}")[1]),
                                                   Float.valueOf(line.split(" {1,}")[2]),
                                                   Float.valueOf(line.split(" {1,}")[3])); 
                } else if (line.startsWith("Ni ")) {
                    material.opticalDens = Float.valueOf(line.split(" {1,}")[1]);
                } else if (line.startsWith("d ")) {
                    material.dissolveFact = Float.valueOf(line.split(" {1,}")[1]);
                } else if (line.startsWith("illum ")) {
                    material.illuminationModel = Integer.parseInt(line.split(" {1,}")[1]);
                } else if (line.startsWith("map_Kd ")) {
                    String[] filePath = line.split(" {1,}");
                    if(filePath.length>=2 && filePath[1].contains("\\")) {
                        filePath = line.split("\\\\");
                        material.diffuseRefColorMap = filePath[filePath.length-1];
                    } else if(filePath.length>=2) {
                        material.diffuseRefColorMap = filePath[1];
                    }
                } else if (line.startsWith("map_d ")) {
                    String[] filePath = line.split(" {1,}");
                    if(filePath.length>=2 && filePath[1].contains("\\")) {
                        filePath = line.split("\\\\");
                        material.dissolveFactColorMap = filePath[filePath.length-1];
                    } else if(filePath.length>=2) {
                        material.dissolveFactColorMap = filePath[1];
                    }
                } else if (line.startsWith("map_Ks ")) {
                    String[] filePath = line.split(" {1,}");
                    if(filePath.length>=2 && filePath[1].contains("\\")) {
                        filePath = line.split("\\\\");
                        material.specularRefColorMap = filePath[filePath.length-1];
                    } else if(filePath.length>=2) {
                        material.specularRefColorMap = filePath[1];
                    }
                }
            } //end of while loop
            //add the last material object to the list (its not being added in the while loop!)
            if(material!=null) {
                material.loadTextures(texturePath);
                materialList.add(material);
            }
        } catch (IOException e) {
            System.out.println("Problem beim Lesen der OBJ-Datei! Das Programm wird beendet.");
            System.exit(0);
        }     
        
        return materialList;
    }
    
    /**
     * Creates an modelPart lists. Each modelPart in the list contains vertex and
     * material information
     * @param objFile ath to OBJ file
     * @return creates modelPart list
     */
    public static List<ModelPart> getModelPartListFromOBJ(String objPath, List<Material> materialList) {
        //Create new ModelPart
        List<ModelPart> modelPartList = new LinkedList<ModelPart>();

        //Create lists to temporary save all vertices, texture coordinates and
        //normals from the obj file. Once filled, they sould never be changed!
        List<Vector3f> vertexList = new LinkedList<Vector3f>();
        List<Vector3f> vertexTextureList = new LinkedList<Vector3f>();
        List<Vector3f> vertexNormalList = new LinkedList<Vector3f>();
        
        //Read the obj file and parse all vertices, texture coordinates
        //and normals from the obj file. Save them in the lists created above.
        try (BufferedReader objBufferedReader = new BufferedReader(new FileReader(objPath))) {
            //Read obj file line by line
            String line = "";
            int i=1;
            System.out.println("Read OBJ-File line by line and parse all "
                    + "vertices, texture coordinates and normals...");

            while((line = objBufferedReader.readLine()) != null) {
                //Parse verticex coordinates (v)
                if(line.startsWith("v ")) {
                    //Spalt Zeile in Elemente anhand von Leerzeichen auf
                    String[] lineElements = line.split(" {1,}");
                    try{
                        if(!lineElements[1].isEmpty() && !lineElements[2].isEmpty() && !lineElements[3].isEmpty()) {
                            Vector3f vertexCoordinates = new Vector3f(Float.parseFloat(lineElements[1]), Float.parseFloat(lineElements[2]), Float.parseFloat(lineElements[3]));
                            vertexList.add(vertexCoordinates);
                        }
                    } catch(ArrayIndexOutOfBoundsException e) {
                        System.out.println("Read error while reading v at line: "+i);
                        System.out.println(e);
                    }
                }
                
                //Parse vertex texture coordinates (vt)
                if(line.startsWith("vt ")) {
                    //Spalt Zeile in Elemente anhand von Leerzeichen auf
                    String[] lineElements = line.split(" {1,}");
                    try{
                        if(lineElements.length>=3) {
                            //Switch for vt lines with 2 or 3 elements (if 2 elements, third element is 0 by default)
                            float thirdElement;
                            if(lineElements.length==3)
                                thirdElement = 0;
                            else
                                thirdElement = Float.parseFloat(lineElements[3]);
                            
                            Vector3f vertexTextureCoordinates = new Vector3f(Float.parseFloat(lineElements[1]), Float.parseFloat(lineElements[2]), thirdElement);
                            vertexTextureList.add(vertexTextureCoordinates);
                        }
                    } catch(ArrayIndexOutOfBoundsException e) {
                        System.out.println("Read error while reading vt at line: "+i);
                        System.out.println(e);
                    }
                }

                //Parse vertex normals (vn)
                if(line.startsWith("vn ")) {
                    //Spalt Zeile in Elemente anhand von Leerzeichen auf
                    String[] lineElements = line.split(" {1,}");
                    if(!lineElements[1].isEmpty() && !lineElements[2].isEmpty() && !lineElements[3].isEmpty()) {
                        Vector3f vertexNormals = new Vector3f(Float.parseFloat(lineElements[1]), Float.parseFloat(lineElements[2]), Float.parseFloat(lineElements[3]));
                        vertexNormalList.add(vertexNormals);
                    }
                }
                i++;
            } //end of while loop
        } catch (IOException e) {
            System.out.println("Problem beim Lesen der OBJ-Datei! Das Programm wird beendet.");
            System.exit(0);
        }
        
        //List to temorary save the faces of an modelPart.
        //The list is deleted and newly created for each modelPart
        List<Face> faceList = null;
        
        //Oject to add to the modelPart lists. The modelPart is added to the modelPart
        //list after parsing of an modelPart ist done. Afer parsing of an modelPart
        //is done, this reference is overwritten by a new modelPart
        PreStageModelPart preStageModelPart = null;
        
        //Read the obj file and parse all faces
        try (BufferedReader objBufferedReader = new BufferedReader(new FileReader(objPath))) {
            //Read obj file line by line
            String line = "";
            int i=1;
            System.out.println("Read OBJ-File line by line and parse all "
                    + "faces to modelParts...");

            while((line = objBufferedReader.readLine()) != null) {
                if(line.startsWith("usemtl ")) { //New modelPart detected. Create a new facelist and a new ModelPart
                    if(preStageModelPart!=null) {
                        preStageModelPart.createBuffers(faceList, vertexList, vertexTextureList, vertexNormalList);
                        modelPartList.add(preStageModelPart.createModelPart(materialList));
                    }
                    System.out.println("Create preStageModelPart...");
                    preStageModelPart = new PreStageModelPart();
                    faceList = new LinkedList<Face>();
                    
                    preStageModelPart.materialName = line.split(" {1,}")[1];
                } else if(line.startsWith("s ")) { //Parse smoothing group
                    if(line.split(" {1,}")[1].equals("off"))
                        preStageModelPart.smoothingGroup = 0;
                    else
                        preStageModelPart.smoothingGroup = Integer.parseInt(line.split(" {1,}")[1]);
                }
                
                //Parse faces into facelist
                if(line.startsWith("f ")) {
                    //Spalt Zeile in Elemente anhand von Leerzeichen auf
                    String[] lineElements = line.split(" {1,}");
                    String[][] faceGroup = new String[4][];
                    //split lineElements into their index components 

                    try {
                        faceGroup[0] = lineElements[1].split("/");
                        faceGroup[1] = lineElements[2].split("/");
                        //switch for facelines with only 2 face groups
                        if(lineElements.length>3)
                            faceGroup[2] = lineElements[3].split("/");
                        else
                            faceGroup[2] = faceGroup[0];
                    } catch(ArrayIndexOutOfBoundsException e) {
                        System.out.println("Read error while splitting line elements (f) at line: "+i);
                        System.out.println(e);
                    }
                        
                    Vector3f faceVertexIndizies = new Vector3f();
                    Vector3f faceTextureIndizies = new Vector3f();
                    Vector3f faceNormalIndizies = new Vector3f();

                    //Switch for index groups with 1 (vertex only), 2 (vertex and texture) or 3 (vertex, texture and normal) elements
                    if(faceGroup[0].length>=1)
                        faceVertexIndizies = new Vector3f(Float.valueOf(faceGroup[0][0])-1, Float.valueOf(faceGroup[1][0])-1, Float.valueOf(faceGroup[2][0])-1);
                    if(faceGroup[0].length>=2 && !faceGroup[0][1].isEmpty())
                        faceTextureIndizies = new Vector3f(Float.valueOf(faceGroup[0][1])-1, Float.valueOf(faceGroup[1][1])-1, Float.valueOf(faceGroup[2][1])-1);
                    if(faceGroup[0].length>=3 && !faceGroup[0][2].isEmpty())
                        faceNormalIndizies = new Vector3f(Float.valueOf(faceGroup[0][2])-1, Float.valueOf(faceGroup[1][2])-1, Float.valueOf(faceGroup[2][2])-1);

                    //Add the created faces to the face list
                    try {
                        faceList.add(new Face(faceVertexIndizies, faceTextureIndizies, faceNormalIndizies));
                    } catch(ArrayIndexOutOfBoundsException e) {
                        System.out.println("Read error while creating Face one at line: "+i);
                        System.out.println(e);
                    }
                        
                    //if face consists of 2 triangles (4 index groups), then create a second triangle
                    if(lineElements.length>4) {
                        faceGroup[3] = lineElements[4].split("/");

                        faceVertexIndizies = new Vector3f();
                        faceTextureIndizies = new Vector3f();
                        faceNormalIndizies = new Vector3f();

                        //Switch for index groups with 1 (vertex only), 2 (vertex and texture) or 3 (vertex, texture and normal) elements
                        if(faceGroup[0].length>=1)
                            faceVertexIndizies = new Vector3f(Float.valueOf(faceGroup[0][0])-1, Float.valueOf(faceGroup[2][0])-1, Float.valueOf(faceGroup[3][0])-1);
                        if(faceGroup[0].length>=2 && !faceGroup[0][1].isEmpty())
                            faceTextureIndizies = new Vector3f(Float.valueOf(faceGroup[0][1])-1, Float.valueOf(faceGroup[2][1])-1, Float.valueOf(faceGroup[3][1])-1);
                        if(faceGroup[0].length>=3 && !faceGroup[0][2].isEmpty())
                            faceNormalIndizies = new Vector3f(Float.valueOf(faceGroup[0][2])-1, Float.valueOf(faceGroup[2][2])-1, Float.valueOf(faceGroup[3][2])-1);

                        try {
                            faceList.add(new Face(faceVertexIndizies, faceTextureIndizies, faceNormalIndizies));
                        } catch(ArrayIndexOutOfBoundsException e) {
                            System.out.println("Read error while creating Face two at line: "+i);
                            System.out.println(e);
                        }
                    }
                }
                i++;
            } //end of while loop
            //Add last modelPart to modelPart list
            if(preStageModelPart!=null) {
                preStageModelPart.createBuffers(faceList, vertexList, vertexTextureList, vertexNormalList);
                modelPartList.add(preStageModelPart.createModelPart(materialList));
            }
        } catch (IOException e) {
            System.out.println("Problem beim Lesen der OBJ-Datei! Das Programm wird beendet.");
            System.exit(0);
        }
        
        return modelPartList;
    }
    
    /**
     * Erzeugt ein Vierexk in der xy-Ebene. (4 Indizes)
     * @return VertexArrayObject ID
     */
    public static Geometry createScreenQuad() {
        int vaid = glGenVertexArrays();
        glBindVertexArray(vaid);
        
        // vertexbuffer
        FloatBuffer vertexData = BufferUtils.createFloatBuffer(16);
        vertexData.put(new float[] {
            -1.0f, -1.0f, -1.0f*0.0f, 1.0f,
            +1.0f, -1.0f, -1.0f*1.0f, 1.0f,
            -1.0f, +1.0f, -1.0f*0.0f, 0.0f,
            +1.0f, +1.0f, -1.0f*1.0f, 0.0f,
        });
        vertexData.position(0);
        
        // indexbuffer
        IntBuffer indexData = BufferUtils.createIntBuffer(4);
        indexData.put(new int[] { 0, 1, 2, 3, });
        indexData.position(0);
        
        Geometry geo = new Geometry();
        geo.setIndices(indexData, GL_TRIANGLE_STRIP);
        geo.setVertices(vertexData);
        geo.addVertexAttribute(ShaderProgram.ATTR_POS, 2, 0);
        geo.addVertexAttribute(ShaderProgram.ATTR_TEX, 2, 8);
        return geo;
    }
    
    /**
     * Erzeugt ein Vierexk in der xy-Ebene. (4 Indizes)
     * @return VertexArrayObject ID
     */
    public static Geometry createTriangle() {        
        int vaid = glGenVertexArrays();
        glBindVertexArray(vaid);        
        
        // vertexbuffer
        FloatBuffer vertexData = BufferUtils.createFloatBuffer((3+4)*3); // world coords, color
        vertexData.put(new float[] {
            -1.0f, -1.0f, +1.0f,  1.0f, 1.0f, 0.4f, 1.0f,
            +1.0f, -1.0f, -1.0f,  0.4f, 1.0f, 0.4f, 1.0f,
            +1.0f, +1.0f, +1.0f,  1.0f, 1.0f, 0.4f, 1.0f,
        });
        vertexData.position(0);
        
        // indexbuffer
        IntBuffer indexData = BufferUtils.createIntBuffer(4);
        indexData.put(new int[] { 0, 1, 2, });
        indexData.position(0);
        
        Geometry geo = new Geometry();
        geo.setIndices(indexData, GL_TRIANGLE_STRIP);
        geo.setVertices(vertexData);
        geo.addVertexAttribute(ShaderProgram.ATTR_POS, 3, 0);
        geo.addVertexAttribute(ShaderProgram.ATTR_COLOR, 4, 12);
        return geo;
    }
    
    public static Geometry createCube() {
    	float[] cubeVertices  = {
       		 0.5f,  0.5f,  0.5f,	1.0f, 1.0f, 1.0f, 1.0f, // front top right
       		-0.5f,  0.5f,  0.5f, 	0.0f, 1.0f, 1.0f, 1.0f, // front top left
       		 0.5f, -0.5f,  0.5f,	1.0f, 0.0f, 1.0f, 1.0f, // front bottom right
       		-0.5f, -0.5f,  0.5f, 	0.0f, 0.0f, 1.0f, 1.0f, // front bottom left
       		
       		 0.5f,  0.5f, -0.5f,	1.0f, 1.0f, 0.0f, 1.0f, // back top right
       		-0.5f,  0.5f, -0.5f,	0.0f, 1.0f, 0.0f, 1.0f, // back top left
       		 0.5f, -0.5f, -0.5f,	1.0f, 0.0f, 0.0f, 1.0f, // back bottom right
       		-0.5f, -0.5f, -0.5f,	0.0f, 0.0f, 0.0f, 1.0f  // back bottom left		
       };
       
       int[] cubeIndices = {
    		5, 4, 7, 6, 2, 4, 0, 
    		5, 1, 7, 3, 2, 1, 0
       };
       
       FloatBuffer cubeVertBuf = BufferUtils.createFloatBuffer(cubeVertices.length);
       IntBuffer cubeIndBuf = BufferUtils.createIntBuffer(cubeIndices.length);
       cubeVertBuf.put(cubeVertices);
       cubeVertBuf.flip();
       cubeIndBuf.put(cubeIndices);
       cubeIndBuf.flip();
       
       Geometry geo = new Geometry();
       geo.setIndices(cubeIndBuf, GL_TRIANGLE_STRIP);
       geo.setVertices(cubeVertBuf);
       geo.addVertexAttribute(ShaderProgram.ATTR_POS, 3, 0);
       geo.addVertexAttribute(ShaderProgram.ATTR_COLOR, 4, 12);
       return geo;
    }
    
        public static Geometry genTerrain(float[][][] terra) {
        	
        	int vertexSize = 7;
        	int maxX = terra.length;
        	int maxZ = terra[0].length; 	
        	
           	FloatBuffer vertices = BufferUtils.createFloatBuffer(vertexSize*maxX*maxZ);

        	
        	
           	// Gen Vbuffer
           	for(int z=0; z < maxZ; ++z) {
                for(int x=0; x < maxX; ++x) {
                	vertices.put(1e-2f * (float)x);
                	vertices.put(terra[x][z][0]);
                	vertices.put(1e-2f * (float)z);
                	
                	vertices.put(0);	// norm.x
                	vertices.put(0);	// norm.y
                	vertices.put(0);	// norm.z
                	vertices.put(terra[x][z][4]);

                }                	    
           	}
           	
           	
           	// Gen IndexBuffer
            IntBuffer indices = BufferUtils.createIntBuffer(3 * 2 * (maxX - 1) * (maxZ - 1));
            for(int z=0; z < maxZ - 1; ++z) {
                for(int x=0; x < maxX - 1; ++x) {
                    indices.put(z * maxX + x);
                    indices.put((z + 1) * maxX + x + 1);
                    
                    indices.put(z * maxX + x + 1);
                    
                    indices.put(z * maxX + x);
                    indices.put((z + 1) * maxX + x);
                    indices.put((z + 1) * maxX + x + 1);
                }
            }
            
            // Gen norms
            indices.position(0);
            for(int i=0; i < indices.capacity();) {
                int index0 = indices.get(i++);
                int index1 = indices.get(i++);
                int index2 = indices.get(i++);
                
                vertices.position(vertexSize * index0);
                Vector3f p0 = new Vector3f();
                p0.load(vertices);
                vertices.position(vertexSize * index1);
                Vector3f p1 = new Vector3f();
                p1.load(vertices);
                vertices.position(vertexSize * index2);
                Vector3f p2 = new Vector3f();
                p2.load(vertices);
                
                Vector3f a = Vector3f.sub(p1, p0, null);
                Vector3f b = Vector3f.sub(p2, p0, null);
                Vector3f normal = Vector3f.cross(a, b, null);
                normal.normalise();
                
                vertices.position(vertexSize * index0 + 3);
                normal.store(vertices);
            }
           	
            vertices.position(0);
            indices.position(0);
            Geometry geo = new Geometry();
            
            geo.setIndices(indices, GL_TRIANGLES);
            geo.setVertices(vertices);
            geo.addVertexAttribute(ShaderProgram.ATTR_POS, 3, 0);
            geo.addVertexAttribute(ShaderProgram.ATTR_NORMAL, 3, 12);
            geo.addVertexAttribute(ShaderProgram.ATTR_MATERIAL, 1, 24);
            return geo;	
        }
}