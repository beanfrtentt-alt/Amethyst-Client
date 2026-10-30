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
 * ESP draws colored outline boxes around entities. Each group can be toggled
 * and recolored; the boxes are depth-tested so occluded parts stay hidden.
 */
public final class EspModule extends Module {
	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Maximum distance to entities.", 64.0, 8.0, 128.0, 1.0, 0));
	private final BooleanSetting players = add(new BooleanSetting("players", "Players",
		"Highlight other players.", true));
	private final BooleanSetting friends = add(new BooleanSetting("friends", "Friends",
		"Highlight friends.", true));
	private final BooleanSetting hostiles = add(new BooleanSetting("hostiles", "Hostile Mobs",
		"Highlight hostile mobs.", true));
	private final BooleanSetting passives = add(new BooleanSetting("passives", "Passive Mobs",
		"Highlight animals.", true));
	private final BooleanSetting items = add(new BooleanSetting("items", "Items",
		"Highlight dropped items.", true));
	private final ColorSetting playerColor = add(new ColorSetting("player-color", "Player Color",
		"Color for enemy players.", 0xFFE8537A));
	private final ColorSetting friendColor = add(new ColorSetting("friend-color", "Friend Color",
		"Color for friends.", 0xFF53B4E8));
	private final ColorSetting hostileColor = add(new ColorSetting("hostile-color", "Hostile Color",
		"Color for hostile mobs.", 0xFFE8B453));
	private final ColorSetting passiveColor = add(new ColorSetting("passive-color", "Passive Color",
		"Color for animals.", 0xFF53E89B));
	private final ColorSetting itemColor = add(new ColorSetting("item-color", "Item Color",
		"Color for items.", 0xFF9CE853));

	public EspModule() {
		super("esp", "ESP", "Outline boxes around entities, color-coded by group.", ModuleCategory.RENDER);
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
			if (entity == player || !entity.isAlive()) {
				continue;
			}
			if (entity.squaredDistanceTo(player) > maxDist) {
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
			RenderUtil.drawBox(entity.getBoundingBox(), color, 1.5f);
		}
	}
}
