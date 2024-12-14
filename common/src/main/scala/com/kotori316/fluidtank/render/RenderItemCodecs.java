package com.kotori316.fluidtank.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class RenderItemCodecs {
    public static final MapCodec<RenderReservoirItemUnbaked> RESERVOIR_ITEM_UNBAKED_MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        RecordCodecBuilder.of(RenderReservoirItemUnbaked::getClassName, "className", Codec.STRING)
    ).apply(i, RenderReservoirItemUnbaked::fromClassString));

    public record RenderReservoirItemUnbaked(
        Class<? extends RenderReservoirItem> clazz)
        implements SpecialModelRenderer.Unbaked {
        @SuppressWarnings("unchecked")
        private static RenderReservoirItemUnbaked fromClassString(String className) {
            try {
                return new RenderReservoirItemUnbaked((Class<? extends RenderReservoirItem>) Class.forName(className, true, RenderReservoirItem.class.getClassLoader()));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        String getClassName() {
            return clazz.getName();
        }

        @Override
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            var model = new ReservoirModel(modelSet.bakeLayer(ReservoirModel.LOCATION));
            try {
                return clazz.getConstructor(ReservoirModel.class)
                    .newInstance(model);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return RESERVOIR_ITEM_UNBAKED_MAP_CODEC;
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
