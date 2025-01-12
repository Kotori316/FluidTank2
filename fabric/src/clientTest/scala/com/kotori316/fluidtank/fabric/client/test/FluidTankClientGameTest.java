package com.kotori316.fluidtank.fabric.client.test;

import net.fabricmc.fabric.api.client.gametest.v1.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public interface FluidTankClientGameTest extends FabricClientGameTest {
    @Override
    default void runTest(ClientGameTestContext context) {
        context.runOnClient(i -> i.options.hideGui = true);
        try (var singlePlayerContext = context.worldBuilder().adjustSettings(FluidTankClientGameTest::setWorldNameByTime).create()) {
            runTest(context, singlePlayerContext);
        }
    }

    void runTest(ClientGameTestContext context, TestSingleplayerContext singlePlayerContext);

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

    static ServerDataContext getServerDataContext(MinecraftServer server) {
        var level = server.getLevel(Level.OVERWORLD);
        var player = server.getPlayerList().getPlayers().getFirst();
        var direction = player.getDirection();
        var pos = player.getOnPos().above();
        return new ServerDataContext(level, player, direction, pos);
    }

    static void takeScreenshot(ClientGameTestContext context, String fileName) {
        var destinationDir = Optional.ofNullable(System.getenv("SCREENSHOT_DIR")).map(Path::of);
        var option = TestScreenshotOptions.of(fileName)
            .disableCounterPrefix();
        // the option is builder-like instance
        destinationDir.ifPresent(option::withDestinationDir);
        context.takeScreenshot(option);
    }

    record ServerDataContext(ServerLevel level, ServerPlayer player, Direction playerDirection, BlockPos playerOnPos) {
        void placeBlock(int offset, BlockState state) {
            placeBlockRelativeOffset(offset, Vec3i.ZERO, state);
        }

        void placeBlock(Vec3i offset, BlockState state) {
            level.setBlock(getPos(offset), state, Block.UPDATE_ALL);
        }

        void placeBlockRelativeOffset(int offset, Vec3i offsetVec, BlockState state) {
            level.setBlock(getPos(offset, offsetVec), state, Block.UPDATE_ALL);
        }

        BlockPos getPos(int offset) {
            return getPos(offset, Vec3i.ZERO);
        }

        BlockPos getPos(Vec3i offset) {
            return playerOnPos.offset(offset);
        }

        BlockPos getPos(int offset, Vec3i offsetVec) {
            return getPos(playerDirection.getUnitVec3i().multiply(offset).offset(offsetVec));
        }
    }
}
