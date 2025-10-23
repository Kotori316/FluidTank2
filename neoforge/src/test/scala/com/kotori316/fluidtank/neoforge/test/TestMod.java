package com.kotori316.fluidtank.neoforge.test;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.neoforge.cat.EntityChestAsTank;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.Consumer;

@Mod("fluidtank_test")
public final class TestMod {
    public TestMod(ModContainer container) {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Initialize {} with {}", container.getModId(), container.getClass().getName());
    }

    public static ResourceHandler<FluidResource> getCatHandler(ResourceHandler<ItemResource> handler) {
        return EntityChestAsTank.getProxy(handler);
    }

    public static PlatformAccess getPlatformAccess() {
        try {
            var clazz = Class.forName("com.kotori316.fluidtank.neoforge.NeoForgePlatformAccess");
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (PlatformAccess) constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void inTransaction(Consumer<Transaction> consumer) {
        try (var tx = Transaction.openRoot()) {
            consumer.accept(tx);
        }
    }
}
