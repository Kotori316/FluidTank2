package com.kotori316.fluidtank.fabric.client.test;

import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import net.fabricmc.fabric.api.client.gametest.v1.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("UnstableApiUsage")
public final class DemoTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var singlePlayerContext = context.worldBuilder().adjustSettings(FluidTankClientGameTest::setWorldNameByTime).create()) {
            singlePlayerContext.getServer().runOnServer(server -> {
                var level = server.getLevel(Level.OVERWORLD);
                var player = server.getPlayerList().getPlayers().getFirst();
                var direction = player.getDirection();
                var pos = player.getOnPos().above();
                level.setBlock(pos.relative(direction, 3), PlatformTankAccess.getInstance().getTankBlockMap().get(Tier.WOOD).get().defaultBlockState(), Block.UPDATE_ALL);
            });
            singlePlayerContext.getClientWorld().waitForChunksRender();
            context.takeScreenshot("demo");
        }
    }
}
