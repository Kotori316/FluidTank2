package com.kotori316.fluidtank.fabric.render;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.render.RenderReservoirItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class RenderReservoirItemFabric extends RenderReservoirItem
    implements IdentifiableResourceReloadListener,
    BuiltinItemRendererRegistry.DynamicItemRenderer {

    @Override
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        super.renderByItem(stack, mode, matrices, vertexConsumers, light, overlay);
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "render_reservoir_item_fabric");
    }

    @Override
    public TextureAtlasSprite getFluidTexture(Tank<FluidLike> tank) {
        return RenderResourceHelper.getSprite(tank.content());
    }

    @Override
    public int getFluidColor(Tank<FluidLike> tank) {
        return RenderResourceHelper.getColorWithPos(tank.content(), Minecraft.getInstance().level, Objects.requireNonNull(Minecraft.getInstance().player).getOnPos());
    }
}
