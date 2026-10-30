package dev.amethyst.client.hud.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import dev.amethyst.client.hud.HudElement;

/** Cardinal direction indicator with the exact yaw. */
public final class DirectionElement extends HudElement {
	private static final String[] DIRECTIONS =
		{"South (+Z)", "West (-X)", "North (-Z)", "East (+X)"};

	public DirectionElement() {
		super("direction", "Direction", 4, 70, true);
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) {
			lastWidth = 0;
			lastHeight = 0;
			return;
		}
		float yaw = MathHelper.wrapDegrees(player.getYaw());
		int index = MathHelper.floor((yaw + 180f) / 90f + 0.5) & 3;
		String label = DIRECTIONS[index] + " (" + MathHelper.floor(yaw) + "°)";
		lastWidth = text.getWidth(label) + 4;
		lastHeight = 11;
		context.drawTextWithShadow(text, label, x(), y(), 0xFFB8A9E8);
	}
}
