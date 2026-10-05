# Aura

**AuraMod + Aura Renderer** — a mobile-first rendering and performance platform for Minecraft Java Edition.

![Aura logo](src/main/resources/assets/aura/icon.svg)

**Branding / Author:** RC Empire  
**Project owner:** RC RAJA  
**Official project channel:** https://youtube.com/@RCRAJAGAMER2.0

## Current development scope

- **AuraMod:** Minecraft 1.20.1 (Fabric).
- **Aura Renderer:** one version-independent renderer core with per-version adapters.
- **Target launchers:** PojavLauncher, Zalith Launcher, LTW and compatible Android Java launchers.
- **Long-term versions:** the same renderer core is designed to serve old Minecraft generations and future releases through small version adapters rather than duplicated renderer implementations.

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

Aura targets lower CPU-side render submission overhead, efficient batching/caching and stable frame times. Minecraft game logic still requires CPU work; Aura's goal is to move as much rendering work as practical toward efficient GPU execution.

## Adaptive performance

Aura detects available JVM memory and processor capacity and uses a bounded cache budget instead of blindly consuming all available RAM. The renderer core also records a rolling 120-frame timing window so later adapters/backends can make adaptive decisions from real frame-time data.

## 1.20.1 integration milestone

The 1.20.1 build now has a real world-render lifecycle telemetry path through Fabric's rendering events. This is intentionally the first safe integration layer: it avoids taking over third-party renderer hooks before the backend is ready.

The deeper renderer replacement is staged behind the same stable interfaces:

1. GPU resource and command abstractions.
2. Chunk extraction/upload pipeline.
3. Frustum/occlusion culling and batching.
4. OpenGL compatibility backend.
5. Native Vulkan backend where the launcher/runtime exposes a usable Vulkan stack.
6. Mod compatibility bridge.
7. Additional Minecraft version adapters.

## Compatibility target

The long-term target is a single Aura renderer core that can serve Minecraft versions from very old releases through 26.x and future versions. Compatibility is implemented with adapters because Minecraft's internal rendering APIs change between releases; a claim of one untouched binary automatically working on every future version would be misleading.

## Status

**Alpha — active development.**

### Production hardening

The 1.20.1 pipeline includes bounded CPU/GPU caches, revision-safe chunk uploads, canonical terrain attribute extraction, backend initialization fallback, OpenGL GPU terrain resources, and regression tests for core mesh/cache invariants. The repository contains the stable architecture, bounded adaptive memory profile, backend boundaries, 1.20.1 lifecycle integration and frame telemetry. The production Vulkan renderer and full vanilla chunk-render replacement are still under active implementation and are not falsely marked as complete.

## License and attribution

Aura is released under the MIT License. The project branding and author attribution are **RC Empire**.

The Aura logo is shared by AuraMod and Aura Renderer.
