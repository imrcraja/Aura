# Contributing to Aura

Aura is split into a stable renderer core and small Minecraft-version adapters.

## Rules

1. Do not put Minecraft-version-specific code into the renderer core.
2. Keep launcher-specific assumptions out of the core; use platform adapters.
3. Performance changes must be measured with frame time, not FPS alone.
4. Avoid unbounded RAM/GPU-memory caches.
5. Compatibility changes should preserve vanilla/mod rendering semantics.

The current development target is Minecraft 1.20.1. Additional version adapters are planned after the core renderer is stable.
