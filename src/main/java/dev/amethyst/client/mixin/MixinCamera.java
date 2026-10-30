package dev.amethyst.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import dev.amethyst.client.util.CameraState;

/**
 * Detaches the camera from the player while Freecam or Freelook is active.
 * The vanilla camera update runs first; afterwards the detached position and
 * rotation are applied on top so no vanilla behavior is disturbed when the
 * features are off.
 */
@Mixin(Camera.class)
abstract class MixinCamera {
	@Invoker("setPos")
	abstract void amethyst$invokeSetPos(double x, double y, double z);

	@Invoker("setRotation")
	abstract void amethyst$invokeSetRotation(float yaw, float pitch);

	// Camera#update(World, Entity focused, boolean thirdPerson, boolean inverseView, float tickDelta)
	@Inject(method = "update", at = @At("TAIL"))
	private void amethyst$afterUpdate(World world, Entity focused, boolean thirdPerson, boolean inverseView,
	                                  float tickDelta, CallbackInfo ci) {
		if (CameraState.freecamActive()) {
			Vec3d pos = CameraState.freecamPos();
			amethyst$invokeSetPos(pos.x, pos.y, pos.z);
			amethyst$invokeSetRotation(CameraState.freecamYaw(), CameraState.freecamPitch());
		} else if (CameraState.freelookActive()) {
			Camera self = (Camera) (Object) this;
			amethyst$invokeSetRotation(self.getYaw() + CameraState.freelookYaw(),
				self.getPitch() + CameraState.freelookPitch());
		}
	}
}
