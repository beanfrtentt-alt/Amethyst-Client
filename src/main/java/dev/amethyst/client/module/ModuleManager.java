package dev.amethyst.client.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import dev.amethyst.client.util.NotificationManager;

/**
 * Central module registry and the single dispatcher for module lifecycle
 * hooks. All Fabric event registrations happen exactly once here, so a module
 * can never accidentally register duplicate listeners.
 */
public final class ModuleManager {
	private final Map<String, Module> modules = new LinkedHashMap<>();
	private final List<Runnable> toggleListeners = new ArrayList<>();

	public void register(Module module) {
		if (modules.containsKey(module.id())) {
			throw new IllegalStateException("Duplicate module id: " + module.id());
		}
		modules.put(module.id(), module);
	}

	public void registerAll(Collection<? extends Module> toRegister) {
		for (Module module : toRegister) {
			register(module);
		}
	}

	public Collection<Module> all() {
		return Collections.unmodifiableCollection(modules.values());
	}

	public Module byId(String id) {
		return id == null ? null : modules.get(id.toLowerCase(Locale.ROOT));
	}

	public List<Module> byCategory(ModuleCategory category) {
		return modules.values().stream()
			.filter(m -> m.category() == category)
			.sorted(Comparator.comparing(Module::name))
			.collect(Collectors.toList());
	}

	/** Modules whose name or description contains the query (case-insensitive). */
	public List<Module> search(String query) {
		String needle = query.trim().toLowerCase(Locale.ROOT);
		if (needle.isEmpty()) {
			return List.of();
		}
		return modules.values().stream()
			.filter(m -> m.name().toLowerCase(Locale.ROOT).contains(needle)
				|| m.description().toLowerCase(Locale.ROOT).contains(needle)
				|| m.id().contains(needle))
			.sorted(Comparator.comparing(Module::name))
			.collect(Collectors.toList());
	}

	public List<Module> enabled() {
		return modules.values().stream()
			.filter(Module::isEnabled)
			.sorted(Comparator.comparing(Module::name))
			.collect(Collectors.toList());
	}

	/** Enabled modules that want to appear in the HUD arraylist. */
	public List<Module> arraylistModules() {
		return modules.values().stream()
			.filter(Module::isEnabled)
			.filter(Module::showInArraylist)
			.sorted(Comparator.comparing(Module::name))
			.collect(Collectors.toList());
	}

	// ---- dispatch (all guarded so one broken module cannot kill the frame) ----

	public void tick() {
		for (Module module : modules.values()) {
			if (module.isEnabled()) {
				try {
					module.onTick();
				} catch (Exception e) {
					handleModuleError(module, e);
				}
			}
		}
	}

	public void render3D() {
		for (Module module : modules.values()) {
			if (module.isEnabled()) {
				try {
					module.onRender3D();
				} catch (Exception e) {
					handleModuleError(module, e);
				}
			}
		}
	}

	public void hudRender(net.minecraft.client.gui.DrawContext context) {
		for (Module module : modules.values()) {
			if (module.isEnabled()) {
				try {
					module.onHudRender(context);
				} catch (Exception e) {
					handleModuleError(module, e);
				}
			}
		}
	}

	public void shutdown() {
		for (Module module : modules.values()) {
			module.onShutdown();
		}
	}

	private void handleModuleError(Module module, Exception e) {
		try {
			module.setEnabled(false);
		} catch (Exception ignored) {
		}
		NotificationManager.error("Module error: " + module.name() + " was disabled. See logs.");
		dev.amethyst.client.AmethystClient.LOGGER.error(
			"Module '{}' threw an exception; disabling it", module.id(), e);
	}

	// ---- toggle notifications ----

	public void addToggleListener(Runnable listener) {
		toggleListeners.add(listener);
	}

	public void fireTogglesChanged() {
		for (Runnable listener : List.copyOf(toggleListeners)) {
			try {
				listener.run();
			} catch (RuntimeException e) {
				dev.amethyst.client.AmethystClient.LOGGER.warn("Module toggle listener failed", e);
			}
		}
	}

	/** Convenience predicate used by GUI filters. */
	public List<Module> matching(Predicate<Module> filter) {
		return modules.values().stream().filter(filter).collect(Collectors.toList());
	}
}
