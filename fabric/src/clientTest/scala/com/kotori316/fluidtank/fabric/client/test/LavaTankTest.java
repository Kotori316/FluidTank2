package com.kotori316.fluidtank.fabric.client.test;

import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import net.fabricmc.fabric.api.client.gametest.v1.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.TestSingleplayerContext;

import static com.kotori316.fluidtank.fabric.client.test.WaterTankTest.testFluid;

@SuppressWarnings("UnstableApiUsage")
public final class LavaTankTest implements FluidTankClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context, TestSingleplayerContext singlePlayerContext) {
        String testName = "lava";
        var fluid = FluidAmountUtil.BUCKET_LAVA();

        testFluid(this, context, singlePlayerContext, testName, fluid);
    }
}
