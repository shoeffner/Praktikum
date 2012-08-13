package util;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/** @author nico3000 */
public final class Camera
{
	private float phi = 0, theta = 0;
	private final Vector3f viewDir = new Vector3f(0, 0, 1);
	private final Vector3f upDir = new Vector3f(0, 1, 0);
	private final Vector3f sideDir = new Vector3f(1, 0, 0);

	private final Vector3f camPos = new Vector3f(100, 1, 300);
	private final Matrix4f view = new Matrix4f();
	private final Matrix4f projection = new Matrix4f();
	private boolean perspective = true;
	private float altX;
	private float altY;
	private float altZ;
	private int size;

	/** Default Constructor. */
	public Camera()
	{
		this.updateView();
		this.updateProjection();
	}

	/** Rotiert die Kamera horizontal und vertikal.
	 * 
	 * @param dPhi horizontale Rotation
	 * @param dTheta vertikale Rotation */
	public void rotate(float dPhi, float dTheta)
	{
		phi += dPhi;
		theta = Util.clamp(theta + dTheta, -Util.PI_DIV2, +Util.PI_DIV2);

		Matrix4f rotX = Util.rotationX(theta, null);
		Matrix4f rotY = Util.rotationY(phi, null);
		Matrix4f rot = Util.mul(null, rotY, rotX);
		sideDir.set(rot.m00, rot.m01, rot.m02);
		upDir.set(rot.m10, rot.m11, rot.m12);
		viewDir.set(rot.m20, rot.m21, rot.m22);
	}

	/** Bewegt die Kamera.
	 * 
	 * @param fb Bewegung in Sichtrichtung
	 * @param lr Bewegung in seitliche Richtung
	 * @param ud Bewegung nach oben/unten */
	public void move(float fb, float lr, float ud)
	{
		altX = fb * viewDir.x + lr * sideDir.x;
		altY = fb * viewDir.y + lr * sideDir.y + ud;
		altZ = fb * viewDir.z + lr * sideDir.z;
		if(camPos.x += altX <= 0 || camPos.x += altX >= (float)Math.sqrt(size))) camPos.x += altX;
		camPos.y += altY;
		camPos.z += altZ;
	}

	public Vector3f getAlt()
	{
		return new Vector3f(altX, altY, altZ);
	}

	/** Aktualisiert die Viewmatrix. */
	public void updateView()
	{
		Vector3f lookAt = Vector3f.add(camPos, viewDir, null);
		Util.lookAtRH(camPos, lookAt, upDir, view);
	}

	/** Aktualisiert die Projektionsmatrix. */
	public void updateProjection()
	{
		if (perspective)
		{
			Util.frustum(-1e-2f, 1e-2f, -1e-2f, 1e-2f, 1e-2f, 1e+5f, projection);
		} else
		{
			Util.ortho(-1.0f, 1.0f, -1.0f, 1.0f, 1e-2f, 1e+5f, projection);
		}
	}

	/** Aendert die Projektion (perspektivisch vs. parellel). */
	public void changeProjection()
	{
		perspective ^= true;
	}

	/** Getter fuer die Projektionsmatrix.
	 * 
	 * @return Projektionsmatrix */
	public Matrix4f getProjection()
	{
		this.updateProjection();
		return projection;
	}

	/** Getter fuer die Viewmatrix.
	 * 
	 * @return Viewmatrix */
	public Matrix4f getView()
	{
		this.updateView();
		return view;
	}

	public Vector3f getCamPos()
	{
		return camPos;
	}

	public Vector3f getViewDir()
	{
		return viewDir;
	}
	
	public void setSize(int size) {
		this.size = size;
	}

}