package dev.amethyst.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** An integer slider with inclusive minimum and maximum bounds. */
public final class IntSetting extends Setting<Integer> {
	private final int min;
	private final int max;

	public IntSetting(String id, String name, String description, int defaultValue, int min, int max) {
		super(id, name, description, defaultValue);
		this.min = min;
		this.max = max;
		set(defaultValue);
	}

	public int min() {
		return min;
	}

	public int max() {
		return max;
	}

	@Override
	protected Integer validate(Integer value) {
		if (value == null) {
			return defaultValue();
		}
		return Math.max(min, Math.min(max, (int) Math.floor(value)));
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
}
