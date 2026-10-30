package dev.amethyst.client.util;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Resolves block ids like {@code ancient_debris} or
 * {@code minecraft:spawner} to {@link Block} instances. Unknown ids return
 * null instead of throwing so bad config values never crash rendering.
 */
public final class BlockLookup {
	private BlockLookup() {
	}

	public static Block byId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		try {
			Identifier identifier = id.contains(":")
				? Identifier.of(id)
				: Identifier.of("minecraft", id);
			return Registries.BLOCK.get(identifier);
		} catch (Exception e) {
			return null;
		}
	}

	public static String idOf(Block block) {
		return Registries.BLOCK.getId(block).getPath();
	}
}
