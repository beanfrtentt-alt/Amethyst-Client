package dev.amethyst.client.module.impl.render;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.render.RenderUtil;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.ColorSetting;
import dev.amethyst.client.setting.DoubleSetting;

import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Spawner Nametags highlights monster spawners near you with a box and an
 * activation-range ring. It iterates the world's live block-entity set, so
 * there is no block scanning at all.
 */
public final class SpawnerNametagsModule extends Module {
	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Scan radius for spawners.", 32.0, 8.0, 64.0, 1.0, 0));
	private final BooleanSetting showBox = add(new BooleanSetting("box", "Draw Box",
		"Draw a box around each spawner.", true));
	private final BooleanSetting activationRing = add(new BooleanSetting("activation-ring",
		"Activation Ring", "Draw a ring showing the activation radius.", true));
	private final ColorSetting color = add(new ColorSetting("color", "Spawner Color",
		"Highlight color for spawners.", 0xFFB388FF));

	public SpawnerNametagsModule() {
		super("spawner-nametags", "Spawner Nametags",
			"Highlights spawners and their activation range.", ModuleCategory.RENDER);
	}

	@Override
	public void onRender3D() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return;
		}
		double maxDist = range.get() * range.get();
		for (var blockEntity : client.world.getBlockEntities()) {
			if (!(blockEntity instanceof MobSpawnerBlockEntity)) {
				continue;
			}
			BlockPos pos = blockEntity.getPos();
			if (pos.getSquaredDistance(player.getEntityPos()) > maxDist) {
				continue;
			}
			if (showBox.get()) {
				RenderUtil.drawBox(new Box(pos), color.get(), 1.5f);
			}
			if (activationRing.get()) {
				RenderUtil.ring(Vec3d.ofCenter(pos), 8.0, 32,
					dev.amethyst.client.util.ColorUtil.withAlpha(color.get(), 90), 1.0f);
			}
		}
	}
}
