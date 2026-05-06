package com.kotori316.fluidtank.recipe;

import com.kotori316.fluidtank.DebugLogging;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.contents.TankUtil;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.FluidLikeKey;
import com.kotori316.fluidtank.item.PlatformItemAccess;
import com.kotori316.fluidtank.tank.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TierRecipe extends NormalCraftingRecipe implements CraftingRecipe {
    private static final Logger LOGGER = LoggerFactory.getLogger(TierRecipe.class);
    public static final RecipeSerializer<TierRecipe> SERIALIZER = new RecipeSerializer<>(Serializer.CODEC, Serializer.STREAM_CODEC);
    public static final String TANK_RECIPE_GROUP = FluidTankCommon.modId + "_" + "tank";

    final Tier tier;
    final Ingredient tankItem;
    final Ingredient subItem;
    public final ItemStackTemplate result;
    final ShapedRecipePattern pattern;

    public TierRecipe(Recipe.CommonInfo info, CraftingRecipe.CraftingBookInfo bookInfo, Tier tier, Ingredient tankItem, Ingredient subItem) {
        super(info, bookInfo);
        this.tier = tier;
        this.tankItem = tankItem;
        this.subItem = subItem;
        this.result = new ItemStackTemplate(PlatformTankAccess.getInstance().getTankBlockMap().get(tier).get().asItem());
        this.pattern = ShapedRecipePattern.of(Map.of('t', tankItem, 's', subItem),
            List.of(
                "tst",
                "s s",
                "tst"
            )
        );

        DebugLogging.LOGGER().debug("{} instance created for Tier {}({}).", getClass().getSimpleName(), tier, result);
    }

    @Override
    public boolean matches(CraftingInput input, @Nullable Level worldIn) {
        if (!this.pattern.matches(input)) {
            return false;
        }
        // Items are placed correctly.
        List<ItemStack> tankStacks = input.items().stream()
            .filter(this.tankItem)
            .toList();
        return tankStacks.size() == 4 &&
            tankStacks.stream().map(s -> s.get(DataComponents.BLOCK_ENTITY_DATA))
                .filter(Objects::nonNull)
                .map(TypedEntityData::copyTagWithoutId)
                .flatMap(n -> n.getCompound(TileTank.KEY_TANK()).stream())
                .map(nbt -> TankUtil.load(nbt, FluidAmountUtil.access()))
                .map(Tank::content)
                .filter(GenericAmount::nonEmpty)
                .map(FluidLikeKey::from)
                .distinct()
                .count() <= 1;
    }

    @NotNull
    @Override
    public ItemStack assemble(CraftingInput inv) {
        if (!this.matches(inv, null)) {
            var stacks = inv.items();
            LOGGER.error("Requested to return crafting result for invalid inventory. {}", stacks);
            DebugLogging.LOGGER().error("Requested to return crafting result for invalid inventory. {}", stacks);
            return ItemStack.EMPTY;
        }
        ItemStack result = this.result.create();
        GenericAmount<FluidLike> fluidAmount = IntStream.range(0, inv.size()).mapToObj(inv::getItem)
            .filter(s -> s.getItem() instanceof ItemBlockTank)
            .map(s -> s.get(DataComponents.BLOCK_ENTITY_DATA))
            .filter(Objects::nonNull)
            .map(TypedEntityData::copyTagWithoutId)
            .flatMap(n -> n.getCompound(TileTank.KEY_TANK()).stream())
            .map(nbt -> TankUtil.load(nbt, FluidAmountUtil.access()))
            .map(Tank::content)
            .filter(GenericAmount::nonEmpty)
            .reduce(GenericAmount::add).orElse(FluidAmountUtil.EMPTY());

        if (fluidAmount.nonEmpty()) {
            try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
                TagValueOutput tagValueOutput = TagValueOutput.createWithoutContext(reporter);
                var tank = new Tank<>(fluidAmount, GenericUnit.apply(tier.getCapacity()));
                tagValueOutput.store(TileTank.KEY_TANK(), Tank.codec(FluidAmountUtil.access()), tank);
                tagValueOutput.putString(TileTank.KEY_TIER(), tier.name());

                PlatformItemAccess.setTileTag(result, tagValueOutput, PlatformTankAccess.getInstance().getNormalType());
            }
        }

        return result;
    }

    @NotNull
    @Override
    public RecipeSerializer<TierRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(this.pattern.ingredients());
    }

    /**
     * Copied from {@link ShapedRecipe#display()}
     */
    @Override
    public List<RecipeDisplay> display() {
        return List.of(
            new ShapedCraftingRecipeDisplay(
                3, 3,
                this.pattern.ingredients().stream().map(e -> e.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(),
                new SlotDisplay.ItemStackSlotDisplay(this.result),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
            )
        );
    }

    @NotNull
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        return IntStream.range(0, inv.size())
            .mapToObj(inv::getItem)
            .map(this::getRemaining)
            .collect(Collectors.toCollection(NonNullList::create));
    }

    private ItemStack getRemaining(ItemStack stack) {
        if (stack.getItem() instanceof ItemBlockTank) {
            return ItemStack.EMPTY;
        }
        var template = PlatformItemAccess.getInstance().getCraftingRemainingItem(stack);
        if (template == null) {
            return ItemStack.EMPTY;
        }
        return template.create();
    }

    public Tier getTier() {
        return tier;
    }

    public Ingredient getTankItem() {
        return tankItem;
    }

    public Ingredient getSubItem() {
        return this.subItem;
    }

    @VisibleForTesting
    public ItemStack getResultItemStack() {
        return result.create();
    }

    public static final String KEY_TIER = "tier";
    public static final String KEY_SUB_ITEM = "sub_item";

    public static final class Serializer {
        public static final Identifier LOCATION = Identifier.fromNamespaceAndPath(FluidTankCommon.modId, "crafting_grade_up");
        public static final MapCodec<TierRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                Codec.STRING.xmap(Tier::valueOfIgnoreCase, Tier::name).fieldOf(KEY_TIER).forGetter(TierRecipe::getTier),
                Ingredient.CODEC.fieldOf(KEY_SUB_ITEM).forGetter(TierRecipe::getSubItem)
            ).apply(instance, Serializer::createInstanceInternal)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, TierRecipe> STREAM_CODEC =
            StreamCodec.ofMember(Serializer::toNetwork, Serializer::fromNetwork);

        private static TierRecipe createInstanceInternal(Recipe.CommonInfo info, CraftingRecipe.CraftingBookInfo bookInfo, Tier tier, Ingredient subItem) {
            return new TierRecipe(info, bookInfo, tier, getIngredientTankForTier(tier), subItem);
        }

        public static TierRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            var info = Recipe.CommonInfo.STREAM_CODEC.decode(buffer);
            var bookInfo = CraftingRecipe.CraftingBookInfo.STREAM_CODEC.decode(buffer);
            String tierName = buffer.readUtf();
            Tier tier = Tier.valueOf(tierName);
            Ingredient tankItem = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            Ingredient subItem = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);

            DebugLogging.LOGGER().debug("Serializer loaded from packet for tier {}, sub {}.", tier, PlatformItemAccess.convertIngredientToString(subItem));
            return new TierRecipe(info, bookInfo, tier, tankItem, subItem);
        }

        public static void toNetwork(TierRecipe recipe, RegistryFriendlyByteBuf buffer) {
            Recipe.CommonInfo.STREAM_CODEC.encode(buffer, recipe.commonInfo);
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC.encode(buffer, recipe.bookInfo);
            buffer.writeUtf(recipe.tier.name());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.tankItem);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.subItem);
            DebugLogging.LOGGER().debug("Serialized to packet for tier {}.", recipe.tier);
        }

        public static Ingredient getIngredientTankForTier(Tier tier) {
            return Ingredient.of(getTankForTier(tier));
        }

        public static Stream<? extends BlockTank> getTankForTier(Tier tier) {
            var targetTiers = Stream.of(Tier.values()).filter(t -> t.getRank() == tier.getRank() - 1);
            return targetTiers.map(PlatformTankAccess.getInstance().getTankBlockMap()::get).map(Supplier::get);
        }
    }
}
