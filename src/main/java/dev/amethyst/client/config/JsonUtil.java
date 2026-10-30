package dev.amethyst.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/** Shared Gson instance used for all config (de)serialization. */
public final class JsonUtil {
	private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

	private JsonUtil() {
	}

	public static String pretty(JsonElement element) {
		return PRETTY.toJson(element);
	}

	public static String compact(JsonElement element) {
		return COMPACT.toJson(element);
	}

	public static Gson compactGson() {
		return COMPACT;
	}
}
