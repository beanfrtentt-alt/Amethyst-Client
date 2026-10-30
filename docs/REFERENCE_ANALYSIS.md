# Amethyst Client — Reference Analysis (Anubis Beta 0.9.9.3)

This document records the technical analysis of the supplied reference archive
`Anubis Beta 0.9.9.3(1).jar` performed **before** any implementation work started.
Its purpose is to establish which behaviors are *verified*, which are *inferred*,
and which *cannot be determined* from the binary alone. Amethyst Client is an
independent implementation: no reference code or assets are reused (see
"Licensing" at the end).

Analysis methods used: ZIP/jar extraction, `javap` bytecode inspection of
`fabric.mod.json`, mixin configs, default configuration JSON, class/package
structure, and selected class signatures. No full decompilation was performed;
internal behavior statements below are limited to what bytecode signatures and
bundled resources can establish.

## 1. Verified metadata (from `fabric.mod.json`)

| Property | Value |
|---|---|
| Mod ID | `anubisplus` |
| Version | `0.9.9.3+mc1.21.11` |
| Name | Anubis |
| License | **GPL-3.0-only** (full GPL-3 text bundled as `LICENSE_anubis`) |
| Environment | client |
| Entrypoints | `preLaunch: net.anubis.client.security.EarlyBuildCheck`, `client: net.anubis.client.AnubisClientMod` |
| Depends | `fabricloader >=0.16.0`, `minecraft ~1.21.11`, `fabric-api *`, `java >=21`, plus several Fabric API modules and `fabric-rendering-fluids-v1 >=2.0.0` |
| Mixin configs | `anubis.mixins.json` (148 client mixins/accessors), `anubis.indigo.mixins.json` (1 mixin, plugin-gated, `required:false`) |
| Bundled jars | 7 Fabric API sub-modules + imgui-java 1.90.0 (binding, lwjgl3, 3× natives) |
| `breaks` | long incompatibility list including Embeddium, OptiFabric, VulkanMod, Sodium add-ons (render-pipeline conflicts) |

Verified externally (Fabric meta API / Mojang piston-meta, October 2026):

- Minecraft **1.21.11** requires **Java 21** (`java-runtime-delta`, major 21), released 2025-12-09.
- Yarn mappings for 1.21.11: **1.21.11+build.6** (latest stable build).
- Fabric Loader latest stable: **0.20.1**.
- Fabric API for 1.21.11: **0.141.6+1.21.11**.
- Fabric Loom current: **1.19.3** (works with Gradle 9.8.2 per fabric-example-mod).

## 2. Verified architecture (from bytecode inspection)

Package root `net.anubis.client` with subpackages: `addon`, `chat`, `config`,
`discord`, `esp`, `event`(+`impl`), `gui`(+`mainmenu`), `imgui`, `input`,
`module`(+`impl` and category subfolders), `security`, `service`(+`humanize`),
`util`, `webhook`. A separate `net.anubis.launcher` package exists (launcher
integration, not analyzed further).

### Module system (verified via `javap`)

`net.anubis.client.module.ClientModule`:
- Fields: `id`, `name`, `description`, `category`, `enabled`, keybind (`int`),
  hold-mode flag, settings list, built-in `showInHud` / `toggleMessage` /
  `notifications` boolean settings, owned listener handles.
- Lifecycle: `onEnable()` / `onDisable()` / `toggle()` / `setEnabled(boolean)`,
  re-entrancy guards (`enabling`, `disabling`, `disableRequested`).
- Defaults/migration: `captureDefaults()`, `resetSettings()`, `enabledByDefault()`,
  `persistEnabled()`, `settingsVersion()`, `migrateSettings(int)`.
- Events: `on(EventType, [Stage], [EventPriority], Consumer)` registration tied
  to module lifecycle.

`ModuleCategory` is a *runtime-registered* enum-like class (not a Java enum):
categories found in the default Click GUI layout: **RENDER, MISC, DONUT,
MOVEMENT, VISUALS, COMBAT, CLIENT, BASE_RAID, EASY_PLACE, MACRO**, plus
`DONUT_MACROS` visible in bytecode. `ModuleRegistry` + `StableId` +
`ModuleRevision` handle registration and stable identifiers across renames.

### Settings (verified class list)

`BooleanSetting`, `NumberSetting`, `ColorSetting`, `KeySetting`, `ModeSetting`
(enum), `StringSetting` (+presets), `StringListSetting` (multi-select),
`ActionSetting`, `BlockColorListSetting`, `ItemColorListSetting`.

### Event bus (verified class list)

Typed event system: `EventType`/`EventPool`/`PooledEventType`,
`CancellableEvent`, `EventPriority`, `Stage`, `LambdaListenerFactory`,
listener registration handles. Concrete events: `TickEvent`, `KeyEvent`,
`MouseInputEvent`, `PlayerMoveEvent`, `PlayerAttackEntityEvent`,
`PacketSendEvent` / `PacketReceiveEvent` (netty + applied variants),
`Render2DEvent`, `Render3DEvent`, `ChatSendEvent`, `BlockUpdateEvent`,
`WorldEntityAddEvent`, `ModuleToggleEvent`.

### Configuration (verified class list + defaults JSON)

- Bundled defaults under `assets/anubis/defaults/`: `modules.json` (schema
  `"version": 3`, 62 module entries with per-module `settings` maps, some with
  `enabled: true` and integer `keybind`), `clickgui.json` (panel positions per
  category + screen size + collapsed panels), `presets.json` (named presets).
- Runtime classes: `ConfigProfiles` (multi-profile with versioned headers and
  `Match`/`Part` migration logic), `BundledDefaults`, `ModuleConfigStore`,
  `ProfileAutoLoad`, `FriendStore`, `ThemeStore`, `ShareDictionary` /
  `ModuleCodes` / `ProfileCodes` (share-code encoding), `ProfileException`.
- Keybind defaults found in `modules.json`: air-stuck `Z` (90), render distance
  toggle `B` (66), freecam `V` (85), block entity debug `.` (46), freelook
  backtick (96), xray `X` (88), zoom `C` (67), weapon inspect key 67.

### GUI (verified)

The reference uses **Dear ImGui via imgui-java 1.90.0** with native binaries
bundled for Windows/Linux/macOS. `clickgui.json` stores per-category panel
positions. This confirms an immediate-mode GUI approach; it does **not** reveal
anything about Amethyst's GUI, which is implemented independently with vanilla
`Screen`/`DrawContext` widgets (no native dependencies).

### Rendering resources (verified)

- Core shaders (`assets/anubis/shaders/core/`): `aura_spread.fsh`,
  `color_grading.fsh`, `glow_esp.fsh`, `hand_aura.fsh`, `motion_blur.fsh`,
  `sky_theme.fsh`/`.vsh` — confirms post-processing (color grading, motion
  blur, shader ESP) and sky theming are implemented as core-shader injections.
- Fonts: Geist, Manrope, Rajdhani, Silkscreen, Jersey 10 — all with SIL OFL
  license files bundled. (Amethyst uses its own typography choices and does not
  bundle these.)
- Sounds: `spawner_notify.ogg` + `sounds.json`.
- Textures: GUI logos, menu backgrounds, cosmetic headdress, sky-emoji set,
  peep art, jump-circle logos, knife skin base.

### External integrations (verified from packages/config)

`discord` (Rich Presence), `webhook` (HTTP webhooks used by notifier modules),
`util/spotify` (Spotify HUD with poller class; integration details not
verifiable from bytecode), addon support (`addon`, `suggests anubis-jsmacros`).

## 3. Module inventory (62 ids from `assets/anubis/defaults/modules.json`)

Combat: aim-assist, triggerbot, kill-aura, mace, w-tap-recode, friend-guard,
auto-clicker, trident-util, hand-aura, target-esp, totem-spirit, slow-swing,
swing-animation.

Movement: freecam, speed, boat-fly, trident-boost, air-stuck, auto-relog,
render-distance-toggle, custom-fov, zoom, player-bypass, freelook.

Visuals/render: esp, glow-esp, nametags, spawner-nametags, block-esp,
block-overlay, storage-esp, tracers, fullbright, xray, custom-crosshair,
custom-glint, hit-particles, particles, motion-blur, atmosphere, color-grading,
jump-circles, character-3d, floating-relics, time-changer, hand-pose,
weapon-inspect.

World/analysis: pearl-trajectory, trajectories, netherite-finder,
sus-chunk-finder, chunk-scanner, block-entity-debug, region-map, radar,
quantum, amethyst-bypass.

Utility/client: name-protect, no-inv-leak, weather-notifier, hotkeys, spotify-hud.

Additional *classes* exist beyond these defaults (e.g. crystal/anchor/mace
auras, anti-vanish, staff-list, waypoints, AH sniper/seller bots, TPA bots,
macros, pink-water, character cosmetics, knife skins, sky lyrics/emoji,
auto-eat/totem/replenish, elytra fly, jesus, sneak, spam, skin-change,
discord-presence, fake-stats). These are **not** part of Amethyst's required
feature set and are intentionally out of scope (see README "Not implemented").

## 4. Verified vs inferred vs undeterminable

**Verified** (directly observable): metadata, versions, license, mixin target
names, event/setting/config class structures, default settings schemas,
keybind defaults, shader/asset inventory, category layout, ImGui GUI usage.

**Inferred** (from names/signatures/settings, not full behavior): what each
module *does* mechanically (e.g. aim-assist `range`/`fov`/`speed` implies
FOV-limited smooth rotation toward targets; auto-relog `y-level`/`delay`
implies disconnect when below a Y threshold; quantum's `advanced-debug`/
`render` flags imply a diagnostics overlay). Amethyst implements equivalents
specified from these settings plus standard client-mod practice, and documents
any interpretation.

**Not determinable from the binary** (and therefore **not claimed**):
- Exact anti-cheat evasion techniques (player-bypass, amethyst-bypass,
  quantum): packet-level behavior cannot be established without runtime
  analysis. Amethyst does **not** reimplement these; see README.
- Server-bot features (AH sniper, sell bots, staff trackers) internals.
- Spotify integration protocol (poller exists; endpoint/auth unknown).
- Any server-side detection capability claims.

## 5. Mixin surface (for Amethyst's planning only)

The reference declares 148 client mixins targeting renderer, input, packet,
entity and GUI classes. Amethyst deliberately keeps a **minimal** mixin surface
(see `src/main/resources/amethyst.mixins.json`) and uses Fabric API events
(`ClientTickEvents`, `WorldRenderEvents`, `HudElementRegistry`,
`ClientCommandRegistrationCallback`, `ClientReceiveMessageEvents`,
`ClientSendMessageEvents`, `KeyBindingHelper`) wherever possible. Mixins are
used only where no supported API exists (FOV, camera override for freecam,
crosshair replacement, time-of-day override, swing duration).

Yarn 1.21.11 method names verified against the published mappings
(`yarn-1.21.11+build.6` tiny file) before writing each mixin:
`GameRenderer.getFov(Camera,float,boolean)` = `method_3196`,
`MinecraftClient.doAttack()` = `method_1536`, `doItemUse()` = `method_1583`,
`InGameHud.renderCrosshair(DrawContext,RenderTickCounter)` = `method_1736`,
`ClientWorld.getTimeOfDay()` = `method_8532`,
`LivingEntity.getHandSwingDuration()` = `method_6028`.

## 6. Licensing conclusion

The reference identifies itself as GPL-3.0-only. Amethyst Client is an
**independent original implementation**: no reference classes were decompiled
into source, no assets (fonts, textures, sounds, shaders) were copied, and no
GPL-covered code is included. Amethyst Client is distributed under the
**MIT license** (see `LICENSE`). The reference jar was used strictly as a
behavioral/metadata reference, which is consistent with the analysis performed
here; nothing from it is redistributed inside this repository.
