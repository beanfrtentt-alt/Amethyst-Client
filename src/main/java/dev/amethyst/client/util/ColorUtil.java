package dev.amethyst.client.util;

/** ARGB color helpers used across the GUI, HUD and renderers. */
public final class ColorUtil {
	private ColorUtil() {
	}

	public static int argb(int alpha, int red, int green, int blue) {
		return (clamp255(alpha) << 24) | (clamp255(red) << 16) | (clamp255(green) << 8) | clamp255(blue);
	}

	public static int rgb(int red, int green, int blue) {
		return argb(255, red, green, blue);
	}

	/** Multiplies only the alpha channel, keeping the RGB components. */
	public static int withAlpha(int color, int alpha) {
		return (clamp255(alpha) << 24) | (color & 0x00FFFFFF);
	}

	public static int alpha(int color) {
		return (color >>> 24) & 0xFF;
	}

	public static int red(int color) {
		return (color >>> 16) & 0xFF;
	}

	public static int green(int color) {
		return (color >>> 8) & 0xFF;
	}

	public static int blue(int color) {
		return color & 0xFF;
	}

	/** Linear interpolation between two ARGB colors. */
	public static int lerp(int from, int to, float factor) {
		factor = Math.max(0f, Math.min(1f, factor));
		int a = (int) (alpha(from) + (alpha(to) - alpha(from)) * factor);
		int r = (int) (red(from) + (red(to) - red(from)) * factor);
		int g = (int) (green(from) + (green(to) - green(from)) * factor);
		int b = (int) (blue(from) + (blue(to) - blue(from)) * factor);
		return argb(a, r, g, b);
	}

	/** Accent pulse used by enabled-state indicators. */
	public static int pulse(int base, long millis, float range) {
		float t = (float) Math.sin(millis / 300.0) * range;
		int a = clamp255((int) (alpha(base) + t * alpha(base)));
		return (a << 24) | (base & 0x00FFFFFF);
	}

	private static int clamp255(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
