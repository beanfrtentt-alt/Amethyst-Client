package dev.amethyst.client.setting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * A multi-select setting backed by a fixed option list (for example a set of
 * block ids). Selection is a set of option strings; unknown options loaded
 * from older configs are dropped, and the fixed option list is preserved
 * verbatim so the GUI can render unselected rows too.
 */
public final class MultiOptionSetting extends Setting<Set<String>> {
	private final List<String> options;

	public MultiOptionSetting(String id, String name, String description,
	                           List<String> options, Set<String> defaults) {
		super(id, name, description, new LinkedHashSet<>(defaults));
		this.options = new ArrayList<>(options);
		set(new LinkedHashSet<>(defaults));
	}

	public List<String> options() {
		return options;
	}

	/** Order-stable list of the currently selected options. */
	public List<String> selected() {
		return new ArrayList<>(get());
	}

	public boolean isSelected(String option) {
		return get().contains(option);
	}

	public void toggleOption(String option) {
		Set<String> next = new LinkedHashSet<>(get());
		if (!next.remove(option)) {
			next.add(option);
		}
		set(next);
	}

	public void selectAll() {
		set(new LinkedHashSet<>(options));
	}

	public void clearAll() {
		set(new LinkedHashSet<>());
	}

	@Override
	protected Set<String> validate(Set<String> value) {
		if (value == null) {
			return defaultValue();
		}
		// keep order stable and drop anything not part of the option list
		Set<String> result = new LinkedHashSet<>();
		for (String option : value) {
			if (options.contains(option)) {
				result.add(option);
			}
		}
		return result;
	}

	@Override
	public JsonElement toJson() {
		JsonArray array = new JsonArray();
		for (String option : get()) {
			array.add(option);
		}
		return array;
	}

	@Override
	public boolean fromJson(JsonElement element) {
		if (element == null || !element.isJsonArray()) {
			return false;
		}
		Set<String> loaded = new LinkedHashSet<>();
		for (JsonElement entry : element.getAsJsonArray()) {
			if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
				loaded.add(entry.getAsString());
			}
		}
		set(loaded);
		return true;
	}
}
