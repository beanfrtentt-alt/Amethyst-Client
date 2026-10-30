package dev.amethyst.client.module.impl.movement;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.IntSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Render Distance Toggle flips between your normal and a low render
 * distance with one key — useful for long travel or weak machines.
 */
public final class RenderDistanceToggleModule extends Module {
	private final IntSetting lowDistance = add(new IntSetting("low-distance", "Low Distance",
		"Render distance used when toggled low.", 4, 2, 16));

	private Integer savedDistance;
	private boolean low;

	public RenderDistanceToggleModule() {
		super("render-distance", "Render Distance Toggle", "Keybind flips render distance between normal and low.",
			ModuleCategory.MOVEMENT);
		setKeybind(org.lwjgl.glfw.GLFW.GLFW_KEY_B);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		toggleDistance();
		setEnabled(false); // acts as a one-shot action key
	}

	private void toggleDistance() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.options == null) {
			return;
		}
		SimpleOption<Integer> viewDistance = client.options.getViewDistance();
		if (viewDistance == null) {
			return;
		}
		if (low) {
			if (savedDistance != null) {
				viewDistance.setValue(savedDistance);
				savedDistance = null;
			}
			low = false;
		} else {
			savedDistance = viewDistance.getValue();
			viewDistance.setValue(lowDistance.get());
			low = true;
		}
	}

	@Override
	public void onShutdown() {
		// Restore the user's original setting when the game exits, but do not
		// undo the one-shot toggle immediately after its keybind fires.
		MinecraftClient client = MinecraftClient.getInstance();
		if (low && savedDistance != null && client.options != null) {
			client.options.getViewDistance().setValue(savedDistance);
			savedDistance = null;
			low = false;
		}
		super.onShutdown();
	}
}
