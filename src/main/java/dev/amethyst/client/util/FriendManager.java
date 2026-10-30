package dev.amethyst.client.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.amethyst.client.AmethystClient;

/**
 * Friend list used by combat modules (never attack friends), ESP color groups
 * and commands. Persisted as a simple JSON array of names.
 */
public final class FriendManager {
	private static final Set<String> friends = new LinkedHashSet<>();
	private static Path file;

	private FriendManager() {
	}

	public static void init(Path directory) {
		file = directory.resolve("friends.json");
		load();
	}

	public static void load() {
		friends.clear();
		if (file == null || !Files.exists(file)) {
			return;
		}
		try {
			JsonElement root = JsonParser.parseString(Files.readString(file));
			if (root.isJsonArray()) {
				for (JsonElement entry : root.getAsJsonArray()) {
					if (entry.isJsonPrimitive()) {
						friends.add(normalize(entry.getAsString()));
					}
				}
			}
		} catch (Exception e) {
			AmethystClient.LOGGER.warn("Could not read friends.json ({}); starting with an empty friend list",
				e.getMessage());
		}
	}

	public static void save() {
		if (file == null) {
			return;
		}
		try {
			Files.createDirectories(file.getParent());
			JsonArray array = new JsonArray();
			for (String friend : friends) {
				array.add(friend);
			}
			Files.writeString(file, dev.amethyst.client.config.JsonUtil.pretty(array));
		} catch (IOException e) {
			AmethystClient.LOGGER.warn("Could not save friends.json: {}", e.getMessage());
		}
	}

	public static boolean add(String name) {
		boolean added = friends.add(normalize(name));
		if (added) {
			save();
		}
		return added;
	}

	public static boolean remove(String name) {
		boolean removed = friends.remove(normalize(name));
		if (removed) {
			save();
		}
		return removed;
	}

	public static boolean isFriend(String name) {
		return friends.contains(normalize(name));
	}

	public static Set<String> all() {
		return Collections.unmodifiableSet(friends);
	}

	public static boolean isFriendOrSelf(String name, String selfName) {
		return normalize(selfName).equals(normalize(name)) || isFriend(name);
	}

	public static void setAll(Collection<String> names) {
		friends.clear();
		for (String name : names) {
			friends.add(normalize(name));
		}
		save();
	}

	private static String normalize(String name) {
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}
}
