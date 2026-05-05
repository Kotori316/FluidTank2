package com.kotori316.fluidtank.fabric.render;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fabric.fluid.FabricConverter;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.VanillaFluid;
import com.kotori316.fluidtank.fluids.VanillaPotion;
import com.kotori316.fluidtank.render.FluidRenderHelper;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class FluidRenderHelperFabric implements FluidRenderHelper {
    static TextureAtlasSprite getSprite(GenericAmount<FluidLike> fluidLike) {
        var fluid = FluidLike.asFluid(fluidLike.content(), Fluids.WATER);
        var fluidState = fluid.defaultFluidState();
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet()
            .get(fluidState).stillMaterial().sprite();
    }

    static int getColor(GenericAmount<FluidLike> fluid) {
        if (fluid.content() instanceof VanillaFluid) {
            return FluidVariantRendering.getColor(FabricConverter.toVariant(fluid, Fluids.EMPTY));
        } else if (fluid.content() instanceof VanillaPotion) {
            return FluidAmountUtil.getComponentPatch(fluid)
                .flatMap(p -> Optional.ofNullable(p.get(DataComponentMap.EMPTY, DataComponents.POTION_CONTENTS)))
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

    @Override
    public TextureAtlasSprite getFluidTexture(Tank<FluidLike> tank, SpriteGetter materialSet) {
        return getSprite(tank.content());
    }

    @Override
    public int getFluidColor(Tank<FluidLike> tank) {
        return FluidRenderHelperFabric.getColorWithPos(tank.content(), Minecraft.getInstance().level, Objects.requireNonNull(Minecraft.getInstance().player).getOnPos());
    }
}
