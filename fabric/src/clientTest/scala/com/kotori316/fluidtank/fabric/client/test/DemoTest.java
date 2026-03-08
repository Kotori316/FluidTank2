package com.kotori316.fluidtank.fabric.client.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public final class DemoTest implements FabricClientGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoTest.class);

    @Override
    public void runTest(ClientGameTestContext context) {
        LOGGER.info("Running test: {}", getClass().getSimpleName());
        context.takeScreenshot("demo");
    }
}
