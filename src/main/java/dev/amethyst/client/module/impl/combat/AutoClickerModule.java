package dev.amethyst.client.module.impl.combat;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.setting.EnumSetting;
import dev.amethyst.client.util.ClientInvoker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Auto Clicker dispatches real attack/use input events with configurable
 * CPS and jitter, mirroring physical clicks (including block breaking).
 */
public final class AutoClickerModule extends Module {
	public enum Mode { LEFT, RIGHT, BOTH}

	private final EnumSetting<Mode> mode = add(new EnumSetting<>("mode", "Mouse Button",
		"Which button to click.", Mode.class, Mode.LEFT));
	private final DoubleSetting minCps = add(new DoubleSetting("min-cps", "Min CPS",
		"Clicks per second lower bound.", 8.0, 1.0, 20.0, 0.5, 1));
	private final DoubleSetting maxCps = add(new DoubleSetting("max-cps", "Max CPS",
		"Clicks per second upper bound (randomization).", 12.0, 1.0, 20.0, 0.5, 1));
	private final BooleanSetting holdOnly = add(new BooleanSetting("hold-only", "Hold Only",
		"Only click while you hold the matching mouse button.", true));
	private final BooleanSetting jitter = add(new BooleanSetting("jitter", "Timing Jitter",
		"Randomize the interval between clicks.", true));

	private long nextClickAt;

	public AutoClickerModule() {
		super("auto-clicker", "Auto Clicker", "Clicks automatically with randomized CPS.",
			ModuleCategory.COMBAT);
	}

	@Override
	public void onEnable() {
		nextClickAt = System.currentTimeMillis();
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.currentScreen != null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now < nextClickAt) {
			return;
		}
		double min = Math.min(minCps.get(), maxCps.get());
		double max = Math.max(minCps.get(), maxCps.get());
		double cps = jitter.get() ? min + Math.random() * (max - min) : min;
		nextClickAt = now + (long) (1000.0 / Math.max(0.5, cps));

		if (client instanceof ClientInvoker invoker) {
			boolean fireLeft = mode.get() != Mode.RIGHT;
			boolean fireRight = mode.get() != Mode.LEFT;
			if (holdOnly.get()) {
				boolean attackHeld = client.options.attackKey.isPressed();
				boolean useHeld = client.options.useKey.isPressed();
				if (fireLeft && attackHeld) {
					invoker.amethyst$doAttack();
				}
				if (fireRight && useHeld) {
					invoker.amethyst$doItemUse();
				}
			} else {
				if (fireLeft) {
					invoker.amethyst$doAttack();
				}
				if (fireRight) {
					invoker.amethyst$doItemUse();
				}
			}
		} else {
			AmethystClient.LOGGER.warn("Auto Clicker could not access the client click entry point.");
		}
	}
}
