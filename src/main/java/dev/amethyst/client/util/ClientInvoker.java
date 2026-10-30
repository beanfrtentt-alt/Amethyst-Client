package dev.amethyst.client.util;

/**
 * Public bridge implemented by the MinecraftClient mixin. Code elsewhere in
 * the client casts {@code MinecraftClient} to this interface to trigger real
 * input handling identical to physical clicks.
 */
public interface ClientInvoker {
	boolean amethyst$doAttack();

	void amethyst$doItemUse();
}
