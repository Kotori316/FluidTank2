package com.kotori316.fluidtank.neoforge.gametest;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.gametest.GameTestFunctions;
import com.kotori316.fluidtank.gametest.LoadTank2032Test;
import com.kotori316.testutil.GameTestUtil;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;

@GameTestHolder(FluidTankCommon.modId)
public final class FromCommon {
    @GameTestGenerator
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
    }
}
