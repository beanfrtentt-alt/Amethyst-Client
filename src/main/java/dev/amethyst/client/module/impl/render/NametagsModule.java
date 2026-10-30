package dev.amethyst.client.module.impl.render;

import dev.amethyst.client.module.Module;
import dev.amethyst.client.module.ModuleCategory;
import dev.amethyst.client.render.RenderUtil;
import dev.amethyst.client.setting.BooleanSetting;
import dev.amethyst.client.setting.DoubleSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Nametags draws an enhanced floating label over nearby entities: name,
 * hearts and distance. Vanilla nametags remain untouched.
 */
public final class NametagsModule extends Module {
	private final DoubleSetting range = add(new DoubleSetting("range", "Range",
		"Maximum distance to entities.", 48.0, 8.0, 96.0, 1.0, 0));
	private final BooleanSetting showHealth = add(new BooleanSetting("health", "Show Health",
		"Draw a health bar under the name.", true));
	private final BooleanSetting showDistance = add(new BooleanSetting("distance", "Show Distance",
		"Append the distance in meters.", true));

	public NametagsModule() {
		super("nametags", "Nametags", "Enhanced nametags with health and distance.", ModuleCategory.RENDER);
	}

	@Override
	public void onRender3D() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return;
		}
		double maxDist = range.get() * range.get();
		for (Entity entity : client.world.getEntities()) {
			if (!(entity instanceof net.minecraft.entity.LivingEntity living)
				|| entity == player || !entity.isAlive()
				|| entity.squaredDistanceTo(player) > maxDist) {
				continue;
			}
			double distance = Math.sqrt(entity.squaredDistanceTo(player));
			String name = entity.getName().getString();
			String label = showDistance.get() ? String.format("%s (%dm)", name, (int) distance) : name;
			float health = living.getHealth();
			float maxHealth = Math.max(0.5f, living.getMaxHealth());
			// horizontal marker line at head height
			Vec3d base = entity.getEntityPos().add(0, entity.getHeight() + 0.4, 0);
			RenderUtil.line(base.add(-0.3, 0, 0), base.add(0.3, 0, 0), 0xFFEAE6F2, 1.0f);
			if (showHealth.get()) {
				int healthColor = health / maxHealth > 0.5f ? 0xFF53E89B
					: health / maxHealth > 0.25f ? 0xFFE8B453 : 0xFFE8537A;
				RenderUtil.line(base.add(-0.3, -0.06, 0), base.add(-0.3 + 0.6 * (health / maxHealth), -0.06, 0),
					healthColor, 1.5f);
			}
		}
	}
}
