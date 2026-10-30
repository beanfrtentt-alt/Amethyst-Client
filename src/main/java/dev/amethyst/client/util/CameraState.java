package dev.amethyst.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * State shared between the Freecam / Freelook modules and the camera + mouse
 * mixins. The mixins are thin: they consult this state and redirect camera
 * positioning and mouse look accordingly.
 */
public final class CameraState {
	// ---- freecam ----
	private static boolean freecamActive;
	private static Vec3d freecamPos = Vec3d.ZERO;
	private static float freecamYaw;
	private static float freecamPitch;

	// ---- freelook ----
	private static boolean freelookActive;
	private static float freelookYaw;
	private static float freelookPitch;

	private static long window = -1;

	private CameraState() {
	}

	// ---- freecam ----

	public static boolean freecamActive() {
		return freecamActive;
	}

	public static Vec3d freecamPos() {
		return freecamPos;
	}

	public static float freecamYaw() {
		return freecamYaw;
	}

	public static float freecamPitch() {
		return freecamPitch;
	}

	public static void enableFreecam(Vec3d startPos, float yaw, float pitch) {
		freecamActive = true;
		freecamPos = startPos;
		freecamYaw = yaw;
		freecamPitch = pitch;
	}

	public static void disableFreecam() {
		freecamActive = false;
	}

	public static void moveFreecam(double dx, double dy, double dz) {
		freecamPos = freecamPos.add(dx, dy, dz);
	}

	public static void rotateFreecam(double yawDelta, double pitchDelta) {
		freecamYaw = (float) (freecamYaw + yawDelta);
		freecamPitch = MathHelper.clamp((float) (freecamPitch + pitchDelta), -90f, 90f);
	}

	// ---- freelook ----

	public static boolean freelookActive() {
		return freelookActive;
	}

	public static float freelookYaw() {
		return freelookYaw;
	}

	public static float freelookPitch() {
		return freelookPitch;
	}

	public static void enableFreelook() {
		freelookActive = true;
		freelookYaw = 0f;
		freelookPitch = 0f;
	}

	public static void disableFreelook() {
		freelookActive = false;
		freelookYaw = 0f;
		freelookPitch = 0f;
	}

	public static void rotateFreelook(double yawDelta, double pitchDelta) {
		freelookYaw = (float) (freelookYaw + yawDelta);
		freelookPitch = MathHelper.clamp((float) (freelookPitch + pitchDelta), -90f, 90f);
	}

	// ---- mouse redirection ----

	/**
	 * Called from the mouse mixin when a detached-look mode is active. Consumes
	 * the raw cursor delta and applies the same sensitivity math the vanilla
	 * mouse handler uses.
	 *
	 * @return true when the delta was consumed (vanilla look must not run)
	 */
	public static boolean handleMouseDelta(double rawDx, double rawDy) {
		if (!freecamActive && !freelookActive) {
			return false;
		}
		if (window == -1) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.getWindow() != null) {
				window = client.getWindow().getHandle();
			}
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.options == null) {
			return true;
		}
		double sensitivity = client.options.getMouseSensitivity().getValue();
		double factor = sensitivity * 0.6 + 0.2;
		double scaled = factor * factor * factor * 1.2;
		double dx = rawDx * scaled;
		double dy = rawDy * scaled;

		if (freecamActive) {
			rotateFreecam(dx, dy);
		} else {
			rotateFreelook(dx, dy);
		}
		return true;
	}
}
