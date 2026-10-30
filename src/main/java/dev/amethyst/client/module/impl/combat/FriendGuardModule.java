package dev.amethyst.client.module.impl.combat;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;

/**
 * Friend Guard blocks every attack against a friended player — vanilla
 * clicks and module attacks both go through the same client choke point
 * (ClientPlayerInteractionManager#attackEntity), where the mixin consults
 * this module through Hooks#shouldCancelAttack.
 */
public final class FriendGuardModule extends Module {
	private final BooleanSetting notify = add(new BooleanSetting("notify", "Notify",
		"Show a notification when an attack is blocked.", true));

	public FriendGuardModule() {
		super("friend-guard", "Friend Guard", "Never attack players on your friend list.", ModuleCategory.COMBAT);
	}

	public boolean notifyOn() {
		return notify.get();
	}
}
