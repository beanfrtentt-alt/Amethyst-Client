package dev.amethyst.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;

import dev.amethyst.client.util.Hooks;

/**
 * Applies the Amethyst FOV stack (Zoom, Custom FOV, Weapon Inspect) to the
 * value the game renderer computed.
 */
@Mixin(GameRenderer.class)
abstract class MixinGameRenderer {
	// GameRenderer#getFov(Camera camera, float fovEffectScale, boolean useFovSetting)
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void amethyst$modifyFov(Camera camera, float fovEffectScale, boolean useFovSetting,
	                                CallbackInfoReturnable<Float> cir) {
		float modified = Hooks.modifyFov(cir.getReturnValueF());
		if (modified != cir.getReturnValueF()) {
			cir.setReturnValue(modified);
		}
	}
}
