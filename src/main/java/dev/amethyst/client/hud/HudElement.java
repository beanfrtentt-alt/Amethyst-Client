package dev.amethyst.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * A single draggable HUD widget. Elements render at their stored position and
 * remember the size they last drew at so the HUD editor can hit-test them.
 */
public abstract class HudElement {
	private final String id;
	private final String name;
	private int x;
	private int y;
	private final int defaultX;
	private final int defaultY;
	private boolean enabled;
	/** Bounds from the most recent render; updated by {@link #render}. */
	protected int lastWidth;
	protected int lastHeight;

	protected HudElement(String id, String name, int defaultX, int defaultY, boolean enabledByDefault) {
		this.id = id;
		this.name = name;
		this.x = defaultX;
		this.y = defaultY;
		this.defaultX = Math.max(0, defaultX);
		this.defaultY = Math.max(0, defaultY);
		this.enabled = enabledByDefault;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public void setPosition(int newX, int newY) {
		this.x = Math.max(0, newX);
		this.y = Math.max(0, newY);
	}

	public void resetPosition() {
		setPosition(defaultX, defaultY);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		this.enabled = value;
	}

	public int lastWidth() {
		return lastWidth;
	}

	public int lastHeight() {
		return lastHeight;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= x + lastWidth && mouseY >= y && mouseY <= y + lastHeight;
	}

	/** Renders the element and updates lastWidth/lastHeight. */
	public final void renderElement(DrawContext context) {
		if (!enabled) {
			return;
		}
		try {
			render(context, MinecraftClient.getInstance().textRenderer);
		} catch (Exception e) {
			// a HUD element must never kill the frame
			dev.amethyst.client.AmethystClient.LOGGER.warn("HUD element '{}' failed: {}", id, e.getMessage());
			setEnabled(false);
		}
	}

	protected abstract void render(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer);

	/** Optional per-tick update (e.g. caching expensive scans). */
	public void tick() {
	}
}
