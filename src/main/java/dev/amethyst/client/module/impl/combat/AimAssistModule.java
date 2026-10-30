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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Aim Assist gently pulls the crosshair toward the nearest valid target while
 * you are actively aiming near it. It only nudges rotation, never snaps.
 */
public final class AimAssistModule extends Module {
	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Maximum distance to targets in blocks.", 3.2, 1.0, 6.0, 0.1, 1));
	private final DoubleSetting fov = add(new DoubleSetting("fov", "FOV",
		"How far off the crosshair a target may be (degrees).", 40.0, 5.0, 180.0, 1.0, 0));
	private final DoubleSetting speed = add(new DoubleSetting("speed", "Speed",
		"How fast the assist pulls (higher = stronger).", 6.0, 1.0, 20.0, 0.5, 0));
	private final BooleanSetting requireAttackKey = add(new BooleanSetting("while-attacking", "Only While Attacking",
		"Assist only while the attack key is held.", true));
	private final BooleanSetting includePlayers = add(new BooleanSetting("players", "Aim At Players",
		"Allow assisting toward other players. Off by default.", false));

	public AimAssistModule() {
		super("aim-assist", "Aim Assist", "Smoothly pulls your crosshair toward nearby targets.", ModuleCategory.COMBAT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return;
		}
		if (requireAttackKey.get() && !client.options.attackKey.isPressed()) {
			return;
		}
		Entity best = findTarget(client, player);
		if (best == null) {
			return;
		}
		Vec3d target = best.getBoundingBox().getCenter();
		Vec3d eyes = player.getEyePos();
		double dx = target.x - eyes.x;
		double dy = target.y - eyes.y;
		double dz = target.z - eyes.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float targetYaw = (float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90f;
		float targetPitch = (float) (-(MathHelper.atan2(dy, horizontal) * MathHelper.DEGREES_PER_RADIAN));
		float strength = (float) (speed.get() / 100.0);
		float yawDelta = MathHelper.wrapDegrees(targetYaw - player.getYaw());
		float pitchDelta = MathHelper.wrapDegrees(targetPitch - player.getPitch());
		player.setYaw(player.getYaw() + yawDelta * strength);
		player.setPitch(MathHelper.clamp(player.getPitch() + pitchDelta * strength, -90f, 90f));
	}

	private Entity findTarget(MinecraftClient client, ClientPlayerEntity player) {
		Entity best = null;
		double bestScore = Double.MAX_VALUE;
		for (Entity entity : client.world.getEntities()) {
			if (!isValid(entity, player, client)) {
				continue;
			}
			double distance = entity.squaredDistanceTo(player);
			if (distance < bestScore) {
				bestScore = distance;
				best = entity;
			}
		}
		return best;
	}

	private boolean isValid(Entity entity, ClientPlayerEntity player, MinecraftClient client) {
		if (entity == player || !entity.isAlive() || !(entity instanceof LivingEntity)) {
			return false;
		}
		if (entity instanceof PlayerEntity && !includePlayers.get()) {
			return false;
		}
		if (dev.amethyst.client.util.FriendManager.isFriend(entity.getName().getString())) {
			return false;
		}
		double distance = entity.squaredDistanceTo(player);
		if (distance > range.get() * range.get()) {
			return false;
		}
		// FOV check against current look vector
		Vec3d toEntity = entity.getBoundingBox().getCenter().subtract(player.getEyePos()).normalize();
		Vec3d look = Vec3d.fromPolar(player.getPitch(), player.getYaw());
		double dot = toEntity.dotProduct(look);
		double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
		return angle <= fov.get();
	}
}
