package com.kotori316.fluidtank.neoforge.tank;

import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.ItemBlockTank;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public final class ItemBlockTankNeoForge extends ItemBlockTank {
    public ItemBlockTankNeoForge(BlockTank b) {
        super(b);
    }

    public static ResourceHandler<FluidResource> initCapabilities(ItemStack stack, ItemAccess access) {
        return new TankFluidItemHandler(((ItemBlockTankNeoForge) stack.getItem()).blockTank().tier(), access);
    }
}
