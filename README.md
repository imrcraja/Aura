# Aura

**AuraMod + Aura Renderer** — a mobile-first rendering platform for Minecraft Java Edition.

## Project layout

- `AuraMod` — Minecraft-side integration, compatibility hooks and configuration.
- `Aura Renderer` — version-independent renderer architecture.
- `renderer/backend` — Vulkan/OpenGL backend boundaries.
- `renderer/compat` — small adapters for individual Minecraft rendering generations.

## Current development target

- AuraMod: Minecraft **1.20.1** (Fabric).
- Renderer core: version-independent API/boundaries.
- Intended launcher targets: PojavLauncher, Zalith Launcher, LTW and compatible Android Java launchers.
- Future: expand version adapters while keeping one renderer core.

## Important engineering goal

Aura does not promise a fixed FPS number. It targets the lowest practical CPU submission overhead, stable frame times, aggressive batching/culling, adaptive memory use and maximum sustainable GPU throughput for the device.

## Status

Early alpha. The repository currently contains the cross-version architecture and backend boundaries; the native Vulkan submission layer and deep Minecraft renderer replacement are subsequent implementation stages.

## License

MIT
