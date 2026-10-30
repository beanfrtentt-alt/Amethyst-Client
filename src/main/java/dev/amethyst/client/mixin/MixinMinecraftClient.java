package dev.amethyst.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.MinecraftClient;

import dev.amethyst.client.util.ClientInvoker;

/**
 * Exposes the private attack/use entry points so the Auto Clicker can
 * dispatch real input events (identical to physical clicks, including block
 * breaking and cooldown checks) instead of raw packets.
 */
@Mixin(MinecraftClient.class)
abstract class MixinMinecraftClient implements ClientInvoker {
	@Invoker("doAttack")
	public abstract boolean amethyst$doAttack();

	@Invoker("doItemUse")
	public abstract void amethyst$doItemUse();
}
