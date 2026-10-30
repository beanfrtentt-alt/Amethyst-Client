package dev.amethyst.client.setting;

import com.google.gson.JsonElement;

/**
 * Base class for all module settings. Settings have a stable string id used for
 * persistence, a human-readable name, an optional description (shown as a GUI
 * tooltip) and a default value they can always be reset to.
 */
public abstract class Setting<T> {
	private final String id;
	private final String name;
	private final String description;
	private final T defaultValue;
	protected T value;

	protected Setting(String id, String name, String description, T defaultValue) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public T defaultValue() {
		return defaultValue;
	}

	public T get() {
		return value;
	}

	/** Sets the value after running it through {@link #validate}. */
	public void set(T value) {
		this.value = validate(value);
	}

	/** Restores the default value. */
	public void reset() {
		this.value = defaultValue;
	}

	public boolean isDefault() {
		return value == null ? defaultValue == null : value.equals(defaultValue);
	}

	/**
	 * Clamps / normalizes an incoming value. Subclasses must override this to
	 * enforce their bounds so that malformed config files cannot produce
	 * out-of-range values.
	 */
	protected abstract T validate(T value);

	/** Serializes the current value to JSON for the config file. */
	public abstract JsonElement toJson();

	/**
	 * Loads a value from JSON. Implementations must accept anything reasonable
	 * (e.g. numbers for booleans are tolerated by GSON) and fall back to the
	 * current value when the element is unusable.
	 *
	 * @return true when the element was applied, false when it was ignored
	 */
	public abstract boolean fromJson(JsonElement element);
}
