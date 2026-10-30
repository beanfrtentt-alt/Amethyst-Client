package dev.amethyst.client.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Shared entity classification used by ESP, Tracers, Glow ESP and the radar.
 */
public final class EntityGroups {
	public enum Group {
		SELF(0xFF9B53E8),
		FRIEND(0xFF53B4E8),
		PLAYER(0xFFE8537A),
		HOSTILE(0xFFE8B453),
		PASSIVE(0xFF53E89B),
		ITEM(0xFF9CE853),
		OTHER(0xFF8A84A0);

		private final int color;

		Group(int color) {
			this.color = color;
		}

		public int color() {
			return color;
		}
	}

	private EntityGroups() {
	}

	public static Group classify(Entity entity, Entity self) {
		if (entity == self) {
			return Group.SELF;
		}
		if (entity instanceof PlayerEntity) {
			return FriendManager.isFriend(entity.getName().getString()) ? Group.FRIEND : Group.PLAYER;
		}
		if (entity instanceof ItemEntity) {
			return Group.ITEM;
		}
		if (entity instanceof Monster) {
			return Group.HOSTILE;
		}
		if (entity instanceof PassiveEntity) {
			return Group.PASSIVE;
		}
		if (entity instanceof LivingEntity) {
			return Group.OTHER;
		}
		return Group.OTHER;
	}
}
