package com.kotori316.fluidtank.gametest.tank;

import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.PotionType;
import com.kotori316.fluidtank.gametest.GameTestFunctions;
import com.kotori316.fluidtank.tank.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public final class TankTest {

    public static Stream<TestFunction> tests(String batchName, String structureName) {
        return Stream.of(
            GameTestFunctions.getTestFunctionStream(batchName, structureName, List.of(
                TankTest.class
            ), 10),
            drainPotionSurvival1(batchName, structureName),
            drainPotionFailSurvival(batchName, structureName)
        ).flatMap(Function.identity());
    }

    static Supplier<? extends BlockTank> getBlock(Tier tier) {
        return PlatformTankAccess.getInstance().getTankBlockMap().get(tier);
    }

    static TileTank placeTank(GameTestHelper helper, BlockPos pos, Tier tier) {
        var block = getBlock(tier);
        helper.setBlock(pos, block.get());
        var tile = helper.getBlockEntity(pos);
        if (tile instanceof TileTank tileTank) {
            tileTank.onBlockPlacedBy();
            return tileTank;
        } else {
            throw new GameTestAssertPosException("Expect tank tile", helper.absolutePos(pos), pos, helper.getTick());
        }
    }

    static void place(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);

        assertFalse(tile.getConnection().isDummy());
        helper.assertBlockPresent(getBlock(Tier.WOOD).get(), basePos);
        helper.assertBlockProperty(basePos, TankPos.TANK_POS_PROPERTY, TankPos.SINGLE);
        helper.succeed();
    }

    static void place2(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile1 = placeTank(helper, basePos, Tier.WOOD);
        var tile2 = placeTank(helper, basePos.above(), Tier.STONE);

        var c1 = tile1.getConnection();
        var c2 = tile2.getConnection();
        assertFalse(c1.isDummy());
        assertSame(c1, c2);
        assertEquals(2, c1.getTiles().size());
        helper.succeed();
    }

    static void fill1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);

        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.useBlock(basePos, player);

        assertEquals(FluidAmountUtil.BUCKET_WATER(), tile.getTank().content());
        assertEquals(Items.WATER_BUCKET, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static void fill2(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.STONE);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.useBlock(basePos, player);
        assertEquals(FluidAmountUtil.BUCKET_WATER(), tile.getTank().content());
        assertEquals(Items.BUCKET, player.getItemInHand(InteractionHand.MAIN_HAND).getItem(),
            "Inventory item must be consumed and replaced.");
        helper.succeed();
    }

    static void drain1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        tile.getConnection().getHandler().fill(FluidAmountUtil.BUCKET_WATER(), true);

        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.useBlock(basePos, player);

        assertTrue(tile.getTank().isEmpty());
        assertEquals(Items.BUCKET, player.getItemInHand(InteractionHand.MAIN_HAND).getItem(), "In creative, the item must not change.");
        helper.succeed();
    }

    static void drain2(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        tile.getConnection().getHandler().fill(FluidAmountUtil.BUCKET_WATER(), true);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.useBlock(basePos, player);

        assertTrue(tile.getTank().isEmpty());
        assertEquals(Items.WATER_BUCKET, player.getItemInHand(InteractionHand.MAIN_HAND).getItem(), "In survival, the item must change.");
        helper.succeed();
    }

    static void drain3(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        tile.getConnection().getHandler().fill(FluidAmountUtil.BUCKET_WATER(), true);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 10));
        assertEquals(0, player.getInventory().countItem(Items.WATER_BUCKET), "Test assumption");
        helper.useBlock(basePos, player);

        assertTrue(tile.getTank().isEmpty());
        assertEquals(Items.BUCKET, player.getItemInHand(InteractionHand.MAIN_HAND).getItem(), "In survival, the item must change.");
        assertEquals(9, player.getItemInHand(InteractionHand.MAIN_HAND).getCount(), "In survival, the item must change.");
        assertEquals(1, player.getInventory().countItem(Items.WATER_BUCKET));
        helper.succeed();
    }

    static void fillFail1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        tile.getConnection().getHandler().fill(FluidAmountUtil.BUCKET_WATER(), true);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        helper.useBlock(basePos, player);

        assertEquals(FluidAmountUtil.BUCKET_WATER(), tile.getTank().content());
        assertEquals(Items.LAVA_BUCKET, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static void capacityWithCreative(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        placeTank(helper, basePos.above(1), Tier.CREATIVE);

        assertEquals(GenericUnit.CREATIVE_TANK(), tile.getConnection().capacity());
        helper.succeed();
    }

    static void amountWithCreative1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        placeTank(helper, basePos.above(1), Tier.CREATIVE);
        tile.getConnection().getHandler().fill(FluidAmountUtil.BUCKET_WATER(), true);

        assertEquals(GenericUnit.CREATIVE_TANK(), tile.getConnection().amount());
        helper.succeed();
    }

    static void amountWithCreative2(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        placeTank(helper, basePos.above(1), Tier.CREATIVE);
        placeTank(helper, basePos.above(2), Tier.CREATIVE);
        tile.getConnection().getHandler().fill(FluidAmountUtil.BUCKET_WATER(), true);

        assertEquals(GenericUnit.CREATIVE_TANK(), tile.getConnection().amount());
        helper.succeed();
    }

    static void fillPotionCreative1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);

        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.POTION, Potions.LONG_INVISIBILITY));
        helper.useBlock(basePos, player);

        var expected = FluidAmountUtil.from(PotionType.NORMAL, Potions.LONG_INVISIBILITY, GenericUnit.ONE_BOTTLE());
        assertEquals(expected, tile.getTank().content());
        assertEquals(Items.POTION, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static void fillPotionCreative2(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var content = FluidAmountUtil.from(PotionType.SPLASH, Potions.LONG_INVISIBILITY, GenericUnit.ONE_BOTTLE());
        tile.getConnection().getHandler().fill(content, true);

        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var potionStack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.LONG_INVISIBILITY);
        player.setItemInHand(InteractionHand.MAIN_HAND, potionStack);
        helper.useBlock(basePos, player);

        assertEquals(content.setAmount(GenericUnit.fromFabric(54000)), tile.getTank().content());
        assertEquals(Items.SPLASH_POTION, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        assertTrue(ItemStack.matches(potionStack, player.getItemInHand(InteractionHand.MAIN_HAND)));
        helper.succeed();
    }

    static void fillPotionCreative3(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var content = FluidAmountUtil.from(PotionType.SPLASH, Potions.INVISIBILITY, GenericUnit.ONE_BOTTLE());
        tile.getConnection().getHandler().fill(content, true);

        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var potionStack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.LONG_INVISIBILITY);
        player.setItemInHand(InteractionHand.MAIN_HAND, potionStack);
        helper.useBlock(basePos, player);

        assertEquals(content, tile.getTank().content());
        assertEquals(Items.SPLASH_POTION, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        assertTrue(ItemStack.matches(potionStack, player.getItemInHand(InteractionHand.MAIN_HAND)));
        helper.succeed();
    }

    static void fillPotionSurvival1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.POTION, Potions.LONG_INVISIBILITY));
        helper.useBlock(basePos, player);

        var expected = FluidAmountUtil.from(PotionType.NORMAL, Potions.LONG_INVISIBILITY, GenericUnit.ONE_BOTTLE());
        assertEquals(expected, tile.getTank().content());
        assertEquals(Items.GLASS_BOTTLE, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static void fillPotionSurvival2(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var content = FluidAmountUtil.from(PotionType.SPLASH, Potions.LONG_INVISIBILITY, GenericUnit.ONE_BOTTLE());
        tile.getConnection().getHandler().fill(content, true);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var potionStack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.LONG_INVISIBILITY);
        player.setItemInHand(InteractionHand.MAIN_HAND, potionStack);
        helper.useBlock(basePos, player);

        assertEquals(content.setAmount(GenericUnit.fromFabric(54000)), tile.getTank().content());
        assertEquals(Items.GLASS_BOTTLE, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static void fillPotionSurvival3(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var content = FluidAmountUtil.from(PotionType.SPLASH, Potions.INVISIBILITY, GenericUnit.ONE_BOTTLE());
        tile.getConnection().getHandler().fill(content, true);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var potionStack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.LONG_INVISIBILITY);
        player.setItemInHand(InteractionHand.MAIN_HAND, potionStack);
        helper.useBlock(basePos, player);

        assertEquals(content, tile.getTank().content());
        assertEquals(Items.SPLASH_POTION, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static void drainPotionCreative1(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var content = FluidAmountUtil.from(PotionType.NORMAL, Potions.LONG_INVISIBILITY, GenericUnit.ONE_BOTTLE());
        tile.getConnection().getHandler().fill(content, true);

        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.useBlock(basePos, player);

        assertTrue(tile.getTank().isEmpty());
        assertEquals(Items.GLASS_BOTTLE, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static void drainPotionCreative2(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var tile2 = placeTank(helper, basePos.above(), Tier.STONE);
        var content = FluidAmountUtil.from(PotionType.NORMAL, Potions.LONG_INVISIBILITY, GenericUnit.fromForge(20000));
        tile.getConnection().getHandler().fill(content, true);

        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.useBlock(basePos, player);

        assertEquals(content.setAmount(GenericUnit.fromFabric(59 * 27000)), tile.getConnection().getContent().get());
        assertEquals(Items.GLASS_BOTTLE, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static Stream<TestFunction> drainPotionSurvival1(String batchName, String structureName) {
        return Stream.of(PotionType.values()).flatMap(t ->
            Stream.of(Potions.LONG_INVISIBILITY, Potions.WATER, Potions.AWKWARD, Potions.NIGHT_VISION).map(p ->
                GameTestFunctions.create(batchName, structureName,
                    "drainPotionSurvival1_" + t.name().toLowerCase(Locale.ROOT) + "_" + p.value().name().toLowerCase(Locale.ROOT),
                    g -> drainPotionSurvival1(g, t, p))
            ));
    }

    static void drainPotionSurvival1(GameTestHelper helper, PotionType potionType, Holder<Potion> potion) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var content = FluidAmountUtil.from(potionType, potion, GenericUnit.ONE_BOTTLE());
        tile.getConnection().getHandler().fill(content, true);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.useBlock(basePos, player);

        assertTrue(tile.getTank().isEmpty());
        assertTrue(ItemStack.matches(PotionContents.createItemStack(potionType.getItem(), potion),
            player.getItemInHand(InteractionHand.MAIN_HAND)));
        helper.succeed();
    }

    static Stream<TestFunction> drainPotionFailSurvival(String batchName, String structureName) {
        return Stream.of(PotionType.values()).flatMap(t ->
            Stream.of(Potions.LONG_INVISIBILITY, Potions.WATER, Potions.AWKWARD, Potions.NIGHT_VISION).map(p ->
                GameTestFunctions.create(batchName, structureName,
                    "drainPotionFailSurvival" + "_" + t.name().toLowerCase(Locale.ROOT) + "_" + p.value().name().toLowerCase(Locale.ROOT),
                    g -> drainPotionSurvival1(g, t, p))
            ));
    }

    static void drainPotionFailSurvival(GameTestHelper helper, PotionType potionType, Holder<Potion> potion) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var content = FluidAmountUtil.from(potionType, potion, GenericUnit.ONE_BOTTLE());
        tile.getConnection().getHandler().fill(content, true);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.useBlock(basePos, player);

        assertEquals(content, tile.getTank().content());
        assertEquals(Items.BUCKET, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static void fillMultiEffectPotion(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var potionStack = new ItemStack(Items.POTION);
        potionStack.set(DataComponents.POTION_CONTENTS,
            new PotionContents(Optional.of(Potions.NIGHT_VISION), Optional.empty(), Potions.REGENERATION.value().getEffects(), Optional.empty())
        );
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, potionStack.copy());
        helper.useBlock(basePos, player);

        var content = FluidAmountUtil.from(FluidLike.POTION_NORMAL(), GenericUnit.ONE_BOTTLE(), potionStack.getComponentsPatch());
        assertEquals(content, tile.getTank().content());
        assertEquals(Items.GLASS_BOTTLE, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());
        helper.succeed();
    }

    static void drainMultiEffectPotion(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var potionStack = new ItemStack(Items.POTION);
        potionStack.set(DataComponents.POTION_CONTENTS,
            new PotionContents(Optional.of(Potions.NIGHT_VISION), Optional.empty(), Potions.REGENERATION.value().getEffects(), Optional.empty())
        );
        var content = FluidAmountUtil.from(FluidLike.POTION_NORMAL(), GenericUnit.ONE_BUCKET(), potionStack.getComponentsPatch());
        tile.getConnection().getHandler().fill(content, true);

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.useBlock(basePos, player);

        assertTrue(ItemStack.matches(potionStack, player.getItemInHand(InteractionHand.MAIN_HAND)));
        helper.succeed();
    }

    static void saveNbt(GameTestHelper helper) {
        var basePos = BlockPos.ZERO.above();
        var tile = placeTank(helper, basePos, Tier.WOOD);
        var content = FluidAmountUtil.BUCKET_WATER();
        tile.getConnection().getHandler().fill(content, true);
        var block = getBlock(Tier.WOOD).get();
        var stack = new ItemStack(block);
        block.saveTankNBT(tile, stack, helper.getLevel().registryAccess());
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        assertNotNull(data);
        assertTrue(data.contains("id"), "Saved nbt must have id field since 1.20.5");

        helper.succeed();
    }
}
