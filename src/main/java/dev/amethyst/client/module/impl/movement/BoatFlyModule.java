package dev.amethyst.client.module.impl.movement;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.DoubleSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

/**
 * Boat Fly steers any boat you control freely through the air. The server
 * remains authoritative — anti-cheat servers may rubber-band the boat, which
 * is expected and documented.
 */
public final class BoatFlyModule extends Module {
	private final DoubleSetting speed = add(new DoubleSetting("speed", "Speed",
		"Flight speed in blocks per tick (scaled).", 0.8, 0.1, 3.0, 0.05, 2));
	private final BooleanSetting antiGravity = add(new BooleanSetting("anti-gravity", "Anti-Gravity",
		"Hold the boat's height while idle instead of sinking.", true));

	public BoatFlyModule() {
		super("boat-fly", "Boat Fly", "Fly freely while steering any boat.", ModuleCategory.MOVEMENT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return;
		}
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof net.minecraft.entity.vehicle.AbstractBoatEntity boat)) {
			return;
		}
		if (player.getControllingVehicle() != vehicle) {
			return; // passenger, not steering
		}
		var input = player.input.playerInput;
		double vx = 0, vy = 0, vz = 0;
		// steering uses the boat's yaw
		double yawRad = Math.toRadians(vehicle.getYaw());
		double sin = -Math.sin(yawRad);
		double cos = Math.cos(yawRad);
		if (input.forward()) {
			vx += sin;
			vz += cos;
		}
		if (input.backward()) {
			vx -= sin;
			vz -= cos;
		}
		if (input.left()) {
			vx += cos;
			vz -= sin;
		}
		if (input.right()) {
			vx -= cos;
			vz += sin;
		}
		if (input.jump()) {
			vy += 0.6;
		}
		if (input.sneak()) {
			vy -= 0.6;
		}
		double speed = this.speed.get();
		if (vx == 0 && vy == 0 && vz == 0) {
			if (antiGravity.get()) {
				vehicle.setVelocity(0, 0, 0);
			}
			return;
		}
		vehicle.setVelocity(vx * speed, vy * speed, vz * speed);
	}
}
