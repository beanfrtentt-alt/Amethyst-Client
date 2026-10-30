package dev.amethyst.client.module.impl.combat;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.ColorSetting;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.util.ColorUtil;
import dev.amethyst.client.util.Hooks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Hand Aura adds an amethyst energy read-out while you are in combat: an
 * aura ring around the crosshair whose intensity follows the closeness of
 * the entity under your cursor and flares after each attack.
 */
public final class HandAuraModule extends Module {
	private final DoubleSetting reach = add(new DoubleSetting("reach", "Reach",
		"Distance the aura reacts at.", 4.5, 1.0, 8.0, 0.1, 1));
	private final DoubleSetting aura = add(new DoubleSetting("aura", "Aura Strength",
		"Base opacity of the aura ring (0-1).", 0.5, 0.05, 1.0, 0.05, 2));
	private final ColorSetting color = add(new ColorSetting("color", "Aura Color",
		"Color of the aura ring.", 0xFF9B53E8));

	private int flareTicks;
	private final Hooks.AttackListener listener = (attacker, target) -> {
		if (attacker == MinecraftClient.getInstance().player) {
			flareTicks = 10;
		}
	};

	public HandAuraModule() {
		super("hand-aura", "Hand Aura", "Combat aura feedback ring around your crosshair.",
			ModuleCategory.COMBAT);
	}

	@Override
	protected void onEnable() {
		super.onEnable();
		flareTicks = 0;
		Hooks.addAttackListener(listener);
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		Hooks.removeAttackListener(listener);
	}

	@Override
	public void onHudRender(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.currentScreen != null) {
			return;
		}
		double proximity = 0.0;
		if (client.crosshairTarget instanceof EntityHitResult hit) {
			Entity entity = hit.getEntity();
			double distance = Math.sqrt(entity.squaredDistanceTo(player));
			proximity = Math.max(0.0, 1.0 - distance / reach.get());
		}
		if (flareTicks > 0) {
			proximity = Math.max(proximity, 1.0);
			flareTicks--;
		}
		if (proximity <= 0.02) {
			return;
		}
		int cx = context.getScaledWindowWidth() / 2;
		int cy = context.getScaledWindowHeight() / 2;
		int baseAlpha = (int) (255 * aura.get() * proximity);
		for (int i = 0; i < 3; i++) {
			int alpha = Math.max(8, baseAlpha * (3 - i) / 3);
			int radius = 14 + i * 4 + (flareTicks > 0 ? 2 : 0);
			int argb = ColorUtil.withAlpha(color.get(), alpha);
			context.fill(cx - radius, cy - 1, cx - radius + 3, cy + 1, argb);
			context.fill(cx + radius - 3, cy - 1, cx + radius, cy + 1, argb);
			context.fill(cx - 1, cy - radius, cx + 1, cy - radius + 3, argb);
			context.fill(cx - 1, cy + radius - 3, cx + 1, cy + radius, argb);
		}
	}
}
