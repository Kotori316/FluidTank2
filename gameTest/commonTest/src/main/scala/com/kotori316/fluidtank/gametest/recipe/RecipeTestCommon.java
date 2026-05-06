package com.kotori316.fluidtank.gametest.recipe;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.PlatformFluidAccess;
import com.kotori316.fluidtank.gametest.GameTestFunctions;
import com.kotori316.fluidtank.recipe.TierRecipe;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.testutil.common.TestFunction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public abstract class RecipeTestCommon {
    protected static BlockTank getTank(Tier tier) {
        return PlatformTankAccess.getInstance().getTankBlockMap().get(tier).get();
    }

    @NotNull
    protected static TierRecipe getRecipe() {
        return new TierRecipe(
            new Recipe.CommonInfo(false), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, TierRecipe.TANK_RECIPE_GROUP),
            Tier.STONE,
            Ingredient.of(getTank(Tier.WOOD)), Ingredient.of(Items.STONE)
        );
    }

    protected static CraftingInput getInv(String s1, String s2, String s3, Map<Character, ItemStack> itemMap) {
        var map = new HashMap<>(itemMap);
        map.put(' ', ItemStack.EMPTY);

        if (s1.length() > 3 || s2.length() > 3 || s3.length() > 3) {
            throw new IllegalArgumentException("Over 4 elements are not allowed. " + List.of(s1, s2, s3));
        }
        if (s1.isEmpty() && s2.isEmpty() && s3.isEmpty()) {
            throw new IllegalArgumentException("All Empty?");
        }

        var chars = new ArrayList<Character>();
        for (String s : List.of(s1, s2, s3)) {
            for (int i = 0; i < s.length(); i++) {
                chars.add(s.charAt(i));
            }
        }
        if (!map.keySet().containsAll(Set.copyOf(chars))) {
            throw new IllegalArgumentException("Contains all keys, " + chars);
        }

        var stacks = new ArrayList<ItemStack>();
        var lines = List.of(s1, s2, s3);
        for (String line : lines) {
            for (int column = 0; column < line.length(); column++) {
                var stack = map.get(line.charAt(column));
                if (stack != null) {
                    stacks.add(stack);
                }
            }
        }
        return CraftingInput.of(3, 3, stacks);
    }

    protected static ItemStack getFilledTankStack(GameTestHelper helper, Tier tier, GenericAmount<FluidLike> fluid) {
        var stack = new ItemStack(getTank(tier));
        var player = helper.makeMockPlayer(GameType.SURVIVAL); // Must be SURVIVAL because creative will not change the item itself
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = PlatformFluidAccess.getInstance().fillItem(fluid, stack, player, InteractionHand.MAIN_HAND, true);
        return result.toReplace().copy();
    }

    protected static Stream<TestFunction> testsInCommon(String batchName, RecipeTestCommon self) {
        var simpleName = RecipeTestCommon.class.getSimpleName();
        return Stream.of(
            GameTestFunctions.create(batchName, TestFunction.NO_PLACE_STRUCTURE, simpleName + "_" + "createInstance", self::createInstance),
            GameTestFunctions.create(batchName, TestFunction.NO_PLACE_STRUCTURE, simpleName + "_" + "match1", self::match1),
            GameTestFunctions.create(batchName, TestFunction.NO_PLACE_STRUCTURE, simpleName + "_" + "match2", self::match2),
            GameTestFunctions.create(batchName, TestFunction.NO_PLACE_STRUCTURE, simpleName + "_" + "match3", self::match3),
            GameTestFunctions.create(batchName, TestFunction.NO_PLACE_STRUCTURE, simpleName + "_" + "notMatch4", self::notMatch4),
            GameTestFunctions.create(batchName, TestFunction.NO_PLACE_STRUCTURE, simpleName + "_" + "notMatch5", self::notMatch5),
            GameTestFunctions.create(batchName, TestFunction.NO_PLACE_STRUCTURE, simpleName + "_" + "notMatch6", self::notMatch6),
            GameTestFunctions.create(batchName, TestFunction.NO_PLACE_STRUCTURE, simpleName + "_" + "assumptionFillTank", self::assumptionFillTank)
        );
    }

    void createInstance() {
        TierRecipe recipe = getRecipe();
        assertNotNull(recipe);
    }

    void assumptionFillTank(GameTestHelper helper) {
        var stack = getFilledTankStack(helper, Tier.WOOD, FluidAmountUtil.BUCKET_WATER());
        var filled = PlatformFluidAccess.getInstance().getFluidContained(stack);
        assertEquals(FluidAmountUtil.BUCKET_WATER(), filled);
        helper.succeed();
    }

    void match1() {
        var recipe = getRecipe();
        assertTrue(recipe.matches(getInv("tst", "s s", "tst", Map.of(
            't', new ItemStack(getTank(Tier.WOOD)),
            's', new ItemStack(Items.STONE)
        )), null));
    }

    void match2(GameTestHelper helper) {
        var recipe = getRecipe();
        var stack = getFilledTankStack(helper, Tier.WOOD, FluidAmountUtil.BUCKET_WATER());

        assertTrue(recipe.matches(getInv("tst", "s s", "tst", Map.of(
            't', stack,
            's', new ItemStack(Items.STONE)
        )), null));
        helper.succeed();
    }

    void match3(GameTestHelper helper) {
        var recipe = getRecipe();
        var stack = getFilledTankStack(helper, Tier.WOOD, FluidAmountUtil.BUCKET_WATER());

        assertTrue(recipe.matches(getInv("tsk", "s s", "kst", Map.of(
            't', stack,
            'k', new ItemStack(getTank(Tier.WOOD)),
            's', new ItemStack(Items.STONE)
        )), null));
        helper.succeed();
    }

    void notMatch4(GameTestHelper helper) {
        var recipe = getRecipe();
        var stack = getFilledTankStack(helper, Tier.WOOD, FluidAmountUtil.BUCKET_WATER());
        var stack2 = getFilledTankStack(helper, Tier.WOOD, FluidAmountUtil.BUCKET_LAVA());

        assertFalse(recipe.matches(getInv("tsk", "s s", "kst", Map.of(
            't', stack,
            'k', stack2,
            's', new ItemStack(Items.STONE)
        )), null));
        helper.succeed();
    }

    void notMatch5() {
        var recipe = getRecipe();
        assertFalse(recipe.matches(getInv("tst", "s s", "ts ", Map.of(
            't', new ItemStack(getTank(Tier.WOOD)),
            's', new ItemStack(Items.STONE)
        )), null));
    }

    void notMatch6(GameTestHelper helper) {
        var recipe = getRecipe();
        var stack = getFilledTankStack(helper, Tier.WOOD, FluidAmountUtil.BUCKET_WATER());
        var stack2 = getFilledTankStack(helper, Tier.STONE, FluidAmountUtil.BUCKET_WATER());

        assertFalse(recipe.matches(getInv("tsk", "s s", "kst", Map.of(
            't', stack,
            'k', stack2,
            's', new ItemStack(Items.STONE)
        )), null));
        helper.succeed();
    }
}
