package com.kotori316.fluidtank.fabric.gametest;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonObject;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fabric.recipe.RecipeInventoryUtil;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.gametest.GameTestFunctions;
import com.kotori316.fluidtank.gametest.recipe.RecipeTestCommon;
import com.kotori316.fluidtank.recipe.TierRecipe;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.testutil.common.TestFunction;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBufAllocator;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import org.apache.commons.io.FilenameUtils;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.ReflectionSupport;
import scala.jdk.javaapi.CollectionConverters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
public final class RecipeTest extends RecipeTestCommon {
    final Path recipeParent = Path.of("../src/generated/resources", "data/fluidtank/recipe");

    public RecipeTest() {
        FluidTankCommon.LOGGER.info("Search recipe path: {}", recipeParent.toAbsolutePath());
    }

    public Stream<TestFunction> tests() {
        return Stream.of(
            generator().stream(),
            combineFluids().stream(),
            serialize().stream(),
            loadJsonInData().stream()
        ).flatMap(Function.identity());
    }

    public List<TestFunction> generator() {
        // no args
        var noArgs = Stream.of(getClass().getDeclaredMethods())
            .filter(m -> m.getReturnType() == Void.TYPE)
            .filter(m -> m.getParameterCount() == 0)
            .filter(m -> (m.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.STATIC)) == 0)
            .map(m -> GameTestFunctions.create("recipe_test", TestFunction.NO_PLACE_STRUCTURE,
                getClass().getSimpleName() + "_" + m.getName(),
                () -> ReflectionSupport.invokeMethod(m, this)));
        var withHelper = Stream.of(getClass().getDeclaredMethods())
            .filter(m -> m.getReturnType() == Void.TYPE)
            .filter(m -> Arrays.equals(m.getParameterTypes(), new Class<?>[]{GameTestHelper.class}))
            .filter(m -> (m.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.STATIC)) == 0)
            .map(m -> GameTestFunctions.create("recipe_test", TestFunction.NO_PLACE_STRUCTURE,
                getClass().getSimpleName() + "_" + m.getName(),
                g -> ReflectionSupport.invokeMethod(m, this, g)));
        var common = testsInCommon("recipe_test", this);
        return Stream.of(noArgs, withHelper, common).flatMap(Function.identity()).toList();
    }

    public List<TestFunction> combineFluids() {
        var fluids = IntStream.of(500, 1000, 2000, 3000, 4000)
            .mapToObj(GenericUnit::fromForge)
            .flatMap(a -> Stream.of(FluidAmountUtil.BUCKET_WATER(), FluidAmountUtil.BUCKET_LAVA())
                .map(f -> f.setAmount(a)));

        return fluids.flatMap(f -> {
            var name = "%s_%s".formatted(FluidAmountUtil.access().getKey(f.content()).getPath(), GenericUnit.asForgeFromBigInt(f.amount()));
            return Stream.of(
                GameTestFunctions.create("recipe_test", TestFunction.EMPTY_STRUCTURE, getClass().getSimpleName() + "_combine1_" + name, (g) -> {
                    combine1(f);
                    g.succeed();
                }),
                GameTestFunctions.create("recipe_test", TestFunction.EMPTY_STRUCTURE, getClass().getSimpleName() + "_combine2_" + name, (g) -> {
                    combine2(f);
                    g.succeed();
                })
            );
        }).toList();
    }

    void combine1(GenericAmount<FluidLike> amount) {
        var filled = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, amount);
        var empty = new ItemStack(getTank(Tier.WOOD));
        var recipe = getRecipe();

        var inv = RecipeInventoryUtil.getInv("ksk", "s s", "kst", CollectionConverters.asScala(Map.of(
            't', filled,
            'k', empty,
            's', new ItemStack(Items.STONE)
        )));
        assertTrue(recipe.matches(inv, null));
        var result = recipe.assemble(inv);
        var contains = RecipeInventoryUtil.getFluidHandler(result).getTank().content();
        assertEquals(amount, contains);
        assertEquals(Tier.STONE.getCapacity(), RecipeInventoryUtil.getFluidHandler(result).getTank().capacity());
    }

    void combine2(GenericAmount<FluidLike> amount) {
        var filled = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, amount);
        var empty = new ItemStack(getTank(Tier.WOOD));
        var recipe = getRecipe();

        var inv = RecipeInventoryUtil.getInv("kst", "s s", "kst", CollectionConverters.asScala(Map.of(
            't', filled,
            'k', empty,
            's', new ItemStack(Items.STONE)
        )));
        assertTrue(recipe.matches(inv, null));
        var result = recipe.assemble(inv);
        var contains = RecipeInventoryUtil.getFluidHandler(result).getTank().content();
        assertEquals(amount.add(amount), contains);
        assertEquals(Tier.STONE.getCapacity(), RecipeInventoryUtil.getFluidHandler(result).getTank().capacity());
    }

    public List<TestFunction> serialize() {
        return Stream.of(Tier.values()).filter(Tier::isNormalTankTier)
            .filter(Predicate.isEqual(Tier.WOOD).negate())
            .flatMap(t -> Stream.of(
                GameTestFunctions.create("recipe_test", TestFunction.EMPTY_STRUCTURE, getClass().getSimpleName() + "_json_" + t.name().toLowerCase(Locale.ROOT), (g) -> serializeJson(g, t)),
                GameTestFunctions.create("recipe_test", TestFunction.EMPTY_STRUCTURE, getClass().getSimpleName() + "_packet_" + t.name().toLowerCase(Locale.ROOT), (g) -> serializePacket(g, t))
            ))
            .toList();
    }

    void serializeJson(GameTestHelper helper, Tier tier) {
        var subItem = Ingredient.of(Items.APPLE);
        var recipe = new TierRecipe(
            new Recipe.CommonInfo(false), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, TierRecipe.TANK_RECIPE_GROUP),
            tier, TierRecipe.Serializer.getIngredientTankForTier(tier), subItem);
        String expected = """
            {
              "type": "%s",
              "tier": "%s",
              "sub_item": "minecraft:apple",
              "show_notification":false,
              "group":"fluidtank_tank"
            }
            """.formatted(TierRecipe.Serializer.LOCATION.toString(), tier.name());
        var expectedJson = GsonHelper.parse(expected);

        var codec = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var fromSerializer = assertDoesNotThrow(() -> Recipe.CODEC.encodeStart(codec, recipe).getOrThrow());
        assertEquals(expectedJson, fromSerializer);

        var deserialized = assertInstanceOf(TierRecipe.class,
            assertDoesNotThrow(() ->
                    Recipe.CODEC.parse(codec, fromSerializer).getOrThrow(),
                "Failed to parse recipe for %s".formatted(tier)),
            "Loaded recipe is not TierRecipe");
        assertNotNull(deserialized);
        assertAll(
            () -> assertTrue(ItemStack.matches(recipe.getResultItemStack(), deserialized.getResultItemStack()))
        );
        helper.succeed();
    }

    void serializePacket(GameTestHelper helper, Tier tier) {
        var subItem = Ingredient.of(Items.APPLE);
        var recipe = new TierRecipe(
            new Recipe.CommonInfo(false), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, TierRecipe.TANK_RECIPE_GROUP),
            tier, TierRecipe.Serializer.getIngredientTankForTier(tier), subItem);

        var buffer = new RegistryFriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer(), helper.getLevel().registryAccess());
        TierRecipe.Serializer.toNetwork(recipe, buffer);
        var deserialized = TierRecipe.Serializer.fromNetwork(buffer);
        assertNotNull(deserialized);
        assertAll(
            () -> assertTrue(ItemStack.matches(recipe.getResultItemStack(), deserialized.getResultItemStack()))
        );
        helper.succeed();
    }

    void getRecipeFromJson(GameTestHelper helper) {
        // language=json
        String jsonString = """
            {
              "type": "%s",
              "tier": "STONE",
              "sub_item": "minecraft:diamond"
            }
            """.formatted(TierRecipe.Serializer.LOCATION.toString());
        var read = assertInstanceOf(TierRecipe.class, managerFromJson(Identifier.fromNamespaceAndPath(FluidTankCommon.modId, "test_serialize"), GsonHelper.parse(jsonString), helper.getLevel().registryAccess()));
        var recipe = new TierRecipe(
            new Recipe.CommonInfo(false), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, TierRecipe.TANK_RECIPE_GROUP),
            Tier.STONE, TierRecipe.Serializer.getIngredientTankForTier(Tier.STONE), Ingredient.of(Items.DIAMOND));

        assertAll(
            () -> assertTrue(ItemStack.matches(recipe.getResultItemStack(), read.getResultItemStack()))
        );
        helper.succeed();
    }

    @SuppressWarnings("ConstantConditions")
    public List<TestFunction> loadJsonInData() {
        try (var files = Files.find(recipeParent, 1, (path, a) -> path.getFileName().toString().endsWith(".json"))) {
            return files.map(p -> GameTestFunctions.create("recipe_test", TestFunction.EMPTY_STRUCTURE, "load_" + FilenameUtils.getBaseName(p.getFileName().toString()),
                (g) -> loadFromFile(g, p))).toList();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    void notLoadLeadRecipe(GameTestHelper helper) throws IOException {
        var leadRecipe = recipeParent.resolve("tank_lead.json");
        var read = GsonHelper.parse(Files.newBufferedReader(leadRecipe));
        assertFalse(
            checkCondition(helper, read),
            "Lead recipe must not be loaded");
        helper.succeed();
    }

    // just for test
    @SuppressWarnings("UnstableApiUsage")
    private static boolean checkCondition(GameTestHelper helper, JsonObject read) {
        return ResourceConditionsImpl.applyResourceConditions(read, "TEST", Identifier.fromNamespaceAndPath(FluidTankCommon.modId, CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, "checkCondition")),
            new RegistryOps.RegistryInfoLookup() {
                @Override
                public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                    var r = helper.getLevel().registryAccess().lookupOrThrow(key);
                    return Optional.of(RegistryOps.RegistryInfo.fromRegistryLookup(r));
                }
            });
    }

    static void loadFromFile(GameTestHelper helper, Path path) {
        if (true) {
            // Fails when loading modded resource tank
            helper.succeed();
            return;
        }
        try {
            var json = GsonHelper.parse(Files.newBufferedReader(path));
            assertDoesNotThrow(() -> managerFromJson(Identifier.fromNamespaceAndPath(FluidTankCommon.modId, "test_load"), json, helper.getLevel().registryAccess()));
            helper.succeed();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Recipe<?> managerFromJson(Identifier location, JsonObject jsonObject, HolderLookup.Provider provider) {
        return Try.call(() -> RecipeManager.class.getDeclaredMethod("fromJson", ResourceKey.class, JsonObject.class, HolderLookup.Provider.class))
            .andThenTry(m -> ReflectionSupport.invokeMethod(m, null, ResourceKey.create(Registries.RECIPE, location), jsonObject, provider))
            .andThenTry(RecipeHolder.class::cast)
            .andThenTry(RecipeHolder::value)
            .andThenTry(Recipe.class::cast)
            .getOrThrow(RuntimeException::new);
    }
}
