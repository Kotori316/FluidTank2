package com.kotori316.fluidtank.neoforge.message;

import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.message.FluidTankContentMessage;
import com.kotori316.fluidtank.message.IMessage;
import com.kotori316.fluidtank.neoforge.FluidTank;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;

public final class FluidTankContentMessageNeoForge extends FluidTankContentMessage {
    static final CustomPacketPayload.Type<FluidTankContentMessageNeoForge> TYPE = new Type<>(IMessage.createIdentifier(FluidTankContentMessageNeoForge.class));
    static final StreamCodec<FriendlyByteBuf, FluidTankContentMessageNeoForge> STREAM_CODEC = CustomPacketPayload.codec(
        IMessage::write, FluidTankContentMessageNeoForge::new
    );

    public FluidTankContentMessageNeoForge(BlockPos pos, ResourceKey<Level> dim, Tank<FluidLike> tank) {
        super(pos, dim, tank);
    }

    public FluidTankContentMessageNeoForge(TileTank tileTank) {
        this(
            tileTank.getBlockPos(),
            Objects.requireNonNull(tileTank.getLevel()).dimension(),
            tileTank.getTank()
        );
    }

    FluidTankContentMessageNeoForge(FriendlyByteBuf buf) {
        super(buf);
    }

    void onReceiveMessage(IPayloadContext context) {
        // Should be client side
        context.enqueueWork(() ->
            this.onReceive(FluidTank.proxy.getLevel(context).orElse(null))
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
