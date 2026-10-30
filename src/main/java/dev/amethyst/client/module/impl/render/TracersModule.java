package dev.amethyst.client.module.impl.render;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.render.RenderUtil;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.ColorSetting;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.util.EntityGroups;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

/**
 * Tracers draws lines from your view position to nearby entities.
 */
public final class TracersModule extends Module {
	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Maximum distance to entities.", 64.0, 8.0, 128.0, 1.0, 0));
	private final BooleanSetting players = add(new BooleanSetting("players", "Players", "Trace players.", true));
	private final BooleanSetting animals = add(new BooleanSetting("animals", "Animals", "Trace passive mobs.", true));
	private final BooleanSetting hostiles = add(new BooleanSetting("hostiles", "Hostile Mobs", "Trace hostile mobs.", true));
	private final BooleanSetting items = add(new BooleanSetting("items", "Items", "Trace dropped items.", false));
	private final ColorSetting playerColor = add(new ColorSetting("player-color", "Player Color",
		"Trace color for players.", 0xFFE8537A));
	private final ColorSetting hostileColor = add(new ColorSetting("hostile-color", "Hostile Color",
		"Trace color for hostile mobs.", 0xFFE8B453));
	private final ColorSetting passiveColor = add(new ColorSetting("passive-color", "Passive Color",
		"Trace color for animals.", 0xFF53E89B));
	private final ColorSetting itemColor = add(new ColorSetting("item-color", "Item Color",
		"Trace color for items.", 0xFF9CE853));

	public TracersModule() {
		super("tracers", "Tracers", "Lines from your view to nearby entities.", ModuleCategory.RENDER);
	}

	@Override
	public void onRender3D() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return;
		}
		double maxDist = range.get() * range.get();
		net.minecraft.util.math.Vec3d eye = player.getEyePos();
		for (Entity entity : client.world.getEntities()) {
			if (entity == player || !entity.isAlive()
				|| entity.squaredDistanceTo(player) > maxDist) {
				continue;
			}
			EntityGroups.Group group = EntityGroups.classify(entity, player);
			int color;
			switch (group) {
				case FRIEND, PLAYER -> {
					if (!players.get()) continue;
					color = playerColor.get();
				}
				case HOSTILE -> {
					if (!hostiles.get()) continue;
					color = hostileColor.get();
				}
				case PASSIVE -> {
					if (!animals.get()) continue;
					color = passiveColor.get();
				}
				case ITEM -> {
					if (!items.get()) continue;
					color = itemColor.get();
				}
				default -> {
					continue;
				}
			}
			net.minecraft.util.math.Vec3d center = entity.getBoundingBox().getCenter();
			RenderUtil.line(eye, center, color, 1.0f);
		}
	}
}
