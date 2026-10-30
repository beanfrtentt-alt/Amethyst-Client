package dev.amethyst.client.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.amethyst.client.module.impl.combat.AimAssistModule;
import dev.amethyst.client.module.impl.combat.AutoClickerModule;
import dev.amethyst.client.module.impl.combat.FriendGuardModule;
import dev.amethyst.client.module.impl.combat.HandAuraModule;
import dev.amethyst.client.module.impl.combat.KillAuraModule;
import dev.amethyst.client.module.impl.combat.MaceModule;
import dev.amethyst.client.module.impl.combat.SlowSwingModule;
import dev.amethyst.client.module.impl.combat.SwingAnimationModule;
import dev.amethyst.client.module.impl.combat.TriggerbotModule;
import dev.amethyst.client.module.impl.combat.WTapModule;

import dev.amethyst.client.module.impl.movement.AirStuckModule;
import dev.amethyst.client.module.impl.movement.AutoRelogModule;
import dev.amethyst.client.module.impl.movement.BoatFlyModule;
import dev.amethyst.client.module.impl.movement.CustomFovModule;
import dev.amethyst.client.module.impl.movement.FreelookModule;
import dev.amethyst.client.module.impl.movement.FreecamModule;
import dev.amethyst.client.module.impl.movement.RenderDistanceToggleModule;
import dev.amethyst.client.module.impl.movement.SpeedModule;
import dev.amethyst.client.module.impl.movement.TridentBoostModule;
import dev.amethyst.client.module.impl.movement.ZoomModule;

import dev.amethyst.client.module.impl.render.BlockEspModule;
import dev.amethyst.client.module.impl.render.BlockOverlayModule;
import dev.amethyst.client.module.impl.render.GlowEspModule;
import dev.amethyst.client.module.impl.render.NetheriteFinderModule;
import dev.amethyst.client.module.impl.render.SpawnerNametagsModule;
import dev.amethyst.client.module.impl.render.TargetEspModule;
import dev.amethyst.client.module.impl.render.TracersModule;
import dev.amethyst.client.module.impl.render.StorageEspModule;
import dev.amethyst.client.module.impl.render.NametagsModule;
import dev.amethyst.client.module.impl.render.EspModule;

/**
 * Aggregates all concrete module instances. Called by {@link AmethystClient} during init.
 */
public final class ModuleRegistrar {
    public static Collection<Module> constructAll() {
        List<Module> modules = new ArrayList<>();
        // Combat modules
        modules.add(new AimAssistModule());
        modules.add(new AutoClickerModule());
        modules.add(new FriendGuardModule());
        modules.add(new HandAuraModule());
        modules.add(new KillAuraModule());
        modules.add(new MaceModule());
        modules.add(new SlowSwingModule());
        modules.add(new SwingAnimationModule());
        modules.add(new TriggerbotModule());
        modules.add(new WTapModule());
        // Movement modules
        modules.add(new AirStuckModule());
        modules.add(new AutoRelogModule());
        modules.add(new BoatFlyModule());
        modules.add(new CustomFovModule());
        modules.add(new FreelookModule());
        modules.add(new FreecamModule());
        modules.add(new RenderDistanceToggleModule());
        modules.add(new SpeedModule());
        modules.add(new TridentBoostModule());
        modules.add(new ZoomModule());
        // Render modules
        modules.add(new BlockEspModule());
        modules.add(new BlockOverlayModule());
        modules.add(new GlowEspModule());
        modules.add(new NetheriteFinderModule());
        modules.add(new SpawnerNametagsModule());
        modules.add(new TargetEspModule());
        modules.add(new TracersModule());
        modules.add(new StorageEspModule());
        modules.add(new NametagsModule());
        modules.add(new EspModule());
        // Subclasses set some defaults (e.g. keybinds and hold mode) after
        // the base constructor, so capture them only once construction ends.
        for (Module module : modules) {
            module.captureDefaults();
        }
        return modules;
    }
}
