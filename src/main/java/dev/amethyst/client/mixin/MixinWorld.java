package dev.amethyst.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.World;

import dev.amethyst.client.util.Hooks;

/**
 * Time Changer overrides the client-visible time of day. The server-visible
 * time is untouched; this only affects local sky/daylight rendering.
 */
@Mixin(World.class)
abstract class MixinWorld {
	@Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
	private void amethyst$modifyTime(CallbackInfoReturnable<Long> cir) {
		long modified = Hooks.modifyTimeOfDay(cir.getReturnValue());
		if (modified != cir.getReturnValue()) {
			cir.setReturnValue(modified);
		}
	}
}
