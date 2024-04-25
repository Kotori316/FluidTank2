package com.kotori316.fluidtank.fabric.render;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.fabric.fluid.FabricConverter;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.VanillaFluid;
import com.kotori316.fluidtank.fluids.VanillaPotion;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

final class RenderResourceHelper {
    static TextureAtlasSprite getSprite(GenericAmount<FluidLike> fluid) {
        return FluidVariantRendering.getSprite(FabricConverter.toVariant(fluid, Fluids.WATER));
    }

    static int getColor(GenericAmount<FluidLike> fluid) {
        if (fluid.content() instanceof VanillaFluid) {
            return FluidVariantRendering.getColor(FabricConverter.toVariant(fluid, Fluids.EMPTY));
        } else if (fluid.content() instanceof VanillaPotion) {
            return FluidAmountUtil.getComponentPatch(fluid)
                .map(p -> p.<PotionContents>get(DataComponents.POTION_CONTENTS))
                .flatMap(Function.identity())
                .map(PotionContents::getColor).orElse(16253176);
        } else {
            throw new AssertionError();
        }
    }

    static int getColorWithPos(GenericAmount<FluidLike> fluid, @Nullable BlockAndTintGetter view, BlockPos pos) {
        if (fluid.content() instanceof VanillaFluid) {
            return FluidVariantRendering.getColor(FabricConverter.toVariant(fluid, Fluids.EMPTY), view, pos);
        } else {
            return getColor(fluid);
        }
    }

    static int getLuminance(GenericAmount<FluidLike> fluid) {
        return FluidVariantAttributes.getLuminance(FabricConverter.toVariant(fluid, Fluids.EMPTY));
    }
}
