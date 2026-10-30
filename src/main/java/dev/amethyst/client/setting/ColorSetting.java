package dev.amethyst.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * An ARGB color with helper accessors. Serialized as a single 32-bit integer
 * (0xAARRGGBB) which is what every Minecraft rendering API expects.
 */
public final class ColorSetting extends Setting<Integer> {

	public ColorSetting(String id, String name, String description, int defaultArgb) {
		super(id, name, description, defaultArgb);
	}

	@Override
	protected Integer validate(Integer value) {
		if (value == null) {
			return defaultValue();
		}
		return value & 0xFFFFFFFF;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public boolean fromJson(JsonElement element) {
		if (element == null || !element.isJsonPrimitive()) {
			return false;
		}
		try {
			// tolerate float color objects written by other tools
			if (element.getAsJsonPrimitive().isNumber()) {
				set(element.getAsInt());
				return true;
			}
			if (element.getAsJsonPrimitive().isString()) {
				String raw = element.getAsString().replace("#", "").trim();
				set((int) Long.parseLong(raw, 16));
				return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public int alpha() {
		return (get() >>> 24) & 0xFF;
	}

	public int red() {
		return (get() >>> 16) & 0xFF;
	}

	public int green() {
		return (get() >>> 8) & 0xFF;
	}

	public int blue() {
		return get() & 0xFF;
	}
}
