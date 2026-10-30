package dev.amethyst.client.module.impl.render;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.render.RenderUtil;
import dev.amethyst.client.setting.ColorSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;

/**
 * Block Overlay replaces the vanilla block outline with a filled, tinted
 * highlight box on whatever block you are looking at.
 */
public final class BlockOverlayModule extends Module {
	private final ColorSetting color = add(new ColorSetting("color", "Overlay Color",
		"Tint and outline color.", 0x8053B4E8));

	public BlockOverlayModule() {
		super("block-overlay", "Block Overlay",
			"Filled highlight on the block you are looking at.", ModuleCategory.RENDER);
	}

	@Override
	public void onRender3D() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK
			|| !(client.crosshairTarget instanceof BlockHitResult hit)) {
			return;
		}
		RenderUtil.drawBox(new Box(hit.getBlockPos()), color.get(), 2.0f);
		RenderUtil.glowBox(hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ(),
			hit.getBlockPos().getX() + 1, hit.getBlockPos().getY() + 1, hit.getBlockPos().getZ() + 1,
			dev.amethyst.client.util.ColorUtil.withAlpha(color.get(), 60), 2, 1.0f);
	}
}
