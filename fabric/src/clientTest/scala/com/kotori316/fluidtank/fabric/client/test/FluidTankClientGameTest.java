package com.kotori316.fluidtank.fabric.client.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@SuppressWarnings("UnstableApiUsage")
public interface FluidTankClientGameTest extends FabricClientGameTest {
    Logger LOGGER = LoggerFactory.getLogger(FluidTankClientGameTest.class);
    int PACKET_WAIT_TICKS = 3;

    @Override
    default void runTest(ClientGameTestContext context) {
        LOGGER.info("Running test: {}", getClass().getSimpleName());
        context.runOnClient(i -> i.options.hideGui = true);
        try (var singlePlayerContext = context.worldBuilder()
            .adjustSettings(FluidTankClientGameTest::setWorldNameByTime)
            .adjustSettings(FluidTankClientGameTest::setFlatWorldSetting)
            .create()) {
            runTest(context, singlePlayerContext);
        }
    }

    void runTest(ClientGameTestContext context, TestSingleplayerContext singlePlayerContext);

    static void setWorldNameByTime(WorldCreationUiState state) {
        var name = createWorldNameByDate(ZonedDateTime.now());
        setWorldName(state, name);
    }

    static void setFlatWorldSetting(WorldCreationUiState state) {
        var settings = new FlatLevelGeneratorSettings(
            Optional.of(HolderSet.direct()),
            state.getSettings().worldgenLoadContext().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS),
            List.of()
        );
        settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.BARRIER));
        settings.updateLayers();

        var updater = PresetEditor.flatWorldConfigurator(settings);
        state.updateDimensions(updater);
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

    default void takeScreenshot(ClientGameTestContext context, UnaryOperator<String> directoryOperator, String fileName) {
        var directory = directoryOperator.apply(getScreenshotSubDirectory());
        takeScreenshot(context, directory, fileName);
    }

    default void takeScreenshot(ClientGameTestContext context, String fileName) {
        takeScreenshot(context, UnaryOperator.identity(), fileName);
    }

    default String getScreenshotSubDirectory() {
        return getClass().getSimpleName();
    }

    static void takeScreenshot(ClientGameTestContext context, String directory, String fileName) {
        var destinationDir = Optional.ofNullable(System.getenv("SCREENSHOT_DIR")).map(Path::of).map(p -> p.resolve(directory));
        var filePath = Path.of(fileName);
        var option = TestScreenshotOptions.of(filePath.getFileName().toString())
            .disableCounterPrefix();
        // the option is builder-like instance
        destinationDir.map(p -> p.resolve(filePath)).map(Path::getParent).ifPresent(option::withDestinationDir);
        var screenshotPath = context.takeScreenshot(option);
        LOGGER.info("Screenshot path: {}", screenshotPath);
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
