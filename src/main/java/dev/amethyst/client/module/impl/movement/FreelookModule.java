package dev.amethyst.client.module.impl.movement;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.util.CameraState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Freelook lets you look around while your player (and the server) keep
 * facing the original direction. Bind it as a hold key for the classic
 * third-person peek behavior.
 */
public final class FreelookModule extends Module {
	public FreelookModule() {
		super("freelook", "Freelook", "Look around freely without turning your player.",
			ModuleCategory.MOVEMENT);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			setEnabled(false);
			return;
		}
		CameraState.enableFreelook();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		CameraState.disableFreelook();
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			setEnabled(false);
		}
	}
}
