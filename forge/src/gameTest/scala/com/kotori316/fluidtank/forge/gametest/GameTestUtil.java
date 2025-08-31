package com.kotori316.fluidtank.forge.gametest;

import com.google.common.base.CaseFormat;
import com.kotori316.fluidtank.FluidTankCommon;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.ReflectionSupport;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

public final class GameTestUtil {
    public static final String EMPTY_STRUCTURE = "empty";
    public static final String NO_PLACE_STRUCTURE = "no_place";

    public static BlockPos getBasePos(GameTestHelper helper) {
        return Try.call(() -> GameTestHelper.class.getDeclaredField("testInfo"))
            .andThen(f -> ReflectionSupport.tryToReadFieldValue(f, helper))
            .andThenTry(GameTestInfo.class::cast)
            .andThenTry(GameTestInfo::getStructureBlockPos)
            .andThenTry(helper.getLevel()::getBlockEntity)
            .andThenTry(StructureBlockEntity.class::cast)
            .andThenTry(StructureBlockEntity::getStructurePos)
            .getOrThrow(RuntimeException::new);
    }

    public static TestFunction create(String modID, String batch, String name, Consumer<GameTestHelper> test) {
        return createWithStructure(modID, batch, name, EMPTY_STRUCTURE, test);
    }

    public static TestFunction create(String modID, String batch, String name, Runnable test) {
        return createWithStructure(modID, batch, name, NO_PLACE_STRUCTURE, test);
    }

    public static TestFunction createWithStructure(String modID, String batch, String testName, String structureName, Consumer<GameTestHelper> test) {
        return createInternal(modID, batch, testName, structureName, wrapper(test));
    }

    public static TestFunction createWithStructure(String modID, String batch, String testName, String structureName, Runnable test) {
        return createInternal(modID, batch, testName, structureName, wrapper(g -> {
            test.run();
            g.succeed();
        }));
    }

    private static TestFunction createInternal(String modID, String batch, String testName, String structureName, Consumer<GameTestHelper> wrapped) {
        var snakeTestName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, testName);
        var structureLocation = ResourceLocation.fromNamespaceAndPath(modID.toLowerCase(Locale.ROOT), structureName.toLowerCase(Locale.ROOT));
        return new TestFunction(
            batch, snakeTestName, structureLocation.toString(), 100, 0L,
            true, wrapped
        );
    }

    private static Consumer<GameTestHelper> wrapper(Consumer<GameTestHelper> original) {
        return g -> {
            try {
                original.accept(g);
            } catch (AssertionError assertionError) {
                var e = new RuntimeException(assertionError.getMessage());
                e.addSuppressed(assertionError);
                throw e;
            }
        };
    }

    public static ICondition.IContext getContext(GameTestHelper helper) {
        return Try.success(helper.getLevel())
            .andThenTry(ServerLevel::getServer)
            .andThenTry(MinecraftServer::getServerResources)
            .andThenTry(MinecraftServer.ReloadableResources::managers)
            .andThenTry(ReloadableServerResources::getConditionContext)
            .getOrThrow(RuntimeException::new);
    }

    public static MockSurvivalPlayer getSurvivalPlayer(GameTestHelper helper) {
        return new MockSurvivalPlayer(helper);
    }

    public static class MockSurvivalPlayer extends Player {
        private boolean isCreative = false;

        public MockSurvivalPlayer(GameTestHelper helper) {
            super(helper.getLevel(), BlockPos.ZERO, 0f, new GameProfile(UUID.randomUUID(), "test-survival-mock-player"));
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return isCreative;
        }

        public void setCreative(boolean creative) {
            isCreative = creative;
        }
    }

    public static void throwExceptionAt(GameTestHelper helper, BlockPos relativePos, String message)
        throws GameTestAssertPosException {
        var absolutePos = helper.absolutePos(relativePos);
        throw new GameTestAssertPosException(message, absolutePos, relativePos, helper.getTick());
    }

    public static void logTestName(Object maybeTest, Method createFrom) {
        if (maybeTest instanceof TestFunction testFunction) {
            FluidTankCommon.LOGGER.info("Register {}(batch: {}, structure: {}) from {}",
                testFunction.getTestName(), testFunction.getBatchName(), testFunction.getStructureName(), createFrom);
        }
    }
}
