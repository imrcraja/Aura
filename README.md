# Aura

**AuraMod + Aura Renderer** — a mobile-first rendering platform for Minecraft Java Edition.

![Aura logo](src/main/resources/assets/aura/icon.svg)

## Current development scope

- **AuraMod:** Minecraft 1.20.1 (Fabric).
- **Aura Renderer:** one version-independent renderer core with per-version adapters.
- **Target launchers:** PojavLauncher, Zalith Launcher, LTW and compatible Android Java launchers.
- **Long-term versions:** adapters can cover releases from old Minecraft generations through future releases without cloning the renderer core.

## Architecture

```text
Minecraft version adapter
        ↓
    Aura Renderer Core
        ↓
  ┌─────┴─────┐
 Vulkan      OpenGL
 backend     backend
  └─────┬─────┘
        ↓
      GPU
```

Aura is designed to reduce CPU-side rendering overhead rather than pretend Minecraft can run with zero CPU work. The performance target is maximum sustainable hardware throughput with stable frame times.

## Status

**Early alpha.** The cross-version architecture, adaptive memory profile and backend boundaries are in place. The deep renderer replacement, native Vulkan submission path, GPU-driven chunk pipeline, and broad mod compatibility layer are the next implementation stages.

## Logo

The Aura brand uses one shared logo for AuraMod and Aura Renderer.
