package com.aura.client.mixin;

import com.aura.client.core.AuraSafeExecutor;
import com.aura.client.core.AuraRuntime;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures completed vanilla chunk layer buffers at the extraction boundary.
 * Aura does not cancel or replace vanilla uploads yet; this is the safe bridge that
 * gives the renderer a version-specific entry point for future GPU extraction.
 */
@Mixin(ChunkBuilder.BuiltChunk.RebuildTask.class)
public abstract class ChunkBuilderAuraMixin {
    @Inject(method = "run", at = @At("HEAD"))
    private void aura$observeRebuild(CallbackInfo ci) {
        AuraSafeExecutor.run(AuraRuntime::observeChunkRebuild);
    }
}
