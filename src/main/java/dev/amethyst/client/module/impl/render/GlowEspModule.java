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
import net.minecraft.util.math.Box;

/**
 * Glow ESP paints layered translucent shells around entities so they appear
 * to glow, with per-group colors.
 */
public final class GlowEspModule extends Module {
	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Maximum distance to entities.", 48.0, 8.0, 96.0, 1.0, 0));
	private final BooleanSetting players = add(new BooleanSetting("players", "Players", "Glow enemy players.", true));
	private final BooleanSetting friends = add(new BooleanSetting("friends", "Friends", "Glow friends.", true));
	private final BooleanSetting hostiles = add(new BooleanSetting("hostiles", "Hostile Mobs", "Glow hostiles.", true));
	private final BooleanSetting passives = add(new BooleanSetting("passives", "Passive Mobs", "Glow animals.", false));
	private final BooleanSetting items = add(new BooleanSetting("items", "Items", "Glow dropped items.", false));
	private final ColorSetting playerColor = add(new ColorSetting("player-color", "Player Color",
		"Color for enemy players.", 0xFFB388FF));
	private final ColorSetting friendColor = add(new ColorSetting("friend-color", "Friend Color",
		"Color for friends.", 0xFF53B4E8));
	private final ColorSetting hostileColor = add(new ColorSetting("hostile-color", "Hostile Color",
		"Color for hostiles.", 0xFFE8B453));
	private final ColorSetting passiveColor = add(new ColorSetting("passive-color", "Passive Color",
		"Color for animals.", 0xFF53E89B));
	private final ColorSetting itemColor = add(new ColorSetting("item-color", "Item Color",
		"Color for items.", 0xFF9CE853));

	public GlowEspModule() {
		super("glow-esp", "Glow ESP", "Layered glow shells around entities.", ModuleCategory.RENDER);
	}

	@Override
	public void onRender3D() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return;
		}
		double maxDist = range.get() * range.get();
		for (Entity entity : client.world.getEntities()) {
			if (entity == player || !entity.isAlive()
				|| entity.squaredDistanceTo(player) > maxDist) {
				continue;
			}
			EntityGroups.Group group = EntityGroups.classify(entity, player);
			int color;
			switch (group) {
				case FRIEND -> {
					if (!friends.get()) continue;
					color = friendColor.get();
				}
				case PLAYER -> {
					if (!players.get()) continue;
					color = playerColor.get();
				}
				case HOSTILE -> {
					if (!hostiles.get()) continue;
					color = hostileColor.get();
				}
				case PASSIVE -> {
					if (!passives.get()) continue;
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
			Box box = entity.getBoundingBox();
			RenderUtil.glowBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
				color, 3, 1.5f);
		}
	}
}
