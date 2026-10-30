# Amethyst Client repair and audit notes

Target: Minecraft Java 1.21.11, Yarn `1.21.11+build.6`, Java 21.

## Fixes applied

- Fixed the compile issues reported in the supplied Gradle log: updated the
  1.21.11 key-binding category API, keyboard polling API, key-label fallback,
  entity-position API, world time callback return handling, `CharInput` handling,
  `IntUnaryOperator` call, missing setting import, reconnect disconnect API,
  vehicle access, floating-point settings, entity-type checks, HUD bounds access,
  and missing GUI reset action. Removed stale imports/references to modules that
  are not present in this source tree.
- Fixed the render-distance toggle undoing itself immediately when its one-shot
  action disabled its module. The original render distance is restored at client
  shutdown instead.
- Released hold-mode modules when a screen opens so a held action cannot remain
  enabled after the key is released.
- Prevented Auto Relog from disconnecting single-player worlds and added a
  threshold latch so it does not repeatedly disconnect after reconnecting below
  the configured Y level.
- Implemented Freecam scroll-wheel speed adjustment and wired its notification
  option into GUI/keybind toggles.
- Added a real module reset operation that restores constructor-defined keybinds,
  hold mode, visibility/notification flags, and each setting's default value.
- Hardened config saving and profile export: failed writes are reported, dirty
  configs remain queued for retry, relative destinations are supported, profile
  names are validated, and profile directory streams are closed.
- Preserved unknown settings for modules that do exist in this version, so
  loading/saving a config with settings from a newer version does not silently
  discard those keys.
- Made HUD config loading tolerant of malformed coordinates, rejected duplicate
  HUD element IDs, and made HUD position reset persist and separate widgets.
- Added the MIT license file referenced by the Gradle JAR task and a README with
  build and usage instructions.

## Checks performed

- Verified the supplied archive and final archive structure.
- Parsed `fabric.mod.json` and `amethyst.mixins.json`; verified every listed mixin
  class and the client entrypoint exists.
- Checked internal `dev.amethyst.client` imports, unique module IDs, and setting
  IDs within each module source file.
- Used the mapped Minecraft 1.21.11 JAR included in the supplied project cache to
  confirm key API signatures and the target methods used by the mixins, including
  `Mouse.onMouseScroll(long, double, double)`.
- Attempted a direct Java compilation against the included local JAR cache. That
  cache does not include the full Gradle/Minecraft compile classpath (notably
  Gson, Brigadier, LWJGL, Mixin, Fabric Loader, and other transitive libraries),
  so it cannot provide a valid full compile result.

## Verification limitation

A full `gradlew clean build` has **not** been verified in this environment because
it cannot resolve the Gradle distribution/dependencies from the internet. Please
run `gradlew.bat clean build --no-configuration-cache` on Windows with Java 21 and
network access. Any remaining compiler output should be treated as a follow-up
bug report, not as proof that the project is already release-ready.
