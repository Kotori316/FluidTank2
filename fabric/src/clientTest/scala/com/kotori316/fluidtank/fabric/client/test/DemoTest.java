package com.kotori316.fluidtank.fabric.client.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

@SuppressWarnings("UnstableApiUsage")
public final class DemoTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        FluidTankClientGameTest.LOGGER.info("Running test: {}", getClass().getSimpleName());
        context.takeScreenshot("demo");
    }
}
