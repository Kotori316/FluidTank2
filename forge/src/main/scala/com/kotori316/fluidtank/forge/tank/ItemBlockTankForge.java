package com.kotori316.fluidtank.forge.tank;

import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.ItemBlockTank;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public final class ItemBlockTankForge extends ItemBlockTank {
    public ItemBlockTankForge(BlockTank b) {
        super(b);
    }

    @Override
    public ICapabilityProvider getCapabilityProvider(ItemStack stack) {
        return new TankFluidItemHandler(blockTank().tier(), stack);
    }
}
