package dev.amethyst.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Mouse;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.module.impl.movement.FreecamModule;
import dev.amethyst.client.util.CameraState;

/**
 * While Freecam or Freelook is active the cursor deltas are consumed by
 * Amethyst instead of rotating the player, which is what makes the detached
 * camera controllable without touching the player's server-visible rotation.
 */
@Mixin(Mouse.class)
abstract class MixinMouse {
	@Shadow
	private double cursorDeltaX;

	@Shadow
	private double cursorDeltaY;

	// Mouse#updateMouse(double time)
	@Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
	private void amethyst$redirectLook(double time, CallbackInfo ci) {
		if (CameraState.handleMouseDelta(cursorDeltaX, cursorDeltaY)) {
			cursorDeltaX = 0.0;
			cursorDeltaY = 0.0;
			ci.cancel();
		}
	}

	// Mouse#onMouseScroll(long, double, double). While freecam is active,
	// use the wheel for camera speed instead of changing the hotbar slot.
	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
	private void amethyst$freecamScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (vertical == 0.0 || AmethystClient.MODULES == null) {
			return;
		}
		var module = AmethystClient.MODULES.byId("freecam");
		if (module instanceof FreecamModule freecam && freecam.isEnabled()) {
			freecam.adjustSpeedBoost((float) Math.signum(vertical));
			ci.cancel();
		}
	}
}
