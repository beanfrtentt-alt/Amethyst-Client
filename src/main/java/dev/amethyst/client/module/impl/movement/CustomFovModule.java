package dev.amethyst.client.module.impl.movement;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.DoubleSetting;

import net.minecraft.client.MinecraftClient;

/**
 * Custom FOV pins the field of view to a chosen value while enabled. The
 * zoom stack still applies on top of it.
 */
public final class CustomFovModule extends Module {
	private final DoubleSetting fov = add(new DoubleSetting("fov", "FOV",
		"Base field of view in degrees.", 90.0, 30.0, 130.0, 1.0, 0));

	private double lastKnownFov = 90.0;

	public CustomFovModule() {
		super("custom-fov", "Custom FOV", "Locks your field of view to a chosen value.", ModuleCategory.MOVEMENT);
	}

	/** The FOV the game should use while this module is enabled. */
	public float fovValue() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.options != null && client.options.getFov() != null) {
			lastKnownFov = client.options.getFov().getValue();
		}
		return fov.get().floatValue();
	}
}
