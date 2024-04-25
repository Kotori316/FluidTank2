package com.kotori316.fluidtank.fabric.gametest;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonObject;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fabric.FluidTank;
import com.kotori316.fluidtank.fabric.recipe.RecipeInventoryUtil;
import com.kotori316.fluidtank.fabric.recipe.TierRecipeFabric;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.tank.Tier;
import io.netty.buffer.ByteBufAllocator;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.ReflectionSupport;
import scala.jdk.javaapi.CollectionConverters;

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

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
public final class RecipeTest implements FabricGameTest {

    @GameTestGenerator
    public List<TestFunction> generator() {
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
    private static TierRecipeFabric getRecipe() {
        return new TierRecipeFabric(Tier.STONE,
            Ingredient.of(FluidTank.TANK_MAP.get(Tier.WOOD)), Ingredient.of(Items.STONE)
        );
    }

    void createInstance() {
        TierRecipeFabric recipe = getRecipe();
        assertNotNull(recipe);
    }

    void match1() {
        var recipe = getRecipe();
        assertTrue(recipe.matches(RecipeInventoryUtil.getInv("tst", "s s", "tst", CollectionConverters.asScala(Map.of(
            't', new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD)),
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
            'k', new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD)),
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
            't', new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD)),
            's', new ItemStack(Items.STONE)
        ))), null));
    }

    @GameTestGenerator
    public List<TestFunction> combineFluids() {
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
        var empty = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD));
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

    void combine2(GenericAmount<FluidLike> amount) {
        var filled = RecipeInventoryUtil.getFilledTankStack(Tier.WOOD, amount);
        var empty = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD));
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
    public List<TestFunction> serialize() {
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
        var recipe = new TierRecipeFabric(
            tier, TierRecipeFabric.Serializer.getIngredientTankForTier(tier), subItem);

        var fromSerializer = TierRecipeFabric.SERIALIZER.toJson(recipe);

        var deserialized = TierRecipeFabric.SERIALIZER.fromJson(fromSerializer);
        assertNotNull(deserialized);
        assertAll(
            () -> assertTrue(ItemStack.matches(recipe.getResultItem(RegistryAccess.EMPTY), deserialized.getResultItem(RegistryAccess.EMPTY)))
        );
    }

    void serializePacket(GameTestHelper helper, Tier tier) {
        var subItem = Ingredient.of(Items.APPLE);
        var recipe = new TierRecipeFabric(
            tier, TierRecipeFabric.Serializer.getIngredientTankForTier(tier), subItem);

        var buffer = new RegistryFriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer(), helper.getLevel().registryAccess());
        TierRecipeFabric.SERIALIZER.toNetwork(recipe, buffer);
        var deserialized = TierRecipeFabric.SERIALIZER.fromNetwork(buffer);
        assertNotNull(deserialized);
        assertAll(
            () -> assertTrue(ItemStack.matches(recipe.getResultItem(helper.getLevel().registryAccess()), deserialized.getResultItem(helper.getLevel().registryAccess())))
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
            """.formatted(TierRecipeFabric.Serializer.LOCATION.toString());
        var read = managerFromJson(new ResourceLocation(FluidTankCommon.modId, "test_serialize"), GsonHelper.parse(jsonString), helper.getLevel().registryAccess());
        var recipe = new TierRecipeFabric(
            Tier.STONE, TierRecipeFabric.Serializer.getIngredientTankForTier(Tier.STONE), Ingredient.of(Items.DIAMOND));

        assertAll(
            () -> assertTrue(ItemStack.matches(recipe.getResultItem(helper.getLevel().registryAccess()), read.getResultItem(helper.getLevel().registryAccess())))
        );
        helper.succeed();
    }

    @GameTestGenerator
    @SuppressWarnings("ConstantConditions")
    public List<TestFunction> loadJsonInData() throws IOException {
        var recipeParent = Path.of("../../common/src/generated/resources", "data/fluidtank/recipes");
        try (var files = Files.find(recipeParent, 1, (path, a) -> path.getFileName().toString().endsWith(".json"))) {
            return files.map(p -> GameTestUtil.create(FluidTankCommon.modId, "recipe_test", "load_" + FilenameUtils.getBaseName(p.getFileName().toString()),
                (g) -> loadFromFile(g, p))).toList();
        }
    }

    // just for test
    @SuppressWarnings("UnstableApiUsage")
    void notLoadLeadRecipe(GameTestHelper helper) throws IOException {
        var recipeParent = Path.of("../../common/src/generated/resources", "data/fluidtank/recipes");
        var leadRecipe = recipeParent.resolve("tank_lead.json");
        var read = GsonHelper.parse(Files.newBufferedReader(leadRecipe));
        assertFalse(
            ResourceConditionsImpl.applyResourceConditions(read, "TEST", new ResourceLocation(FluidTankCommon.modId, CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, "notLoadLeadRecipe")), helper.getLevel().registryAccess()),
            "Lead recipe must not be loaded");
        helper.succeed();
    }

    static void loadFromFile(GameTestHelper helper, Path path) {
        try {
            var json = GsonHelper.parse(Files.newBufferedReader(path));
            assertDoesNotThrow(() -> managerFromJson(new ResourceLocation(FluidTankCommon.modId, "test_load"), json, helper.getLevel().registryAccess()));
            helper.succeed();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Recipe<?> managerFromJson(ResourceLocation location, JsonObject jsonObject, HolderLookup.Provider provider) {
        return Try.call(() -> RecipeManager.class.getDeclaredMethod("fromJson", ResourceLocation.class, JsonObject.class, HolderLookup.Provider.class))
            .andThenTry(m -> ReflectionSupport.invokeMethod(m, null, location, jsonObject, provider))
            .andThenTry(RecipeHolder.class::cast)
            .andThenTry(RecipeHolder::value)
            .andThenTry(Recipe.class::cast)
            .getOrThrow(RuntimeException::new);
    }
}
