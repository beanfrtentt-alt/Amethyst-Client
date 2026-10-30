package dev.amethyst.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A free-form single-line text input. Length is capped to keep configs sane. */
public final class StringSetting extends Setting<String> {
	private static final int MAX_LENGTH = 120;

	public StringSetting(String id, String name, String description, String defaultValue) {
		super(id, name, description, defaultValue);
	}

	@Override
	protected String validate(String value) {
		if (value == null) {
			return defaultValue();
		}
		if (value.length() > MAX_LENGTH) {
			return value.substring(0, MAX_LENGTH);
		}
		// strip control characters that would corrupt JSON round-trips
		return value.replaceAll("[\\p{Cntrl}]", "");
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public boolean fromJson(JsonElement element) {
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			return false;
		}
		set(element.getAsString());
		return true;
	}
}
