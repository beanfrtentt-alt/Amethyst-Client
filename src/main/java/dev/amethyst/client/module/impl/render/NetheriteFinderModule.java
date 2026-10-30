package dev.amethyst.client.module.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.render.RenderUtil;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.ColorSetting;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.util.NotificationManager;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Netherite Finder scans loaded chunks for ancient debris, highlights hits
 * and notifies once per new find. It only ever reports what your client can
 * already see — nothing hidden by the server is revealed.
 */
public final class NetheriteFinderModule extends Module {
	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Scan radius in blocks.", 48.0, 8.0, 96.0, 1.0, 0));
	private final BooleanSetting notifications = add(new BooleanSetting("notifications", "Notifications",
		"Notify when new debris is discovered.", true));
	private final BooleanSetting tracers = add(new BooleanSetting("tracers", "Tracers",
		"Draw tracer lines to found debris.", true));
	private final ColorSetting color = add(new ColorSetting("color", "Color",
		"Highlight color for ancient debris.", 0xFF8A5CF6));

	/** Known debris positions per chunk region to avoid duplicate notifications. */
	private final Map<Long, List<BlockPos>> known = new HashMap<>();
	private final List<BlockPos> hits = new ArrayList<>();
	private int scanCooldown;
	private int totalFound;

	public NetheriteFinderModule() {
		super("netherite-finder", "Netherite Finder",
			"Scans loaded chunks for ancient debris and tracks finds.", ModuleCategory.RENDER);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		known.clear();
		hits.clear();
		totalFound = 0;
		scanCooldown = 0;
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
		scanCooldown = 30; // every 1.5 s
		hits.clear();
		int radius = (int) Math.min(32, range.get());
		int budget = 120_000;
		BlockPos center = player.getBlockPos();
		List<BlockPos> fresh = new ArrayList<>();
		for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
			if (budget-- <= 0) {
				break;
			}
			if (client.world.getBlockState(pos).getBlock() != Blocks.ANCIENT_DEBRIS) {
				continue;
			}
			hits.add(pos.toImmutable());
			long region = regionKey(pos);
			List<BlockPos> list = known.computeIfAbsent(region, k -> new ArrayList<>());
			if (list.stream().noneMatch(p -> p.equals(pos))) {
				list.add(pos.toImmutable());
				fresh.add(pos.toImmutable());
			}
		}
		if (notifications.get() && !fresh.isEmpty()) {
			totalFound += fresh.size();
			NotificationManager.success("Netherite Finder: found " + fresh.size()
				+ " ancient debris (total " + totalFound + ")");
		}
	}

	private static long regionKey(BlockPos pos) {
		return ChunkSectionPos.asLong(pos.getX() >> 5, pos.getY() >> 5, pos.getZ() >> 5);
	}

	@Override
	public void onRender3D() {
		for (BlockPos pos : hits) {
			RenderUtil.drawBox(new Box(pos), color.get(), 1.25f);
			if (tracers.get()) {
				RenderUtil.line(RenderUtil.FRAME.cameraPos, Vec3d.ofCenter(pos),
					dev.amethyst.client.util.ColorUtil.withAlpha(color.get(), 140), 0.75f);
			}
		}
	}
}
