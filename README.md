# Amethyst Client

A client-side Fabric mod for Minecraft Java Edition 1.21.11, targeting Java 21.
It provides a configurable module system, a Click GUI, HUD widgets, keybinds,
JSON configuration/profiles, friends, and client commands.

## Build

Use Java 21 and run `./gradlew build` on Linux/macOS or `gradlew.bat build` on
Windows. The first build needs internet access to download Gradle and project
dependencies. The resulting remapped mod JAR is written under `build/libs/`.

## Controls and commands

- Press **Right Shift** to open the Click GUI.
- Use `.am help` in chat or `/amethyst` for available commands.
- Press **B** for the render-distance toggle and **Z** for hold-mode Air Stuck
  by default. Other module binds can be assigned in the GUI or with `.am bind`.

## Notes

This is a client-side mod; some movement/combat modules may not work on every
server and can violate server rules. Use them only where permitted. Configuration
files are stored in the `amethyst` directory under the Minecraft config folder.
