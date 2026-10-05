package com.aura.client.mixin;

import com.aura.client.core.AuraSafeExecutor;
import com.aura.client.core.AuraRuntime;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1 render-layer bridge.
 *
 * This hook intentionally observes the vanilla terrain boundary without cancelling it.
 * Aura can only replace a layer after a compatible vertex format/shader/material bridge
 * has been installed.
 */
@Mixin(WorldRenderer.class)
public final class WorldRendererAuraMixin {
    @Inject(
            method = "renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLorg/joml/Matrix4f;)V",
            at = @At("HEAD")
    )
    private void aura$observeRenderLayer(
            RenderLayer renderLayer,
            MatrixStack matrices,
            double cameraX,
            double cameraY,
            double cameraZ,
            Matrix4f positionMatrix,
            CallbackInfo ci
    ) {
        AuraSafeExecutor.run(() -> {
            if (AuraRuntime.isInitialized()) {
                AuraRuntime.observeRenderLayer(renderLayer);
            }
        });
    }
}
