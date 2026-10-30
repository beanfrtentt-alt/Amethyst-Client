package dev.amethyst.client.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.config.ConfigManager;
import dev.amethyst.client.hud.impl.ArraylistElement;
import dev.amethyst.client.hud.impl.CoordsElement;
import dev.amethyst.client.hud.impl.DirectionElement;
import dev.amethyst.client.hud.impl.FpsElement;
import dev.amethyst.client.hud.impl.HotkeysElement;
import dev.amethyst.client.hud.impl.NotificationsElement;
import dev.amethyst.client.hud.impl.RadarElement;
import dev.amethyst.client.hud.impl.SpotifyElement;
import dev.amethyst.client.hud.impl.TargetInfoElement;
import dev.amethyst.client.hud.impl.WatermarkElement;
import dev.amethyst.client.util.NotificationManager;

/**
 * Registry, renderer and persistence for HUD widgets. Positions and enabled
 * flags are persisted through the "hud" config section; the HUD editor drags
 * elements by mutating positions and marking the config dirty.
 */
public final class HudManager {
	private static final Map<String, HudElement> ELEMENTS = new TreeMap<>();
	private static HudElement dragging;
	private static int dragOffsetX;
	private static int dragOffsetY;
	private static boolean editorOpen;

	private HudManager() {
	}

	public static void init(ConfigManager config) {
		register(new WatermarkElement());
		register(new ArraylistElement());
		register(new FpsElement());
		register(new CoordsElement());
		register(new DirectionElement());
		register(new NotificationsElement());
		register(new RadarElement());
		register(new HotkeysElement());
		register(new TargetInfoElement());
		register(new SpotifyElement());
		config.registerSection("hud", new Section());
	}

	public static void register(HudElement element) {
		if (ELEMENTS.putIfAbsent(element.id(), element) != null) {
			throw new IllegalStateException("Duplicate HUD element id: " + element.id());
		}
	}

	public static List<HudElement> all() {
		return new ArrayList<>(ELEMENTS.values());
	}

	public static HudElement byId(String id) {
		return ELEMENTS.get(id);
	}

	public static boolean isEditorOpen() {
		return editorOpen;
	}

	public static void setEditorOpen(boolean value) {
		editorOpen = value;
	}

	public static void tick() {
		for (HudElement element : ELEMENTS.values()) {
			element.tick();
		}
	}

	public static void render(DrawContext context) {
		for (HudElement element : ELEMENTS.values()) {
			if (element.isEnabled() || editorOpen) {
				element.renderElement(context);
			}
		}
		if (editorOpen) {
			renderEditorOverlay(context);
		}
	}

	private static void renderEditorOverlay(DrawContext context) {
		for (HudElement element : ELEMENTS.values()) {
			int accent = element.isEnabled() ? 0x809B53E8 : 0x60808080;
			context.fill(element.x() - 1, element.y() - 1,
				element.x() + element.lastWidth() + 1, element.y() + element.lastHeight() + 1, accent);
		}
	}

	// ---- editor interaction (called from the HUD editor screen) ----

	public static boolean startDrag(double mouseX, double mouseY) {
		for (HudElement element : ELEMENTS.values()) {
			if (element.isMouseOver(mouseX, mouseY)) {
				dragging = element;
				dragOffsetX = (int) mouseX - element.x();
				dragOffsetY = (int) mouseY - element.y();
				return true;
			}
		}
		return false;
	}

	public static void dragTo(double mouseX, double mouseY) {
		if (dragging != null) {
			dragging.setPosition((int) mouseX - dragOffsetX, (int) mouseY - dragOffsetY);
			if (AmethystClient.CONFIG != null) {
				AmethystClient.CONFIG.markDirty();
			}
		}
	}

	public static void stopDrag() {
		dragging = null;
	}

	/** Cycles an element's visibility from the HUD editor click. */
	public static void toggleElementAt(double mouseX, double mouseY) {
		for (HudElement element : ELEMENTS.values()) {
			if (element.isMouseOver(mouseX, mouseY)) {
				element.setEnabled(!element.isEnabled());
				if (AmethystClient.CONFIG != null) {
					AmethystClient.CONFIG.markDirty();
				}
				return;
			}
		}
	}

	/** Persists element positions and enabled flags. */
	private static final class Section implements ConfigManager.Section {
		@Override
		public JsonObject toJson() {
			JsonObject root = new JsonObject();
			for (HudElement element : ELEMENTS.values()) {
				JsonObject entry = new JsonObject();
				entry.addProperty("x", element.x());
				entry.addProperty("y", element.y());
				entry.addProperty("enabled", element.isEnabled());
				root.add(element.id(), entry);
			}
			return root;
		}

		private static boolean isInteger(JsonObject object, String key) {
			if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
				return false;
			}
			try {
				object.get(key).getAsInt();
				return true;
			} catch (RuntimeException ignored) {
				return false;
			}
		}

		@Override
		public void apply(JsonObject root) {
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				HudElement element = byId(entry.getKey());
				if (element == null || !entry.getValue().isJsonObject()) {
					continue;
				}
				JsonObject data = entry.getValue().getAsJsonObject();
				if (isInteger(data, "x") && isInteger(data, "y")) {
					try {
						element.setPosition(data.get("x").getAsInt(), data.get("y").getAsInt());
					} catch (RuntimeException ignored) {
						// Ignore malformed coordinates and retain this element's defaults.
					}
				}
				if (data.has("enabled") && data.get("enabled").isJsonPrimitive()) {
					element.setEnabled(data.get("enabled").getAsBoolean());
				}
			}
		}
	}

	/** Used by tests to verify serialization round-trips. */
	public static Map<String, HudElement> elementsView() {
		return Collections.unmodifiableMap(ELEMENTS);
	}

	public static void resetPositionsToDefaults() {
		// defaults are re-derived from a fresh registration layout
		for (HudElement element : ELEMENTS.values()) {
			element.resetPosition();
		}
		if (AmethystClient.CONFIG != null) {
			AmethystClient.CONFIG.markDirty();
		}
		NotificationManager.info("HUD positions reset (drag elements in the editor).");
	}
}
