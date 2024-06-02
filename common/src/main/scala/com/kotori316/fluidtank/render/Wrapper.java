package com.kotori316.fluidtank.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Objects;

final class Wrapper {

    private static final Vector4f vector4f = new Vector4f();
    private final VertexConsumer buffer;

    Wrapper(VertexConsumer buffer) {
        this.buffer = buffer;
    }

    private static Vector4f getPosVector(float x, float y, float z, PoseStack matrix) {
        Matrix4f matrix4f = matrix.last().pose();

        vector4f.set(x, y, z, 1.0F);
        vector4f.mul(matrix4f);
        return vector4f;
    }

    Wrapper pos(double x, double y, double z, PoseStack matrix) {
        Vector4f vector4f = getPosVector(((float) x), ((float) y), ((float) z), matrix);
        buffer.addVertex(vector4f.x(), vector4f.y(), vector4f.z());
        return this;
    }

    Wrapper color(int red, int green, int blue, int alpha) {
        buffer.setColor(red, green, blue, alpha);
        return this;
    }

    Wrapper tex(float u, float v) {
        buffer.setUv(u, v);
        return this;
    }

    @SuppressWarnings("SpellCheckingInspection")
    Wrapper lightmap(int sky, int block) {
        buffer.setUv1(10, 10).setUv2(block, sky);
        return this;
    }

    Wrapper lightMap(int light, int overlay) {
        buffer.setOverlay(overlay).setLight(light);
        return this;
    }

    void endVertex() {
        buffer.setNormal(0, 1, 0);
    }

    public VertexConsumer buffer() {
        return buffer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Wrapper) obj;
        return Objects.equals(this.buffer, that.buffer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buffer);
    }

    @Override
    public String toString() {
        return "Wrapper[" +
                "buffer=" + buffer + ']';
    }
}
