package com.kotori316.fluidtank.neoforge.reservoir;

import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.Tier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public final class ItemReservoirNeoForge extends ItemReservoir {
    public ItemReservoirNeoForge(Tier tier) {
        super(tier);
    }

    public static ResourceHandler<FluidResource> initCapabilities(ItemStack stack, ItemAccess ignored) {
        return new ReservoirFluidHandler((ItemReservoir) stack.getItem(), stack);
    }
}
