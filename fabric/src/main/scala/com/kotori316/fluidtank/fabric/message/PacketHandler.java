package com.kotori316.fluidtank.fabric.message;

import com.kotori316.fluidtank.message.IMessage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class PacketHandler {
    public static class Server {
        public static void initServer() {
            // Use ServerPlayNetworking.registerGlobalReceiver
        }

    }

    @Environment(EnvType.CLIENT)
    public static class Client {
        public static void initClient() {
            ClientPlayNetworking.registerGlobalReceiver(FluidTankContentMessageFabric.TYPE, FluidTankContentMessageFabric.HandlerHolder.HANDLER);
        }
    }

    public static void sendToClientWorld(@NotNull IMessage<?> message, @NotNull Level level) {
        for (ServerPlayer player : PlayerLookup.world((ServerLevel) level)) {
            ServerPlayNetworking.send(player, message);
        }
    }
}
