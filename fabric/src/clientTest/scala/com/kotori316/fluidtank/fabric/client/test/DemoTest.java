package com.kotori316.fluidtank.fabric.client.test;

import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import net.fabricmc.fabric.api.client.gametest.v1.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("UnstableApiUsage")
public final class DemoTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var singlePlayerContext = context.worldBuilder().adjustSettings(DemoTest::setWorldNameByTime).create()) {
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

    static void setWorldNameByTime(WorldCreationUiState state) {
        var name = createWorldNameByDate(ZonedDateTime.now());
        setWorldName(state, name);
    }

    static void setWorldName(WorldCreationUiState state, String name) {
        state.setName(name);
    }

    static String createWorldNameByDate(ZonedDateTime now) {
        return now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
}
