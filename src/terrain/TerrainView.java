package terrain;

import util.Camera;

/** 
 * Klasse, die die aktuell benutzten Block Objekte im RAM verwaltet
 * 
 * @author daniel, lukas, mareike
 */
public class TerrainView
{
	// private values
	private static Block[][] myBl;
	private static Camera cam;
	private static int[] middle;
	private static Block dummy;
	private static Terrain terra;
	private static boolean initialised;

	/** 
	 * static constructor
	 */
	static
	{
		myBl = new Block[9][9];
		dummy = BlockUtil.readBlockData(BlockUtil.writeBlockData(new Block(-1, -1)));
		initialised = false;
	}

	/**
	 * Initlialisierung der TerrainView
	 * 
	 * @param terra		Terrain
	 * @param cam		Kamera
	 */
	public static void init(Terrain terra, Camera cam)
	{
		TerrainView.terra = terra;
		TerrainView.cam = cam;
		initialised = true;
		myBl[4][4] = terra.getBlock((int)cam.getCamPos().x / 512, (int)cam.getCamPos().z / 512);
		middle = myBl[4][4].getID();

		int idI = middle[0];
		int idJ = middle[1];

		int dim = terra.getSize() / 512;
		
		for (int i = 0; i < 9; i++)
		{
			for (int j = 0; j < 9; j++)
			{
				if (!(i == 4 && j == 4))
				{
					if (idI + i - 4 >= 0 && idJ + j - 4 >= 0 && idI + i - 4 < dim && idJ + j - 4 < dim)
					{
						myBl[i][j] = terra.getBlock(idI+i-4,idJ+j-4);
					}
					else
					{
						myBl[i][j] = dummy;
					}
				}
			}
		}
	}


	/**
	 * Methode zum updaten der Block Objekte
	 */
	public static void updateTerrainView()
	{
		if (!initialised)
		{
			throw new IllegalStateException("Klasse wurde nicht initialisiert!");
		}
			
		// hier muss der erste Block mit Fehlerbehandlung gesetzt werden, falls
		// Camera ausserhalb des Terrains
		// einschraenkung der Camera oder spezielle Fehlerbehandlung hier
		
		int diffX = ((int) cam.getCamPos().x / 512) - middle[0];
		int diffY = ((int) cam.getCamPos().z / 512) - middle[1];

		if (!(Math.abs(diffX) <= 1 && Math.abs(diffY) <= 1))
		{
			int dim = terra.getSize() / 512;
			myBl[4][4] = terra.getBlock((int)cam.getCamPos().x / 512, (int)cam.getCamPos().z / 512);
			middle = myBl[4][4].getID();

			for (int i = 0; i < 9; i++)
			{
				for (int j = 0; j < 9; j++)
				{
					if (!(i == 4 && j == 4))
					{
						if (i + diffX < 0 || i + diffX > 8 || j + diffY < 0 || j + diffY > 8)
						{
							if ((middle[0] - 4 + i) >= 0 && (middle[1] - 4 + j) >= 0 && (middle[0] - 4 + i) < dim && (middle[1] - 4 + j) < dim)
							{
								myBl[i][j] = terra.getBlock(myBl[i][j].getID()[0] + diffX,myBl[i][j].getID()[1] + diffY);
							}
							else
							{
								myBl[i][j] = dummy;
							}
						} else
						{
							myBl[i][j] = myBl[i + diffX][j + diffY];
						}
					}
				}
			}
		}
	}

	/**
	 * Getter HoehenMap
	 * 
	 * @return float Array[][]		HeightMap 
	 */
	public static float[][] getHeightMap()
	{
		if (!initialised)
		{
			throw new IllegalStateException("Klasse wurde nicht initialisiert!");
		}		
		float[][] heightMap = new float[9 * 512][9 * 512];
		
		for (int x = 0; x < heightMap.length; x++)
		{
			for (int z = 0; z < heightMap[0].length; z++)
			{
				int bx = (int) x / 512;
				int bz = (int) z / 512;

				if (myBl[bx][bz] == null)
				{
					System.out.println("error");
				}
				heightMap[x][z] = myBl[bx][bz].getInfo(x % 512, z % 512, 0) * 20f;
			}
		}
		return heightMap;
	}

	/** 
	 * method gives the camPosX as related to the float[][][]
	 * 
	 * @return int 
	 */
	public static int arrayCamPosX()
	{
		if (!initialised)
		{
			throw new IllegalStateException("Klasse wurde nicht initialisiert!");			
		}	
		return (int) ((cam.getCamPos().x % 512) + 512);
	}

	/** 
	 * method gives the camPosZ as related to the float[][][]
	 * 
	 * @return 
	 */
	public static int arrayCamPosZ()
	{
		if (!initialised)
		{
			throw new IllegalStateException("Klasse wurde nicht initialisiert!");
		}
		return (int) ((cam.getCamPos().z % 512) + 512);
	}
}
