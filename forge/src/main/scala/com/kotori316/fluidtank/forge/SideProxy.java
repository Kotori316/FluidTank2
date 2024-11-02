package com.kotori316.fluidtank.forge;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.forge.render.RenderReservoirItemForge;
import com.kotori316.fluidtank.forge.render.RenderTankForge;
import com.kotori316.fluidtank.render.ReservoirModel;
import com.kotori316.fluidtank.tank.PlatformTankAccess;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class SideProxy {

    public abstract Optional<Level> getLevel(CustomPayloadEvent.Context context);

    public static SideProxy get() {
        return switch (FMLEnvironment.dist) {
            case CLIENT -> ClientProxy.client();
            case DEDICATED_SERVER -> ServerProxy.server();
        };
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientProxy extends SideProxy {
        private static SideProxy client() {
            return new ClientProxy();
        }

        @SubscribeEvent
        public void registerTESR(FMLClientSetupEvent event) {
            FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Client Initialize {}", FluidTankCommon.modId);
            BlockEntityRenderers.register(FluidTank.TILE_TANK_TYPE.get(), RenderTankForge::new);
            BlockEntityRenderers.register(FluidTank.TILE_CREATIVE_TANK_TYPE.get(), RenderTankForge::new);

            setRenderLayer();
            FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Client Initialize finished {}", FluidTankCommon.modId);
        }

        @SuppressWarnings("deprecation")
        private void setRenderLayer() {
            // Loading from JSON is now broken in Forge?
            PlatformTankAccess.getInstance().getTankBlockMap().values().stream().map(Supplier::get)
                .forEach(b -> ItemBlockRenderTypes.setRenderLayer(b, RenderType.cutout()));
        }

        @Override
        public Optional<Level> getLevel(CustomPayloadEvent.Context context) {
            var serverWorld = Optional.ofNullable(context.getSender()).map(ServerPlayer::getCommandSenderWorld);
            return serverWorld.or(() -> LogicalSidedProvider.CLIENTWORLD.get(context.isClientSide()));
        }

        @SubscribeEvent
        public void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ReservoirModel.LOCATION, ReservoirModel::createDefinition);
        }

        @SubscribeEvent
        public void registerReloadListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(RenderReservoirItemForge.INSTANCE);
        }
    }

    private static class ServerProxy extends SideProxy {
        private static SideProxy server() {
            return new ServerProxy();
        }

        @Override
        public Optional<Level> getLevel(CustomPayloadEvent.Context context) {
            return Optional.ofNullable(context.getSender()).map(ServerPlayer::getCommandSenderWorld);
        }
    }
}
