package dev.amethyst.client.module.impl.movement;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Air Stuck freezes your position while the key is held (bind it with hold
 * mode enabled). Useful for waiting out timed mechanics without falling.
 */
public final class AirStuckModule extends Module {
	private Vec3d frozenPos;

	public AirStuckModule() {
		super("air-stuck", "Air Stuck", "Hold to freeze your position mid-air.", ModuleCategory.MOVEMENT);
		setKeybind(org.lwjgl.glfw.GLFW.GLFW_KEY_Z);
		setHoldMode(true);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		frozenPos = player == null ? null : player.getEntityPos();
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return;
		}
		if (frozenPos == null) {
			frozenPos = player.getEntityPos();
		}
		player.setPosition(frozenPos);
		player.setVelocity(0, 0, 0);
		player.fallDistance = 0;
	}
}
