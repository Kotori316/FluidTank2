package com.kotori316.fluidtank.fabric.gametest;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.testutil.common.TestFunctionRegister;
import net.fabricmc.api.ModInitializer;

import java.util.function.Function;
import java.util.stream.Stream;

public final class FluidTankFabricGameTest implements ModInitializer {
    @Override
    public void onInitialize() {
        var tests = Stream.of(
            new CatTest().tests().stream(),
            new ConnectionStorageTest().generator().stream(),
            new FromCommon().load2032Tank().stream(),
            new FromCommon().createTestFunctionsNoPlace().stream(),
            new FromCommon().createTestFunctionsPlace().stream(),
            new PlatformAccessTest().tests().stream(),
            new RecipeTest().tests()
        ).flatMap(Function.identity());
        tests.forEach(TestFunctionRegister::registerTestFunction);
        TestFunctionRegister.addFunctionsToRegistry(FluidTankCommon.modId, TestFunctionRegister::vanillaTestFunctionRegister);
    }
}
