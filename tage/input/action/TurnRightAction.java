package tage.input.action;

import net.java.games.input.Event;
import tage.*;

import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import org.joml.*;

import a3.MyGame;

public class TurnRightAction extends AbstractInputAction {
	
	private MyGame game;
	private GameObject av;
	private final float steerInc = 0.04f;
	private final float steerMax = -0.3f;
	private float steer;
	private float turnConst, turnCoef;
	private RaycastVehicle vehicle;

	public TurnRightAction(MyGame g, float turnConst, float turnCoef, RaycastVehicle v)
	{
		game = g;
		this.turnCoef = turnCoef;
		this.turnConst = turnConst;
		this.vehicle = v;
		this.steer = 0.0f;
	}

	@Override
	public void performAction(float time, Event e)
	{
		steer -= steerInc;
		if(steer < steerMax){
			steer = steerMax;
		}

		this.vehicle.setSteeringValue(steer, 0);
		this.vehicle.setSteeringValue(steer, 1);

		// float keyValue = e.getValue();
		// if (keyValue > -.2 && keyValue < .2)
		// 	return; // deadzone

		// av = game.getAvatar();

		// if (game.getSpeed() > 0 && !game.getIsFalling())
		// {
		// 	float speed = (float) game.getSpeed();
		// 	float max = (float) game.getMaxSpeed();
		// 	float yaw = time * turnCoef * (speed / max) + turnConst;

		// 	av.worldYaw(-yaw);
		// }
	}
}

