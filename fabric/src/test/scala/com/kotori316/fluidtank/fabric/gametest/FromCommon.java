package com.kotori316.fluidtank.fabric.gametest;

import com.kotori316.fluidtank.gametest.GameTestFunctions;
import com.kotori316.fluidtank.gametest.LoadTank2032Test;
import com.kotori316.testutil.common.TestFunction;

import java.util.List;

public final class FromCommon {
    public List<TestFunction> createTestFunctionsNoPlace() {
        return GameTestFunctions.createTestFunctionsNoPlace(GameTestFunctions.BATCH, TestFunction.NO_PLACE_STRUCTURE);
    }

    public List<TestFunction> createTestFunctionsPlace() {
        return GameTestFunctions.createTestFunctionsPlace(GameTestFunctions.BATCH, TestFunction.EMPTY_STRUCTURE);
    }

    public List<TestFunction> load2032Tank() {
        return LoadTank2032Test.tests("load_tank2032", "load_20_3_tanks").toList();
    }
}
