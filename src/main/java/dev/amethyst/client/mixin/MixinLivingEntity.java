package dev.amethyst.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.LivingEntity;

import dev.amethyst.client.util.Hooks;

/**
 * Slow Swing / Swing Animation scale the visible swing duration.
 */
@Mixin(LivingEntity.class)
abstract class MixinLivingEntity {
	@Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
	private void amethyst$swingDuration(CallbackInfoReturnable<Integer> cir) {
		int modified = Hooks.modifySwingDuration((LivingEntity) (Object) this, cir.getReturnValueI());
		if (modified != cir.getReturnValueI()) {
			cir.setReturnValue(modified);
		}
	}
}
