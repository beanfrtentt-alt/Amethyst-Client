package dev.amethyst.client.setting;

import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A keyboard binding stored as a GLFW keycode. {@code GLFW_KEY_UNKNOWN} (-1)
 * means "not bound". Serialization stores the numeric code so profiles stay
 * compatible across keyboard layouts, plus the human readable name in a
 * comment-free way (the name is derived at runtime instead).
 */
public final class KeybindSetting extends Setting<Integer> {
	public static final int UNBOUND = GLFW.GLFW_KEY_UNKNOWN;

	public KeybindSetting(String id, String name, String description, int defaultKey) {
		super(id, name, description, defaultKey);
	}

	@Override
	protected Integer validate(Integer value) {
		if (value == null) {
			return defaultValue();
		}
		// -1 (unknown) is allowed; positive values are GLFW keycodes (max 348)
		if (value == UNBOUND || (value >= 0 && value <= GLFW.GLFW_KEY_LAST)) {
			return value;
		}
		return defaultValue();
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
			set(element.getAsInt());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isBound() {
		return get() != UNBOUND;
	}
}
