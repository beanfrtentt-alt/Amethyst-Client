package dev.amethyst.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import dev.amethyst.client.hud.HudManager;
import dev.amethyst.client.util.NotificationManager;

/**
 * Minimal overlay for dragging and toggling HUD widgets. Click-drag moves an
 * element, right-click toggles its visibility, middle-click resets positions.
 */
public final class HudEditorScreen extends Screen {
	private final Screen back;

	private HudEditorScreen(Screen back) {
		super(MinecraftClient.getInstance(), MinecraftClient.getInstance().textRenderer,
			Text.literal("HUD Editor"));
		this.back = back;
	}

	public static void open(Screen back) {
		HudManager.setEditorOpen(true);
		MinecraftClient.getInstance().setScreen(new HudEditorScreen(back));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
		int button = click.button();
		if (button == 0) {
			if (!HudManager.startDrag(click.x(), click.y())) {
				HudManager.toggleElementAt(click.x(), click.y());
			}
			return true;
		}
		if (button == 1) {
			HudManager.toggleElementAt(click.x(), click.y());
			return true;
		}
		if (button == 2) {
			HudManager.resetPositionsToDefaults();
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
		HudManager.dragTo(click.x(), click.y());
		return true;
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.gui.Click click) {
		HudManager.stopDrag();
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput key) {
		if (key.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
			HudManager.resetPositionsToDefaults();
			return true;
		}
		return super.keyPressed(key);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0x44000000);
		// hint bar
		String hint = "Drag to move • Right-click to toggle • Backspace resets • Esc closes";
		int w = textRenderer.getWidth(hint) + 12;
		int x = (width - w) / 2;
		context.fill(x, height - 26, x + w, height - 8, 0xD8151024);
		context.fill(x, height - 26, x + w, height - 25, 0xFF9B53E8);
		context.drawTextWithShadow(textRenderer, hint, x + 6, height - 21, 0xFFEAE6F2);
	}

	@Override
	public void close() {
		HudManager.setEditorOpen(false);
		if (dev.amethyst.client.AmethystClient.CONFIG != null) {
			dev.amethyst.client.AmethystClient.CONFIG.markDirty();
		}
		NotificationManager.info("HUD layout saved.");
		MinecraftClient.getInstance().setScreen(back);
	}
}
