# Amethyst Client repair status

This archive contains source-level compatibility repairs based on the previous Gradle compiler report and inspection of the cached Minecraft 1.21.11 Yarn-mapped jar.

## Repairs applied

- Updated the GUI key binding to use `KeyBinding.Category`.
- Updated keyboard polling to pass `Window` rather than its native long handle.
- Updated key localization to use the 1.21.11 `KeyInput` API.
- Updated character input to use `CharInput.isValidChar()` and `asString()`.
- Fixed the palette operator call and added the missing GUI reset handler.
- Made HUD render bounds protected for HUD subclasses in subpackages.
- Fixed integer setting validation and several boxed-number conversions.
- Replaced removed entity-position calls with current position APIs.
- Updated vehicle control checks and mob spawn-group classification.
- Updated disconnect and mixin callback return-value usage.
- Removed compile-time references to optional modules that are not implemented in this repository; their hooks now preserve vanilla behavior.
- Added basic setting unit tests, README, `.gitignore`, MIT license file, and a GitHub Actions build workflow.
- Removed placeholder GitHub URLs from Fabric metadata.

## Verification status

- JSON syntax: `fabric.mod.json` and `amethyst.mixins.json` parse successfully.
- Internal Java imports: no missing `dev.amethyst.client` imports were found by a static source scan.
- API inspection: the cached Minecraft 1.21.11 Yarn-mapped jar confirms the updated `InputUtil`, `KeyInput`, `CharInput`, `KeyBinding.Category`, `Entity`, and `EntityType` signatures used by the fixes.
- Gradle build: **NOT VERIFIED**. The environment could not resolve `services.gradle.org` to download the Gradle 9.8.2 distribution (`UnknownHostException`). A direct Java compilation attempt was also blocked by missing external dependency jars in this environment. Do not treat this archive as a successfully built or runtime-tested release yet.

## Next verification step

On a network-connected machine with Java 21, run `gradlew.bat clean build` from the repository root. Fix any newly exposed compiler errors, then launch the mod in a clean Minecraft 1.21.11 + Fabric profile and test the GUI, module toggles, configuration persistence, and mixins. The source-level fixes have not been represented as a successful build.
