package dev.amethyst.client.module.impl.combat;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.DoubleSetting;

/**
 * Slow Swing scales the visible arm-swing duration for every living entity
 * (checked via Hooks from the LivingEntity mixin).
 */
public final class SlowSwingModule extends Module {
	private final DoubleSetting speed = add(new DoubleSetting("speed", "Swing Speed",
		"Multiplier applied to swing duration. Lower = slower.", 1.0, 0.1, 3.0, 0.05, 2));

	public SlowSwingModule() {
		super("slow-swing", "Slow Swing", "Slows down or speeds up visible swing animations.",
			ModuleCategory.COMBAT);
	}

	public double swingSpeed() {
		return speed.get();
	}
}
