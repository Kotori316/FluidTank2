package com.kotori316.fluidtank.fabric.recipe;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.minecraft.world.item.ItemStack;

public final class ModifiableSingleItemStorage extends SingleItemStorage {
    public ModifiableSingleItemStorage(ItemStack stack) {
        this.variant = ItemVariant.of(stack);
        this.amount = stack.getCount();
    }

    @Override
    protected long getCapacity(ItemVariant variant) {
        return Long.MAX_VALUE;
    }

    public static ContainerItemContext getContext(ItemStack stack) {
        return ContainerItemContext.ofSingleSlot(new ModifiableSingleItemStorage(stack));
    }
}
