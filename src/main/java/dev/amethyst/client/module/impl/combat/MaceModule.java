package dev.amethyst.client.module.impl.combat;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.DoubleSetting;
import dev.amethyst.client.util.NotificationManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Mace utilities warn you when your fall height is enough for a full mace
 * smash so you can time the hit.
 */
public final class MaceModule extends Module {
	private final DoubleSetting minFall = add(new DoubleSetting("min-fall", "Min Fall Height",
		"Blocks of fall distance required for a smash alert.", 4.0, 1.5, 12.0, 0.5, 1));
	private final BooleanSetting requireMace = add(new BooleanSetting("require-mace", "Only With Mace",
		"Only alert while holding a mace.", true));

	private boolean alerted;

	public MaceModule() {
		super("mace", "Mace Utilities", "Alerts when your fall height is enough for a mace smash.",
			ModuleCategory.COMBAT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return;
		}
		ItemStack held = player.getMainHandStack();
		boolean holdingMace = held.getItem() == Items.MACE;
		double fallDistance = player.fallDistance;
		boolean ready = fallDistance >= minFall.get()
			&& (!requireMace.get() || holdingMace)
			&& !player.isOnGround();
		if (ready && !alerted) {
			NotificationManager.post(String.format("Mace smash ready (%.1f blocks)", fallDistance),
				NotificationManager.Type.SUCCESS, 1200);
			alerted = true;
		} else if (!ready) {
			alerted = false;
		}
	}
}
