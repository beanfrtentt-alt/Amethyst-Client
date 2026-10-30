package dev.amethyst.client.command;

import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.gui.AmethystClickGui;
import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.ColorSetting;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.setting.EnumSetting;
import dev.amethyst.client.setting.IntSetting;
import dev.amethyst.client.setting.MultiOptionSetting;
import dev.amethyst.client.setting.Setting;
import dev.amethyst.client.setting.StringSetting;
import dev.amethyst.client.util.FriendManager;
import dev.amethyst.client.util.NotificationManager;

/**
 * Amethyst commands. Registered both as:
 *
 * <ul>
 *   <li>a chat-prefix command: {@code .am <sub>...} (intercepted before the
 *       server sees the message)</li>
 *   <li>a vanilla-style client command: {@code /amethyst <sub>...}</li>
 *
 * <li>All arguments are validated and errors produce a friendly chat message
 * instead of a crash.</li>
 */
public final class AmethystCommandSystem {
	public static final String PREFIX = ".am";

	private AmethystCommandSystem() {
	}

	/** Wires the chat-prefix interception (call once from the entrypoint). */
	public static void init() {
		ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
			String trimmed = message.trim();
			if (trimmed.equals(PREFIX) || trimmed.startsWith(PREFIX + " ")) {
				try {
					execute(trimmed.substring(PREFIX.length()).trim());
				} catch (Exception e) {
					AmethystClient.LOGGER.warn("Command failed: {}", e.getMessage());
					feedback("Command failed: " + e.getMessage(), 0xFFE8537A);
				}
				return false; // never send Amethyst commands to the server
			}
			return true;
		});
	}

	/** Registers {@code /amethyst} through the Fabric client command API. */
	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("amethyst");
		root.then(ClientCommandManager.literal("gui").executes(ctx -> {
			AmethystClickGui.open();
			return 1;
		}));
		root.then(ClientCommandManager.literal("version").executes(ctx -> {
			feedback("Amethyst Client " + AmethystClient.VERSION + " (Minecraft 1.21.11, Fabric)", 0xFFB388FF);
			return 1;
		}));
		root.then(ClientCommandManager.literal("list").executes(ctx -> {
			execute("list");
			return 1;
		}));
		root.then(ClientCommandManager.literal("toggle")
			.then(ClientCommandManager.argument("module", StringArgumentType.string())
				.executes(ctx -> {
					execute("toggle " + StringArgumentType.getString(ctx, "module"));
					return 1;
				})));
		root.then(ClientCommandManager.literal("info")
			.then(ClientCommandManager.argument("module", StringArgumentType.string())
				.executes(ctx -> {
					execute("info " + StringArgumentType.getString(ctx, "module"));
					return 1;
				})));
		root.then(ClientCommandManager.literal("profile")
			.then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
				.executes(ctx -> {
					execute("profile " + StringArgumentType.getString(ctx, "args"));
					return 1;
				})));
		root.then(ClientCommandManager.literal("friend")
			.then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
				.executes(ctx -> {
					execute("friend " + StringArgumentType.getString(ctx, "args"));
					return 1;
				})));
		root.then(ClientCommandManager.literal("set")
			.then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
				.executes(ctx -> {
					execute("set " + StringArgumentType.getString(ctx, "args"));
					return 1;
				})));
		root.then(ClientCommandManager.literal("bind")
			.then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
				.executes(ctx -> {
					execute("bind " + StringArgumentType.getString(ctx, "args"));
					return 1;
				})));
		root.then(ClientCommandManager.literal("hud").executes(ctx -> {
			dev.amethyst.client.gui.HudEditorScreen.open(null);
			return 1;
		}));
		root.then(ClientCommandManager.literal("reset")
			.then(ClientCommandManager.argument("module", StringArgumentType.string())
				.executes(ctx -> {
					execute("reset " + StringArgumentType.getString(ctx, "module"));
					return 1;
				})));
		root.executes(ctx -> {
			execute("help");
			return 1;
		});
		dispatcher.register(root);
	}

	/** Executes a space-separated argument list starting with the subcommand. */
	public static void execute(String argsLine) {
		String[] args = argsLine.isBlank() ? new String[0] : argsLine.split("\\s+");
		String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
		switch (sub) {
			case "help" -> help();
			case "gui" -> AmethystClickGui.open();
			case "version" -> feedback(
				"Amethyst Client " + AmethystClient.VERSION + " (Minecraft 1.21.11, Fabric)", 0xFFB388FF);
			case "list" -> listModules();
			case "toggle" -> {
				if (args.length < 2) {
					feedback("Usage: " + PREFIX + " toggle <module>", 0xFFE8B453);
					return;
				}
				toggle(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
			}
			case "info" -> {
				if (args.length < 2) {
					feedback("Usage: " + PREFIX + " info <module>", 0xFFE8B453);
					return;
				}
				info(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
			}
			case "set" -> set(args);
			case "bind" -> bind(args);
			case "reset" -> {
				if (args.length < 2) {
					feedback("Usage: " + PREFIX + " reset <module|all>", 0xFFE8B453);
					return;
				}
				reset(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
			}
			case "profile" -> profile(args);
			case "friend" -> friend(args);
			case "hud" -> dev.amethyst.client.gui.HudEditorScreen.open(null);
			default -> feedback("Unknown subcommand '" + sub + "'. Try " + PREFIX + " help", 0xFFE8B453);
		}
	}

	private static void help() {
		feedback("Amethyst " + AmethystClient.VERSION + " commands:", 0xFFB388FF);
		feedback(PREFIX + " help — this list", 0xFF8A84A0);
		feedback(PREFIX + " gui — open the Click GUI", 0xFF8A84A0);
		feedback(PREFIX + " hud — open the HUD editor", 0xFF8A84A0);
		feedback(PREFIX + " list [category] — list modules", 0xFF8A84A0);
		feedback(PREFIX + " toggle <module>", 0xFF8A84A0);
		feedback(PREFIX + " info <module>", 0xFF8A84A0);
		feedback(PREFIX + " set <module> <setting> <value>", 0xFF8A84A0);
		feedback(PREFIX + " bind <module> <key|none>", 0xFF8A84A0);
		feedback(PREFIX + " reset <module|all>", 0xFF8A84A0);
		feedback(PREFIX + " profile list|save|load|export|import ...", 0xFF8A84A0);
		feedback(PREFIX + " friend add|remove|list [name]", 0xFF8A84A0);
	}

	private static void listModules() {
		for (ModuleCategory category : ModuleCategory.values()) {
			var modules = AmethystClient.MODULES.byCategory(category);
			StringBuilder line = new StringBuilder();
			line.append(category.displayName()).append(": ");
			for (int i = 0; i < modules.size(); i++) {
				Module module = modules.get(i);
				if (i > 0) {
					line.append(", ");
				}
				line.append(module.name());
				if (module.isEnabled()) {
					line.append(" [on]");
				}
			}
			feedback(line.toString(), category.accentColor());
		}
	}

	private static void toggle(String name) {
		Module module = find(name);
		if (module == null) {
			feedback("Unknown module '" + name + "'", 0xFFE8537A);
			return;
		}
		if (module.isIncomplete()) {
			feedback(module.name() + " is not implemented in this build.", 0xFFE8B453);
			return;
		}
		module.toggle();
		AmethystClient.MODULES.fireTogglesChanged();
		AmethystClient.CONFIG.markDirty();
		feedback(module.name() + (module.isEnabled() ? " enabled" : " disabled"),
			module.isEnabled() ? 0xFF53E89B : 0xFF8A84A0);
	}

	private static void info(String name) {
		Module module = find(name);
		if (module == null) {
			feedback("Unknown module '" + name + "'", 0xFFE8537A);
			return;
		}
		feedback(module.name() + " (" + module.id() + ") — " + module.category().displayName(),
			module.category().accentColor());
		feedback(module.description(), 0xFF8A84A0);
		feedback("State: " + (module.isEnabled() ? "enabled" : "disabled")
			+ " • Bind: " + dev.amethyst.client.util.KeybindManager.keyName(module.keybind()), 0xFF8A84A0);
		for (Setting<?> setting : module.settings()) {
			feedback("  " + setting.name() + " [" + setting.id() + "] = " + describeValue(setting), 0xFFB0A8C8);
		}
	}

	private static String describeValue(Setting<?> setting) {
		if (setting instanceof BooleanSetting bool) {
			return String.valueOf(bool.get());
		}
		if (setting instanceof DoubleSetting dbl) {
			return String.format("%." + dbl.decimals() + "f", dbl.get());
		}
		if (setting instanceof IntSetting intSetting) {
			return String.valueOf(intSetting.get());
		}
		if (setting instanceof EnumSetting<?> mode) {
			return mode.get().name();
		}
		if (setting instanceof ColorSetting color) {
			return String.format("#%08X", color.get());
		}
		if (setting instanceof MultiOptionSetting multi) {
			return String.join("|", multi.selected());
		}
		return String.valueOf(setting.get());
	}

	private static void set(String[] args) {
		if (args.length < 4) {
			feedback("Usage: " + PREFIX + " set <module> <setting> <value>", 0xFFE8B453);
			return;
		}
		Module module = find(args[1]);
		if (module == null) {
			feedback("Unknown module '" + args[1] + "'", 0xFFE8537A);
			return;
		}
		String settingId = args[2].toLowerCase(Locale.ROOT);
		Setting<?> setting = null;
		for (Setting<?> candidate : module.settings()) {
			if (candidate.id().equalsIgnoreCase(settingId)
				|| candidate.name().equalsIgnoreCase(args[2])) {
				setting = candidate;
				break;
			}
		}
		if (setting == null) {
			feedback("Module '" + module.name() + "' has no setting '" + args[2] + "'", 0xFFE8537A);
			return;
		}
		String value = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
		boolean applied = applyValue(setting, value);
		if (applied) {
			AmethystClient.CONFIG.markDirty();
			feedback(module.name() + "." + setting.name() + " = " + describeValue(setting), 0xFF53E89B);
		} else {
			feedback("Could not set " + setting.name() + " to '" + value + "' (invalid value)", 0xFFE8537A);
		}
	}

	private static boolean applyValue(Setting<?> setting, String raw) {
		try {
			if (setting instanceof BooleanSetting bool) {
				if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
					return false;
				}
				bool.set(Boolean.parseBoolean(raw));
				return true;
			}
			if (setting instanceof DoubleSetting dbl) {
				dbl.set(Double.parseDouble(raw));
				return true;
			}
			if (setting instanceof IntSetting intSetting) {
				intSetting.set(Integer.parseInt(raw));
				return true;
			}
			if (setting instanceof ColorSetting color) {
				raw = raw.replace("#", "");
				color.set((int) Long.parseLong(raw, 16));
				return true;
			}
			if (setting instanceof StringSetting str) {
				str.set(raw);
				return true;
			}
			if (setting instanceof MultiOptionSetting multi) {
				String cleaned = raw.replace("|", ",").replace(" ", ",");
				multi.set(new java.util.LinkedHashSet<>(java.util.Arrays.asList(cleaned.split(","))));
				return true;
			}
			if (setting instanceof EnumSetting<?> mode) {
				for (Enum<?> option : mode.options()) {
					if (option.name().equalsIgnoreCase(raw)) {
						setting.fromJson(com.google.gson.JsonParser.parseString("\"" + option.name() + "\""));
						return true;
					}
				}
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	private static void bind(String[] args) {
		if (args.length < 3) {
			feedback("Usage: " + PREFIX + " bind <module> <key|none>", 0xFFE8B453);
			return;
		}
		Module module = find(args[1]);
		if (module == null) {
			feedback("Unknown module '" + args[1] + "'", 0xFFE8537A);
			return;
		}
		String key = args[2].toLowerCase(Locale.ROOT);
		int keycode = -1;
		if (!key.equals("none") && !key.equals("clear")) {
			keycode = org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;
			// match against GLFW key names via the input util registry
			for (int i = 32; i <= 348; i++) {
				String name = dev.amethyst.client.util.KeybindManager.keyName(i);
				if (name.toLowerCase(Locale.ROOT).equals(key)
					|| name.toLowerCase(Locale.ROOT).equals("key " + key)) {
					keycode = i;
					break;
				}
			}
			if (keycode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
				try {
					keycode = Integer.parseInt(key);
				} catch (NumberFormatException e) {
					feedback("Unknown key '" + args[2] + "'. Use the name shown in the GUI or a raw keycode.",
						0xFFE8537A);
					return;
				}
			}
		}
		module.setKeybind(keycode);
		AmethystClient.CONFIG.markDirty();
		feedback(module.name() + " bound to " + dev.amethyst.client.util.KeybindManager.keyName(keycode), 0xFF53E89B);
	}

	private static void reset(String name) {
		if (name.equalsIgnoreCase("all")) {
			for (Module module : AmethystClient.MODULES.all()) {
				module.resetToDefaults();
			}
			AmethystClient.CONFIG.saveAll();
			feedback("All modules were reset to defaults.", 0xFF53E89B);
			return;
		}
		Module module = find(name);
		if (module == null) {
			feedback("Unknown module '" + name + "'", 0xFFE8537A);
			return;
		}
		module.resetToDefaults();
		AmethystClient.CONFIG.markDirty();
		feedback(module.name() + " was reset to defaults.", 0xFF53E89B);
	}

	private static void profile(String[] args) {
		if (args.length < 2) {
			feedback("Usage: " + PREFIX + " profile list|save <name>|load <name>|export <name>|import <path>",
				0xFFE8B453);
			return;
		}
		try {
			switch (args[1].toLowerCase(Locale.ROOT)) {
				case "list" -> {
					var names = AmethystClient.CONFIG.listProfiles();
					feedback(names.isEmpty() ? "No saved profiles." : "Profiles: " + String.join(", ", names),
						0xFFB0A8C8);
				}
				case "save" -> {
					requireName(args);
					AmethystClient.CONFIG.saveProfile(args[2]);
				}
				case "load" -> {
					requireName(args);
					AmethystClient.CONFIG.loadProfile(args[2]);
				}
				case "export" -> {
					requireName(args);
					var dest = AmethystClient.CONFIG.directory()
						.resolve("exports").resolve(args[2].replaceAll("[^A-Za-z0-9._-]", "") + ".json");
					AmethystClient.CONFIG.exportProfile(args[2], dest);
				}
				case "import" -> {
					if (args.length < 3) {
						feedback("Usage: " + PREFIX + " profile import <path-to-file>", 0xFFE8B453);
						return;
					}
					String path = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
					AmethystClient.CONFIG.importProfile(java.nio.file.Path.of(path));
				}
				default -> feedback("Unknown profile action '" + args[1] + "'", 0xFFE8537A);
			}
		} catch (Exception e) {
			feedback("Profile operation failed: " + e.getMessage(), 0xFFE8537A);
		}
	}

	private static void requireName(String[] args) {
		if (args.length < 3 || dev.amethyst.client.config.ConfigManager.sanitizeProfileName(args[2]).isEmpty()) {
			throw new IllegalArgumentException("Provide a valid profile name (letters, digits, . _ -).");
		}
	}

	private static void friend(String[] args) {
		if (args.length < 2) {
			feedback("Usage: " + PREFIX + " friend add|remove|list [name]", 0xFFE8B453);
			return;
		}
		switch (args[1].toLowerCase(Locale.ROOT)) {
			case "list" -> {
				var friends = FriendManager.all();
				feedback(friends.isEmpty() ? "Friend list is empty." : "Friends: " + String.join(", ", friends),
					0xFF53B4E8);
			}
			case "add" -> {
				if (args.length < 3) {
					feedback("Usage: " + PREFIX + " friend add <name>", 0xFFE8B453);
					return;
				}
				if (FriendManager.add(args[2])) {
					feedback("Added " + args[2] + " to friends.", 0xFF53E89B);
				} else {
					feedback(args[2] + " is already a friend.", 0xFFE8B453);
				}
			}
			case "remove" -> {
				if (args.length < 3) {
					feedback("Usage: " + PREFIX + " friend remove <name>", 0xFFE8B453);
					return;
				}
				if (FriendManager.remove(args[2])) {
					feedback("Removed " + args[2] + " from friends.", 0xFF53E89B);
				} else {
					feedback(args[2] + " is not on the friend list.", 0xFFE8B453);
				}
			}
			default -> feedback("Unknown friend action '" + args[1] + "'", 0xFFE8537A);
		}
	}

	private static Module find(String name) {
		Module module = AmethystClient.MODULES.byId(name);
		if (module != null) {
			return module;
		}
		String needle = name.toLowerCase(Locale.ROOT);
		for (Module candidate : AmethystClient.MODULES.all()) {
			if (candidate.name().toLowerCase(Locale.ROOT).equals(needle)) {
				return candidate;
			}
		}
		for (Module candidate : AmethystClient.MODULES.all()) {
			if (candidate.name().toLowerCase(Locale.ROOT).contains(needle)) {
				return candidate;
			}
		}
		return null;
	}

	private static void feedback(String message, int color) {
		net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		MutableText text = Text.literal(message)
			.fillStyle(net.minecraft.text.Style.EMPTY.withColor(color));
		if (client.inGameHud != null) {
			client.inGameHud.getChatHud().addMessage(text);
		}
		AmethystClient.LOGGER.info("[chat] {}", message);
	}
}
