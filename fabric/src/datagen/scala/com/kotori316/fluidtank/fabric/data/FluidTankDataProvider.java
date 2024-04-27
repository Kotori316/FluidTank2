package com.kotori316.fluidtank.fabric.data;

import com.kotori316.fluidtank.FluidTankCommon;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public final class FluidTankDataProvider implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FluidTankCommon.LOGGER.info("FluidTank data generator initialized");
        var pack = fabricDataGenerator.createPack();

        pack.addProvider(RecipeProvider::new);
        pack.addProvider((o, future) -> new LootTableProvider(
            o,
            Set.of(),
            List.of(new LootTableProvider.SubProviderEntry(() -> new LootSubProvider(o, future), LootContextParamSets.BLOCK)),
            future)
        );
        pack.addProvider(ModelProvider::new);
        FluidTankCommon.LOGGER.info("FluidTank data generator registered");
    }
}
