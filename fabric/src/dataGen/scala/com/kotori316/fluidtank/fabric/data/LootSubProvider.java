package com.kotori316.fluidtank.fabric.data;

import com.kotori316.fluidtank.fabric.FluidTank;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.TankLootFunction;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

final class LootSubProvider extends FabricBlockLootTableProvider {
    LootSubProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        FluidTank.TANK_MAP.values().forEach(b -> this.add(b, tankContent(b)));
        Stream.of(FluidTank.BLOCK_CREATIVE_TANK, FluidTank.BLOCK_VOID_TANK).forEach(b -> this.add(b, tankContent(b)));
        this.add(FluidTank.BLOCK_CAT, this::createSingleItemTable);
    }

    /*@Override
    protected Iterable<Block> getKnownBlocks() {
        var list = new ArrayList<Block>(FluidTank.TANK_MAP.values().stream().toList());
        list.add(FluidTank.BLOCK_CREATIVE_TANK.get());
        list.add(FluidTank.BLOCK_VOID_TANK.get());
        list.add(FluidTank.BLOCK_CAT.get());
        return list;
    }*/

    private LootTable.Builder tankContent(BlockTank tank) {
        return createSingleItemTable(tank)
            .apply(TankLootFunction.builder());
    }
}
