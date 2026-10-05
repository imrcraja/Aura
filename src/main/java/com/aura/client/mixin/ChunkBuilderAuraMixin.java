package com.aura.client.mixin;

import com.aura.client.core.AuraSafeExecutor;
import com.aura.client.core.AuraRuntime;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1 chunk upload bridge.
 *
 * It observes the exact BuiltBuffer produced by vanilla chunk building. Aura only
 * decodes/caches a copy; vanilla upload and rendering remain untouched until the
 * replacement path is proven safe.
 */
@Mixin(ChunkBuilder.class)
public abstract class ChunkBuilderAuraMixin {
    @Inject(
            method = "scheduleUpload(Lnet/minecraft/client/render/BufferBuilder$BuiltBuffer;Lnet/minecraft/client/gl/VertexBuffer;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD")
    )
    private void aura$observeBuiltBuffer(
            BufferBuilder.BuiltBuffer builtBuffer,
            net.minecraft.client.gl.VertexBuffer glBuffer,
            CallbackInfo ci
    ) {
        AuraSafeExecutor.run(() -> AuraRuntime.observeBuiltBuffer(builtBuffer));
    }
}
