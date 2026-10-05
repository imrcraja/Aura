package com.aura.client.renderer.backend;

/** Runtime marker for a backend that must be disabled after a recoverable GPU failure. */
public final class AuraBackendFailure extends RuntimeException {
    public AuraBackendFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
