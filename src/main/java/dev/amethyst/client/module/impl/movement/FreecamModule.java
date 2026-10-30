package dev.amethyst.client.module.impl.movement;

import org.lwjgl.glfw.GLFW;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.util.CameraState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec3d;

/**
 * Freecam detaches the camera and flies it freely while your player stays
 * where they were. Movement keys steer the camera, scroll adjusts speed, and
 * the player's input is suppressed while active so you do not drift.
 */
public final class FreecamModule extends Module {
	private final DoubleSetting speed = add(new DoubleSetting("speed", "Speed",
		"Camera fly speed (blocks per second multiplier).", 1.0, 0.2, 4.0, 0.1, 1));
	private final BooleanSetting toggleMessage = add(new BooleanSetting("toggle-message", "Toggle Message",
		"Show a notification when freecam toggles.", false));

	private float scrollSpeedBoost = 1.0f;

	public FreecamModule() {
		super("freecam", "Freecam", "Fly a free camera while your player stays put.",
			ModuleCategory.MOVEMENT);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			setEnabled(false);
			return;
		}
		Vec3d eye = player.getEyePos();
		CameraState.enableFreecam(eye, player.getYaw(), player.getPitch());
		scrollSpeedBoost = 1.0f;
	}

	@Override
	public void onDisable() {
		super.onDisable();
		CameraState.disableFreecam();
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			setEnabled(false);
			return;
		}
		// suppress player input so the player stands still
		player.input.playerInput = new net.minecraft.util.PlayerInput(
			false, false, false, false, false, false, false);

		if (client.currentScreen != null || client.getWindow() == null) {
			return;
		}
		net.minecraft.client.util.Window window = client.getWindow();
		double speed = this.speed.get() * scrollSpeedBoost * 0.5;
		double yawRad = Math.toRadians(CameraState.freecamYaw());
		double pitchRad = Math.toRadians(CameraState.freecamPitch());

		double forward = 0, strafe = 0, vertical = 0;
		if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_W)) forward += 1;
		if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_S)) forward -= 1;
		if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_A)) strafe -= 1;
		if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_D)) strafe += 1;
		if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_SPACE)) vertical += 1;
		if (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)) vertical -= 1;

		if (forward == 0 && strafe == 0 && vertical == 0) {
			return;
		}
		double sinYaw = Math.sin(yawRad);
		double cosYaw = Math.cos(yawRad);
		double sinPitch = Math.sin(pitchRad);
		double cosPitch = Math.cos(pitchRad);

		double dx = (-sinYaw * forward) + (cosYaw * strafe);
		double dz = (cosYaw * forward) + (sinYaw * strafe);
		double dy = (-sinPitch * forward) + vertical;
		CameraState.moveFreecam(dx * speed, dy * speed, dz * speed);
	}

	@Override
	public void onHudRender(net.minecraft.client.gui.DrawContext context) {
		// no HUD content; the scroll boost adjusts via keybind below
	}

	/** Called by the keybind manager when scroll should adjust speed. */
	public void adjustSpeedBoost(float direction) {
		scrollSpeedBoost = Math.max(0.25f, Math.min(4.0f, scrollSpeedBoost + direction * 0.25f));
	}

	public boolean showToggleNotification() {
		return toggleMessage.get();
	}
}
