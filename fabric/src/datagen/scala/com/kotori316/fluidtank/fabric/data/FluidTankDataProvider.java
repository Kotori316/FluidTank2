package com.kotori316.fluidtank.fabric.data;

import com.kotori316.fluidtank.FluidTankCommon;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class FluidTankDataProvider implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FluidTankCommon.LOGGER.info("FluidTank data generator initialized");
    }
}
