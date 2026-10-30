package dev.amethyst.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.config.ConfigManager;
import dev.amethyst.client.hud.HudManager;
import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.ColorSetting;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.setting.EnumSetting;
import dev.amethyst.client.setting.IntSetting;
import dev.amethyst.client.setting.KeybindSetting;
import dev.amethyst.client.setting.MultiOptionSetting;
import dev.amethyst.client.setting.Setting;
import dev.amethyst.client.setting.StringSetting;
import dev.amethyst.client.util.KeybindManager;
import dev.amethyst.client.util.NotificationManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/**
 * The Amethyst Click GUI. Original dark/amethyst design drawn entirely with
 * DrawContext fills and text: category sidebar, live search, expandable
 * module settings, keybind capture, tooltips, profiles and reset-to-defaults.
 * All clicks are handled in {@link #mouseClicked}, never during render.
 */
public final class AmethystClickGui extends Screen {
	// ---- theme ----
	private static final int BACKDROP = 0x66000000;
	private static final int PANEL = 0xF2140E20;
	private static final int SIDEBAR = 0xF2100A18;
	private static final int BORDER = 0xFF2A2340;
	private static final int ROW_HOVER = 0x331A1230;
	private static final int ACCENT = 0xFF9B53E8;
	private static final int ACCENT_BRIGHT = 0xFFB388FF;
	private static final int TEXT = 0xFFEAE6F2;
	private static final int TEXT_DIM = 0xFF8A84A0;
	private static final int DANGER = 0xFFE8537A;
	private static final int SUCCESS = 0xFF53E89B;

	// ---- layout ----
	private static final int SIDEBAR_W = 118;
	private static final int HEADER_H = 30;
	private static final int CATEGORY_H = 22;
	private static final int MODULE_ROW_H = 24;
	private static final int SETTING_ROW_H = 22;

	// ---- state ----
	private ModuleCategory selected = ModuleCategory.COMBAT;
	private String search = "";
	private boolean searchFocused;
	private final java.util.Set<String> expanded = new java.util.HashSet<>();
	private int listScroll;
	private Module bindingTarget;
	private boolean profileMenuOpen;
	private boolean confirmReset;
	private String newProfileName = "";
	private boolean profileNameFocused;

	private int panelX, panelY, panelW, panelH;
	private double mouseX, mouseY;
	private SliderDrag sliderDrag;
	private final Screen prevScreen;

	private record SliderDrag(Setting<?> setting, double min, double max, int trackX, int trackW) {
	}

	private String tooltip;
	private String stringEditTargetId;
	private String stringEditBuffer = "";

	public AmethystClickGui(Screen prevScreen) {
		super(MinecraftClient.getInstance(), MinecraftClient.getInstance().textRenderer,
			Text.literal("Amethyst Client"));
		this.prevScreen = prevScreen;
	}

	/** Opens (or focuses) the Click GUI from the keybind or command. */
	public static void open() {
		MinecraftClient client = MinecraftClient.getInstance();
		Screen prev = client.currentScreen instanceof AmethystClickGui ? null : client.currentScreen;
		client.setScreen(new AmethystClickGui(prev));
	}

	@Override
	protected void init() {
		panelW = Math.min(560, Math.max(380, this.width - 80));
		panelH = Math.min(340, Math.max(280, this.height - 60));
		panelX = (this.width - panelW) / 2;
		panelY = (this.height - panelH) / 2;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	// ---------------------------------------------------------------- render

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		context.fill(0, 0, this.width, this.height, BACKDROP);
		context.fill(panelX - 3, panelY - 3, panelX + panelW + 6, panelY + panelH + 6, 0x40000000);
		context.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
		context.fill(panelX, panelY, panelX + panelW, panelY + HEADER_H, SIDEBAR);
		context.fill(panelX, panelY + HEADER_H - 1, panelX + panelW, panelY + HEADER_H, BORDER);
		context.drawTextWithShadow(textRenderer, "AMETHYST", panelX + 12, panelY + 11, ACCENT_BRIGHT);
		String version = "v" + AmethystClient.VERSION;
		int versionW = textRenderer.getWidth(version);
		context.drawTextWithShadow(textRenderer, version, panelX + panelW - versionW - 26, panelY + 11, TEXT_DIM);
		int closeX = panelX + panelW - 18;
		context.drawTextWithShadow(textRenderer, "x", closeX + 2, panelY + 11,
			hover(closeX, panelY + 8, closeX + 12, panelY + 20) ? DANGER : TEXT_DIM);

		renderSidebar(context);
		renderModuleList(context);
		if (profileMenuOpen) {
			renderProfileMenu(context);
		}
		if (confirmReset) {
			renderConfirmReset(context);
		}
		renderTooltip(context);
	}

	private void renderSidebar(DrawContext context) {
		int sx = panelX;
		int sy = panelY + HEADER_H;
		int sw = SIDEBAR_W;
		int sh = panelH - HEADER_H;
		context.fill(sx, sy, sx + sw, sy + sh, SIDEBAR);
		context.fill(sx + sw, sy, sx + sw + 1, sy + sh, BORDER);

		int searchY = sy + 8;
		boolean searchHover = hover(sx + 8, searchY, sx + sw - 8, searchY + 18);
		int searchBg = searchFocused ? 0xFF1E1530 : searchHover ? 0x50191331 : 0xFF171129;
		context.fill(sx + 8, searchY, sx + sw - 8, searchY + 18, searchBg);
		context.fill(sx + 8, searchY + 17, sx + sw - 8, searchY + 18, BORDER);
		String shown = search;
		if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
			shown = search + "_";
		}
		context.drawTextWithShadow(textRenderer, shown.isEmpty() ? "Search..." : shown,
			sx + 13, searchY + 5, search.isEmpty() && !searchFocused ? TEXT_DIM : TEXT);

		int cy = searchY + 26;
		for (ModuleCategory category : ModuleCategory.values()) {
			boolean selectedNow = category == selected && search.isEmpty();
			boolean hovered = hover(sx, cy, sx + sw, cy + CATEGORY_H);
			if (selectedNow) {
				context.fill(sx, cy, sx + sw, cy + CATEGORY_H, 0x401A1230);
				context.fill(sx, cy, sx + 2, cy + CATEGORY_H, category.accentColor());
			} else if (hovered) {
				context.fill(sx, cy, sx + sw, cy + CATEGORY_H, ROW_HOVER);
			}
			context.fill(sx + 12, cy + 7, sx + 18, cy + 13, category.accentColor());
			context.drawTextWithShadow(textRenderer, category.displayName(), sx + 24, cy + 7,
				selectedNow ? TEXT : TEXT_DIM);
			cy += CATEGORY_H;
		}

		int by = sy + sh - 96;
		footerButton(context, sx, by, "HUD Editor");
		by += 24;
		footerButton(context, sx, by, profileMenuOpen ? "Close Profiles" : "Profiles");
		by += 24;
		footerButton(context, sx, by, "Reset Defaults");
		by += 24;
		footerButton(context, sx, by, "Close (Esc)");
	}

	private void footerButton(DrawContext context, int sx, int y, String label) {
		boolean hovered = hover(sx + 6, y, sx + SIDEBAR_W - 6, y + 20)
			&& !profileMenuOpen && !confirmReset;
		context.fill(sx + 6, y, sx + SIDEBAR_W - 6, y + 20, hovered ? 0x50201838 : 0xFF171129);
		context.drawTextWithShadow(textRenderer, label, sx + 14, y + 6, hovered ? TEXT : TEXT_DIM);
	}

	private java.util.List<Module> visibleModules() {
		return search.isBlank()
			? AmethystClient.MODULES.byCategory(selected)
			: AmethystClient.MODULES.search(search);
	}

	private void renderModuleList(DrawContext context) {
		int lx = panelX + SIDEBAR_W + 1;
		int ly = panelY + HEADER_H;
		int lw = panelW - SIDEBAR_W - 1;
		var modules = visibleModules();
		String title = search.isBlank()
			? selected.displayName() + " — " + modules.size() + " modules"
			: "Search: " + modules.size() + " results";
		context.drawTextWithShadow(textRenderer, title, lx + 10, ly + 8, TEXT_DIM);

		int listY = ly + 22;
		int listBottom = panelY + panelH - 6;
		int maxVisible = (listBottom - listY) / MODULE_ROW_H;
		listScroll = Math.max(0, Math.min(listScroll, Math.max(0, modules.size() - maxVisible)));

		int rowY = listY - listScroll * MODULE_ROW_H;
		for (Module module : modules) {
			if (rowY + MODULE_ROW_H <= listY || rowY >= listBottom) {
				// still account for expanded settings height
				rowY += MODULE_ROW_H;
				if (expanded.contains(module.id())) {
					rowY += module.settings().size() * SETTING_ROW_H;
				}
				continue;
			}
			boolean hovered = hover(lx + 4, Math.max(listY, rowY), lx + lw - 4, Math.min(listBottom, rowY + MODULE_ROW_H));
			if (module.isEnabled()) {
				context.fill(lx + 4, rowY, lx + lw - 4, rowY + MODULE_ROW_H - 1, 0x301A1230);
				context.fill(lx + 4, rowY, lx + 7, rowY + MODULE_ROW_H - 1, module.category().accentColor());
			} else if (hovered) {
				context.fill(lx + 4, rowY, lx + lw - 4, rowY + MODULE_ROW_H - 1, ROW_HOVER);
			}
			int nameColor = module.isEnabled() ? TEXT : module.isIncomplete() ? 0xFF6A6484 : TEXT_DIM;
			context.drawTextWithShadow(textRenderer, module.name(), lx + 14, rowY + 8, nameColor);
			if (module.isIncomplete()) {
				context.drawTextWithShadow(textRenderer, "WIP",
					lx + 14 + textRenderer.getWidth(module.name()) + 5, rowY + 8, 0xFF706888);
			}
			int chipX = lx + lw - 52;
			boolean chipHovered = hover(chipX, rowY + 4, chipX + 36, rowY + MODULE_ROW_H - 4);
			if (bindingTarget == module) {
				context.fill(chipX, rowY + 4, chipX + 36, rowY + 19, ACCENT);
				context.drawTextWithShadow(textRenderer, "listen", chipX + 3, rowY + 8, 0xFFFFFFFF);
			} else {
				context.fill(chipX, rowY + 4, chipX + 36, rowY + 19, chipHovered ? 0x30201838 : 0xFF171129);
				context.drawTextWithShadow(textRenderer, KeybindManager.keyName(module.keybind()),
					chipX + 8, rowY + 8, chipHovered ? ACCENT_BRIGHT
						: module.keybind() != -1 ? TEXT_DIM : 0x55503A70);
			}
			boolean isExpanded = expanded.contains(module.id());
			context.drawTextWithShadow(textRenderer, isExpanded ? "-" : "+", lx + lw - 16, rowY + 8,
				isExpanded ? ACCENT_BRIGHT : TEXT_DIM);

			rowY += MODULE_ROW_H;
			if (isExpanded) {
				int settingY = rowY;
				for (Setting<?> setting : module.settings()) {
					if (settingY + SETTING_ROW_H > listBottom) {
						break;
					}
					renderSetting(context, module, setting, lx + 12, settingY, lw - 16);
					settingY += SETTING_ROW_H;
					rowY += SETTING_ROW_H;
				}
			}
		}
		if (modules.size() > maxVisible) {
			int trackH = listBottom - listY;
			int thumbH = Math.max(14, trackH * maxVisible / modules.size());
			int thumbY = listY + (trackH - thumbH) * listScroll / Math.max(1, modules.size() - maxVisible);
			context.fill(lx + lw - 2, listY, lx + lw - 1, listBottom, 0x20FFFFFF);
			context.fill(lx + lw - 3, thumbY, lx + lw - 1, thumbY + thumbH, 0x609B53E8);
		}
	}

	private void renderSetting(DrawContext context, Module module, Setting<?> setting,
	                           int x, int y, int width) {
		context.fill(x - 4, y, x + width, y + SETTING_ROW_H - 2, 0x80110919);
		String label = setting.name();
		if (setting instanceof BooleanSetting bool) {
			int boxX = x + width - 26;
			boolean hovered = hover(boxX, y + 3, boxX + 20, y + 17);
			context.fill(boxX, y + 3, boxX + 20, y + 17, bool.get() ? SUCCESS : 0xFF221A33);
			context.drawTextWithShadow(textRenderer, bool.get() ? "ON" : "OFF",
				boxX + (bool.get() ? 3 : 2), y + 7, bool.get() ? 0xFF0D0B16 : TEXT_DIM);
			context.drawTextWithShadow(textRenderer, label, x + 4, y + 7, TEXT);
		} else if (setting instanceof DoubleSetting dbl) {
			context.drawTextWithShadow(textRenderer, label, x + 4, y + 2, TEXT);
			String value = String.format("%." + dbl.decimals() + "f", dbl.get());
			context.drawTextWithShadow(textRenderer, value, x + width - textRenderer.getWidth(value) - 4, y + 2, ACCENT_BRIGHT);
			int trackX = x + 4;
			int trackW = width - 8;
			int trackY = y + 14;
			context.fill(trackX, trackY, trackX + trackW, trackY + 3, 0xFF221A33);
			int fill = (int) ((dbl.get() - dbl.min()) / (dbl.max() - dbl.min()) * trackW);
			context.fill(trackX, trackY, trackX + fill, trackY + 3, ACCENT);
			context.fill(trackX + fill - 2, trackY - 2, trackX + fill + 2, trackY + 5, ACCENT_BRIGHT);
		} else if (setting instanceof IntSetting intSetting) {
			context.drawTextWithShadow(textRenderer, label, x + 4, y + 2, TEXT);
			String value = String.valueOf(intSetting.get());
			context.drawTextWithShadow(textRenderer, value, x + width - textRenderer.getWidth(value) - 4, y + 2, ACCENT_BRIGHT);
			int trackX = x + 4;
			int trackW = width - 8;
			int trackY = y + 14;
			context.fill(trackX, trackY, trackX + trackW, trackY + 3, 0xFF221A33);
			int fill = (int) ((intSetting.get() - intSetting.min()) / (float) (intSetting.max() - intSetting.min()) * trackW);
			context.fill(trackX, trackY, trackX + fill, trackY + 3, ACCENT);
			context.fill(trackX + fill - 2, trackY - 2, trackX + fill + 2, trackY + 5, ACCENT_BRIGHT);
		} else if (setting instanceof EnumSetting<?> mode) {
			String chip = "‹ " + mode.get().name() + " ›";
			int chipX = x + width - 6 - textRenderer.getWidth(chip);
			boolean hovered = hover(chipX, y + 3, x + width - 4, y + 17);
			context.drawTextWithShadow(textRenderer, label, x + 4, y + 7, TEXT);
			context.fill(chipX, y + 3, x + width - 4, y + 17, hovered ? 0x30201838 : 0xFF171129);
			context.drawTextWithShadow(textRenderer, chip, chipX + 4, y + 7, ACCENT_BRIGHT);
		} else if (setting instanceof ColorSetting color) {
			context.drawTextWithShadow(textRenderer, label, x + 4, y + 7, TEXT);
			int boxX = x + width - 24;
			context.fill(boxX, y + 3, boxX + 20, y + 17, color.get());
			context.fill(boxX, y + 15, boxX + 20, y + 17, 0x30000000);
			String rgb = String.format("#%02X%02X%02X", color.red(), color.green(), color.blue());
			context.drawTextWithShadow(textRenderer, rgb, boxX - textRenderer.getWidth(rgb) - 4, y + 7, TEXT_DIM);
		} else if (setting instanceof KeybindSetting key) {
			context.drawTextWithShadow(textRenderer, label, x + 4, y + 7, TEXT);
			String chip = KeybindManager.keyName(key.get());
			int chipX = x + width - 6 - textRenderer.getWidth(chip) - 8;
			boolean hovered = hover(chipX, y + 3, x + width - 4, y + 17);
			context.fill(chipX, y + 3, x + width - 4, y + 17, hovered ? 0x30201838 : 0xFF171129);
			context.drawTextWithShadow(textRenderer, chip, chipX + 4, y + 7, ACCENT_BRIGHT);
		} else if (setting instanceof StringSetting str) {
			context.drawTextWithShadow(textRenderer, label, x + 4, y + 2, TEXT_DIM);
			boolean editing = setting.id().equals(stringEditTargetId);
			String shown = editing && (System.currentTimeMillis() / 500) % 2 == 0
				? stringEditBuffer + "_" : str.get().isEmpty() ? "click to edit" : str.get();
			int boxX = x + width / 2;
			context.fill(boxX, y + 3, x + width - 4, y + 17, hover(boxX, y + 3, x + width - 4, y + 17) ? 0x30201838 : 0xFF171129);
			context.drawTextWithShadow(textRenderer, shown, boxX + 4, y + 7,
				str.get().isEmpty() && !editing ? TEXT_DIM : TEXT);
		} else if (setting instanceof MultiOptionSetting multi) {
			context.drawTextWithShadow(textRenderer, label, x + 4, y + 2, TEXT);
			int chipX = x + 4;
			int drawY = y + 12;
			for (String option : multi.options()) {
				boolean on = multi.isSelected(option);
				int chipW = textRenderer.getWidth(option) + 6;
				if (chipX + chipW > x + width) {
					break;
				}
				context.fill(chipX, drawY, chipX + chipW, drawY + 9, on ? 0x50201838 : 0xFF171129);
				context.drawTextWithShadow(textRenderer, option, chipX + 3, drawY + 1,
					on ? ACCENT_BRIGHT : TEXT_DIM);
				chipX += chipW + 2;
			}
		}
	}

	private void renderProfileMenu(DrawContext context) {
		int mx = panelX + SIDEBAR_W;
		int my = panelY + HEADER_H + 4;
		int mw = 240;
		var profiles = AmethystClient.CONFIG.listProfiles();
		int mh = 26 + profiles.size() * 22 + 46;
		context.fill(mx, my, mx + mw, my + mh, PANEL);
		context.fill(mx, my, mx + mw, my + 1, ACCENT);
		context.drawTextWithShadow(textRenderer, "Profiles", mx + 8, my + 6, ACCENT_BRIGHT);
		int py = my + 18;
		for (String profile : profiles) {
			boolean hovered = hover(mx + 4, py, mx + mw - 4, py + 20);
			context.fill(mx + 4, py, mx + mw - 4, py + 20, hovered ? ROW_HOVER : 0xFF171129);
			context.drawTextWithShadow(textRenderer, profile, mx + 10, py + 6, TEXT);
			context.drawTextWithShadow(textRenderer, "load", mx + mw - 34, py + 6, TEXT_DIM);
			py += 22;
		}
		context.fill(mx + 4, py + 2, mx + mw - 4, py + 22, 0xFF171129);
		String shown = profileNameFocused && (System.currentTimeMillis() / 500) % 2 == 0
			? newProfileName + "_" : newProfileName;
		context.drawTextWithShadow(textRenderer, shown.isEmpty() ? "type a new name" : shown,
			mx + 10, py + 8, newProfileName.isEmpty() && !profileNameFocused ? TEXT_DIM : TEXT);
		py += 26;
		context.drawTextWithShadow(textRenderer, "Save new profile", mx + 8, py, SUCCESS);
	}

	private void renderConfirmReset(DrawContext context) {
		int mw = 260;
		int mx = panelX + (panelW - mw) / 2;
		int my = panelY + panelH / 2 - 30;
		context.fill(mx, my, mx + mw, my + 64, PANEL);
		context.fill(mx, my, mx + mw, my + 1, DANGER);
		context.drawTextWithShadow(textRenderer, "Reset ALL modules to defaults?", mx + 10, my + 10, TEXT);
		context.drawTextWithShadow(textRenderer, "States, binds and settings are cleared.", mx + 10, my + 22, TEXT_DIM);
		boolean yes = hover(mx + 10, my + 38, mx + 90, my + 54);
		boolean no = hover(mx + mw - 90, my + 38, mx + mw - 10, my + 54);
		context.fill(mx + 10, my + 38, mx + 90, my + 54, yes ? DANGER : 0xFF221A33);
		context.drawTextWithShadow(textRenderer, "Reset", mx + 28, my + 43, TEXT);
		context.fill(mx + mw - 90, my + 38, mx + mw - 10, my + 54, no ? 0xFF221A33 : 0xFF221A33);
		context.drawTextWithShadow(textRenderer, "Cancel", mx + mw - 74, my + 43, TEXT);
	}

	private void renderTooltip(DrawContext context) {
		if (tooltip == null || tooltip.isEmpty()) {
			return;
		}
		int tw = textRenderer.getWidth(tooltip) + 8;
		int tx = (int) Math.min(mouseX + 10, this.width - tw - 4);
		int ty = (int) mouseY - 16;
		context.fill(tx - 2, ty - 2, tx + tw + 2, ty + 14, 0xF0100A18);
		context.fill(tx - 2, ty - 2, tx + tw + 2, ty - 1, ACCENT);
		context.drawTextWithShadow(textRenderer, tooltip, tx + 4, ty + 3, TEXT);
		tooltip = null;
	}

	// ---------------------------------------------------------------- input

	private boolean hover(double x1, double y1, double x2, double y2) {
		return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
		mouseX = click.x();
		mouseY = click.y();
		int button = click.button();

		if (confirmReset) {
			int mw = 260;
			int mx = panelX + (panelW - mw) / 2;
			int my = panelY + panelH / 2 - 30;
			if (hover(mx + 10, my + 38, mx + 90, my + 54)) {
				resetAllModules();
				confirmReset = false;
			} else if (hover(mx + mw - 90, my + 38, mx + mw - 10, my + 54)) {
				confirmReset = false;
			}
			return true;
		}

		if (profileMenuOpen && handleProfileClick(click)) {
			return true;
		}

		if (button == 0) {
			// close button
			int closeX = panelX + panelW - 18;
			if (hover(closeX, panelY + 8, closeX + 12, panelY + 20)) {
				close();
				return true;
			}
			// search box
			int searchY = panelY + HEADER_H + 8;
			if (hover(panelX + 8, searchY, panelX + SIDEBAR_W - 8, searchY + 18)) {
				searchFocused = true;
				profileNameFocused = false;
				return true;
			}
			// categories
			int cy = searchY + 26;
			for (ModuleCategory category : ModuleCategory.values()) {
				if (hover(panelX, cy, panelX + SIDEBAR_W, cy + CATEGORY_H)) {
					selected = category;
					search = "";
					searchFocused = false;
					listScroll = 0;
					return true;
				}
				cy += CATEGORY_H;
			}
			// footer buttons
			int by = panelY + HEADER_H + panelH - HEADER_H - 96;
			if (hover(panelX + 6, by, panelX + SIDEBAR_W - 6, by + 20)) {
				HudManager.setEditorOpen(true);
				HudEditorScreen.open(this);
				return true;
			}
			by += 24;
			if (hover(panelX + 6, by, panelX + SIDEBAR_W - 6, by + 20)) {
				profileMenuOpen = true;
				return true;
			}
			by += 24;
			if (hover(panelX + 6, by, panelX + SIDEBAR_W - 6, by + 20)) {
				confirmReset = true;
				return true;
			}
			by += 24;
			if (hover(panelX + 6, by, panelX + SIDEBAR_W - 6, by + 20)) {
				close();
				return true;
			}
			searchFocused = false;
		}

		// module rows + settings
		int lx = panelX + SIDEBAR_W + 1;
		int lw = panelW - SIDEBAR_W - 1;
		int listY = panelY + HEADER_H + 22;
		int listBottom = panelY + panelH - 6;
		var modules = visibleModules();
		int rowY = listY - listScroll * MODULE_ROW_H;
		for (Module module : modules) {
			int rowBottom = rowY + MODULE_ROW_H;
			if (rowBottom > listY && rowY < listBottom
				&& hover(lx + 4, rowY, lx + lw - 4, rowBottom)) {
				int chipX = lx + lw - 52;
				if (button == 0 && bindingTarget != module && hover(chipX, rowY + 4, chipX + 36, rowY + 19)) {
					bindingTarget = module;
					return true;
				}
				if (bindingTarget == module && (button == 0 || button == 1)) {
					bindingTarget = null; // click again to cancel
					return true;
				}
				if (button == 0) {
					if (!module.isIncomplete()) {
						try {
							module.toggle();
							AmethystClient.MODULES.fireTogglesChanged();
							AmethystClient.CONFIG.markDirty();
							if (KeybindManager.shouldAnnounce(module)) {
								NotificationManager.post(
									module.name() + (module.isEnabled() ? " enabled" : " disabled"),
									module.isEnabled() ? NotificationManager.Type.SUCCESS : NotificationManager.Type.INFO,
									1500);
							}
						} catch (Exception e) {
							NotificationManager.error("Could not toggle " + module.name() + ": " + e.getMessage());
						}
					} else {
						NotificationManager.warn(module.name() + " is not implemented in this build (see docs).");
					}
					return true;
				} else if (button == 1) {
					if (!expanded.remove(module.id())) {
						expanded.add(module.id());
					}
					return true;
				}
			}
			rowY += MODULE_ROW_H;
			if (expanded.contains(module.id())) {
				int settingY = rowY;
				for (Setting<?> setting : module.settings()) {
					if (settingY + SETTING_ROW_H <= listBottom
						&& hover(lx + 8, settingY, lx + lw - 5, settingY + SETTING_ROW_H)) {
						tooltip = setting.description();
						handleSettingClick(module, setting, lx + 12, settingY, lw - 16, button);
						return true;
					}
					settingY += SETTING_ROW_H;
					rowY += SETTING_ROW_H;
				}
			}
		}
		return true;
	}

	private boolean handleProfileClick(net.minecraft.client.gui.Click click) {
		if (click.button() != 0) {
			return true;
		}
		int mx = panelX + SIDEBAR_W;
		int my = panelY + HEADER_H + 4;
		int mw = 240;
		var profiles = AmethystClient.CONFIG.listProfiles();
		int py = my + 18;
		for (String profile : profiles) {
			if (hover(mx + 4, py, mx + mw - 4, py + 20)) {
				try {
					AmethystClient.CONFIG.loadProfile(profile);
				} catch (Exception e) {
					NotificationManager.error("Could not load profile: " + e.getMessage());
				}
				profileMenuOpen = false;
				return true;
			}
			py += 22;
		}
		if (hover(mx + 4, py + 2, mx + mw - 4, py + 22)) {
			profileNameFocused = true;
			searchFocused = false;
			return true;
		}
		py += 26;
		if (hover(mx + 4, py, mx + mw - 4, py + 12)) {
			saveNewProfile();
			profileMenuOpen = false;
			return true;
		}
		if (!hover(mx, my, mx + mw, my + 26 + profiles.size() * 22 + 46)) {
			profileMenuOpen = false;
			return true;
		}
		return true;
	}

	private void saveNewProfile() {
		String name = ConfigManager.sanitizeProfileName(newProfileName);
		if (name.isEmpty()) {
			NotificationManager.warn("Profile name must not be empty.");
			return;
		}
		try {
			AmethystClient.CONFIG.saveProfile(name);
			newProfileName = "";
			profileNameFocused = false;
		} catch (Exception e) {
			NotificationManager.error("Save failed: " + e.getMessage());
		}
	}

	private void resetAllModules() {
		for (Module module : AmethystClient.MODULES.all()) {
			try {
				module.resetToDefaults();
			} catch (Exception e) {
				AmethystClient.LOGGER.warn("Could not reset module {}", module.id(), e);
			}
		}
		if (AmethystClient.CONFIG != null) {
			AmethystClient.CONFIG.markDirty();
		}
		NotificationManager.info("Module settings reset to defaults.");
	}

	private void handleSettingClick(Module module, Setting<?> setting, int x, int y, int width, int button) {
		if (setting instanceof BooleanSetting bool) {
			bool.toggle();
		} else if (setting instanceof EnumSetting<?> mode) {
			if (button == 0) {
				mode.cycle();
			}
		} else if (setting instanceof ColorSetting color) {
			color.set(PALETTE_NEXT.applyAsInt(color.get()));
		} else if (setting instanceof StringSetting str) {
			if (stringEditTargetId != null && stringEditTargetId.equals(setting.id())) {
				stringEditTargetId = null;
			} else {
				stringEditTargetId = setting.id();
				stringEditBuffer = str.get();
			}
		} else if (setting instanceof MultiOptionSetting multi) {
			int chipX = x + 4;
			int drawY = y + 12;
			boolean anyHit = false;
			for (String option : multi.options()) {
				int chipW = textRenderer.getWidth(option) + 6;
				if (chipX + chipW > x + width) {
					break;
				}
				if (button == 0 && hover(chipX, drawY, chipX + chipW, drawY + 9)) {
					multi.toggleOption(option);
					anyHit = true;
					break;
				}
				chipX += chipW + 2;
			}
			if (!anyHit && button == 2) {
				multi.selectAll();
			}
		} else if (setting instanceof DoubleSetting dbl) {
			sliderDrag = new SliderDrag(dbl, dbl.min(), dbl.max(), x + 4, width - 8);
			applySliderValue();
		} else if (setting instanceof IntSetting intSetting) {
			sliderDrag = new SliderDrag(intSetting, intSetting.min(), intSetting.max(), x + 4, width - 8);
			applySliderValue();
		}
		AmethystClient.CONFIG.markDirty();
	}

	private static final int[] PALETTE = {
		0xFF9B53E8, 0xFFB388FF, 0xFF53E89B, 0xFF53B4E8, 0xFFE8B453,
		0xFFE8537A, 0xFF9CE853, 0xFF53D4E8, 0xFFFFFFFF, 0xFF2A2340
	};

	private static final java.util.function.IntUnaryOperator PALETTE_NEXT = current -> {
		for (int i = 0; i < PALETTE.length; i++) {
			if (PALETTE[i] == current) {
				return PALETTE[(i + 1) % PALETTE.length];
			}
		}
		return PALETTE[0];
	};

	@Override
	public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
		mouseX = click.x();
		mouseY = click.y();
		if (sliderDrag != null) {
			applySliderValue();
			return true;
		}
		return false;
	}

	private void applySliderValue() {
		if (sliderDrag == null) {
			return;
		}
		float fraction = (float) ((mouseX - sliderDrag.trackX()) / (double) sliderDrag.trackW());
		fraction = Math.max(0f, Math.min(1f, fraction));
		Setting<?> setting = sliderDrag.setting();
		if (setting instanceof DoubleSetting dbl) {
			dbl.set(sliderDrag.min() + fraction * (sliderDrag.max() - sliderDrag.min()));
		} else if (setting instanceof IntSetting intSetting) {
			intSetting.set((int) Math.round(sliderDrag.min() + fraction * (sliderDrag.max() - sliderDrag.min())));
		}
		AmethystClient.CONFIG.markDirty();
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.gui.Click click) {
		sliderDrag = null;
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			listScroll = Math.max(0, listScroll + (scrollY > 0 ? -1 : 1));
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput key) {
		int keycode = key.key();

		if (bindingTarget != null) {
			if (keycode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
				bindingTarget.setKeybind(-1);
			} else {
				bindingTarget.setKeybind(keycode);
			}
			AmethystClient.CONFIG.markDirty();
			bindingTarget = null;
			return true;
		}

		if (profileNameFocused) {
			if (keycode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
				saveNewProfile();
				profileMenuOpen = false;
				return true;
			}
			if (keycode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !newProfileName.isEmpty()) {
				newProfileName = newProfileName.substring(0, newProfileName.length() - 1);
				return true;
			}
			return false;
		}

		if (searchFocused) {
			if (keycode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
				return true;
			}
			if (keycode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
				if (search.isEmpty()) {
					searchFocused = false;
					return true;
				}
				search = "";
				return true;
			}
			return false;
		}

		if (stringEditTargetId != null) {
			if (keycode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
				commitStringEdit();
				return true;
			}
			if (keycode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !stringEditBuffer.isEmpty()) {
				stringEditBuffer = stringEditBuffer.substring(0, stringEditBuffer.length() - 1);
				return true;
			}
			return false;
		}

		return super.keyPressed(key);
	}

	private void commitStringEdit() {
		if (stringEditTargetId == null) {
			return;
		}
		for (Module module : AmethystClient.MODULES.all()) {
			for (Setting<?> setting : module.settings()) {
				if (setting instanceof StringSetting str && setting.id().equals(stringEditTargetId)) {
					str.set(stringEditBuffer);
					AmethystClient.CONFIG.markDirty();
				}
			}
		}
		stringEditTargetId = null;
	}

	@Override
	public boolean charTyped(CharInput input) {
		int codePoint = input.codepoint();
		if (!Character.isValidCodePoint(codePoint) || Character.isISOControl(codePoint)) {
			return false;
		}
		String typed = new String(Character.toChars(codePoint));
		if (profileNameFocused) {
			newProfileName += typed;
			return true;
		}
		if (searchFocused) {
			search += typed;
			return true;
		}
		if (stringEditTargetId != null) {
			stringEditBuffer += typed;
			return true;
		}
		return false;
	}

	@Override
	public void close() {
		if (AmethystClient.CONFIG != null) {
			AmethystClient.CONFIG.saveAll();
		}
		MinecraftClient.getInstance().setScreen(prevScreen);
	}
}
