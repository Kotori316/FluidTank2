package com.kotori316.fluidtank.neoforge.message;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.message.IMessage;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;

public final class PacketHandler {
    public static void init(RegisterPayloadHandlerEvent event) {
        var registrar = event.registrar(FluidTankCommon.modId).versioned("1");
        registrar.play(
            FluidTankContentMessageNeoForge.TYPE,
            FluidTankContentMessageNeoForge.STREAM_CODEC,
            handler -> handler.client(FluidTankContentMessageNeoForge::onReceiveMessage)
        );
    }

    public static void sendToClient(IMessage<?> message, Level level) {
        PacketDistributor.DIMENSION.with(level.dimension()).send(message);
    }
}
