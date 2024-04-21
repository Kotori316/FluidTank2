package com.kotori316.fluidtank.neoforge.gametest;

import com.google.gson.JsonObject;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.neoforge.FluidTank;
import com.kotori316.fluidtank.neoforge.recipe.TierRecipeNeoForge;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.testutil.GameTestUtil;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import scala.jdk.javaapi.CollectionConverters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@GameTestHolder(FluidTankCommon.modId)
final class RecipeTest {

    @GameTestGenerator
    List<TestFunction> generator() {
        return GetGameTestMethods.getTests(getClass(), this, "recipe_test");
    }

    @NotNull
    private static TierRecipeNeoForge getRecipe() {
        return new TierRecipeNeoForge(Tier.STONE,
            Ingredient.of(FluidTank.TANK_MAP.get(Tier.WOOD).get()), Ingredient.of(Tags.Items.STONES)
        );
    }

    void createInstance() {
        TierRecipeNeoForge recipe = getRecipe();
        assertNotNull(recipe);
    }

    void match1() {
        var recipe = getRecipe();
        assertTrue(recipe.matches(RecipeInventoryUtil.getInv("tst", "s s", "tst", CollectionConverters.<Character, ItemStack>asScala(Map.of(
            't', new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get()),
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    void match2() {
        var recipe = getRecipe();
        var stack = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, FluidAmountUtil.BUCKET_WATER());

        assertTrue(recipe.matches(RecipeInventoryUtil.getInv("tst", "s s", "tst", CollectionConverters.<Character, ItemStack>asScala(Map.of(
            't', stack,
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    void match3() {
        var recipe = getRecipe();
        var stack = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, FluidAmountUtil.BUCKET_WATER());

        assertTrue(recipe.matches(RecipeInventoryUtil.getInv("tsk", "s s", "kst", CollectionConverters.<Character, ItemStack>asScala(Map.of(
            't', stack,
            'k', new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get()),
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    void notMatch4() {
        var recipe = getRecipe();
        var stack = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, FluidAmountUtil.BUCKET_WATER());
        var stack2 = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, FluidAmountUtil.BUCKET_LAVA());

        assertFalse(recipe.matches(RecipeInventoryUtil.getInv("tsk", "s s", "kst", CollectionConverters.<Character, ItemStack>asScala(Map.of(
            't', stack,
            'k', stack2,
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    void notMatch5() {
        var recipe = getRecipe();
        assertFalse(recipe.matches(RecipeInventoryUtil.getInv("tst", "s s", "ts ", CollectionConverters.<Character, ItemStack>asScala(Map.of(
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

    void combine1(GenericAmount<FluidLike> amount) {
        var filled = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, amount);
        var empty = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get());
        var recipe = getRecipe();

        var inv = RecipeInventoryUtil.getInv("ksk", "s s", "kst", CollectionConverters.<Character, ItemStack>asScala(Map.of(
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

    void combine2(GenericAmount<FluidLike> amount) {
        var filled = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, amount);
        var empty = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get());
        var recipe = getRecipe();

        var inv = RecipeInventoryUtil.getInv("kst", "s s", "kst", CollectionConverters.<Character, ItemStack>asScala(Map.of(
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
                GameTestUtil.create(FluidTankCommon.modId, "recipe_test", getClass().getSimpleName() + "_packet_" + t.name().toLowerCase(Locale.ROOT), (g) -> serializePacket(g, t))
            ))
            .toList();
    }

    void serializeJson(Tier tier) {
        var subItem = Ingredient.of(Items.APPLE);
        var id = new ResourceLocation(FluidTankCommon.modId, "test_" + tier.name().toLowerCase(Locale.ROOT));
        var recipe = new TierRecipeNeoForge(
            tier, TierRecipeNeoForge.Serializer.getIngredientTankForTier(tier), subItem);

        var fromSerializer = ((TierRecipeNeoForge.Serializer) TierRecipeNeoForge.SERIALIZER).toJson(recipe);

        var deserialized = ((TierRecipeNeoForge.Serializer) TierRecipeNeoForge.SERIALIZER).fromJson(fromSerializer);
        assertNotNull(deserialized);
        assertAll(
            () -> assertTrue(ItemStack.matches(recipe.getResultItem(RegistryAccess.EMPTY), deserialized.getResultItem(RegistryAccess.EMPTY)))
        );
    }

    void serializePacket(GameTestHelper helper, Tier tier) {
        var subItem = Ingredient.of(Items.APPLE);
        var recipe = new TierRecipeNeoForge(tier, TierRecipeNeoForge.Serializer.getIngredientTankForTier(tier), subItem);

        var buffer = new RegistryFriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer(), helper.getLevel().registryAccess());
        var streamCodec = TierRecipeNeoForge.SERIALIZER.streamCodec();
        streamCodec.encode(buffer, recipe);
        var deserialized = streamCodec.decode(buffer);
        assertNotNull(deserialized);
        assertAll(
            () -> assertTrue(ItemStack.matches(recipe.getResultItem(RegistryAccess.EMPTY), deserialized.getResultItem(RegistryAccess.EMPTY)))
        );
        helper.succeed();
    }

    void getRecipeFromJson(GameTestHelper helper) {
        // language=json
        String jsonString = """
            {
              "type": "%s",
              "tier": "STONE",
              "sub_item": {
                "item": "minecraft:diamond"
              }
            }
            """.formatted(TierRecipeNeoForge.Serializer.LOCATION.toString());
        var read = managerFromJson(new ResourceLocation(FluidTankCommon.modId, "test_serialize"), GsonHelper.parse(jsonString), helper.getLevel().registryAccess()).orElseThrow();
        var recipe = new TierRecipeNeoForge(
            Tier.STONE, TierRecipeNeoForge.Serializer.getIngredientTankForTier(Tier.STONE), Ingredient.of(Items.DIAMOND));

        assertAll(
            () -> assertTrue(ItemStack.matches(recipe.getResultItem(RegistryAccess.EMPTY), read.getResultItem(RegistryAccess.EMPTY)))
        );
        helper.succeed();
    }

    @GameTestGenerator
    @SuppressWarnings("ConstantConditions")
    List<TestFunction> loadJsonInData() throws IOException {
        var recipeParent = Path.of("../../common/src/generated/resources", "data/fluidtank/recipes");
        try (var files = Files.find(recipeParent, 1, (path, a) -> path.getFileName().toString().endsWith(".json"))) {
            return files.map(p -> GameTestUtil.create(FluidTankCommon.modId, "recipe_test", "load_" + FilenameUtils.getBaseName(p.getFileName().toString()),
                (g) -> {
                    loadFromFile(g, p);
                    g.succeed();
                })).toList();
        }
    }

    void notLoadLeadRecipe(GameTestHelper helper) {
        var recipeParent = Path.of("../../common/src/generated/resources", "data/fluidtank/recipes");
        var leadRecipe = recipeParent.resolve("tank_lead.json");
        var read = loadFromFile(helper, leadRecipe);
        assertTrue(read.isEmpty(), "Lead recipe must not be loaded");
        helper.succeed();
    }

    static Optional<Recipe<?>> loadFromFile(GameTestHelper helper, Path path) {
        try {
            var json = GsonHelper.parse(Files.newBufferedReader(path));
            return assertDoesNotThrow(() -> managerFromJson(new ResourceLocation(FluidTankCommon.modId, "test_load"), json, helper.getLevel().registryAccess()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<Recipe<?>> managerFromJson(ResourceLocation location, JsonObject jsonObject, HolderLookup.Provider provider) {
        return Recipe.CONDITIONAL_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, provider), jsonObject)
            .getOrThrow()
            .map(WithConditions::carrier);
    }
}
