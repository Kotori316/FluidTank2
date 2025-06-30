package com.kotori316.fluidtank.fabric;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.fabric.message.PacketHandler;
import com.kotori316.fluidtank.fabric.render.FluidRenderHelperFabric;
import com.kotori316.fluidtank.fabric.render.RenderTankFabric;
import com.kotori316.fluidtank.render.RenderItemCodecs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

import java.util.Map;

public final class FluidTankClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Client Initialize {}", FluidTankCommon.modId);
        PacketHandler.Client.initClient();

        var renderType = ChunkSectionLayer.CUTOUT_MIPPED;
        FluidTank.TANK_MAP.values().forEach(b -> BlockRenderLayerMap.putBlock(b, renderType));
        BlockRenderLayerMap.putBlock(FluidTank.BLOCK_CREATIVE_TANK, renderType);
        BlockRenderLayerMap.putBlock(FluidTank.BLOCK_VOID_TANK, renderType);
        // FluidTank.TANK_MAP.values().forEach(b -> BuiltinItemRendererRegistry.INSTANCE.register(b, RenderItemTank.INSTANCE()));
        // BuiltinItemRendererRegistry.INSTANCE.register(FluidTank.BLOCK_CREATIVE_TANK, RenderItemTank.INSTANCE());
        // BuiltinItemRendererRegistry.INSTANCE.register(FluidTank.BLOCK_VOID_TANK, RenderItemTank.INSTANCE());
        // var reservoirRenderer = new RenderReservoirItemFabric();
        RenderItemCodecs.registerLayerDefinitions(EntityModelLayerRegistry::registerModelLayer, t -> t::get);
        // FluidTank.RESERVOIR_MAP.values().forEach(b -> BuiltinItemRendererRegistry.INSTANCE.register(b, reservoirRenderer));
        RenderItemCodecs.registerSpecialModelRenderersCodec(new FluidRenderHelperFabric(), SpecialModelRenderers.ID_MAPPER::put, Map.of());

        BlockEntityRenderers.register(FluidTank.TILE_TANK_TYPE, RenderTankFabric::new);
        BlockEntityRenderers.register(FluidTank.TILE_CREATIVE_TANK_TYPE, RenderTankFabric::new);
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Client Initialize finished {}", FluidTankCommon.modId);
    }
}
