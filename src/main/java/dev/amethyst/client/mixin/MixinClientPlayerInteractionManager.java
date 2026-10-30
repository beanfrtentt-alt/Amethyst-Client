package dev.amethyst.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import dev.amethyst.client.util.Hooks;

/**
 * Friend Guard cancels attacks on friends; attack listeners (Hit Particles,
 * W-Tap) observe successful attacks. Both cover vanilla clicks and any
 * module-initiated attack because they share this choke point.
 */
@Mixin(ClientPlayerInteractionManager.class)
abstract class MixinClientPlayerInteractionManager {
	@Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
	private void amethyst$guardAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
		if (Hooks.shouldCancelAttack(player, target)) {
			ci.cancel();
		}
	}

	@Inject(method = "attackEntity", at = @At("TAIL"))
	private void amethyst$postAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
		Hooks.onAttack(player, target);
	}
}
