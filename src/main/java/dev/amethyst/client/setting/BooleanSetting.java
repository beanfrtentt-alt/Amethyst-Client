package dev.amethyst.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A simple on/off toggle. */
public final class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String id, String name, String description, boolean defaultValue) {
		super(id, name, description, defaultValue);
	}

	@Override
	protected Boolean validate(Boolean value) {
		return value == null ? defaultValue() : value;
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
			set(element.getAsBoolean());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void toggle() {
		set(!get());
	}
}
