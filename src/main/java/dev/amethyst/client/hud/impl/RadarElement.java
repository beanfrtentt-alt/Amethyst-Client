package dev.amethyst.client.hud.impl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import dev.amethyst.client.hud.HudElement;
import dev.amethyst.client.util.FriendManager;

/**
 * Square radar showing nearby entities as colored dots, rotating with the
 * player like a minimap. Players, hostile mobs and passive mobs each get
 * their own color; friends are highlighted.
 */
public final class RadarElement extends HudElement {
	public static final int SIZE = 92;
	private static final double RANGE = 48.0;

	public RadarElement() {
		super("radar", "Radar", 8, 96, true);
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		lastWidth = SIZE;
		lastHeight = SIZE;
		context.fill(x(), y(), x() + SIZE, y() + SIZE, 0xA0151024);
		// border + crosshair
		context.drawHorizontalLine(x(), x() + SIZE - 1, y(), 0xFF2A2340);
		context.drawHorizontalLine(x(), x() + SIZE - 1, y() + SIZE - 1, 0xFF2A2340);
		context.drawVerticalLine(x(), y(), y() + SIZE - 1, 0xFF2A2340);
		context.drawVerticalLine(x() + SIZE - 1, y(), y() + SIZE - 1, 0xFF2A2340);
		int cx = x() + SIZE / 2;
		int cy = y() + SIZE / 2;
		context.drawHorizontalLine(cx - 1, cx + 1, cy, 0x60503A78);
		context.drawVerticalLine(cx, cy - 1, cy + 1, 0x60503A78);

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) {
			return;
		}
		float yawRad = client.player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		double sin = Math.sin(yawRad);
		double cos = Math.cos(yawRad);
		double scale = (SIZE / 2 - 3) / RANGE;

		List<Entity> entities = new ArrayList<>();
		for (Entity entity : client.world.getEntities()) {
			if (entity != client.player && entity.isAlive()
				&& entity.squaredDistanceTo(client.player) <= RANGE * RANGE) {
				entities.add(entity);
			}
		}
		for (Entity entity : entities) {
			Vec3d relative = entity.getEntityPos().subtract(client.player.getEntityPos());
			// rotate so that "up" on the radar is where the player looks
			double rotatedX = relative.x * cos - relative.z * sin;
			double rotatedZ = relative.x * sin + relative.z * cos;
			int dotX = cx + (int) Math.round(rotatedX * scale);
			int dotY = cy + (int) Math.round(rotatedZ * scale);
			if (dotX < x() + 1 || dotX > x() + SIZE - 2 || dotY < y() + 1 || dotY > y() + SIZE - 2) {
				continue;
			}
			context.fill(dotX - 1, dotY - 1, dotX + 1, dotY + 1, colorFor(entity));
		}
		// self marker
		context.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
	}

	private static int colorFor(Entity entity) {
		if (entity instanceof PlayerEntity) {
			return FriendManager.isFriend(entity.getName().getString()) ? 0xFF53B4E8 : 0xFFE8537A;
		}
		if (entity instanceof EndCrystalEntity) {
			return 0xFFB388FF;
		}
		if (entity instanceof LivingEntity) {
			return 0xFFE8B453;
		}
		return 0xFF8A84A0;
	}
}
