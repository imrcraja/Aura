package com.aura.client.mixin;

import com.aura.client.core.AuraSafeExecutor;
import com.aura.client.core.AuraRuntime;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Safe 1.20.1 chunk rebuild boundary.
 *
 * The nested RebuildTask is private in the mapped API, so this bridge observes the
 * public BuiltChunk task creation method instead of targeting the private class directly.
 */
@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class ChunkBuilderAuraMixin {
    @Inject(method = "createRebuildTask", at = @At("RETURN"))
    private void aura$observeRebuildTask(
            ChunkRendererRegionBuilder builder,
            CallbackInfoReturnable<ChunkBuilder.BuiltChunk.Task> cir
    ) {
        AuraSafeExecutor.run(() -> AuraRuntime.observeChunkRebuildTask(cir.getReturnValue()));
    }
}
