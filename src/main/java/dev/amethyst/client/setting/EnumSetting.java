package dev.amethyst.client.setting;

import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A single-choice setting cycling through an enum's constants, persisted by
 * the enum constant name (stable across refactors as long as names stay).
 */
public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
	private final Class<E> type;

	public EnumSetting(String id, String name, String description, Class<E> type, E defaultValue) {
		super(id, name, description, defaultValue);
		this.type = type;
	}

	public Class<E> type() {
		return type;
	}

	/** All selectable constants in declaration order. */
	public E[] options() {
		return type.getEnumConstants();
	}

	/** Cycles to the next constant in declaration order. */
	public void cycle() {
		E[] options = options();
		set(options[(get().ordinal() + 1) % options.length]);
	}

	@Override
	protected E validate(E value) {
		if (value == null) {
			return defaultValue();
		}
		for (E option : options()) {
			if (option == value) {
				return value;
			}
		}
		return defaultValue();
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get().name());
	}

	@Override
	public boolean fromJson(JsonElement element) {
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			return false;
		}
		String raw = element.getAsString();
		try {
			set(Enum.valueOf(type, raw));
			return true;
		} catch (IllegalArgumentException e) {
			// tolerate case changes between versions
			for (E option : options()) {
				if (option.name().toLowerCase(Locale.ROOT).equals(raw.toLowerCase(Locale.ROOT))) {
					set(option);
					return true;
				}
			}
			return false;
		}
	}
}
