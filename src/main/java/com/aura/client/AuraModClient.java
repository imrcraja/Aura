package com.aura.client;

import com.aura.client.core.AuraRuntime;
import net.fabricmc.api.ClientModInitializer;

/** Entry point for AuraMod. */
public final class AuraModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AuraRuntime.initialize();
    }
}
