package com.kotori316.fluidtank.forge.reservoir;

import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.Tier;

public final class ItemReservoirForge extends ItemReservoir {
    public ItemReservoirForge(Tier tier) {
        super(tier);
    }

    /*@Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ReservoirFluidHandler(this, stack);
    }*/
}
