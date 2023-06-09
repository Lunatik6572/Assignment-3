package tage.physics.JBullet;

import javax.vecmath.Vector3f;
import java.util.HashMap;

import tage.physics.PhysicsObject;

import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.VehicleTuning;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import org.joml.Matrix4f;

public abstract class JBulletPhysicsObject implements PhysicsObject {

	public static HashMap<RigidBody, JBulletPhysicsObject> lookUpObject = new HashMap<>();
	public static JBulletPhysicsObject getJBulletPhysicsObject(RigidBody r) { return lookUpObject.get(r); }

    private int uid;
    private float mass;
    protected Transform transform;
    private CollisionShape shape;
    private RigidBody body;
    private boolean isVehicle;
    private boolean isDynamic;
    private Vector3f localInertia;
    private DefaultMotionState myMotionState;
    private RigidBodyConstructionInfo rbInfo;
    private javax.vecmath.Vector3f scale;

    public JBulletPhysicsObject(int uid, float mass, double[] xform, CollisionShape shape)
    {
        this.uid = uid;
        this.mass = mass;
        this.transform = new Transform();
        this.transform.setFromOpenGLMatrix(JBulletUtils.double_to_float_array(xform));
        this.isDynamic = (mass != 0f);
        this.shape = shape;
        this.isVehicle = false;
      
        System.out.println("Transform Origin: " + this.transform.origin);
        localInertia = new Vector3f(0, 0, 0);
        if (isDynamic) {
            shape.calculateLocalInertia(mass, localInertia);
        }
        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
        myMotionState = new DefaultMotionState(this.transform);
        rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, shape, localInertia);
        body = new RigidBody(rbInfo);
        
        //TODO set reasonable defaults
        body.setSleepingThresholds(0.05f, 0.05f); //fix for objects stopping too soon
        body.setDamping(0.1f, 0.1f);

	JBulletPhysicsObject.lookUpObject.put(body,this);
    }

    public JBulletPhysicsObject(int uid, float mass, double[] xform, CollisionShape shape, Transform transform)
    {
        this.uid = uid;
        this.mass = mass;
        this.transform = new Transform();
        this.transform.setFromOpenGLMatrix(JBulletUtils.double_to_float_array(xform));
        this.isDynamic = (mass != 0f);
        this.shape = shape;
        this.isVehicle = false;
      
        this.transform.set(transform);
        System.out.println("Transform Origin: " + this.transform.origin);
        localInertia = new Vector3f(0, 0, 0);
        if (isDynamic) {
            shape.calculateLocalInertia(mass, localInertia);
        }
        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
        myMotionState = new DefaultMotionState(this.transform);
        rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, shape, localInertia);
        body = new RigidBody(rbInfo);
        
        //TODO set reasonable defaults
        body.setSleepingThresholds(0.05f, 0.05f); //fix for objects stopping too soon
        body.setDamping(0.1f, 0.1f);

        Transform test = new Transform();
        // body.setWorldTransform(transform);
        body.getWorldTransform(test);
        System.out.println("Test: "+test.origin);

	JBulletPhysicsObject.lookUpObject.put(body,this);
    }

    public JBulletPhysicsObject(int uid, float mass, double[] xform, CollisionShape chassis, boolean isVehicle)
    {
        this.uid = uid;
        this.mass = mass;
        this.transform = new Transform();
        this.transform.setFromOpenGLMatrix(JBulletUtils.double_to_float_array(xform));
        this.isDynamic = (mass != 0f);
        this.scale = new javax.vecmath.Vector3f(0.5f, 0.5f, 0.5f);
        this.isVehicle = isVehicle;


        // this.transform.origin.set(0,0,0);

        // Vector3f translationVector = new Vector3f(0, 2f, 0);

        // Transform translation = new Transform();
        // translation.setIdentity();
        // translation.origin.set(translationVector);

        // this.transform.mul(translation);
  
        CompoundShape compound = new CompoundShape();

        Transform localTransform = new Transform();

        localTransform.setIdentity();
        localTransform.origin.set(0,1,0);

        // add compund object to dynamicsWorld
        // compound.setLocalScaling(scale);

        chassis.setLocalScaling(scale);
        compound.addChildShape(localTransform, chassis);

        this.shape = chassis;

        localInertia = new Vector3f(0, 0, 0);

        if (isDynamic) {
            shape.calculateLocalInertia(mass, localInertia);
        }

        // Start position should match matrix passed in
        myMotionState = new DefaultMotionState(this.transform);
        rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, compound, localInertia);
        
        body =  new RigidBody(rbInfo);

        // TODO set reasonable defaults
        body.setSleepingThresholds(0.05f, 0.05f); //fix for objects stopping too soon
        body.setDamping(0.1f, 0.1f);

        JBulletPhysicsObject.lookUpObject.put(body,this);
    }

    public javax.vecmath.Matrix4f convertJomlToJavax(org.joml.Matrix4f m) {
        javax.vecmath.Matrix4f convert = new javax.vecmath.Matrix4f();
    
        convert.m00 = m.m00(); convert.m01 = m.m01(); convert.m02 = m.m02(); convert.m03 = m.m03();
        convert.m10 = m.m10(); convert.m11 = m.m11(); convert.m12 = m.m12(); convert.m13 = m.m13();
        convert.m20 = m.m20(); convert.m21 = m.m21(); convert.m22 = m.m22(); convert.m23 = m.m23();
        convert.m30 = m.m30(); convert.m31 = m.m31(); convert.m32 = m.m32(); convert.m33 = m.m33();
    
        return convert;
    }
    
    public int getUID() {
        return uid;
    }

    public void setTransform(double[] xform) {
        synchronized(this)
        {
            transform.setFromOpenGLMatrix(JBulletUtils.double_to_float_array(xform));
            this.body.setWorldTransform(transform);
        }
    }

    public double[] getTransform() {
        synchronized(this)
        {
            float[] new_xform = new float[16];
            //this.transform.getOpenGLMatrix(new_xform);
            this.body.getWorldTransform(transform).getOpenGLMatrix(new_xform);
            return JBulletUtils.float_to_double_array(new_xform);
        }
    }
    public float getMass()
    {
        return this.mass;
    }
    public void setMass(float mass)
    {
        this.mass = mass;
        this.isDynamic = mass != 0;
    }

    public void setRigidBody(RigidBody newRigidBody){
        if(isVehicle){
            this.body = newRigidBody;
        }
    }

    public boolean isVehicle(){
        return isVehicle;
    }

    public RigidBody getRigidBody()
    {
        return this.body;
    }

    public CollisionShape getShape()
    {
        return this.shape;
    }

    public boolean isDynamic()
    {
        return isDynamic;
    }
    public float getFriction()
    {
        return this.body.getFriction();
    }
    public void setFriction(float friction)
    {
        this.body.setFriction(friction);
    }
    /**
     * Returns the restitution coefficient
     * 
     * @return the bounciness (restitution coefficient) of this object
     */
    public float getBounciness()
    {
        return this.body.getRestitution();
    }
    /**
     * Set the bounciness (restitution) coefficient. The value should be kept
     * between 0 and 1. Anything above 1 will make bouncing objects bounce further
     * on each bounce. The true bouncieness value of a collision between any two
     * objects in the physics world is the muplication of the two object's bounciness
     * coefficient.
     *
     * @param bounciness
     */
    public void setBounciness(float bounciness)
    {
        this.body.setRestitution(bounciness);
    }
    public float[] getLinearVelocity()
    {
        
        Vector3f out = new Vector3f();
        this.body.getLinearVelocity(out);
        float[] velocity = {out.x, out.y, out.z};
        return velocity;
    }
    public void setLinearVelocity(float[] velocity)
    {
        this.body.setLinearVelocity(new Vector3f(velocity));
    }
    public float[] getAngularVelocity()
    {

        Vector3f out = new Vector3f();
        this.body.getAngularVelocity(out);
        float[] velocity = {out.x, out.y, out.z};
        return velocity;
    }
    public void setAngularVelocity(float[] velocity)
    {
        this.body.setAngularVelocity(new Vector3f(velocity));
    }
    
    @Override
	public void setSleepThresholds(float linearThreshold, float angularThreshold) 
    {
    	body.setSleepingThresholds(linearThreshold, angularThreshold);
	}

	@Override
	public float getLinearSleepThreshold() 
	{
		return body.getLinearSleepingThreshold();
	}

	@Override
	public float getAngularSleepThreshold() 
	{
		return body.getAngularSleepingThreshold();
	}

	@Override
	public void setDamping(float linearDamping, float angularDamping) 
	{
		body.setDamping(linearDamping, angularDamping);
	}

	@Override
	public float getLinearDamping() 
	{
		return body.getLinearDamping();
	}

	@Override
	public float getAngularDamping() 
	{
		return body.getAngularDamping();
	}
	
	@Override
	public void applyForce(float fx, float fy, float fz, float px, float py, float pz){
		body.applyForce(new Vector3f(fx, fy, fz), new Vector3f(px, py, pz));
	}
	
	@Override
	public void applyTorque(float fx, float fy, float fz){
		body.applyTorque(new Vector3f(fx, fy, fz));
	}
}
