package com.kotori316.fluidtank.fabric;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.fabric.message.PacketHandler;
import com.kotori316.fluidtank.fabric.render.RenderItemTank;
import com.kotori316.fluidtank.fabric.render.RenderReservoirItemFabric;
import com.kotori316.fluidtank.fabric.render.RenderTankFabric;
import com.kotori316.fluidtank.render.ReservoirModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.server.packs.PackType;

public final class FluidTankClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Client Initialize {}", FluidTankCommon.modId);
        PacketHandler.Client.initClient();

        var renderType = RenderType.cutoutMipped();
        FluidTank.TANK_MAP.values().forEach(b -> BlockRenderLayerMap.INSTANCE.putBlock(b, renderType));
        BlockRenderLayerMap.INSTANCE.putBlock(FluidTank.BLOCK_CREATIVE_TANK, renderType);
        BlockRenderLayerMap.INSTANCE.putBlock(FluidTank.BLOCK_VOID_TANK, renderType);
        FluidTank.TANK_MAP.values().forEach(b -> BuiltinItemRendererRegistry.INSTANCE.register(b, RenderItemTank.INSTANCE()));
        BuiltinItemRendererRegistry.INSTANCE.register(FluidTank.BLOCK_CREATIVE_TANK, RenderItemTank.INSTANCE());
        BuiltinItemRendererRegistry.INSTANCE.register(FluidTank.BLOCK_VOID_TANK, RenderItemTank.INSTANCE());
        var reservoirRenderer = new RenderReservoirItemFabric();
        EntityModelLayerRegistry.registerModelLayer(ReservoirModel.LOCATION, ReservoirModel::createDefinition);
        FluidTank.RESERVOIR_MAP.values().forEach(b -> BuiltinItemRendererRegistry.INSTANCE.register(b, reservoirRenderer));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(reservoirRenderer);

        BlockEntityRenderers.register(FluidTank.TILE_TANK_TYPE, RenderTankFabric::new);
        BlockEntityRenderers.register(FluidTank.TILE_CREATIVE_TANK_TYPE, RenderTankFabric::new);
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Client Initialize finished {}", FluidTankCommon.modId);
    }
}
