package com.kotori316.fluidtank.forge.message;

import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.forge.FluidTank;
import com.kotori316.fluidtank.message.FluidTankContentMessage;
import com.kotori316.fluidtank.message.IMessage;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.Objects;

public final class FluidTankContentMessageForge extends FluidTankContentMessage {
    static final CustomPacketPayload.Type<FluidTankContentMessageForge> TYPE = new Type<>(IMessage.createIdentifier(FluidTankContentMessageForge.class));
    static final StreamCodec<FriendlyByteBuf, FluidTankContentMessageForge> STREAM_CODEC = CustomPacketPayload.codec(
        IMessage::write, FluidTankContentMessageForge::new
    );

    public FluidTankContentMessageForge(BlockPos pos, ResourceKey<Level> dim, Tank<FluidLike> tank) {
        super(pos, dim, tank);
    }

    public FluidTankContentMessageForge(TileTank tileTank) {
        this(
            tileTank.getBlockPos(),
            Objects.requireNonNull(tileTank.getLevel()).dimension(),
            tileTank.getTank()
        );
    }

    FluidTankContentMessageForge(FriendlyByteBuf buf) {
        super(buf);
    }

    void onReceiveMessage(CustomPayloadEvent.Context context) {
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
