package com.kotori316.fluidtank.fabric.client.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public final class DemoStoneTest implements FabricClientGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoStoneTest.class);

    @Override
    public void runTest(ClientGameTestContext context) {
        LOGGER.info("Running test: {}", getClass().getSimpleName());
        try (var singlePlayerContext = context.worldBuilder().create()) {
            singlePlayerContext.getServer().runOnServer(server -> {
                var level = server.getLevel(Level.OVERWORLD);
                assert level != null;
                var player = server.getPlayerList().getPlayers().getFirst();
                var direction = player.getDirection();
                var pos = player.getOnPos().above();
                level.setBlock(pos.relative(direction, 3), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            });
            singlePlayerContext.getClientWorld().waitForChunksRender();
            context.waitTicks(10);
            context.takeScreenshot("demo-stone");
        }
    }
}
