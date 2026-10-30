package dev.amethyst.client.module.impl.combat;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.DoubleSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Kill Aura selects a target around you and attacks it when your attack
 * cooldown is charged. Attacking players is opt-in (off by default) and
 * friends are never targeted.
 */
public final class KillAuraModule extends Module {
	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Attack range in blocks.", 3.0, 1.0, 6.0, 0.1, 1));
	private final BooleanSetting players = add(new BooleanSetting("players", "Attack Players",
		"Target other players. Off by default.", false));
	private final BooleanSetting hostiles = add(new BooleanSetting("hostiles", "Attack Hostile Mobs",
		"Target hostile mobs.", true));
	private final BooleanSetting passives = add(new BooleanSetting("passives", "Attack Passive Mobs",
		"Target animals. Off by default.", false));
	private final BooleanSetting rotate = add(new BooleanSetting("rotate", "Face Target",
		"Turn toward the target before attacking.", true));
	private final BooleanSetting requireCursor = add(new BooleanSetting("cursor-check", "Cursor Check",
		"Only attack entities the crosshair can reach (legit mode).", true));
	private final DoubleSetting cooldown = add(new DoubleSetting("cooldown", "Min Cooldown",
		"Vanilla attack cooldown charge required (0-1).", 1.0, 0.2, 1.0, 0.05, 2));

	public KillAuraModule() {
		super("kill-aura", "Kill Aura", "Automatically attacks a selected target in range.", ModuleCategory.COMBAT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || client.interactionManager == null) {
			return;
		}
		Entity target = chooseTarget(client, player);
		if (target == null) {
			return;
		}
		if (requireCursor.get()) {
			// only attack when the crosshair actually overlaps the target hitbox
			if (!(client.crosshairTarget instanceof EntityHitResult hit) || hit.getEntity() != target) {
				return;
			}
		}
		if (rotate.get()) {
			Vec3d center = target.getBoundingBox().getCenter();
			Vec3d eyes = player.getEyePos();
			float yaw = (float) (Math.toDegrees(Math.atan2(center.z - eyes.z, center.x - eyes.x)) - 90.0);
			float pitch = (float) -Math.toDegrees(Math.atan2(center.y - eyes.y,
				Math.hypot(center.x - eyes.x, center.z - eyes.z)));
			player.setYaw(yaw);
			player.setPitch(net.minecraft.util.math.MathHelper.clamp(pitch, -90f, 90f));
		}
		if (player.getAttackCooldownProgress(0f) < cooldown.get()) {
			return;
		}
		client.interactionManager.attackEntity(player, target);
		player.swingHand(Hand.MAIN_HAND);
	}

	private Entity chooseTarget(MinecraftClient client, ClientPlayerEntity player) {
		Entity best = null;
		double bestDistance = range.get() * range.get();
		for (Entity entity : client.world.getEntities()) {
			if (entity == player || !(entity instanceof LivingEntity) || !entity.isAlive()) {
				continue;
			}
			if (entity instanceof PlayerEntity && !players.get()) {
				continue;
			}
			if (entity instanceof Monster && !hostiles.get()) {
				continue;
			}
			if (entity instanceof PassiveEntity && !passives.get()) {
				continue;
			}
			if (dev.amethyst.client.util.FriendManager.isFriend(entity.getName().getString())) {
				continue;
			}
			double distance = entity.squaredDistanceTo(player);
			if (distance <= bestDistance) {
				bestDistance = distance;
				best = entity;
			}
		}
		return best;
	}
}
