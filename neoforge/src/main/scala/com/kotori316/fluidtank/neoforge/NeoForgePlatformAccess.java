package com.kotori316.fluidtank.neoforge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.cat.BlockChestAsTank;
import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.VanillaFluid;
import com.kotori316.fluidtank.fluids.VanillaPotion;
import com.kotori316.fluidtank.neoforge.cat.EntityChestAsTank;
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter;
import com.kotori316.fluidtank.potions.PotionFluidHandler;
import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.TankLootFunction;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class NeoForgePlatformAccess implements PlatformAccess {

    @Override
    public boolean isGaseous(Fluid fluid) {
        return fluid.getFluidType().isLighterThanAir();
    }

    @Override
    @NotNull
    public Fluid getBucketContent(BucketItem bucketItem) {
        return bucketItem.content;
    }

    @Override
    @NotNull
    public GenericAmount<FluidLike> getFluidContained(ItemStack stack) {
        var potionHandler = PotionFluidHandler.apply(stack);
        if (potionHandler.isValidHandler()) {
            return potionHandler.getContent();
        }
        return FluidUtil.getFluidContained(stack)
            .map(NeoForgeConverter::toAmount)
            .orElse(FluidAmountUtil.EMPTY());
    }

    @Override
    public boolean isFluidContainer(ItemStack stack) {
        return FluidUtil.getFluidHandler(stack).isPresent() ||
            PotionFluidHandler.apply(stack).isValidHandler();
    }

    @Override
    public Component getDisplayName(GenericAmount<FluidLike> amount) {
        if (amount.content() instanceof VanillaFluid) {
            return NeoForgeConverter.toStack(amount).getHoverName();
        } else if (amount.content() instanceof VanillaPotion vanillaPotion) {
            return vanillaPotion.getVanillaPotionName(amount.componentPatch());
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public @NotNull TransferStack fillItem(GenericAmount<FluidLike> toFill, ItemStack fluidContainer, Player player, InteractionHand hand, boolean execute) {
        if (toFill.content() instanceof VanillaPotion vanillaPotion) {
            var potionHandler = PotionFluidHandler.apply(fluidContainer);
            return potionHandler.fill(toFill, vanillaPotion);
        }
        return FluidUtil.getFluidHandler(fluidContainer.copyWithCount(1))
            .map(h -> {
                int filledAmount = h.fill(NeoForgeConverter.toStack(toFill), IFluidHandler.FluidAction.EXECUTE);
                return new TransferStack(toFill.setAmount(GenericUnit.fromForge(filledAmount)), h.getContainer());
            })
            .orElse(new TransferStack(FluidAmountUtil.EMPTY(), fluidContainer));
    }

    @Override
    public @NotNull TransferStack drainItem(GenericAmount<FluidLike> toDrain, ItemStack fluidContainer, Player player, InteractionHand hand, boolean execute) {
        if (toDrain.content() instanceof VanillaPotion v) {
            var potionHandler = PotionFluidHandler.apply(fluidContainer);
            return potionHandler.drain(toDrain, v);
        }
        return FluidUtil.getFluidHandler(fluidContainer.copyWithCount(1))
            .map(h -> {
                var drained = h.drain(NeoForgeConverter.toStack(toDrain), IFluidHandler.FluidAction.EXECUTE);
                return new TransferStack(NeoForgeConverter.toAmount(drained), h.getContainer());
            })
            .orElse(new TransferStack(FluidAmountUtil.EMPTY(), fluidContainer));
    }

    @Override
    public @Nullable SoundEvent getEmptySound(GenericAmount<FluidLike> fluid) {
        return FluidLike.asFluid(fluid.content(), Fluids.WATER).getFluidType().getSound(NeoForgeConverter.toStack(fluid), SoundActions.BUCKET_EMPTY);
    }

    @Override
    public @Nullable SoundEvent getFillSound(GenericAmount<FluidLike> fluid) {
        return FluidLike.asFluid(fluid.content(), Fluids.WATER).getFluidType().getSound(NeoForgeConverter.toStack(fluid), SoundActions.BUCKET_FILL);
    }

    @Override
    public BlockEntityType<? extends TileTank> getNormalType() {
        return FluidTank.TILE_TANK_TYPE.get();
    }

    @Override
    public BlockEntityType<? extends TileTank> getCreativeType() {
        return FluidTank.TILE_CREATIVE_TANK_TYPE.get();
    }

    @Override
    public BlockEntityType<? extends TileTank> getVoidType() {
        return FluidTank.TILE_VOID_TANK_TYPE.get();
    }

    @Override
    public LootItemFunctionType<TankLootFunction> getTankLoot() {
        return FluidTank.TANK_LOOT_FUNCTION.get();
    }

    @Override
    public Map<Tier, Supplier<? extends BlockTank>> getTankBlockMap() {
        return Stream.concat(FluidTank.TANK_MAP.entrySet().stream(), Stream.of(Map.entry(Tier.CREATIVE, FluidTank.BLOCK_CREATIVE_TANK), Map.entry(Tier.VOID, FluidTank.BLOCK_VOID_TANK)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<Tier, ? extends Supplier<? extends ItemReservoir>> getReservoirMap() {
        return FluidTank.RESERVOIR_MAP;
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.getCraftingRemainingItem();
    }

    @Override
    public Codec<Ingredient> ingredientCodec() {
        // OK, forge magic is included in the codec.
        return Ingredient.CODEC;
    }

    @Override
    public JsonElement ingredientToJson(Ingredient ingredient) {
        if (ingredient.hasNoItems()) {
            // NeoForge throws error when ingredient is empty.
            return new JsonArray();
        }
        return PlatformAccess.super.ingredientToJson(ingredient);
    }

    @Override
    public DataComponentType<Tank<FluidLike>> fluidTankComponentType() {
        return FluidTank.FLUID_TANK_DATA_COMPONENT.get();
    }

    @Override
    public Supplier<? extends BlockChestAsTank> getCATBlock() {
        return FluidTank.BLOCK_CAT;
    }

    @Override
    public BlockEntity createCATEntity(BlockPos pos, BlockState state) {
        return new EntityChestAsTank(pos, state);
    }

    @Override
    public List<GenericAmount<FluidLike>> getCATFluids(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EntityChestAsTank cat) {
            return cat.getFluids().orElse(List.of());
        } else {
            return List.of();
        }
    }
}
