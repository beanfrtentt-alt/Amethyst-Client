package dev.amethyst.client.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dev.amethyst.client.AmethystClient;

/**
 * Queued toast notifications rendered by the HUD. The queue is bounded so a
 * spammy module can never grow memory without limit, and entries expire.
 */
public final class NotificationManager {
	public enum Type {
		INFO(0x9B53E8),
		SUCCESS(0x53E89B),
		WARN(0xE8B453),
		ERROR(0xE8537A);

		private final int color;

		Type(int color) {
			this.color = color;
		}

		public int color() {
			return color;
		}
	}

	public static final class Notification {
		public final String message;
		public final Type type;
		public final long createdAt = System.currentTimeMillis();
		public final long durationMillis;

		Notification(String message, Type type, long durationMillis) {
			this.message = message;
			this.type = type;
			this.durationMillis = durationMillis;
		}

		public float age(long now) {
			return Math.max(0f, Math.min(1f, (now - createdAt) / (float) durationMillis));
		}

		public boolean expired(long now) {
			return now - createdAt > durationMillis;
		}
	}

	private static final int MAX_QUEUED = 8;
	private static final List<Notification> notifications = new ArrayList<>();

	private NotificationManager() {
	}

	public static void info(String message) {
		post(message, Type.INFO, 3000);
	}

	public static void success(String message) {
		post(message, Type.SUCCESS, 3000);
	}

	public static void warn(String message) {
		post(message, Type.WARN, 3500);
	}

	public static void error(String message) {
		post(message, Type.ERROR, 4500);
	}

	public static void post(String message, Type type, long durationMillis) {
		AmethystClient.LOGGER.info("[notification/{}] {}", type, message);
		synchronized (notifications) {
			notifications.add(new Notification(message, type, Math.max(1000, durationMillis)));
			while (notifications.size() > MAX_QUEUED) {
				notifications.remove(0);
			}
		}
	}

	public static List<Notification> snapshot() {
		synchronized (notifications) {
			return new ArrayList<>(notifications);
		}
	}

	/** Drops expired notifications; called once per tick. */
	public static void tick() {
		long now = System.currentTimeMillis();
		synchronized (notifications) {
			Iterator<Notification> it = notifications.iterator();
			while (it.hasNext()) {
				if (it.next().expired(now)) {
					it.remove();
				}
			}
		}
	}

	public static void clear() {
		synchronized (notifications) {
			notifications.clear();
		}
	}
}
