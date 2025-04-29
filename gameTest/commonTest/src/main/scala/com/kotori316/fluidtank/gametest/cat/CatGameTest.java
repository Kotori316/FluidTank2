package com.kotori316.fluidtank.gametest.cat;

import com.kotori316.fluidtank.cat.PlatformChestAsTankAccess;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.gametest.GameTestFunctions;
import com.kotori316.testutil.common.TestFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public final class CatGameTest {
    public static Stream<TestFunction> tests(String batchName, String structureName) {
        return Stream.concat(
            GameTestFunctions.getTestFunctionStream(batchName, structureName, List.of(
                CatEmptyTest.class,
                CatFillCatTest.class,
                CatFillItemTest.class,
                CatGameTest.class
            ), 10),
            placeTest(batchName, structureName)
        );
    }

    private static Stream<TestFunction> placeTest(String batchName, String structureName) {
        return Stream.of(Direction.values())
            .map(d -> GameTestFunctions.create(batchName, structureName, "CatGameTestPlaceTest_%s".formatted(d.getName()), g -> placeTest(g, d)));
    }

    private static void setBlocks(GameTestHelper helper, BlockPos basePos, Direction direction, @Nullable Item chestItem) {
        helper.setBlock(basePos, Blocks.CHEST);
        Container container = helper.getBlockEntity(basePos, ChestBlockEntity.class);
        if (chestItem != null) {
            container.setItem(0, chestItem.getDefaultInstance());
        }
        var relative = basePos.relative(direction);
        helper.setBlock(relative, PlatformChestAsTankAccess.getInstance().getCATBlock().get().defaultBlockState().setValue(BlockStateProperties.FACING, direction.getOpposite()));
    }

    private static void placeTest(GameTestHelper helper, Direction direction) {
        var basePos = new BlockPos(3, 3, 3);
        var relative = basePos.relative(direction);
        setBlocks(helper, basePos, direction, Items.WATER_BUCKET);

        var fluids = PlatformChestAsTankAccess.getInstance().getCATFluids(helper.getLevel(), helper.absolutePos(relative));
        assertFalse(fluids.isEmpty(), "Fluid must be present");
        assertEquals(FluidAmountUtil.BUCKET_WATER(), fluids.getFirst(), "Fluid must be 1000 mb of water");

        helper.succeed();
    }

    private static class CatEmptyTest {
        public static void interactWithEmpty(GameTestHelper helper) {
            Direction direction = Direction.NORTH;
            var basePos = new BlockPos(3, 3, 3);
            var relative = basePos.relative(direction);
            setBlocks(helper, basePos, direction, Items.BUCKET);

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.useBlock(relative, player);

            var fluids = PlatformChestAsTankAccess.getInstance().getCATFluids(helper.getLevel(), helper.absolutePos(relative));
            assertTrue(fluids.isEmpty(), "Fluid must not be present");

            helper.succeed();
        }

        public static void interactButNothingHappensWater(GameTestHelper helper) {
            Direction direction = Direction.NORTH;
            var basePos = new BlockPos(3, 2, 3);
            var relative = basePos.relative(direction);
            setBlocks(helper, basePos, direction, Items.WATER_BUCKET);

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            helper.useBlock(relative, player);

            var fluids = PlatformChestAsTankAccess.getInstance().getCATFluids(helper.getLevel(), helper.absolutePos(relative));
            assertEquals(FluidAmountUtil.BUCKET_WATER(), fluids.getFirst(), "Fluid must be 1000 mb of water");

            var replaced = player.getItemInHand(InteractionHand.MAIN_HAND);
            assertEquals(Items.WATER_BUCKET, replaced.getItem());

            helper.succeed();
        }
    }

    private static class CatFillCatTest {
        public static void interactWithWater(GameTestHelper helper) {
            Direction direction = Direction.NORTH;
            var basePos = new BlockPos(3, 3, 3);
            var relative = basePos.relative(direction);
            setBlocks(helper, basePos, direction, Items.BUCKET);

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            helper.useBlock(relative, player);

            var fluids = PlatformChestAsTankAccess.getInstance().getCATFluids(helper.getLevel(), helper.absolutePos(relative));
            assertFalse(fluids.isEmpty(), "Fluid must be present");
            assertEquals(FluidAmountUtil.BUCKET_WATER(), fluids.getFirst(), "Fluid must be 1000 mb of water");

            var replaced = player.getItemInHand(InteractionHand.MAIN_HAND);
            assertEquals(Items.BUCKET, replaced.getItem(), "Water Bucket should be replaced to Empty Bucket");

            helper.succeed();
        }

        public static void interactWithLava(GameTestHelper helper) {
            Direction direction = Direction.NORTH;
            var basePos = new BlockPos(3, 3, 3);
            var relative = basePos.relative(direction);
            setBlocks(helper, basePos, direction, Items.BUCKET);

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
            helper.useBlock(relative, player);

            var fluids = PlatformChestAsTankAccess.getInstance().getCATFluids(helper.getLevel(), helper.absolutePos(relative));
            assertFalse(fluids.isEmpty(), "Fluid must be present");
            assertEquals(FluidAmountUtil.BUCKET_LAVA(), fluids.getFirst(), "Fluid must be 1000 mb of lava");

            var replaced = player.getItemInHand(InteractionHand.MAIN_HAND);
            assertEquals(Items.BUCKET, replaced.getItem(), "Lava Bucket should be replaced to Empty Bucket");

            helper.succeed();
        }
    }

    private static class CatFillItemTest {
        public static void interactToFillBucketWater(GameTestHelper helper) {
            Direction direction = Direction.NORTH;
            var basePos = new BlockPos(3, 3, 3);
            var relative = basePos.relative(direction);
            setBlocks(helper, basePos, direction, Items.WATER_BUCKET);

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            helper.useBlock(relative, player);

            var fluids = PlatformChestAsTankAccess.getInstance().getCATFluids(helper.getLevel(), helper.absolutePos(relative));
            assertTrue(fluids.isEmpty(), "Fluid must be empty");

            var replaced = player.getItemInHand(InteractionHand.MAIN_HAND);
            assertEquals(Items.WATER_BUCKET, replaced.getItem());

            helper.succeed();
        }

        public static void interactToFillBucketLava(GameTestHelper helper) {
            Direction direction = Direction.NORTH;
            var basePos = new BlockPos(3, 3, 3);
            var relative = basePos.relative(direction);
            setBlocks(helper, basePos, direction, Items.LAVA_BUCKET);

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            helper.useBlock(relative, player);

            var fluids = PlatformChestAsTankAccess.getInstance().getCATFluids(helper.getLevel(), helper.absolutePos(relative));
            assertTrue(fluids.isEmpty(), "Fluid must be empty");

            var replaced = player.getItemInHand(InteractionHand.MAIN_HAND);
            assertEquals(Items.LAVA_BUCKET, replaced.getItem());

            helper.succeed();
        }
    }
}
