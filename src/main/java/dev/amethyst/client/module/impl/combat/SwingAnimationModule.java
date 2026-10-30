package dev.amethyst.client.module.impl.combat;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.EnumSetting;

/**
 * Swing Animation restyles your own attack swing (checked via Hooks from the
 * LivingEntity mixin; only your own player is affected).
 */
public final class SwingAnimationModule extends Module {
	public enum Style { DEFAULT, SMOOTH, FAST }

	private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style",
		"SMOOTH draws a longer, floatier swing; FAST snaps quickly.", Style.class, Style.SMOOTH));

	public SwingAnimationModule() {
		super("swing-animation", "Swing Animation", "Restyles your own attack swing animation.",
			ModuleCategory.COMBAT);
	}

	/** Called by Hooks for the local player only. */
	public int adjustSelfDuration(int vanillaDuration) {
		return switch (style.get()) {
			case SMOOTH -> Math.round(vanillaDuration * 1.75f);
			case FAST -> Math.max(1, Math.round(vanillaDuration * 0.5f));
			case DEFAULT -> vanillaDuration;
		};
	}
}
