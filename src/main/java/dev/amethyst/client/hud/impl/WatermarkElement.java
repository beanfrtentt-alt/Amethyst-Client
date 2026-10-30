package dev.amethyst.client.hud.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.hud.HudElement;

/** The Amethyst version watermark. */
public final class WatermarkElement extends HudElement {
	public WatermarkElement() {
		super("watermark", "Watermark", 4, 4, true);
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		String label = "Amethyst " + AmethystClient.VERSION;
		int width = text.getWidth(label) + 10;
		lastWidth = width;
		lastHeight = 13;
		context.fill(x() - 2, y() - 2, x() + width - 2, y() + 11, 0x90151024);
		context.fill(x() - 2, y() - 2, x() + width - 2, y() - 1, 0xFF9B53E8);
		context.drawTextWithShadow(text, label, x() + 2, y() + 1, 0xFFE4D9F8);
	}
}
