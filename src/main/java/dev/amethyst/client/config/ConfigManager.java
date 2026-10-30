package dev.amethyst.client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.util.NotificationManager;

/**
 * JSON configuration persistence with:
 *
 * <ul>
 *   <li>named sections (modules, hud, gui, ...) supplied by their owning systems</li>
 *   <li>atomic writes (temp file + move) so a crash can never corrupt a config</li>
 *   <li>debounced autosaving to avoid pointless disk writes every tick</li>
 *   <li>malformed-file quarantine (renamed, not deleted) and default recovery</li>
 *   <li>named profiles bundling every section, with validation on import</li>
 *   <li>round-tripping of unknown keys so new versions never lose user data</li>
 * </ul>
 *
 * Nothing here ever evaluates config content as code; JSON is data only.
 */
public final class ConfigManager {
	public interface Section {
		/** Serialize the current state of this section to a JSON object. */
		JsonObject toJson();

		/** Apply a loaded JSON object to this section (must tolerate junk). */
		void apply(JsonObject object);
	}

	public static final int SCHEMA_VERSION = 1;
	private static final long AUTOSAVE_INTERVAL_MILLIS = 15_000;

	private final Map<String, Section> sections = new LinkedHashMap<>();
	private final Path directory;
	private final Path profilesDirectory;
	private long dirtySince = -1;
	private long lastSave = 0;

	public ConfigManager(Path directory) {
		this.directory = directory;
		this.profilesDirectory = directory.resolve("profiles");
	}

	public void registerSection(String id, Section section) {
		sections.put(id, section);
	}

	public Path directory() {
		return directory;
	}

	public Path profilesDirectory() {
		return profilesDirectory;
	}

	// ------------------------------------------------------------ loading

	public void loadAll() {
		try {
			Files.createDirectories(directory);
			Files.createDirectories(profilesDirectory);
		} catch (IOException e) {
			AmethystClient.LOGGER.error("Cannot create config directory {}", directory, e);
			return;
		}
		for (String id : sections.keySet()) {
			loadSection(id);
		}
	}

	private void loadSection(String id) {
		Path file = fileFor(id);
		if (!Files.exists(file)) {
			return; // defaults already apply
		}
		String raw;
		try {
			raw = Files.readString(file);
		} catch (IOException e) {
			AmethystClient.LOGGER.warn("Cannot read {}: {}", file, e.getMessage());
			return;
		}
		JsonObject root = parseOrQuarantine(raw, file, id);
		if (root == null) {
			return;
		}
		applySection(id, root);
		AmethystClient.LOGGER.info("Loaded config section '{}' from {}", id, file.getFileName());
	}

	private void applySection(String id, JsonObject root) {
		// unwrap {"version":1,"<id>":{...}} or accept the bare section object
		JsonObject section = root;
		if (root.has(id) && root.get(id).isJsonObject()) {
			section = root.getAsJsonObject(id);
		}
		try {
			sections.get(id).apply(section);
		} catch (Exception e) {
			AmethystClient.LOGGER.error("Failed to apply config section '{}'", id, e);
			NotificationManager.warn("Config section '" + id + "' had an error; some defaults were used.");
		}
	}

	private JsonObject parseOrQuarantine(String raw, Path file, String id) {
		try {
			JsonElement parsed = JsonParser.parseString(raw);
			if (!parsed.isJsonObject()) {
				throw new IllegalStateException("root is not a JSON object");
			}
			return parsed.getAsJsonObject();
		} catch (Exception e) {
			String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
			Path broken = file.resolveSibling(file.getFileName() + ".broken-" + stamp);
			try {
				Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
				AmethystClient.LOGGER.warn(
					"Config file {} was invalid ({}); moved to {} and using defaults",
					file.getFileName(), e.getMessage(), broken.getFileName());
				NotificationManager.warn("Invalid " + file.getFileName() + " was backed up; defaults restored.");
			} catch (IOException io) {
				AmethystClient.LOGGER.error("Could not quarantine broken config file {}", file, io);
			}
			return null;
		}
	}

	private Path fileFor(String id) {
		return directory.resolve(id + ".json");
	}

	// ------------------------------------------------------------ saving

	/** Marks the config dirty; an internal autosave flushes it shortly after. */
	public void markDirty() {
		dirtySince = System.currentTimeMillis();
	}

	public boolean isDirty() {
		return dirtySince >= 0;
	}

	/** Called once per client tick; saves at most every few seconds when dirty. */
	public void tick() {
		if (dirtySince < 0) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - dirtySince >= 1000 && now - lastSave >= AUTOSAVE_INTERVAL_MILLIS) {
			saveAll();
		}
	}

	/** Saves every section immediately. */
	public void saveAll() {
		boolean allSaved = true;
		for (String id : sections.keySet()) {
			try {
				if (!saveSection(id)) {
					allSaved = false;
				}
			} catch (RuntimeException e) {
				allSaved = false;
				AmethystClient.LOGGER.error("Could not serialize config section '{}'", id, e);
			}
		}
		lastSave = System.currentTimeMillis();
		// Keep the dirty flag when any write fails so a later tick retries it.
		if (allSaved) {
			dirtySince = -1;
		} else if (dirtySince < 0) {
			dirtySince = lastSave;
		}
	}

	private boolean saveSection(String id) {
		JsonObject section = sections.get(id).toJson();
		JsonObject root = new JsonObject();
		root.addProperty("version", SCHEMA_VERSION);
		root.add(id, section);
		return writeAtomically(fileFor(id), JsonUtil.pretty(root));
	}

	// ------------------------------------------------------------ profiles

	/** Bundles every section into one profile JSON object. */
	public JsonObject buildProfile(String name, String description) {
		JsonObject profile = new JsonObject();
		profile.addProperty("amethystProfile", true);
		profile.addProperty("version", SCHEMA_VERSION);
		profile.addProperty("name", name);
		if (description != null && !description.isBlank()) {
			profile.addProperty("description", description);
		}
		profile.addProperty("createdAt", LocalDateTime.now().toString());
		for (Map.Entry<String, Section> entry : sections.entrySet()) {
			profile.add(entry.getKey(), entry.getValue().toJson());
		}
		return profile;
	}

	public List<String> listProfiles() {
		List<String> names = new ArrayList<>();
		try (var paths = Files.list(profilesDirectory)) {
			paths.filter(Files::isRegularFile).forEach(path -> {
				String fileName = path.getFileName().toString();
				if (fileName.endsWith(".json")) {
					names.add(fileName.substring(0, fileName.length() - ".json".length()));
				}
			});
		} catch (IOException e) {
			AmethystClient.LOGGER.warn("Cannot list profiles: {}", e.getMessage());
		}
		names.sort(String::compareToIgnoreCase);
		return names;
	}

	public void saveProfile(String name) throws IOException {
		String safe = sanitizeProfileName(name);
		if (safe.isEmpty()) {
			throw new IOException("Profile name is empty.");
		}
		Files.createDirectories(profilesDirectory);
		if (!writeAtomically(profilesDirectory.resolve(safe + ".json"),
			JsonUtil.pretty(buildProfile(safe, null)))) {
			throw new IOException("Could not write profile '" + safe + "'.");
		}
		NotificationManager.success("Profile '" + safe + "' saved.");
	}

	/** Loads a profile by name, validating it before anything is applied. */
	public void loadProfile(String name) throws IOException {
		String safe = sanitizeProfileName(name);
		if (safe.isEmpty()) {
			throw new IOException("Profile name is empty or invalid.");
		}
		Path file = profilesDirectory.resolve(safe + ".json");
		if (!Files.exists(file)) {
			throw new IOException("Profile '" + name + "' does not exist.");
		}
		JsonObject profile = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
		applyProfile(profile, name);
		NotificationManager.success("Profile '" + name + "' loaded.");
		saveAll();
	}

	/** Validates then applies an arbitrary profile JSON (import path). */
	public void applyProfile(JsonObject profile, String label) throws IOException {
		if (profile == null || !profile.isJsonObject()) {
			throw new IOException("Not a JSON object.");
		}
		if (!profile.has("amethystProfile") || !profile.get("amethystProfile").getAsBoolean()) {
			throw new IOException("Missing amethystProfile marker.");
		}
		if (profile.has("version") && profile.get("version").getAsInt() > SCHEMA_VERSION) {
			throw new IOException("Profile was created by a newer Amethyst version (schema "
				+ profile.get("version").getAsInt() + " > " + SCHEMA_VERSION + ").");
		}
		int applied = 0;
		for (String id : sections.keySet()) {
			if (profile.has(id) && profile.get(id).isJsonObject()) {
				applySection(id, profile.getAsJsonObject(id));
				applied++;
			}
		}
		if (applied == 0) {
			throw new IOException("Profile contains no known sections.");
		}
		AmethystClient.LOGGER.info("Applied profile '{}' ({} sections)", label, applied);
	}

	public void exportProfile(String name, Path destination) throws IOException {
		String safe = sanitizeProfileName(name);
		if (safe.isEmpty()) {
			throw new IOException("Profile name is empty or invalid.");
		}
		Path parent = destination.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		if (!writeAtomically(destination, JsonUtil.pretty(buildProfile(safe, "export")))) {
			throw new IOException("Could not export profile to " + destination);
		}
		NotificationManager.success("Exported profile to " + destination);
	}

	public void importProfile(Path source) throws IOException {
		if (!Files.exists(source)) {
			throw new IOException("File does not exist: " + source);
		}
		JsonObject profile = JsonParser.parseString(Files.readString(source)).getAsJsonObject();
		applyProfile(profile, source.getFileName().toString());
		saveAll();
		NotificationManager.success("Imported profile from " + source.getFileName());
	}

	public static String sanitizeProfileName(String name) {
		return name == null ? "" : name.replaceAll("[^A-Za-z0-9._-]", "").trim();
	}

	// ------------------------------------------------------------ io

	private boolean writeAtomically(Path target, String content) {
		Path absoluteTarget = target.toAbsolutePath();
		Path parent = absoluteTarget.getParent();
		Path temp = absoluteTarget.resolveSibling(absoluteTarget.getFileName() + ".tmp");
		try {
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Files.writeString(temp, content);
			try {
				Files.move(temp, absoluteTarget, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException atomicUnsupported) {
				Files.move(temp, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			AmethystClient.LOGGER.error("Could not save {}", target, e);
			return false;
		}
	}
}
