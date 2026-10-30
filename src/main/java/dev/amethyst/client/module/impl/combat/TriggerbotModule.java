package dev.amethyst.client.module.impl.combat;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.DoubleSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.Hand;

/**
 * Triggerbot attacks whatever is under the crosshair after a configurable
 * reaction delay. Attacking players is opt-in and disabled by default.
 */
public final class TriggerbotModule extends Module {
	private final DoubleSetting reaction = add(new DoubleSetting("reaction", "Reaction Delay",
		"Delay in tenths of a tick after a target appears.", 3.0, 0.0, 20.0, 0.5, 0));
	private final BooleanSetting players = add(new BooleanSetting("players", "Attack Players",
		"Allow attacking other players. Off by default.", false));
	private final BooleanSetting mobs = add(new BooleanSetting("mobs", "Attack Mobs",
		"Allow attacking hostile and neutral mobs.", true));
	private final BooleanSetting animals = add(new BooleanSetting("animals", "Attack Animals",
		"Allow attacking passive animals.", false));
	private final DoubleSetting cooldown = add(new DoubleSetting("cooldown", "Min Cooldown",
		"Wait until the vanilla attack cooldown has charged this much (0-1).", 1.0, 0.2, 1.0, 0.05, 2));

	private int delayTicks;

	public TriggerbotModule() {
		super("triggerbot", "Triggerbot", "Automatically attacks the entity under your crosshair.",
			ModuleCategory.COMBAT);
	}

	@Override
	public void onEnable() {
		delayTicks = 0;
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.interactionManager == null) {
			return;
		}
		Entity target = client.crosshairTarget instanceof EntityHitResult hit ? hit.getEntity() : null;
		if (target == null || !allowed(target)) {
			delayTicks = 0;
			return;
		}
		if (delayTicks < reaction.get()) {
			delayTicks++;
			return;
		}
		if (player.getAttackCooldownProgress(0f) < cooldown.get()) {
			return;
		}
		client.interactionManager.attackEntity(player, target);
		player.swingHand(Hand.MAIN_HAND);
		delayTicks = 0;
	}

	private boolean allowed(Entity entity) {
		if (!entity.isAlive() || !(entity instanceof LivingEntity)) {
			return false;
		}
		if (dev.amethyst.client.util.FriendManager.isFriend(entity.getName().getString())) {
			return false;
		}
		if (entity instanceof PlayerEntity) {
			return players.get();
		}
		if (entity instanceof net.minecraft.entity.mob.Monster) {
			return mobs.get();
		}
		if (entity instanceof net.minecraft.entity.passive.PassiveEntity) {
			return animals.get();
		}
		return mobs.get();
	}
}
