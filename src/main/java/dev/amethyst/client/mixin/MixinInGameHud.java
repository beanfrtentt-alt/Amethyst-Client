package dev.amethyst.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;

import dev.amethyst.client.util.Hooks;

/**
 * Replaces the vanilla crosshair when the Custom Crosshair module is enabled.
 */
@Mixin(InGameHud.class)
abstract class MixinInGameHud {
	// InGameHud#renderCrosshair(DrawContext, RenderTickCounter)
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void amethyst$renderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (Hooks.renderCustomCrosshair(context, tickCounter)) {
			ci.cancel();
		}
	}
}
