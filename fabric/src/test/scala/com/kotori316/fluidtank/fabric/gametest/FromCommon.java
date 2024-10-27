package com.kotori316.fluidtank.fabric.gametest;

import com.kotori316.fluidtank.gametest.GameTestFunctions;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.List;

public final class FromCommon implements FabricGameTest {
    @GameTestGenerator
    public List<TestFunction> createTestFunctionsNoPlace() {
        return GameTestFunctions.createTestFunctionsNoPlace("defaultBatch", GameTestUtil.NO_PLACE_STRUCTURE);
    }

    @GameTestGenerator
    public List<TestFunction> createTestFunctionsPlace() {
        return GameTestFunctions.createTestFunctionsPlace("defaultBatch", GameTestUtil.EMPTY_STRUCTURE);
    }
}
