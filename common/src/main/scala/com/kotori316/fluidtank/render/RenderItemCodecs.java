package com.kotori316.fluidtank.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class RenderItemCodecs {

    public static class RenderReservoirItemUnbaked implements SpecialModelRenderer.Unbaked {
        private final FluidRenderHelper helper;
        private final MapCodec<RenderReservoirItemUnbaked> codec;

        public RenderReservoirItemUnbaked(FluidRenderHelper helper) {
            this.helper = helper;
            this.codec = MapCodec.unit(() -> new RenderReservoirItemUnbaked(helper));
        }

        @Override
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            var model = new ReservoirModel(modelSet.bakeLayer(ReservoirModel.LOCATION));
            return new RenderReservoirItem(model, helper);
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return this.codec;
        }
    }

    public record RenderTankItemUnbaked() implements SpecialModelRenderer.Unbaked {

        @Override
        public @Nullable SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return null;
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static ResourceLocation atlas() {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
