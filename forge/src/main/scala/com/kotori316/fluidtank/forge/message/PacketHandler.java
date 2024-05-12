package com.kotori316.fluidtank.forge.message;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.message.IMessage;
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
        ChannelBuilder.named(new ResourceLocation(FluidTankCommon.modId, "main"))
            .networkProtocolVersion(PROTOCOL)
            .acceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .simpleChannel()
            // FluidTankContentMessageForge
            .messageBuilder(FluidTankContentMessageForge.class)
            .decoder(FluidTankContentMessageForge.STREAM_CODEC::decode)
            .encoder((message, buf) -> FluidTankContentMessageForge.STREAM_CODEC.encode(buf, message))
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
        CHANNEL.send(message, PacketDistributor.DIMENSION.with(level.dimension()));
    }
}
