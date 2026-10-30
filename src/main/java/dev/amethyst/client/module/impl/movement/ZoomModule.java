package dev.amethyst.client.module.impl.movement;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.DoubleSetting;

import net.minecraft.client.MinecraftClient;

/**
 * Zoom smoothly narrows the FOV while bound (hold mode recommended). The
 * effective factor eases toward the target each frame for a cinematic feel.
 */
public final class ZoomModule extends Module {
	private final DoubleSetting factor = add(new DoubleSetting("factor", "Zoom Factor",
		"How far to zoom in (higher = closer).", 4.0, 1.5, 10.0, 0.5, 1));
	private final BooleanSetting reduceSensitivity = add(new BooleanSetting("reduce-sensitivity",
		"Reduce Sensitivity", "Scale look speed down while zoomed.", true));

	private float currentFactor = 1.0f;

	public ZoomModule() {
		super("zoom", "Zoom", "Smooth hold-to-zoom with sensitivity scaling.", ModuleCategory.MOVEMENT);
		setKeybind(org.lwjgl.glfw.GLFW.GLFW_KEY_C);
		setHoldMode(true);
	}

	public boolean shouldReduceSensitivity() {
		return reduceSensitivity.get();
	}

	/** The effective zoom factor applied by the FOV hook this frame. */
	public float currentZoomFactor() {
		float target = isEnabled() ? factor.get().floatValue() : 1.0f;
		float step = 0.08f;
		if (Math.abs(currentFactor - target) <= step) {
			currentFactor = target;
		} else {
			currentFactor += Math.signum(target - currentFactor) * step;
		}
		return Math.max(1.0f, currentFactor);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		currentFactor = 1.0f;
	}

	/** Called by the mouse hook to scale look speed while zooming. */
	public static float sensitivityScale() {
		if (MinecraftClient.getInstance() == null) {
			return 1f;
		}
		dev.amethyst.client.module.Module zoom =
			dev.amethyst.client.AmethystClient.MODULES == null
				? null : dev.amethyst.client.AmethystClient.MODULES.byId("zoom");
		if (zoom instanceof ZoomModule zoomModule && zoomModule.isEnabled() && zoomModule.shouldReduceSensitivity()) {
			return 1f / Math.max(1f, zoomModule.currentZoomFactor());
		}
		return 1f;
	}
}
