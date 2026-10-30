package dev.amethyst.client.hud.impl;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.gui.DrawContext;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.hud.HudElement;

/**
 * Optional Spotify "now playing" card. The integration is deliberately
 * file-based: any tool that can write
 * {@code config/amethyst/spotify-status.json} ({"track": "...", "artist":
 * "..."}) feeds the card. When the file is missing, stale or malformed the
 * element simply shows "not playing" — the client never talks to Spotify
 * directly and needs no credentials.
 */
public final class SpotifyElement extends HudElement {
	private static final long REFRESH_MILLIS = 5000;

	private String track = "";
	private String artist = "";
	private long lastRead;
	private Path statusFile;

	public SpotifyElement() {
		super("spotify", "Spotify Card", 4, 260, false);
	}

	private Path file() {
		if (statusFile == null && AmethystClient.CONFIG != null) {
			statusFile = AmethystClient.CONFIG.directory().resolve("spotify-status.json");
		}
		return statusFile;
	}

	@Override
	public void tick() {
		long now = System.currentTimeMillis();
		if (now - lastRead < REFRESH_MILLIS) {
			return;
		}
		lastRead = now;
		Path path = file();
		if (path == null || !Files.exists(path)) {
			track = "";
			artist = "";
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			track = root.has("track") ? root.get("track").getAsString() : "";
			artist = root.has("artist") ? root.get("artist").getAsString() : "";
		} catch (Exception e) {
			// malformed or half-written file: keep last known state, show nothing new
			track = "";
			artist = "";
		}
	}

	@Override
	protected void render(DrawContext context, net.minecraft.client.font.TextRenderer text) {
		String line1;
		String line2;
		if (track.isBlank()) {
			line1 = "Not playing";
			line2 = "Spotify";
		} else {
			line1 = track;
			line2 = artist.isBlank() ? "Spotify" : artist;
		}
		int width = Math.max(
			Math.max(text.getWidth(line1), text.getWidth(line2)) + 16, 96);
		lastWidth = width;
		lastHeight = 26;
		context.fill(x() - 2, y() - 2, x() + width, y() + 24, 0x90151024);
		context.fill(x() - 2, y() - 2, x() - 1, y() + 24, 0xFF53E89B);
		context.drawTextWithShadow(text, line1, x() + 4, y() + 2, 0xFFEAE6F2);
		context.drawTextWithShadow(text, line2, x() + 4, y() + 13, 0xFF8A84A0);
	}
}
