package com.kotori316.fluidtank.fabric.message;

import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.message.FluidTankContentMessage;
import com.kotori316.fluidtank.message.IMessage;
import com.kotori316.fluidtank.tank.TileTank;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * To client only.
 */
public final class FluidTankContentMessageFabric extends FluidTankContentMessage {
    static final CustomPacketPayload.Type<FluidTankContentMessageFabric> TYPE = new Type<>(IMessage.createIdentifier(FluidTankContentMessageFabric.class));
    static final StreamCodec<FriendlyByteBuf, FluidTankContentMessageFabric> STREAM_CODEC = CustomPacketPayload.codec(
        IMessage::write, FluidTankContentMessageFabric::new
    );
    static final ResourceLocation NAME = IMessage.createIdentifier(FluidTankContentMessageFabric.class);

    public FluidTankContentMessageFabric(BlockPos pos, ResourceKey<Level> dim, Tank<FluidLike> tank) {
        super(pos, dim, tank);
    }

    public FluidTankContentMessageFabric(TileTank tileTank) {
        this(
            tileTank.getBlockPos(),
            Objects.requireNonNull(tileTank.getLevel()).dimension(),
            tileTank.getTank()
        );
    }

    FluidTankContentMessageFabric(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    static class HandlerHolder {
        static final ClientPlayNetworking.PlayPayloadHandler<FluidTankContentMessageFabric> HANDLER = (message, context) -> {
            var level = context.client().level;
            context.client().execute(() -> message.onReceive(level));
        };
    }
}
