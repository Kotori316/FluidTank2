package com.kotori316.fluidtank.forge.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.platform.commons.support.ReflectionSupport;
import scala.jdk.javaapi.CollectionConverters;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.forge.FluidTank;
import com.kotori316.fluidtank.forge.recipe.RecipeInventoryUtil;
import com.kotori316.fluidtank.forge.recipe.TierRecipeForge;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.testutil.GameTestUtil;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@GameTestHolder(FluidTankCommon.modId)
@PrefixGameTestTemplate(value = false)
final class RecipeTest {

    @GameTestGenerator
    List<TestFunction> generator() {
        // no args
        var noArgs = Stream.of(getClass().getDeclaredMethods())
            .filter(m -> m.getReturnType() == Void.TYPE)
            .filter(m -> m.getParameterCount() == 0)
            .filter(m -> (m.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.STATIC)) == 0)
            .map(m -> GameTestUtil.create(FluidTankCommon.modId, "recipe_test",
                getClass().getSimpleName() + "_" + m.getName(),
                () -> ReflectionSupport.invokeMethod(m, this)));
        var withHelper = Stream.of(getClass().getDeclaredMethods())
            .filter(m -> m.getReturnType() == Void.TYPE)
            .filter(m -> Arrays.equals(m.getParameterTypes(), new Class<?>[]{GameTestHelper.class}))
            .filter(m -> (m.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.STATIC)) == 0)
            .map(m -> GameTestUtil.create(FluidTankCommon.modId, "recipe_test",
                getClass().getSimpleName() + "_" + m.getName(),
                g -> ReflectionSupport.invokeMethod(m, this, g)));
        return Stream.concat(noArgs, withHelper).toList();
    }

    @NotNull
    private static TierRecipeForge getRecipe() {
        return new TierRecipeForge(new ResourceLocation(FluidTankCommon.modId, "test1"), Tier.STONE,
            Ingredient.of(FluidTank.TANK_MAP.get(Tier.WOOD).get()), Ingredient.of(Tags.Items.STONE)
        );
    }

    void createInstance() {
        TierRecipeForge recipe = getRecipe();
        assertNotNull(recipe);
    }

    void match1() {
        var recipe = getRecipe();
        assertTrue(recipe.matches(RecipeInventoryUtil.getInv("tst", "s s", "tst", CollectionConverters.asScala(Map.of(
            't', new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get()),
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    void match2() {
        var recipe = getRecipe();
        var stack = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, FluidAmountUtil.BUCKET_WATER());

        assertTrue(recipe.matches(RecipeInventoryUtil.getInv("tst", "s s", "tst", CollectionConverters.asScala(Map.of(
            't', stack,
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    void match3() {
        var recipe = getRecipe();
        var stack = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, FluidAmountUtil.BUCKET_WATER());

        assertTrue(recipe.matches(RecipeInventoryUtil.getInv("tsk", "s s", "kst", CollectionConverters.asScala(Map.of(
            't', stack,
            'k', new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get()),
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    void notMatch4() {
        var recipe = getRecipe();
        var stack = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, FluidAmountUtil.BUCKET_WATER());
        var stack2 = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, FluidAmountUtil.BUCKET_LAVA());

        assertFalse(recipe.matches(RecipeInventoryUtil.getInv("tsk", "s s", "kst", CollectionConverters.asScala(Map.of(
            't', stack,
            'k', stack2,
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    void notMatch5() {
        var recipe = getRecipe();
        assertFalse(recipe.matches(RecipeInventoryUtil.getInv("tst", "s s", "ts ", CollectionConverters.asScala(Map.of(
            't', new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get()),
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    @GameTestGenerator
    List<TestFunction> combineFluids() {
        var fluids = IntStream.of(500, 1000, 2000, 3000, 4000)
            .mapToObj(GenericUnit::fromForge)
            .flatMap(a -> Stream.of(FluidAmountUtil.BUCKET_WATER(), FluidAmountUtil.BUCKET_LAVA())
                .map(f -> f.setAmount(a)));

        return fluids.flatMap(f -> {
            var name = "%s_%s".formatted(FluidAmountUtil.access().getKey(f.content()).getPath(), GenericUnit.asForgeFromBigInt(f.amount()));
            return Stream.of(
                GameTestUtil.create(FluidTankCommon.modId, "recipe_test", getClass().getSimpleName() + "_combine1_" + name, () -> combine1(f)),
                GameTestUtil.create(FluidTankCommon.modId, "recipe_test", getClass().getSimpleName() + "_combine2_" + name, () -> combine2(f))
            );
        }).toList();
    }

    void combine1(GenericAmount<Fluid> amount) {
        var filled = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, amount);
        var empty = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get());
        var recipe = getRecipe();

        var inv = RecipeInventoryUtil.getInv("ksk", "s s", "kst", CollectionConverters.asScala(Map.of(
            't', filled,
            'k', empty,
            's', new ItemStack(Items.STONE)
        )));
        assertTrue(recipe.matches(inv, null));
        var result = recipe.assemble(inv, RegistryAccess.EMPTY);
        var contains = RecipeInventoryUtil.getFluidHandler(result).getTank().content();
        assertEquals(amount, contains);
        assertEquals(Tier.STONE.getCapacity(), RecipeInventoryUtil.getFluidHandler(result).getTank().capacity());
    }

    void combine2(GenericAmount<Fluid> amount) {
        var filled = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, amount);
        var empty = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get());
        var recipe = getRecipe();

        var inv = RecipeInventoryUtil.getInv("kst", "s s", "kst", CollectionConverters.asScala(Map.of(
            't', filled,
            'k', empty,
            's', new ItemStack(Items.STONE)
        )));
        assertTrue(recipe.matches(inv, null));
        var result = recipe.assemble(inv, RegistryAccess.EMPTY);
        var contains = RecipeInventoryUtil.getFluidHandler(result).getTank().content();
        assertEquals(amount.add(amount), contains);
        assertEquals(Tier.STONE.getCapacity(), RecipeInventoryUtil.getFluidHandler(result).getTank().capacity());
    }

    @GameTestGenerator
    List<TestFunction> serialize() {
        return Stream.of(Tier.values()).filter(Tier::isNormalTankTier)
            .filter(Predicate.isEqual(Tier.WOOD).negate())
            .flatMap(t -> Stream.of(
                GameTestUtil.create(FluidTankCommon.modId, "recipe_test", getClass().getSimpleName() + "_json_" + t.name().toLowerCase(Locale.ROOT), () -> serializeJson(t)),
                GameTestUtil.create(FluidTankCommon.modId, "recipe_test", getClass().getSimpleName() + "_packet_" + t.name().toLowerCase(Locale.ROOT), () -> serializePacket(t))
            ))
            .toList();
    }

    void serializeJson(Tier tier) {
        var subItem = Ingredient.of(Items.APPLE);
        var recipe = new TierRecipeForge(new ResourceLocation(FluidTankCommon.modId, "test_" + tier.name().toLowerCase(Locale.ROOT)),
            tier, TierRecipeForge.Serializer.getIngredientTankForTier(tier), subItem);

        var fromSerializer = new JsonObject();
        ((TierRecipeForge.Serializer) TierRecipeForge.SERIALIZER).toJson(fromSerializer, recipe);
        var finishedRecipe = new TierRecipeForge.TierFinishedRecipe(recipe.getId(), tier, subItem);
        var fromFinishedRecipe = new JsonObject();
        finishedRecipe.serializeRecipeData(fromFinishedRecipe);
        assertEquals(fromSerializer, fromFinishedRecipe);

        var deserialized = TierRecipeForge.SERIALIZER.fromJson(recipe.getId(), fromSerializer, ICondition.IContext.EMPTY);
        assertNotNull(deserialized);
        assertAll(
            () -> assertEquals(recipe.getId(), deserialized.getId()),
            () -> assertTrue(ItemStack.matches(recipe.getResultItem(RegistryAccess.EMPTY), deserialized.getResultItem(RegistryAccess.EMPTY)))
        );
    }

    void serializePacket(Tier tier) {
        var subItem = Ingredient.of(Items.APPLE);
        var recipe = new TierRecipeForge(new ResourceLocation(FluidTankCommon.modId, "test_" + tier.name().toLowerCase(Locale.ROOT)),
            tier, TierRecipeForge.Serializer.getIngredientTankForTier(tier), subItem);

        var buffer = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
        TierRecipeForge.SERIALIZER.toNetwork(buffer, recipe);
        var deserialized = TierRecipeForge.SERIALIZER.fromNetwork(recipe.getId(), buffer);
        assertNotNull(deserialized);
        assertAll(
            () -> assertEquals(recipe.getId(), deserialized.getId()),
            () -> assertTrue(ItemStack.matches(recipe.getResultItem(RegistryAccess.EMPTY), deserialized.getResultItem(RegistryAccess.EMPTY)))
        );
    }

    void getRecipeFromJson() {
        // language=json
        String jsonString = """
            {
              "type": "%s",
              "tier": "STONE",
              "sub_item": {
                "item": "minecraft:diamond"
              }
            }
            """.formatted(TierRecipeForge.Serializer.LOCATION.toString());
        var read = RecipeManager.fromJson(new ResourceLocation(FluidTankCommon.modId, "test_serialize"), GsonHelper.parse(jsonString), ICondition.IContext.EMPTY);
        var recipe = new TierRecipeForge(new ResourceLocation(FluidTankCommon.modId, "test_serialize"),
            Tier.STONE, TierRecipeForge.Serializer.getIngredientTankForTier(Tier.STONE), Ingredient.of(Items.DIAMOND));

        assertAll(
            () -> assertEquals(recipe.getId(), read.getId()),
            () -> assertTrue(ItemStack.matches(recipe.getResultItem(RegistryAccess.EMPTY), read.getResultItem(RegistryAccess.EMPTY)))
        );
    }

    @GameTestGenerator
    @SuppressWarnings("ConstantConditions")
    List<TestFunction> loadJsonInData() throws IOException {
        var recipeParent = Path.of("../../common/src/generated/resources", "data/fluidtank/recipes");
        try (var files = Files.find(recipeParent, 1, (path, a) -> path.getFileName().toString().endsWith(".json"))) {
            return files.map(p -> GameTestUtil.create(FluidTankCommon.modId, "recipe_test", "load_" + FilenameUtils.getBaseName(p.getFileName().toString()),
                () -> loadFromFile(p))).toList();
        }
    }

    void loadFromFile(Path path) {
        try {
            var json = GsonHelper.parse(Files.newBufferedReader(path));
            assertDoesNotThrow(() -> RecipeManager.fromJson(new ResourceLocation(FluidTankCommon.modId, "test_load"), json, ICondition.IContext.EMPTY));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}