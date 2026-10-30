package dev.amethyst.client.hud.impl;

import net.minecraft.client.gui.DrawContext;

import dev.amethyst.client.hud.HudElement;
import dev.amethyst.client.util.ColorUtil;
import dev.amethyst.client.util.NotificationManager;

/** Toast notifications; anchored at its stored position, growing downward. */
public final class NotificationsElement extends HudElement {
	private static final int TOAST_HEIGHT = 16;
	private static final int MAX_SHOWN = 6;

	public NotificationsElement() {
		super("notifications", "Notifications", 260, 40, true);
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		var notifications = NotificationManager.snapshot();
		long now = System.currentTimeMillis();
		int widest = 90;
		int drawn = 0;
		int drawY = y();
		for (NotificationManager.Notification notification : notifications) {
			if (drawn >= MAX_SHOWN) {
				break;
			}
			float age = notification.age(now);
			// fade the last 20% of lifetime
			float alphaFactor = age < 0.8f ? 1f : Math.max(0f, (1f - age) / 0.2f);
			int width = text.getWidth(notification.message) + 14;
			widest = Math.max(widest, width);
			int accent = ColorUtil.withAlpha(notification.type.color(), (int) (255 * alphaFactor));
			int background = ColorUtil.withAlpha(0xD8151024, (int) (216 * alphaFactor));
			context.fill(x(), drawY, x() + width, drawY + TOAST_HEIGHT, background);
			context.fill(x(), drawY, x() + 2, drawY + TOAST_HEIGHT, accent);
			context.drawTextWithShadow(text, notification.message, x() + 7, drawY + 4,
				ColorUtil.withAlpha(0xFFEAE6F2, (int) (255 * alphaFactor)));
			drawY += TOAST_HEIGHT + 2;
			drawn++;
		}
		lastWidth = widest;
		lastHeight = Math.max(0, drawn * (TOAST_HEIGHT + 2));
	}
}
