package dev.amethyst.client.module.impl.movement;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.EnumSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Speed enforces a client-side horizontal speed limit boost while moving.
 * Vanilla mode only smooths small slowdowns; Strafe multiplies sprint speed
 * when strafing (always leaves knock-back legit movement shapes alone).
 */
public final class SpeedModule extends Module {
	public enum Mode { VANILLA, STRAFE }

	private final EnumSetting<Mode> mode = add(new EnumSetting<>("mode", "Mode",
		"VANILLA applies a small constant boost; STRAFE boosts while strafing.",
		Mode.class, Mode.VANILLA));
	private final DoubleSetting strafeSpeed = add(new DoubleSetting("strafe-speed", "Strafe Boost",
		"Strafe speed multiplier.", 1.3, 1.0, 2.0, 0.05, 2));
	private final DoubleSetting boost = add(new DoubleSetting("boost", "Boost",
		"Vanilla-mode speed multiplier.", 1.15, 1.0, 2.0, 0.05, 2));
	private final BooleanSetting requireGround = add(new BooleanSetting("only-on-ground", "Only On Ground",
		"Apply the boost only while on the ground.", false));

	public SpeedModule() {
		super("speed", "Speed", "Client-side horizontal speed boost while moving.", ModuleCategory.MOVEMENT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || player.input == null) {
			return;
		}
		if (requireGround.get() && !player.isOnGround()) {
			return;
		}
		var movement = player.input.getMovementInput();
		if (movement.x == 0 && movement.y == 0) {
			return; // not moving; nothing to boost
		}
		double multiplier = switch (mode.get()) {
			case VANILLA -> boost.get();
			case STRAFE -> movement.x != 0 && movement.y != 0 ? strafeSpeed.get() : 1.0;
		};
		if (multiplier <= 1.0) {
			return;
		}
		// scale current horizontal velocity; vertical motion is untouched
		double vx = player.getVelocity().x * multiplier;
		double vz = player.getVelocity().z * multiplier;
		player.setVelocity(vx, player.getVelocity().y, vz);
	}
}
