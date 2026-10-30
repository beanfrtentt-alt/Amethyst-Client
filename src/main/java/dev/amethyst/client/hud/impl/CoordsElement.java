package dev.amethyst.client.hud.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

import dev.amethyst.client.hud.HudElement;

/** Current coordinates plus the block the player is standing on. */
public final class CoordsElement extends HudElement {
	public CoordsElement() {
		super("coords", "Coordinates", 4, 56, true);
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) {
			lastWidth = 0;
			lastHeight = 0;
			return;
		}
		String label = String.format("XYZ: %.1f / %.1f / %.1f",
			player.getX(), player.getY(), player.getZ());
		lastWidth = text.getWidth(label) + 4;
		lastHeight = 11;
		context.drawTextWithShadow(text, label, x(), y(), 0xFF9CE8E0);
	}
}
