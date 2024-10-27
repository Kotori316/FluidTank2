package com.kotori316.fluidtank.fabric.gametest;

import com.kotori316.fluidtank.gametest.GameTestFunctions;
import com.kotori316.fluidtank.gametest.LoadTank2032Test;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.List;

public final class FromCommon implements FabricGameTest {
    @GameTestGenerator
    public List<TestFunction> createTestFunctionsNoPlace() {
        return GameTestFunctions.createTestFunctionsNoPlace(GameTestFunctions.BATCH, GameTestUtil.NO_PLACE_STRUCTURE);
    }

    @GameTestGenerator
    public List<TestFunction> createTestFunctionsPlace() {
        return GameTestFunctions.createTestFunctionsPlace(GameTestFunctions.BATCH, GameTestUtil.EMPTY_STRUCTURE);
    }

    @GameTestGenerator
    public List<TestFunction> load2032Tank() {
        return LoadTank2032Test.tests("loadTank2032", "load_20_3_tanks").toList();
    }
}
