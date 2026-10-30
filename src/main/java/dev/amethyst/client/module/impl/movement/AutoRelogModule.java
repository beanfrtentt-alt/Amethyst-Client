package dev.amethyst.client.module.impl.movement;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.setting.IntSetting;
import dev.amethyst.client.util.NotificationManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/**
 * Auto Relog disconnects automatically when you fall below a configured Y
 * level, then attempts to reconnect after a short delay. If the client cannot
 * recover the server address, it disconnects only and tells you why.
 */
public final class AutoRelogModule extends Module {
	private final DoubleSetting yLevel = add(new DoubleSetting("y-level", "Y Level",
		"Disconnect when below this Y coordinate.", -3.0, -64.0, 320.0, 1.0, 0));
	private final IntSetting delay = add(new IntSetting("delay", "Reconnect Delay",
		"Seconds to wait before reconnecting.", 3, 1, 30));

	private int reconnectIn = -1;
	private boolean triggered;
	private ServerInfo serverInfo;

	public AutoRelogModule() {
		super("auto-relog", "Auto Relog",
			"Auto-disconnect below a Y level and try to reconnect.", ModuleCategory.MOVEMENT);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		reconnectIn = -1;
		triggered = false;
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player != null && player.getY() >= yLevel.get()) {
			triggered = false;
		}
		if (!triggered && player != null && player.getY() < yLevel.get() && client.world != null
			&& client.getCurrentServerEntry() != null) {
			serverInfo = client.getCurrentServerEntry();
			triggered = true;
			NotificationManager.warn("Auto relog: below Y" + yLevel.get() + ", disconnecting.");
			client.disconnect(Text.literal("Auto relog"));
			reconnectIn = delay.get() * 20;
			return;
		}
		if (reconnectIn > 0) {
			reconnectIn--;
			return;
		}
		if (reconnectIn == 0) {
			reconnectIn = -1;
			reconnect(client);
		}
	}

	private void reconnect(MinecraftClient client) {
		if (serverInfo == null || serverInfo.address == null || serverInfo.address.isBlank()) {
			NotificationManager.warn("Auto relog: no server address known, cannot reconnect.");
			return;
		}
		try {
			ServerAddress address = ServerAddress.parse(serverInfo.address);
			ConnectScreen.connect(null, client, address, serverInfo, false, null);
			NotificationManager.info("Auto relog: reconnecting to " + serverInfo.address + "...");
		} catch (Exception e) {
			NotificationManager.error("Auto relog failed: " + e.getMessage());
		}
	}
}
