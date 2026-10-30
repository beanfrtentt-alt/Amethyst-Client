package dev.amethyst.client.module.impl.render;

import java.util.ArrayList;
import java.util.List;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.render.RenderUtil;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.ColorSetting;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.setting.MultiOptionSetting;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Block ESP highlights configured blocks near you (ancient debris, spawners,
 * chests...). Scanning is throttled: positions are cached in a scan every
 * few ticks instead of re-walking blocks every frame.
 */
public final class BlockEspModule extends Module {
	private static final List<String> DEFAULT = List.of(
		"ancient_debris", "spawner", "chest", "ender_chest", "shulker_box");

	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Scan radius in blocks.", 32.0, 8.0, 64.0, 1.0, 0));
	private final MultiOptionSetting blocks = add(new MultiOptionSetting("blocks", "Blocks",
		"Which blocks to highlight.", DEFAULT, new java.util.LinkedHashSet<>(DEFAULT)));
	private final ColorSetting color = add(new ColorSetting("color", "Highlight Color",
		"Box color for highlighted blocks.", 0xFF9B53E8));
	private final BooleanSetting tracers = add(new BooleanSetting("tracers", "Tracers",
		"Draw tracer lines to matched blocks.", false));

	private final List<BlockPos> cache = new ArrayList<>();
	private int scanCooldown;
	private Vec3d scanCenter = Vec3d.ZERO;

	public BlockEspModule() {
		super("block-esp", "Block ESP", "Highlights chosen blocks within range.", ModuleCategory.RENDER);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return;
		}
		if (scanCooldown-- > 0) {
			return;
		}
		scanCooldown = 20; // rescan once per second
		cache.clear();
		scanCenter = player.getEntityPos();
		int radius = (int) Math.min(32, range.get());
		List<Block> wanted = new ArrayList<>();
		for (String id : blocks.selected()) {
			Block block = dev.amethyst.client.util.BlockLookup.byId(id);
			if (block != null) {
				wanted.add(block);
			}
		}
		if (wanted.isEmpty()) {
			return;
		}
		// scan a bounding cube but stop after a generous budget to protect FPS
		int budget = 120_000;
		BlockPos center = player.getBlockPos();
		for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
			if (budget-- <= 0) {
				break;
			}
			Block block = client.world.getBlockState(pos).getBlock();
			if (wanted.contains(block)) {
				cache.add(pos.toImmutable());
			}
		}
	}

	@Override
	public void onRender3D() {
		if (cache.isEmpty()) {
			return;
		}
		Vec3d eye = RenderUtil.FRAME.cameraPos;
		for (BlockPos pos : cache) {
			RenderUtil.drawBox(new Box(pos), color.get(), 1.0f);
			if (tracers.get()) {
				RenderUtil.line(eye, Vec3d.ofCenter(pos),
					dev.amethyst.client.util.ColorUtil.withAlpha(color.get(), 140), 0.75f);
			}
		}
	}
}
