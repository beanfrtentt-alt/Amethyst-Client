package dev.amethyst.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import dev.amethyst.client.util.Hooks;

/**
 * No Inventory Leak redacts item stacks drawn on screens (inventory, chest,
 * crafting, armor slots). HUD elements such as the hotbar are unaffected
 * because they render outside the screen-gated path.
 */
@Mixin(DrawContext.class)
abstract class MixinDrawContext {
	@Inject(method = "drawItem(Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
	private void amethyst$redactItem(ItemStack stack, int x, int y, CallbackInfo ci) {
		redact(stack, x, y, ci);
	}

	@Inject(method = "drawItem(Lnet/minecraft/item/ItemStack;III)V", at = @At("HEAD"), cancellable = true)
	private void amethyst$redactItemSeeded(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
		redact(stack, x, y, ci);
	}

	private void redact(ItemStack stack, int x, int y, CallbackInfo ci) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		if (Hooks.shouldRedactItem()) {
			DrawContext self = (DrawContext) (Object) this;
			// flat dark panel keeps the slot layout readable while leaking nothing
			self.fill(x, y, x + 16, y + 16, 0xC81A1426);
			self.fill(x, y, x + 16, y + 1, 0xFF2A2340);
			self.fill(x, y + 15, x + 16, y + 16, 0xFF2A2340);
			self.fill(x, y, x + 1, y + 16, 0xFF2A2340);
			self.fill(x + 15, y, x + 16, y + 16, 0xFF2A2340);
			ci.cancel();
		}
	}
}
