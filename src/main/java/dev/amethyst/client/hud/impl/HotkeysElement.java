package dev.amethyst.client.hud.impl;

import net.minecraft.client.gui.DrawContext;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.module.Module;
import dev.amethyst.client.hud.HudElement;
import dev.amethyst.client.util.KeybindManager;

/** Lists every module that currently has a key bound, enabled or not. */
public final class HotkeysElement extends HudElement {
	private static final int ROW_HEIGHT = 11;

	public HotkeysElement() {
		super("hotkeys", "Keybind List", 4, 140, true);
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		if (AmethystClient.MODULES == null) {
			return;
		}
		int rows = 0;
		int widest = 0;
		for (Module module : AmethystClient.MODULES.all()) {
			if (module.keybind() != -1) {
				rows++;
				widest = Math.max(widest, text.getWidth(
					module.name() + " [" + KeybindManager.keyName(module.keybind()) + "]"));
			}
		}
		if (rows == 0) {
			lastWidth = 0;
			lastHeight = 0;
			return;
		}
		lastWidth = widest + 4;
		lastHeight = rows * ROW_HEIGHT;
		int drawY = y();
		for (Module module : AmethystClient.MODULES.all()) {
			if (module.keybind() == -1) {
				continue;
			}
			String label = module.name() + " [" + KeybindManager.keyName(module.keybind()) + "]";
			int color = module.isEnabled() ? 0xFF53E89B : 0xFF8A84A0;
			context.drawTextWithShadow(text, label, x(), drawY, color);
			drawY += ROW_HEIGHT;
		}
	}
}
