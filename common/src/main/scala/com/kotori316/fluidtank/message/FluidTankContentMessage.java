package com.kotori316.fluidtank.message;

import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.contents.TankUtil;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class FluidTankContentMessage implements IMessage<FluidTankContentMessage> {
    static final StreamCodec<FriendlyByteBuf, FluidTankContentMessage> STREAM_CODEC = CustomPacketPayload.codec(
        FluidTankContentMessage::write, FluidTankContentMessage::new
    );
    static final CustomPacketPayload.Type<FluidTankContentMessage> TYPE = new Type<>(IMessage.createIdentifier(FluidTankContentMessage.class));

    private final BlockPos pos;
    private final ResourceKey<Level> dim;
    private final Tank<FluidLike> tank;

    public FluidTankContentMessage(BlockPos pos, ResourceKey<Level> dim, Tank<FluidLike> tank) {
        this.pos = pos;
        this.dim = dim;
        this.tank = tank;
    }

    public FluidTankContentMessage(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.dim = buf.readResourceKey(Registries.DIMENSION);
        this.tank = TankUtil.load(buf.readNbt(), FluidAmountUtil.access());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos).writeResourceKey(dim);
        var tag = TankUtil.save(tank, FluidAmountUtil.access());
        buf.writeNbt(tag);
    }

    public void onReceive(@Nullable Level level) {
        if (level == null || !level.dimension().equals(this.dim)) return;

        if (level.getBlockEntity(this.pos) instanceof TileTank tileTank) {
            tileTank.setTank(this.tank);
        }
    }
}
