package dev.amethyst.client.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleManager;
import dev.amethyst.client.setting.Setting;

/**
 * Serializes module state to JSON and back:
 *
 * <pre>{@code
 * {
 *   "aim-assist": {
 *     "enabled": false, "keybind": -1, "holdMode": false,
 *     "showInArraylist": true, "toggleMessages": true,
 *     "settings": { "range": 3.2, "speed": 10.0 }
 *   }
 * }
 * }</pre>
 *
 * Unknown modules and unknown settings from other versions are preserved
 * round-trip (kept in memory and re-written) so upgrading or renaming can
 * never silently destroy user configuration.
 */
public final class ModuleConfigSection implements ConfigManager.Section {
	private final ModuleManager modules;
	/** Entries for modules that no longer exist in this build. */
	private final Map<String, JsonObject> orphans = new LinkedHashMap<>();
	private final Map<String, JsonObject> orphanSettings = new LinkedHashMap<>();

	public ModuleConfigSection(ModuleManager modules) {
		this.modules = modules;
	}

	@Override
	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		for (Module module : modules.all()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("enabled", module.isEnabled());
			entry.addProperty("keybind", module.keybind());
			entry.addProperty("holdMode", module.isHoldMode());
			entry.addProperty("showInArraylist", module.showInArraylist());
			entry.addProperty("toggleMessages", module.toggleMessages());
			JsonObject settings = orphanSettings.getOrDefault(module.id(), new JsonObject()).deepCopy();
			for (Setting<?> setting : module.settings()) {
				try {
					settings.add(setting.id(), setting.toJson());
				} catch (Exception e) {
					AmethystClient.LOGGER.warn("Could not serialize setting {}/{}: {}",
						module.id(), setting.id(), e.getMessage());
				}
			}
			entry.add("settings", settings);
			root.add(module.id(), entry);
		}
		// round-trip modules this version does not know
		for (Map.Entry<String, JsonObject> orphan : orphans.entrySet()) {
			if (!root.has(orphan.getKey())) {
				root.add(orphan.getKey(), orphan.getValue());
			}
		}
		return root;
	}

	@Override
	public void apply(JsonObject root) {
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			String id = entry.getKey();
			Module module = modules.byId(id);
			if (module == null) {
				// keep for round-trip; dropped on the next successful match
				if (entry.getValue().isJsonObject()) {
					orphans.put(id, entry.getValue().getAsJsonObject());
				}
				continue;
			}
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			JsonObject moduleConfig = entry.getValue().getAsJsonObject();
			applyFlags(module, moduleConfig);
			applySettings(module, moduleConfig);
		}
	}

	private void applyFlags(Module module, JsonObject moduleConfig) {
		if (hasBoolean(moduleConfig, "holdMode")) {
			module.setHoldMode(moduleConfig.get("holdMode").getAsBoolean());
		}
		if (hasBoolean(moduleConfig, "showInArraylist")) {
			module.setShowInArraylist(moduleConfig.get("showInArraylist").getAsBoolean());
		}
		if (hasBoolean(moduleConfig, "toggleMessages")) {
			module.setToggleMessages(moduleConfig.get("toggleMessages").getAsBoolean());
		}
		if (moduleConfig.has("keybind") && moduleConfig.get("keybind").isJsonPrimitive()) {
			try {
				module.setKeybind(moduleConfig.get("keybind").getAsInt());
			} catch (Exception ignored) {
			}
		}
	}

	/** Applies settings, then enables the module (so onEnable sees loaded values). */
	private void applySettings(Module module, JsonObject moduleConfig) {
		JsonElement settingsElement = moduleConfig.get("settings");
		if (settingsElement != null && settingsElement.isJsonObject()) {
			for (Map.Entry<String, JsonElement> entry : settingsElement.getAsJsonObject().entrySet()) {
				Setting<?> target = findSetting(module, entry.getKey());
				if (target == null) {
					// Keep settings introduced by a newer build so a save does not
					// destroy them when this version does not recognize their IDs.
					JsonObject unknown = orphanSettings.computeIfAbsent(module.id(), ignored -> new JsonObject());
					unknown.add(entry.getKey(), entry.getValue().deepCopy());
					continue;
				}
				try {
					target.fromJson(entry.getValue());
				} catch (Exception e) {
					AmethystClient.LOGGER.warn("Bad value for setting {}/{}: {}", module.id(), entry.getKey(), e.getMessage());
				}
			}
		}
		if (hasBoolean(moduleConfig, "enabled")) {
			boolean enabled = moduleConfig.get("enabled").getAsBoolean();
			if (module.isIncomplete()) {
				enabled = false; // never auto-enable unimplemented modules
			}
			try {
				module.setEnabled(enabled);
			} catch (Exception e) {
				AmethystClient.LOGGER.warn("Could not apply enabled state to {}: {}", module.id(), e.getMessage());
			}
		}
	}

	private static Setting<?> findSetting(Module module, String id) {
		for (Setting<?> setting : module.settings()) {
			if (setting.id().equals(id)) {
				return setting;
			}
		}
		return null;
	}

	private static boolean hasBoolean(JsonObject object, String member) {
		return object.has(member) && object.get(member).isJsonPrimitive()
			&& object.get(member).getAsJsonPrimitive().isBoolean();
	}
}
