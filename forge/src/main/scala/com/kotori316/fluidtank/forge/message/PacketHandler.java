package com.kotori316.fluidtank.forge.message;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.message.IMessage;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.function.BiConsumer;

public final class PacketHandler {
    private static final int PROTOCOL = 1;
    private static final SimpleChannel CHANNEL =
        ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "main"))
            .networkProtocolVersion(PROTOCOL)
            .acceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .simpleChannel()
            // FluidTankContentMessageForge
            .messageBuilder(FluidTankContentMessageForge.class)
            .codec(FluidTankContentMessageForge.STREAM_CODEC)
            .consumerNetworkThread(setHandled(FluidTankContentMessageForge::onReceiveMessage))
            .add();

    public static void init() {
    }

    static <MSG> BiConsumer<MSG, CustomPayloadEvent.Context> setHandled(BiConsumer<MSG, CustomPayloadEvent.Context> messageConsumer) {
        return (msg, context) -> {
            messageConsumer.accept(msg, context);
            context.setPacketHandled(true);
        };
    }

    public static void sendToClient(IMessage<?> message, Level level) {
        if (level.getServer() instanceof GameTestServer) {
            // sending message to test server will cause NPE
            return;
        }
        CHANNEL.send(message, PacketDistributor.DIMENSION.with(level.dimension()));
    }
}
