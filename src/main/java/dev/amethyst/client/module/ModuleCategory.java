package dev.amethyst.client.module;

/**
 * Module categories shown in the Click GUI. Order here is the display order.
 */
public enum ModuleCategory {
	COMBAT("Combat", 0xE8537A),
	MOVEMENT("Movement", 0x53B4E8),
	RENDER("Render", 0x53E89B),
	VISUALS("Visuals", 0xE8B453),
	WORLD("World", 0x9CE853),
	PLAYER("Player", 0xE853D4),
	UTILITY("Utility", 0x9B53E8),
	CLIENT("Client", 0x6478E8);

	private final String displayName;
	private final int accentColor;

	ModuleCategory(String displayName, int accentColor) {
		this.displayName = displayName;
		this.accentColor = accentColor;
	}

	public String displayName() {
		return displayName;
	}

	/** ARGB accent used for category headers and module dots in the GUI. */
	public int accentColor() {
		return accentColor;
	}
}
