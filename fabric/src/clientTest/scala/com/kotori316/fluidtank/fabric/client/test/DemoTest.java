package com.kotori316.fluidtank.fabric.client.test;

import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import net.fabricmc.fabric.api.client.gametest.v1.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("UnstableApiUsage")
public final class DemoTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var singlePlayerContext = context.worldBuilder().create()) {
            singlePlayerContext.getServer().runOnServer(server -> {
                var level = server.getLevel(Level.OVERWORLD);
                level.setBlock(new BlockPos(5, -60, 6), PlatformTankAccess.getInstance().getTankBlockMap().get(Tier.WOOD).get().defaultBlockState(), Block.UPDATE_ALL);
                for (var player : level.players()) {
                    player.moveTo(3, -60, 6, 0, 0);
                }
            });
            singlePlayerContext.getClientWorld().waitForChunksRender();
            context.takeScreenshot("demo");
        }
    }
}
