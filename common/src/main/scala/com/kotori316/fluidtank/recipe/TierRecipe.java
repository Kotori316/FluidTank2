package com.kotori316.fluidtank.recipe;

import com.google.gson.JsonObject;
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
import com.kotori316.fluidtank.tank.ItemBlockTank;
import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TierRecipe implements CraftingRecipe {
    private static final Logger LOGGER = LoggerFactory.getLogger(TierRecipe.class);
    public static final Serializer SERIALIZER = new Serializer();

    final Tier tier;
    final Ingredient tankItem;
    final Ingredient subItem;
    final ItemStack result;
    final ShapedRecipePattern pattern;
    final PlacementInfo placementInfo;
    private static final int recipeWidth = 3;
    private static final int recipeHeight = 3;

    public TierRecipe(Tier tier, Ingredient tankItem, Ingredient subItem) {
        this.tier = tier;
        this.tankItem = tankItem;
        this.subItem = subItem;
        this.result = new ItemStack(PlatformTankAccess.getInstance().getTankBlockMap().get(tier).get());
        this.pattern = ShapedRecipePattern.of(Map.of('t', tankItem, 's', subItem),
            List.of(
                "tst",
                "s s",
                "tst"
            )
        );
        this.placementInfo = PlacementInfo.createFromOptionals(this.pattern.ingredients());

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
                .map(CustomData::copyTag)
                .map(nbt -> TankUtil.load(nbt.getCompound(TileTank.KEY_TANK()), FluidAmountUtil.access()))
                .map(Tank::content)
                .filter(GenericAmount::nonEmpty)
                .map(FluidLikeKey::from)
                .distinct()
                .count() <= 1;
    }

    @NotNull
    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider access) {
        if (!this.matches(inv, null)) {
            var stacks = inv.items();
            LOGGER.error("Requested to return crafting result for invalid inventory. {}", stacks);
            DebugLogging.LOGGER().error("Requested to return crafting result for invalid inventory. {}", stacks);
            return ItemStack.EMPTY;
        }
        ItemStack result = this.result.copy();
        GenericAmount<FluidLike> fluidAmount = IntStream.range(0, inv.size()).mapToObj(inv::getItem)
            .filter(s -> s.getItem() instanceof ItemBlockTank)
            .map(s -> s.get(DataComponents.BLOCK_ENTITY_DATA))
            .filter(Objects::nonNull)
            .map(CustomData::copyTag)
            .map(nbt -> TankUtil.load(nbt.getCompound(TileTank.KEY_TANK()), FluidAmountUtil.access()))
            .map(Tank::content)
            .filter(GenericAmount::nonEmpty)
            .reduce(GenericAmount::add).orElse(FluidAmountUtil.EMPTY());

        if (fluidAmount.nonEmpty()) {
            CompoundTag compound = new CompoundTag();

            var tank = new Tank<>(fluidAmount, GenericUnit.apply(tier.getCapacity()));
            CompoundTag tankTag = TankUtil.save(tank, FluidAmountUtil.access());
            compound.put(TileTank.KEY_TANK(), tankTag);
            compound.putString(TileTank.KEY_TIER(), tier.name());

            var location = BlockEntityType.getKey(PlatformTankAccess.getInstance().getNormalType());
            assert location != null : "The tile type must be registered";
            PlatformItemAccess.setTileTag(result, compound, location.toString());
        }

        return result;
    }

    @NotNull
    @Override
    public RecipeSerializer<TierRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo() {
        return placementInfo;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapedCraftingRecipeDisplay(
            this.pattern.width(), this.pattern.height(),
            this.pattern.ingredients().stream().map((optional) -> optional.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(),
            new SlotDisplay.ItemStackSlotDisplay(this.result),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }

    @NotNull
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        return IntStream.range(0, inv.size())
            .mapToObj(inv::getItem)
            .map(stack -> {
                if (stack.getItem() instanceof ItemBlockTank) return ItemStack.EMPTY;
                else return PlatformItemAccess.getInstance().getCraftingRemainingItem(stack);
            })
            .collect(Collectors.toCollection(NonNullList::create));
    }

    public Tier getTier() {
        return tier;
    }

    private Ingredient getSubItem() {
        return this.subItem;
    }

    @NotNull
    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static final String KEY_TIER = "tier";
    public static final String KEY_SUB_ITEM = "sub_item";

    public static final class Serializer implements RecipeSerializer<TierRecipe> {
        public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "crafting_grade_up");
        private final MapCodec<TierRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, TierRecipe> streamCodec;

        public Serializer() {
            this.codec = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                    Codec.STRING.xmap(Tier::valueOfIgnoreCase, Tier::name).fieldOf(KEY_TIER).forGetter(TierRecipe::getTier),
                    Ingredient.CODEC.fieldOf(KEY_SUB_ITEM).forGetter(TierRecipe::getSubItem)
                ).apply(instance, this::createInstanceInternal)
            );
            this.streamCodec = StreamCodec.ofMember(this::toNetwork, this::fromNetwork);
        }

        private TierRecipe createInstance(Tier tier, Ingredient tankItem, Ingredient subItem) {
            return new TierRecipe(tier, tankItem, subItem);
        }

        private TierRecipe createInstanceInternal(Tier tier, Ingredient subItem) {
            return this.createInstance(tier, getIngredientTankForTier(tier), subItem);
        }

        @NotNull
        @Override
        public MapCodec<TierRecipe> codec() {
            return this.codec;
        }

        @NotNull
        @Override
        @Deprecated
        public StreamCodec<RegistryFriendlyByteBuf, TierRecipe> streamCodec() {
            return this.streamCodec;
        }

        public TierRecipe fromJson(JsonObject object) {
            return this.codec.codec().parse(JsonOps.INSTANCE, object)
                .getOrThrow(s -> {
                    var message = "Error in parsing TierRecipe (%s) from JSON %s".formatted(s, object);
                    LOGGER.error(message);
                    DebugLogging.LOGGER().error(message);
                    return new IllegalStateException(message);
                });
        }

        public JsonObject toJson(TierRecipe recipe) {
            return this.codec.codec().encodeStart(JsonOps.INSTANCE, recipe)
                .getOrThrow(s -> {
                    var message = "Error in encoding TierRecipe (%s) from Recipe %s".formatted(s, recipe);
                    LOGGER.error(message);
                    DebugLogging.LOGGER().error(message);
                    return new IllegalStateException(message);
                })
                .getAsJsonObject();
        }

        public TierRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String tierName = buffer.readUtf();
            Tier tier = Tier.valueOf(tierName);
            Ingredient tankItem = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            Ingredient subItem = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);

            DebugLogging.LOGGER().debug("Serializer loaded from packet for tier {}, sub {}.", tier, PlatformItemAccess.convertIngredientToString(subItem));
            return createInstance(tier, tankItem, subItem);
        }

        public void toNetwork(TierRecipe recipe, RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(recipe.tier.name());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.tankItem);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.subItem);
            DebugLogging.LOGGER().debug("Serialized to packet for tier {}.", recipe.tier);
        }

        public static Ingredient getIngredientTankForTier(Tier tier) {
            var targetTiers = Stream.of(Tier.values()).filter(t -> t.getRank() == tier.getRank() - 1);
            var itemStream = targetTiers.map(PlatformTankAccess.getInstance().getTankBlockMap()::get).map(Supplier::get);
            return Ingredient.of(itemStream);
        }
    }
}
