package com.kotori316.fluidtank.forge.gametest;

import com.kotori316.fluidtank.FluidTankCommon;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(FluidTankCommon.modId)
public final class FromCommon {
    /*@GameTestGenerator
    public List<TestFunction> createTestFunctionsNoPlace() {
        return GameTestFunctions.createTestFunctionsNoPlace(FluidTankCommon.modId + "." + GameTestFunctions.BATCH, "%1$s:%1$s.%2$s".formatted(FluidTankCommon.modId, GameTestUtil.NO_PLACE_STRUCTURE));
    }

    @GameTestGenerator
    public List<TestFunction> createTestFunctionsPlace() {
        return GameTestFunctions.createTestFunctionsPlace(FluidTankCommon.modId + "." + GameTestFunctions.BATCH, "%1$s:%1$s.%2$s".formatted(FluidTankCommon.modId, GameTestUtil.EMPTY_STRUCTURE));
    }

    @GameTestGenerator
    public List<TestFunction> load2032Tank() {
        return LoadTank2032Test.tests(FluidTankCommon.modId + "." + "loadTank2032", "%1$s:%1$s.%2$s".formatted(FluidTankCommon.modId, "load_20_3_tanks")).toList();
    }*/
}
