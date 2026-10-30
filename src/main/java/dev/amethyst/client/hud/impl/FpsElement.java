package dev.amethyst.client.hud.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import dev.amethyst.client.hud.HudElement;

/** Frames-per-second counter. */
public final class FpsElement extends HudElement {
	public FpsElement() {
		super("fps", "FPS Counter", 4, 42, true);
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		int fps = MinecraftClient.getInstance().getCurrentFps();
		int color = fps >= 60 ? 0xFF53E89B : fps >= 30 ? 0xFFE8B453 : 0xFFE8537A;
		String label = "FPS: " + fps;
		lastWidth = text.getWidth(label) + 4;
		lastHeight = 11;
		context.drawTextWithShadow(text, label, x(), y(), color);
	}
}
