package dev.amethyst.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import dev.amethyst.client.command.AmethystCommandSystem;
import dev.amethyst.client.config.ConfigManager;
import dev.amethyst.client.config.ModuleConfigSection;
import dev.amethyst.client.gui.AmethystClickGui;
import dev.amethyst.client.hud.HudManager;
import dev.amethyst.client.module.ModuleRegistrar;
import dev.amethyst.client.module.ModuleManager;
import dev.amethyst.client.render.RenderUtil;
import dev.amethyst.client.util.FriendManager;
import dev.amethyst.client.util.KeybindManager;
import dev.amethyst.client.util.NotificationManager;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Amethyst Client entrypoint. Owns the single instances of every subsystem
 * and wires the Fabric events exactly once.
 */
public final class AmethystClient implements ClientModInitializer {
	public static final String MOD_ID = "amethyst";
	public static final String VERSION = "1.0.0";
	public static final Logger LOGGER = LoggerFactory.getLogger("Amethyst");

	public static ModuleManager MODULES;
	public static ConfigManager CONFIG;
	public static KeybindManager KEYBINDS;

	private static KeyBinding openGuiKey;
	private static boolean initialized = false;

	@Override
	public void onInitializeClient() {
		if (initialized) {
			return;
		}
		initialized = true;

		// --- core singletons ---
		MODULES = new ModuleManager();
		MODULES.registerAll(ModuleRegistrar.constructAll());

		var configDir = net.fabricmc.loader.api.FabricLoader.getInstance()
			.getConfigDir().resolve("amethyst");
		CONFIG = new ConfigManager(configDir);
		CONFIG.registerSection("modules", new ModuleConfigSection(MODULES));

		FriendManager.init(CONFIG.directory());

		HudManager.init(CONFIG);

		CONFIG.loadAll();

		KEYBINDS = new KeybindManager(MODULES);

		// --- keybindings (vanilla options screen shows them) ---
		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.amethyst.open_gui", InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT, net.minecraft.client.option.KeyBinding.Category.create(net.minecraft.util.Identifier.of("amethyst", "general"))));

		// --- ticks ---
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (KEYBINDS != null) {
				KEYBINDS.tick();
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			MODULES.tick();
			NotificationManager.tick();
			HudManager.tick();
			if (CONFIG != null) {
				CONFIG.tick();
			}
			while (openGuiKey.wasPressed()) {
				AmethystClickGui.open();
			}
		});

		// --- world rendering ---
		WorldRenderEvents.END_MAIN.register(context -> {
			try {
				MinecraftClient client = MinecraftClient.getInstance();
				RenderUtil.FRAME.matrices = context.matrices();
				RenderUtil.FRAME.cameraPos = client.gameRenderer.getCamera().getCameraPos();
				RenderUtil.FRAME.tickDelta = 0f;
				MODULES.render3D();
				RenderUtil.flush();
			} catch (Exception e) {
				LOGGER.warn("World overlay rendering failed", e);
			} finally {
				RenderUtil.FRAME.matrices = null;
			}
		});

		// --- HUD ---
		HudElementRegistry.addLast(VanillaHudElements.MISC_OVERLAYS,
			(drawContext, renderTickCounter) -> {
				HudManager.render(drawContext);
				MODULES.hudRender(drawContext);
			});

		// --- commands ---
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			AmethystCommandSystem.register(dispatcher));
		AmethystCommandSystem.init();

		// --- shutdown ---
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());

		LOGGER.info("Amethyst Client {} initialized ({} modules)", VERSION, MODULES.all().size());
		NotificationManager.post("Amethyst Client " + VERSION + " loaded — press Right Shift",
			NotificationManager.Type.INFO, 5000);
	}

	public static void shutdown() {
		try {
			MODULES.shutdown();
		} catch (Exception e) {
			LOGGER.warn("Module shutdown error: {}", e.getMessage());
		}
		try {
			CONFIG.saveAll();
		} catch (Exception e) {
			LOGGER.warn("Config save error: {}", e.getMessage());
		}
		LOGGER.info("Amethyst Client shut down cleanly.");
	}
}
