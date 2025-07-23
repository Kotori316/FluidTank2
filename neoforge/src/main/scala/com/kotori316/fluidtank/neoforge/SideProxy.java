package com.kotori316.fluidtank.neoforge;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.neoforge.render.FluidRenderHelperNeoForge;
import com.kotori316.fluidtank.neoforge.render.FluidRenderHelperNeoForge$;
import com.kotori316.fluidtank.neoforge.render.RenderTankNeoForge;
import com.kotori316.fluidtank.render.RenderItemCodecs;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class SideProxy {

    public abstract Optional<Level> getLevel(IPayloadContext context);

    public static SideProxy get() {
        return switch (FMLEnvironment.dist) {
            case CLIENT -> ClientProxy.client();
            case DEDICATED_SERVER -> ServerProxy.server();
        };
    }

    private static class ClientProxy extends SideProxy {
        private static SideProxy client() {
            return new ClientProxy();
        }

        @SubscribeEvent
        public void registerTESR(FMLClientSetupEvent event) {
            FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Client Initialize {}", FluidTankCommon.modId);
            BlockEntityRenderers.register(FluidTank.TILE_TANK_TYPE.get(), RenderTankNeoForge::new);
            BlockEntityRenderers.register(FluidTank.TILE_CREATIVE_TANK_TYPE.get(), RenderTankNeoForge::new);
            FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Client Initialize finished {}", FluidTankCommon.modId);
        }

        @Override
        public Optional<Level> getLevel(IPayloadContext context) {
            return Optional.of(context.player().level());
        }

        @SubscribeEvent
        public void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            RenderItemCodecs.registerLayerDefinitions(event::registerLayerDefinition, Function.identity());
        }

        @SubscribeEvent
        public void registerReloadListener(AddClientReloadListenersEvent event) {
        }

        @SubscribeEvent
        public void registerClientItemExtension(RegisterClientExtensionsEvent event) {
        }

        @SubscribeEvent
        public void registerSpecialModelRenderer(RegisterSpecialModelRendererEvent event) {
            RenderItemCodecs.registerSpecialModelRenderersCodec(
                FluidRenderHelperNeoForge$.MODULE$,
                event::register,
                Map.of(
                    RenderItemCodecs.RESERVOIR_MODEL, FluidRenderHelperNeoForge.reservoirUnbaked(),
                    RenderItemCodecs.TANK_MODEL, FluidRenderHelperNeoForge.tankUnbaked()
                )
            );
        }
    }

    private static class ServerProxy extends SideProxy {
        private static SideProxy server() {
            return new ServerProxy();
        }

        @Override
        public Optional<Level> getLevel(IPayloadContext context) {
            return Optional.of(context.player().level());
        }

        /**
         * Here to avoid the exception to "class has no @SubscribeEvent methods, but register was called anyway."
         */
        @SubscribeEvent
        public void dummy(FMLCommonSetupEvent event) {
        }
    }
}
