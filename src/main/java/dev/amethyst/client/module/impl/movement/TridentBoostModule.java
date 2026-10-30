package dev.amethyst.client.module.impl.movement;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.DoubleSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Trident Boost amplifies the riptide velocity of a thrown trident while you
 * are being launched by it.
 */
public final class TridentBoostModule extends Module {
	private final DoubleSetting boost = add(new DoubleSetting("boost", "Boost",
		"Velocity multiplier while riptiding.", 1.5, 1.0, 3.0, 0.1, 1));

	public TridentBoostModule() {
		super("trident-boost", "Trident Boost", "Boosts riptide launch velocity.", ModuleCategory.MOVEMENT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || !player.isUsingRiptide()) {
			return;
		}
		var velocity = player.getVelocity();
		player.setVelocity(velocity.x * boost.get(), velocity.y, velocity.z * boost.get());
	}
}
