package com.aura.client.renderer.culling;

/** Simple bounding-sphere frustum implementation for backend-independent culling tests. */
public final class SphereFrustum implements AuraFrustum {
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;

    public SphereFrustum(double centerX, double centerY, double centerZ, double radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = Math.max(0.0, radius);
    }

    @Override
    public boolean isVisible(double minX, double minY, double minZ,
                             double maxX, double maxY, double maxZ) {
        double x = clamp(centerX, minX, maxX);
        double y = clamp(centerY, minY, maxY);
        double z = clamp(centerZ, minZ, maxZ);
        double dx = centerX - x;
        double dy = centerY - y;
        double dz = centerZ - z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
