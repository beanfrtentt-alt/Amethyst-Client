package dev.amethyst.client.hud.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import dev.amethyst.client.hud.HudElement;
import dev.amethyst.client.util.ColorUtil;

/** Shows health, distance and name of whatever is under the crosshair. */
public final class TargetInfoElement extends HudElement {
	public TargetInfoElement() {
		super("target-info", "Target Info", 4, 220, true);
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			lastWidth = 0;
			lastHeight = 0;
			return;
		}
		Entity target = findTarget(client);
		if (!(target instanceof LivingEntity living) || target == player) {
			lastWidth = 0;
			lastHeight = 0;
			return;
		}
		String name = target.getDisplayName().getString();
		float health = living.getHealth();
		float maxHealth = Math.max(1f, living.getMaxHealth());
		int distance = (int) Math.round(player.distanceTo(target));
		String first = name + "  " + distance + "m";
		int width = Math.max(text.getWidth(first) + 8, 84);
		lastWidth = width;
		lastHeight = 28;
		context.fill(x() - 2, y() - 2, x() + width, y() + 26, 0x90151024);
		context.fill(x() - 2, y() - 2, x() + width, y() - 1, 0xFFE8537A);
		context.drawTextWithShadow(text, first, x() + 2, y() + 2, 0xFFEAE6F2);
		// health bar
		context.fill(x() + 2, y() + 14, x() + width - 2, y() + 21, 0xFF221A33);
		int barWidth = (int) ((width - 4) * Math.max(0f, Math.min(1f, health / maxHealth)));
		int barColor = health / maxHealth > 0.5f ? 0xFF53E89B
			: health / maxHealth > 0.25f ? 0xFFE8B453 : 0xFFE8537A;
		context.fill(x() + 2, y() + 14, x() + 2 + barWidth, y() + 21, barColor);
		context.drawTextWithShadow(text, String.format("%.1f / %.1f HP", health, maxHealth),
			x() + 2, y() + 23, 0xFFB0A8C8);
	}

	private static Entity findTarget(MinecraftClient client) {
		if (client.crosshairTarget instanceof EntityHitResult entityHit) {
			return entityHit.getEntity();
		}
		// fall back to the nearest living entity within arm's reach
		if (client.player != null && client.world != null) {
			double best = 4.5 * 4.5;
			Entity bestEntity = null;
			for (Entity entity : client.world.getEntities()) {
				if (entity instanceof PlayerEntity || entity instanceof LivingEntity) {
					if (entity == client.player || !entity.isAlive()) {
						continue;
					}
					double distance = entity.squaredDistanceTo(client.player);
					if (distance < best) {
						best = distance;
						bestEntity = entity;
					}
				}
			}
			return bestEntity;
		}
		return null;
	}
}
