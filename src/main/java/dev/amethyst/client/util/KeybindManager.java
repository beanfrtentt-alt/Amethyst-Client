package dev.amethyst.client.util;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleManager;

/**
 * Polls module keybinds once per client tick and applies toggle/hold
 * semantics with proper edge detection. Bindings only fire while no screen is
 * open, so typing in chat or search boxes can never trigger a module.
 */
public final class KeybindManager {
	private final ModuleManager modules;
	private final Map<String, Boolean> wasPressed = new HashMap<>();

	public KeybindManager(ModuleManager modules) {
		this.modules = modules;
	}

	/** Called at the start of every client tick. */
	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null || client.getWindow() == null) {
			// Release hold-mode modules while a screen is open; otherwise an
			// action such as Air Stuck can remain active after its key is released.
			for (Module module : modules.all()) {
				if (module.isHoldMode() && module.isEnabled()) {
					safeSetEnabled(module, false);
				}
				wasPressed.put(module.id(), false);
			}
			return;
		}
		net.minecraft.client.util.Window window = client.getWindow();
		for (Module module : modules.all()) {
			int key = module.keybind();
			boolean pressed = key != -1 && InputUtil.isKeyPressed(window, key);
			boolean previous = wasPressed.getOrDefault(module.id(), false);
			if (module.isHoldMode()) {
				if (pressed != previous) {
					safeSetEnabled(module, pressed);
				}
			} else if (pressed && !previous) {
				safeToggle(module);
			}
			wasPressed.put(module.id(), pressed);
		}
	}

	private void safeToggle(Module module) {
		try {
			module.toggle();
			announce(module, module.isEnabled());
			modules.fireTogglesChanged();
		} catch (Exception e) {
			AmethystClient.LOGGER.warn("Keybind toggle failed for module {}: {}", module.id(), e.getMessage());
		}
	}

	private void safeSetEnabled(Module module, boolean value) {
		try {
			module.setEnabled(value);
			announce(module, value);
			modules.fireTogglesChanged();
		} catch (Exception e) {
			AmethystClient.LOGGER.warn("Keybind hold failed for module {}: {}", module.id(), e.getMessage());
		}
	}

	public static boolean shouldAnnounce(Module module) {
		if (module instanceof dev.amethyst.client.module.impl.movement.FreecamModule freecam
			&& !freecam.showToggleNotification()) {
			return false;
		}
		return module.toggleMessages();
	}

	private void announce(Module module, boolean enabled) {
		if (shouldAnnounce(module)) {
			NotificationManager.post(
				module.name() + (enabled ? " enabled" : " disabled"),
				enabled ? NotificationManager.Type.SUCCESS : NotificationManager.Type.INFO,
				1500);
		}
	}

	/** Human readable name for a GLFW keycode, used by GUI labels. */
	public static String keyName(int key) {
		if (key == -1) {
			return "None";
		}
		if (key == GLFW.GLFW_KEY_UNKNOWN) {
			return "None";
		}
		String name = GLFW.glfwGetKeyName(key, 0);
		if (name != null) {
			return name.toUpperCase(java.util.Locale.ROOT);
		}
		return "Key " + key;
	}
}
