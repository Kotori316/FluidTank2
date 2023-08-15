package com.kotori316.fluidtank.forge.data;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.forge.FluidTank;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.TankLootFunction;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

final class LootSubProvider extends BlockLootSubProvider {
    LootSubProvider() {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS);
    }

    @Override
    protected void generate() {
        FluidTankCommon.LOGGER.info(FluidTankDataProvider.MARKER(), "Generating Loot table");

        FluidTank.TANK_MAP.values().stream().map(RegistryObject::get).forEach(b -> this.add(b, tankContent(b)));
        Stream.of(FluidTank.BLOCK_CREATIVE_TANK, FluidTank.BLOCK_VOID_TANK).map(RegistryObject::get).forEach(b -> this.add(b, tankContent(b)));
        this.add(FluidTank.BLOCK_CAT.get(), this::createSingleItemTable);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        var list = new ArrayList<Block>(FluidTank.TANK_MAP.values().stream().map(RegistryObject::get).toList());
        list.add(FluidTank.BLOCK_CREATIVE_TANK.get());
        list.add(FluidTank.BLOCK_VOID_TANK.get());
        list.add(FluidTank.BLOCK_CAT.get());
        return list;
    }

    private LootTable.Builder tankContent(BlockTank tank) {
        return createSingleItemTable(tank)
                .apply(TankLootFunction.builder());
    }
}
