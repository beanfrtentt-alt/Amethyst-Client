package dev.amethyst.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dev.amethyst.client.setting.Setting;

/**
 * Base class for every Amethyst module. A module owns its settings, a
 * keybind, an enabled state and a small lifecycle:
 *
 * <ul>
 *   <li>{@link #onEnable()} / {@link #onDisable()} bracket the enabled state</li>
 *   <li>{@link #onTick()} runs once per client tick while enabled</li>
 *   <li>{@link #onRender3D} runs in the world render phase while enabled</li>
 *   <li>{@link #onHudRender} runs in the HUD phase while enabled (rare)</li>
 * </ul>
 *
 * Subclasses must never register global hooks themselves; the
 * {@link ModuleManager} is the single dispatcher, which keeps duplicate
 * listeners impossible by construction.
 */
public abstract class Module {
	private final String id;
	private final String name;
	private final String description;
	private final ModuleCategory category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private final boolean incomplete;

	private boolean enabled;
	private int keybind = -1; // GLFW keycode, -1 = unbound
	private boolean holdMode; // keybind acts as hold instead of toggle
	private boolean showInArraylist = true;
	private boolean toggleMessages = true;
	private int defaultKeybind = -1;
	private boolean defaultHoldMode;
	private boolean defaultShowInArraylist = true;
	private boolean defaultToggleMessages = true;
	private boolean defaultsCaptured;

	protected Module(String id, String name, String description, ModuleCategory category) {
		this(id, name, description, category, false);
	}

	protected Module(String id, String name, String description, ModuleCategory category, boolean incomplete) {
		this.id = id.toLowerCase(Locale.ROOT);
		this.name = name;
		this.description = description;
		this.category = category;
		this.incomplete = incomplete;
	}

	/** Registers a setting to this module; called only from the constructor. */
	protected <S extends Setting<?>> S add(S setting) {
		for (Setting<?> existing : settings) {
			if (existing.id().equals(setting.id())) {
				throw new IllegalStateException("Duplicate setting id '" + setting.id() + "' on module " + id);
			}
		}
		settings.add(setting);
		return setting;
	}

	// ---- identity ----

	public final String id() {
		return id;
	}

	public final String name() {
		return name;
	}

	public final String description() {
		return description;
	}

	public final ModuleCategory category() {
		return category;
	}

	/** True when the module is registered but intentionally not implemented yet. */
	public final boolean isIncomplete() {
		return incomplete;
	}

	/** Capture subclass-defined keybind defaults after construction is complete. */
	public final void captureDefaults() {
		defaultKeybind = keybind;
		defaultHoldMode = holdMode;
		defaultShowInArraylist = showInArraylist;
		defaultToggleMessages = toggleMessages;
		defaultsCaptured = true;
	}

	/** Disable this module and restore all settings and module-level flags. */
	public final void resetToDefaults() {
		try {
			setEnabled(false);
		} finally {
			for (Setting<?> setting : settings) {
				setting.reset();
			}
			if (defaultsCaptured) {
				keybind = defaultKeybind;
				holdMode = defaultHoldMode;
				showInArraylist = defaultShowInArraylist;
				toggleMessages = defaultToggleMessages;
			}
		}
	}

	public final List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	@SuppressWarnings("unchecked")
	public final <S extends Setting<?>> S setting(String settingId) {
		for (Setting<?> setting : settings) {
			if (setting.id().equals(settingId)) {
				return (S) setting;
			}
		}
		throw new IllegalArgumentException("No setting '" + settingId + "' on module " + id);
	}

	// ---- lifecycle ----

	public final boolean isEnabled() {
		return enabled;
	}

	/**
	 * Changes the enabled state, running lifecycle hooks. Hooks that throw are
	 * caught, logged and force-disable the module so a broken module can never
	 * crash the client in a loop.
	 */
	public final void setEnabled(boolean value) {
		if (incomplete) {
			return;
		}
		if (enabled == value) {
			return;
		}
		enabled = value;
		try {
			if (value) {
				onEnable();
			} else {
				onDisable();
			}
		} catch (Exception e) {
			enabled = false;
			try {
				onDisable();
			} catch (Exception suppressed) {
				// ignore - we are already failing
			}
			throw e;
		}
	}

	public final void toggle() {
		setEnabled(!enabled);
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Runs once per client tick while enabled. Never throws (manager guards). */
	public void onTick() {
	}

	/** Runs in the world render phase while enabled. Never throws (manager guards). */
	public void onRender3D() {
	}

	/** Runs in the HUD render phase while enabled. Never throws (manager guards). */
	public void onHudRender(net.minecraft.client.gui.DrawContext context) {
	}

	/** Per-module cleanup, called when the client is shutting down. */
	public void onShutdown() {
		try {
			if (enabled) {
				setEnabled(false);
			}
		} catch (Exception ignored) {
		}
	}

	// ---- keybind ----

	public final int keybind() {
		return keybind;
	}

	public final void setKeybind(int key) {
		this.keybind = key;
	}

	public final boolean isHoldMode() {
		return holdMode;
	}

	public final void setHoldMode(boolean holdMode) {
		this.holdMode = holdMode;
	}

	// ---- HUD arraylist ----

	public final boolean showInArraylist() {
		return showInArraylist;
	}

	public final void setShowInArraylist(boolean value) {
		this.showInArraylist = value;
	}

	public final boolean toggleMessages() {
		return toggleMessages;
	}

	public final void setToggleMessages(boolean value) {
		this.toggleMessages = value;
	}

	@Override
	public final String toString() {
		return id;
	}
}
