package com.kotori316.fluidtank.fabric.tank;

import com.kotori316.fluidtank.fabric.BeforeMC;
import com.kotori316.fluidtank.fabric.FluidTank;
import com.kotori316.fluidtank.fabric.recipe.ModifiableSingleItemStorage;
import com.kotori316.fluidtank.fabric.recipe.RecipeInventoryUtil;
import com.kotori316.fluidtank.tank.Tier;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ItemStorageCheckTest extends BeforeMC {
    @Test
    void create() {
        var storage = assertDoesNotThrow(() -> ModifiableSingleItemStorage.getContext(new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD))));
        assertNotNull(storage);
    }

    @Test
    void exchangeCommit() {
        var storage = ModifiableSingleItemStorage.getContext(new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD)));
        try (var tx = Transaction.openOuter()) {
            var exchanged = storage.exchange(ItemVariant.of(Items.APPLE), 1, tx);
            assertEquals(1L, exchanged);
            assertEquals(ItemVariant.of(Items.APPLE), storage.getItemVariant());
            tx.commit();
        }
        assertEquals(ItemVariant.of(Items.APPLE), storage.getItemVariant());
    }

    @Test
    void exchangeAbort() {
        var storage = ModifiableSingleItemStorage.getContext(new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD)));
        try (var tx = Transaction.openOuter()) {
            var exchanged = storage.exchange(ItemVariant.of(Items.APPLE), 1, tx);
            assertEquals(1L, exchanged);
            assertEquals(ItemVariant.of(Items.APPLE), storage.getItemVariant());
        }
        assertEquals(ItemVariant.of(FluidTank.TANK_MAP.get(Tier.WOOD)), storage.getItemVariant());
    }

    @Test
    void filling() {
        var stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD));
        var storage = RecipeInventoryUtil.getFluidHandler(stack);
        try (var tx = Transaction.openOuter()) {
            var inserted = storage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BLOCK, tx);
            assertEquals(FluidConstants.BLOCK, inserted);
            var item = storage.context().getItemVariant();
            assertEquals(FluidTank.TANK_MAP.get(Tier.WOOD).asItem(), item.getItem());
            assertTrue(item.hasComponents());
        }
    }
}
