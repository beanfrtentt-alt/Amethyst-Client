package dev.amethyst.client.module.impl.combat;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.EnumSetting;
import dev.amethyst.client.util.Hooks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * W-Tap resets your sprint on every hit so knock-back registers stronger.
 * Modes: W Tap (classic re-tap), S Tap (brief back-tap) or Off.
 */
public final class WTapModule extends Module {
	public enum Mode { OFF, W_TAP, S_TAP }

	private final EnumSetting<Mode> mode = add(new EnumSetting<>("mode", "Mode",
		"Sprint reset style. W Tap stops and re-starts sprint; S Tap briefly moves backwards.",
		Mode.class, Mode.W_TAP));

	private boolean resetting;
	private int resetTicks;

	public WTapModule() {
		super("w-tap", "W-Tap", "Resets sprinting on every hit for stronger knock-back.", ModuleCategory.COMBAT);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		Hooks.addAttackListener(listener);
	}

	@Override
	public void onDisable() {
		Hooks.removeAttackListener(listener);
	}

	private final Hooks.AttackListener listener = new Hooks.AttackListener() {
		@Override
		public void onAttack(PlayerEntity attacker, Entity target) {
			if (mode.get() != Mode.OFF && attacker == MinecraftClient.getInstance().player) {
				resetting = true;
				resetTicks = 0;
			}
		}
	};

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
		if (!resetting) {
			return;
		}
		var player = client.player;
		switch (mode.get()) {
			case W_TAP -> {
				// stop sprint for two ticks, then let vanilla re-sprint handle it
				player.setSprinting(false);
				resetTicks++;
				if (resetTicks >= 2) {
					resetting = false;
					player.setSprinting(true);
				}
			}
			case S_TAP -> {
				player.setSprinting(false);
				player.input.playerInput = new net.minecraft.util.PlayerInput(
					false, true, false, false, false, false, false);
				resetTicks++;
				if (resetTicks >= 2) {
					resetting = false;
					player.input.playerInput = new net.minecraft.util.PlayerInput(
						true, false, false, false, false, false, false);
					player.setSprinting(true);
				}
			}
			default -> resetting = false;
		}
	}
}
