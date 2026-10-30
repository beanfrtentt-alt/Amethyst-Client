package dev.amethyst.client.module.impl.render;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.render.RenderUtil;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.ColorSetting;
import dev.amethyst.client.setting.DoubleSetting;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Storage ESP highlights loot containers: chests, ender chests, shulkers,
 * barrels, hoppers, dispensers and furnaces. Storage block entities are
 * enumerated directly from the world — no block scanning.
 */
public final class StorageEspModule extends Module {
	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Highlight distance for containers.", 48.0, 8.0, 96.0, 1.0, 0));
	private final BooleanSetting tracers = add(new BooleanSetting("tracers", "Tracers",
		"Draw lines to containers.", false));
	private final ColorSetting chestColor = add(new ColorSetting("chest-color", "Chest Color",
		"Color for chests and barrels.", 0xFF53B4E8));
	private final ColorSetting enderColor = add(new ColorSetting("ender-color", "Ender Chest Color",
		"Color for ender chests.", 0xFFB388FF));
	private final ColorSetting shulkerColor = add(new ColorSetting("shulker-color", "Shulker Color",
		"Color for shulker boxes.", 0xFFE8B453));

	public StorageEspModule() {
		super("storage-esp", "Storage ESP", "Highlights chests and other containers.", ModuleCategory.RENDER);
	}

	@Override
	public void onRender3D() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return;
		}
		double maxDist = range.get() * range.get();
		Vec3d eye = RenderUtil.FRAME.cameraPos;
		for (BlockEntity blockEntity : client.world.getBlockEntities()) {
			BlockPos pos = blockEntity.getPos();
			if (pos.getSquaredDistance(player.getEntityPos()) > maxDist) {
				continue;
			}
			int color = colorFor(blockEntity);
			if (color == 0) {
				continue;
			}
			Box box = new Box(pos);
			RenderUtil.drawBox(box, color, 1.25f);
			if (tracers.get()) {
				RenderUtil.line(eye, Vec3d.ofCenter(pos),
					dev.amethyst.client.util.ColorUtil.withAlpha(color, 140), 0.75f);
			}
		}
	}

	private int colorFor(BlockEntity blockEntity) {
		if (blockEntity instanceof ChestBlockEntity) {
			return chestColor.get();
		}
		if (blockEntity instanceof EnderChestBlockEntity) {
			return enderColor.get();
		}
		if (blockEntity instanceof ShulkerBoxBlockEntity) {
			return shulkerColor.get();
		}
		return switch (blockEntity.getType().toString()) {
			case "minecraft:barrel", "minecraft:hopper", "minecraft:dispenser",
				"minecraft:dropper", "minecraft:furnace", "minecraft:blast_furnace",
				"minecraft:smoker" -> chestColor.get();
			default -> 0;
		};
	}
}
