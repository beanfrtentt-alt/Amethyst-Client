package dev.amethyst.client.module.impl.render;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.render.RenderUtil;
import dev.amethyst.client.setting.ColorSetting;
import dev.amethyst.client.setting.EnumSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Target ESP decorates whatever entity is under your crosshair with an
 * animated halo ring, making your current target obvious.
 */
public final class TargetEspModule extends Module {
	public enum Style { RING, PULSING_RING, BARS }

	private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style",
		"RING is a static circle; PULSING breathes; BARS draws angle markers.",
		Style.class, Style.PULSING_RING));
	private final ColorSetting color = add(new ColorSetting("color", "Color",
		"Marker color.", 0xFFE8B453));

	public TargetEspModule() {
		super("target-esp", "Target ESP", "Animated halo around your crosshair target.", ModuleCategory.RENDER);
	}

	@Override
	public void onRender3D() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null
			|| !(client.crosshairTarget instanceof EntityHitResult hit)) {
			return;
		}
		Entity target = hit.getEntity();
		if (!target.isAlive()) {
			return;
		}
		Vec3d center = target.getBoundingBox().getCenter();
		long time = System.currentTimeMillis();
		switch (style.get()) {
			case RING -> RenderUtil.ring(center, target.getWidth() * 0.9 + 0.25, 24, color.get(), 1.25f);
			case PULSING_RING -> {
				float pulse = 0.5f + 0.5f * (float) Math.sin(time / 150.0);
				double radius = target.getWidth() * 0.75 + 0.25 + pulse * 0.35;
				RenderUtil.ring(center, radius, 24,
					dev.amethyst.client.util.ColorUtil.withAlpha(color.get(), (int) (120 + 100 * pulse)), 1.25f);
			}
			case BARS -> {
				Vec3d[] corners = {
					center.add(-0.8, -0.1, -0.8), center.add(0.8, -0.1, -0.8),
					center.add(-0.8, -0.1, 0.8), center.add(0.8, -0.1, 0.8)
				};
				for (Vec3d corner : corners) {
					RenderUtil.line(corner.add(0, -0.4, 0), corner.add(0, 0.4, 0), color.get(), 1.5f);
				}
			}
		}
	}
}
