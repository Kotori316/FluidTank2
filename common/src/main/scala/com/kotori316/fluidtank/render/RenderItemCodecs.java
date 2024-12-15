package com.kotori316.fluidtank.render;

import com.kotori316.fluidtank.FluidTankCommon;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class RenderItemCodecs {
    public static final ResourceLocation RESERVOIR_MODEL = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "reservoir");
    public static final ResourceLocation TANK_MODEL = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "tank");

    public static SpecialModelRenderer.Unbaked reservoirModelUnbaked(FluidRenderHelper helper) {
        return new RenderReservoirItemUnbaked(helper);
    }

    public static SpecialModelRenderer.Unbaked tankModelUnbaked(FluidRenderHelper helper) {
        return new RenderTankItemUnbaked(helper);
    }

    private static class RenderReservoirItemUnbaked implements SpecialModelRenderer.Unbaked {
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

    private static class RenderTankItemUnbaked implements SpecialModelRenderer.Unbaked {
        private final FluidRenderHelper helper;
        private final MapCodec<RenderTankItemUnbaked> codec;

        public RenderTankItemUnbaked(FluidRenderHelper helper) {
            this.helper = helper;
            this.codec = MapCodec.unit(() -> new RenderTankItemUnbaked(helper));
        }

        @Override
        public @Nullable SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            var model = new TankModel(modelSet.bakeLayer(TankModel.LOCATION));
            return new RenderItemTank(model, helper);
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return codec;
        }
    }

    @SuppressWarnings("deprecation")
    public static ResourceLocation atlas() {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
