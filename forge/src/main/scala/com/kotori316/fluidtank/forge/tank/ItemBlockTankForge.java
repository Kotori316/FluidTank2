package com.kotori316.fluidtank.forge.tank;

import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.ItemBlockTank;

public final class ItemBlockTankForge extends ItemBlockTank {
    public ItemBlockTankForge(BlockTank b) {
        super(b);
    }

    /*@Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new TankFluidItemHandler(blockTank().tier(), stack);
    }*/
}
