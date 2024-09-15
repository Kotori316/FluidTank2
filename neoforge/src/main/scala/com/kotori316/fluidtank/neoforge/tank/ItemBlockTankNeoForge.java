package com.kotori316.fluidtank.neoforge.tank;

import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.ItemBlockTank;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public final class ItemBlockTankNeoForge extends ItemBlockTank {
    public ItemBlockTankNeoForge(BlockTank b) {
        super(b);
    }

    public static IFluidHandlerItem initCapabilities(ItemStack stack, Void ignored) {
        return new TankFluidItemHandler(((ItemBlockTankNeoForge) stack.getItem()).blockTank().tier(), stack);
    }
}
