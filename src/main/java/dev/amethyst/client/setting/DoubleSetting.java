package dev.amethyst.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A floating point slider with minimum, maximum, step and display precision. */
public final class DoubleSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;
	private final int decimals;

	public DoubleSetting(String id, String name, String description,
	                     double defaultValue, double min, double max, double step, int decimals) {
		super(id, name, description, defaultValue);
		this.min = min;
		this.max = max;
		this.step = Math.max(0.001, step);
		this.decimals = Math.max(0, decimals);
		set(defaultValue);
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public double step() {
		return step;
	}

	public int decimals() {
		return decimals;
	}

	@Override
	protected Double validate(Double value) {
		if (value == null) {
			return defaultValue();
		}
		double clamped = Math.max(min, Math.min(max, value));
		// snap to the step grid relative to the minimum
		double steps = Math.round((clamped - min) / step);
		double snapped = min + steps * step;
		if (snapped < min) snapped = min;
		if (snapped > max) snapped = max;
		double factor = Math.pow(10, decimals);
		return Math.round(snapped * factor) / factor;
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
			set(element.getAsDouble());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
