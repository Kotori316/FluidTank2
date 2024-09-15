package com.kotori316.fluidtank.neoforge.reservoir;

import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.Tier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public final class ItemReservoirNeoForge extends ItemReservoir {
    public ItemReservoirNeoForge(Tier tier) {
        super(tier);
    }

    public static IFluidHandlerItem initCapabilities(ItemStack stack, Void ignored) {
        return new ReservoirFluidHandler((ItemReservoir) stack.getItem(), stack);
    }
}
