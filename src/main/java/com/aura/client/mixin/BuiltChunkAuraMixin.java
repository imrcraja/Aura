package com.aura.client.mixin;

import com.aura.client.core.AuraSafeExecutor;
import com.aura.client.renderer.chunk.AuraChunkBufferRegistry;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class BuiltChunkAuraMixin {
    @Inject(method = "getBuffer", at = @At("RETURN"))
    private void aura$bindBuffer(RenderLayer layer, CallbackInfoReturnable<VertexBuffer> cir) {
        AuraSafeExecutor.run(() ->
                AuraChunkBufferRegistry.bind((ChunkBuilder.BuiltChunk) (Object) this, layer, cir.getReturnValue()));
    }

    @Inject(method = "delete", at = @At("HEAD"))
    private void aura$unbindBuffers(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        AuraSafeExecutor.run(() ->
                AuraChunkBufferRegistry.unbind((ChunkBuilder.BuiltChunk) (Object) this));
    }
}
