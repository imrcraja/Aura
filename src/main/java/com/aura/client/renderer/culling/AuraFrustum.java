package com.aura.client.renderer.culling;

/** Version-neutral frustum interface. Version adapters provide camera/frustum data. */
public interface AuraFrustum {
    boolean isVisible(double minX, double minY, double minZ,
                      double maxX, double maxY, double maxZ);
}
