package com.kotori316.fluidtank.neoforge.message;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.message.IMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PacketHandler {
    public static void init(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(FluidTankCommon.modId).versioned("1");
        registrar.playToClient(
            FluidTankContentMessageNeoForge.TYPE,
            FluidTankContentMessageNeoForge.STREAM_CODEC,
            FluidTankContentMessageNeoForge::onReceiveMessage
        );
    }

    public static void sendToClient(IMessage<?> message, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersInDimension(serverLevel, message);
        } else {
            FluidTankCommon.LOGGER.error("PacketHandler#sendToClient is called in client level");
        }
    }
}
