package com.aura.client.renderer.chunk;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Decodes vanilla 1.20.1 chunk vertex buffers into Aura's canonical position-only mesh.
 *
 * This is intentionally an extraction bridge: it does not alter or cancel vanilla
 * rendering, and it only accepts POSITION elements encoded as three FLOAT components.
 */
public final class ChunkMeshExtractor {
    private ChunkMeshExtractor() {}

    public static ChunkMeshData positions(float[] positions, int[] indices) {
        if (positions == null || indices == null) {
            throw new IllegalArgumentException("mesh arrays must be non-null");
        }
        if ((positions.length % 3) != 0) {
            throw new IllegalArgumentException("positions must contain XYZ triples");
        }
        return new ChunkMeshData(positions.clone(), indices.clone());
    }

    public static ChunkMeshData fromBuiltBuffer(BufferBuilder.BuiltBuffer builtBuffer) {
        if (builtBuffer == null || builtBuffer.isEmpty()) return null;

        BufferBuilder.DrawParameters parameters = builtBuffer.getParameters();
        VertexFormat format = parameters.format();
        int stride = format.getVertexSizeByte();
        int vertexCount = parameters.vertexCount();
        if (stride <= 0 || vertexCount <= 0) return null;

        int positionOffset = 0;
        boolean foundPosition = false;
        for (VertexFormatElement element : format.getElements()) {
            if (element.isPosition()) {
                if (element.getComponentType() != VertexFormatElement.ComponentType.FLOAT
                        || element.getComponentCount() != 3) {
                    return null;
                }
                foundPosition = true;
                break;
            }
            positionOffset += element.getByteLength();
        }
        if (!foundPosition || positionOffset + 3 * Float.BYTES > stride) return null;

        ByteBuffer source = builtBuffer.getVertexBuffer().duplicate().order(ByteOrder.nativeOrder());
        long requiredLong = (long) vertexCount * stride;
        if (requiredLong > source.remaining() || requiredLong > Integer.MAX_VALUE) return null;

        float[] positions = new float[vertexCount * 3];
        int base = source.position();
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            int offset = base + vertex * stride + positionOffset;
            positions[vertex * 3] = source.getFloat(offset);
            positions[vertex * 3 + 1] = source.getFloat(offset + Float.BYTES);
            positions[vertex * 3 + 2] = source.getFloat(offset + Float.BYTES * 2);
        }

        return positions(positions, sequentialTriangles(vertexCount));
    }

    private static int[] sequentialTriangles(int vertexCount) {
        int triangleVertices = vertexCount - (vertexCount % 3);
        int[] indices = new int[triangleVertices];
        for (int i = 0; i < triangleVertices; i++) {
            indices[i] = i;
        }
        return indices;
    }
}
