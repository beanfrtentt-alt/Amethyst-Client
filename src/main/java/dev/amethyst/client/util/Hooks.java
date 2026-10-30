package dev.amethyst.client.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Delegation points between the (thin) mixins and the module system. Mixins
 * call into these static methods; each hook finds the relevant module and
 * applies its behavior, returning the value vanilla should continue with.
 *
 * Every hook tolerates an uninitialized module system (during early client
 * boot) by returning the vanilla value.
 */
public final class Hooks {
	private Hooks() {
	}

	private static dev.amethyst.client.module.Module module(String id) {
		dev.amethyst.client.module.ModuleManager manager = dev.amethyst.client.AmethystClient.MODULES;
		if (manager == null) {
			return null;
		}
		return manager.byId(id);
	}

	private static boolean enabled(String id) {
		dev.amethyst.client.module.Module module = module(id);
		return module != null && module.isEnabled();
	}

	// ------------------------------------------------------------ FOV

	/** Applies Zoom / CustomFov / WeaponInspect FOV adjustments. */
	public static float modifyFov(float vanillaFov) {
		float fov = vanillaFov;
		try {
			dev.amethyst.client.module.Module zoom = module("zoom");
			if (zoom instanceof dev.amethyst.client.module.impl.movement.ZoomModule zoomModule
				&& zoomModule.isEnabled()) {
				fov *= zoomModule.currentZoomFactor();
			}
			dev.amethyst.client.module.Module customFov = module("custom-fov");
			if (customFov instanceof dev.amethyst.client.module.impl.movement.CustomFovModule fovModule
				&& fovModule.isEnabled()) {
				fov = fovModule.fovValue() * (fov / Math.max(1.0f, vanillaFov));
			}
		} catch (Exception ignored) {
		}
		return fov;
	}

	// ------------------------------------------------------------ swing

	/** Scales the visible arm-swing duration for SlowSwing / SwingAnimation. */
	public static int modifySwingDuration(LivingEntity entity, int original) {
		try {
			int duration = original;
			dev.amethyst.client.module.Module slowSwing = module("slow-swing");
			if (slowSwing instanceof dev.amethyst.client.module.impl.combat.SlowSwingModule slow
				&& slow.isEnabled()) {
				double speed = Math.max(0.05, slow.swingSpeed());
				duration = Math.max(1, (int) Math.round(duration / speed));
			}
			dev.amethyst.client.module.Module swingAnim = module("swing-animation");
			if (swingAnim instanceof dev.amethyst.client.module.impl.combat.SwingAnimationModule anim
				&& anim.isEnabled() && entity instanceof PlayerEntity player
				&& player.equals(net.minecraft.client.MinecraftClient.getInstance().player)) {
				duration = anim.adjustSelfDuration(duration);
			}
			return duration;
		} catch (Exception e) {
			return original;
		}
	}

	// ------------------------------------------------------------ time

	public static long modifyTimeOfDay(long original) {
		try {
		} catch (Exception ignored) {
		}
		return original;
	}

	// ------------------------------------------------------------ crosshair

	/** @return true when a module replaced the vanilla crosshair. */
	public static boolean renderCustomCrosshair(DrawContext context, RenderTickCounter tickCounter) {
		try {
		} catch (Exception e) {
			dev.amethyst.client.AmethystClient.LOGGER.warn("Custom crosshair rendering failed", e);
		}
		return false;
	}

	// ------------------------------------------------------------ attacks

	/**
	 * Called before the client attacks an entity. FriendGuard cancels attacks
	 * against friends here (covers vanilla clicks and every module attack).
	 */
	public static boolean shouldCancelAttack(PlayerEntity attacker, Entity target) {
		try {
			dev.amethyst.client.module.Module guard = module("friend-guard");
			if (guard instanceof dev.amethyst.client.module.impl.combat.FriendGuardModule friendGuard
				&& friendGuard.isEnabled()
				&& target instanceof PlayerEntity targetPlayer
				&& attacker != null) {
				String selfName = attacker.getName().getString();
				if (FriendManager.isFriend(targetPlayer.getName().getString())
					&& !targetPlayer.getName().getString().equalsIgnoreCase(selfName)) {
					if (friendGuard.notifyOn()) {
						NotificationManager.warn("FriendGuard blocked an attack on "
							+ targetPlayer.getName().getString() + ".");
					}
					return true;
				}
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	// ---- attack listeners (HitParticles, WTap, ...) ----

	private static final List<AttackListener> attackListeners = new ArrayList<>();

	public interface AttackListener {
		void onAttack(PlayerEntity attacker, Entity target);
	}

	public static void addAttackListener(AttackListener listener) {
		if (!attackListeners.contains(listener)) {
			attackListeners.add(listener);
		}
	}

	public static void removeAttackListener(AttackListener listener) {
		attackListeners.remove(listener);
	}

	/** Called after the client successfully attacked an entity. */
	public static void onAttack(PlayerEntity attacker, Entity target) {
		List<AttackListener> snapshot = new ArrayList<>(attackListeners);
		for (AttackListener listener : snapshot) {
			try {
				listener.onAttack(attacker, target);
			} catch (Exception e) {
				dev.amethyst.client.AmethystClient.LOGGER.warn("Attack listener failed: {}", e.getMessage());
			}
		}
	}

	// ------------------------------------------------------------ inventory leak

	/** @return true when the item draw should be redacted (NoInvLeak). */
	public static boolean shouldRedactItem() {
		try {
		} catch (Exception ignored) {
		}
		return false;
	}

	// ------------------------------------------------------------ camera

	/** The combined detached-look state the camera mixin should apply. */
	public static boolean cameraDetached() {
		return CameraState.freecamActive() || CameraState.freelookActive();
	}
}
