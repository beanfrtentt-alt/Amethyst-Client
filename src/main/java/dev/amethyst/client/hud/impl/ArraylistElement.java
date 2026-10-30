package dev.amethyst.client.hud.impl;

import net.minecraft.client.gui.DrawContext;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.hud.HudElement;

/** The classic enabled-module list, color-coded by category. */
public final class ArraylistElement extends HudElement {
	private static final int ENTRY_HEIGHT = 11;

	public ArraylistElement() {
		super("arraylist", "Module List", 4, 22, true);
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		var modules = AmethystClientClientHelper.arraylistModules();
		int widest = 0;
		for (Module module : modules) {
			widest = Math.max(widest, text.getWidth(module.name()));
		}
		lastWidth = widest + 6;
		lastHeight = Math.max(ENTRY_HEIGHT, modules.size() * ENTRY_HEIGHT);
		context.fill(x() - 2, y() - 2, x() + lastWidth, y() + lastHeight, 0x70151024);
		int drawY = y();
		for (Module module : modules) {
			int color = module.category().accentColor();
			context.fill(x() - 2, drawY - 1, x() + lastWidth, drawY + ENTRY_HEIGHT - 1, 0x501A1230);
			context.fill(x() - 2, drawY - 1, x(), drawY + ENTRY_HEIGHT - 1, color);
			context.drawTextWithShadow(text, module.name(), x() + 4, drawY, 0xFFEAE6F2);
			drawY += ENTRY_HEIGHT;
		}
	}
}

/** Small indirection so the element does not depend on client classes for tests. */
final class AmethystClientClientHelper {
	private AmethystClientClientHelper() {
	}

	static java.util.List<Module> arraylistModules() {
		if (dev.amethyst.client.AmethystClient.MODULES == null) {
			return java.util.List.of();
		}
		return dev.amethyst.client.AmethystClient.MODULES.arraylistModules();
	}
}
