package com.kotori316.fluidtank.fabric;

import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fabric.cat.ChestAsTankStorage;
import com.kotori316.fluidtank.fabric.fluid.FabricConverter;
import com.kotori316.fluidtank.fluids.*;
import com.kotori316.fluidtank.potions.PotionFluidHandler;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
final class FabricPlatformAccess implements PlatformAccess {
    @Override
    public boolean isGaseous(Fluid fluid) {
        return FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid));
    }

    @Override
    public @NotNull Fluid getBucketContent(BucketItem bucketItem) {
        return ((BucketItemAccessor) bucketItem).fabric_getFluid();
    }

    @Override
    public @NotNull GenericAmount<FluidLike> getFluidContained(ItemStack stack) {
        var potionHandler = PotionFluidHandler.apply(stack);
        if (potionHandler.isValidHandler()) {
            return potionHandler.getContent();
        }
        var storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
        if (storage != null) {
            for (StorageView<FluidVariant> view : storage) {
                var variant = view.getResource();
                var amount = view.getAmount();
                return FluidAmountUtil.from(FluidLike.of(variant.getFluid()), GenericUnit.fromFabric(amount), variant.getComponents());
            }
        }
        return FluidAmountUtil.EMPTY();
    }

    @Override
    public boolean isFluidContainer(ItemStack stack) {
        if (PotionFluidHandler.apply(stack).isValidHandler()) return true;
        var storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
        return storage != null;
    }

    @Override
    public Component getDisplayName(GenericAmount<FluidLike> amount) {
        if (amount.content() instanceof VanillaFluid) {
            return FluidVariantAttributes.getName(FabricConverter.toVariant(amount, Fluids.EMPTY));
        } else if (amount.content() instanceof VanillaPotion vanillaPotion) {
            return vanillaPotion.getVanillaPotionName(amount.componentPatch());
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public @NotNull TransferStack fillItem(GenericAmount<FluidLike> toFill, ItemStack stack, Player player, InteractionHand hand, boolean execute) {
        var context = ContainerItemContext.ofPlayerHand(player, hand);
        if (toFill.content() instanceof VanillaPotion vanillaPotion) {
            var potionHandler = PotionFluidHandler.apply(stack);
            var result = potionHandler.fill(toFill, vanillaPotion);
            return moveItem(stack, player, execute, result, context);
        }

        var storage = FluidStorage.ITEM.find(stack, context);
        if (storage == null) {
            return new TransferStack(FluidAmountUtil.EMPTY(), stack, false);
        }

        long filled;
        try (Transaction transaction = Transaction.openOuter()) {
            filled = storage.insert(FabricConverter.toVariant(toFill, Fluids.EMPTY), FabricConverter.fabricAmount(toFill), transaction);
            // Items in creative player should not be changed.
            if (execute && TransferFluid.shouldMoveItem(player)) transaction.commit();
        }
        // FluidTankCommon.LOGGER.warn("Fill context {} {} execute={}", context.getItemVariant(), context.getAmount(), execute);
        return new TransferStack(toFill.setAmount(GenericUnit.fromFabric(filled)), context.getItemVariant().toStack((int) context.getAmount()), false);
    }

    @Override
    public @NotNull TransferStack drainItem(GenericAmount<FluidLike> toDrain, ItemStack stack, Player player, InteractionHand hand, boolean execute) {
        var context = ContainerItemContext.ofPlayerHand(player, hand);
        if (toDrain.content() instanceof VanillaPotion v) {
            var potionHandler = PotionFluidHandler.apply(stack);
            var result = potionHandler.drain(toDrain, v);
            return moveItem(stack, player, execute, result, context);
        }

        var storage = FluidStorage.ITEM.find(stack, context);
        if (storage == null) {
            return new TransferStack(FluidAmountUtil.EMPTY(), stack, false);
        }
        long drained;
        try (Transaction transaction = Transaction.openOuter()) {
            drained = storage.extract(FabricConverter.toVariant(toDrain, Fluids.EMPTY), FabricConverter.fabricAmount(toDrain), transaction);
            // Items in creative player should not be changed.
            if (execute && TransferFluid.shouldMoveItem(player)) transaction.commit();
        }
        // FluidTankCommon.LOGGER.warn("Drain context {} {} execute={}", context.getItemVariant(), context.getAmount(), execute);
        return new TransferStack(toDrain.setAmount(GenericUnit.fromFabric(drained)), context.getItemVariant().toStack((int) context.getAmount()), false);
    }

    @NotNull
    private static TransferStack moveItem(ItemStack stack, Player player, boolean execute, TransferStack result, ContainerItemContext context) {
        if (result.moved().nonEmpty()) {
            try (Transaction transaction = Transaction.openOuter()) {
                var exchanged = context.exchange(ItemVariant.of(result.toReplace()), 1, transaction);
                if (exchanged == 1) {
                    if (execute && TransferFluid.shouldMoveItem(player)) {
                        transaction.commit();
                    }
                    return result.setShouldMove(false);
                }
                // Failed to exchange drained item, abort
            }
            return new TransferStack(FluidAmountUtil.EMPTY(), stack, false);
        } else {
            // Nothing moved
            return result;
        }
    }

    @Override
    public @Nullable SoundEvent getEmptySound(GenericAmount<FluidLike> fluid) {
        return FluidVariantAttributes.getEmptySound(FabricConverter.toVariant(fluid, Fluids.WATER));
    }

    @Override
    public @Nullable SoundEvent getFillSound(GenericAmount<FluidLike> fluid) {
        return FluidVariantAttributes.getFillSound(FabricConverter.toVariant(fluid, Fluids.WATER));
    }

    @Override
    public BlockEntityType<? extends TileTank> getNormalType() {
        return FluidTank.TILE_TANK_TYPE;
    }

    @Override
    public BlockEntityType<? extends TileTank> getCreativeType() {
        return FluidTank.TILE_CREATIVE_TANK_TYPE;
    }

    @Override
    public BlockEntityType<? extends TileTank> getVoidType() {
        return FluidTank.TILE_VOID_TANK_TYPE;
    }

    @Override
    public LootItemFunctionType getTankLoot() {
        return FluidTank.TANK_LOOT_FUNCTION;
    }

    @Override
    public Map<Tier, Supplier<? extends BlockTank>> getTankBlockMap() {
        return Stream.concat(FluidTank.TANK_MAP.entrySet().stream(),
                Stream.of(Map.entry(Tier.CREATIVE, FluidTank.BLOCK_CREATIVE_TANK), Map.entry(Tier.VOID, FluidTank.BLOCK_VOID_TANK)))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e::getValue));
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.getRecipeRemainder();
    }

    @Override
    public Codec<Ingredient> ingredientCodec() {
        // OK, fabric mixins the creation method to insert own codec
        return Ingredient.CODEC;
    }

    @Override
    public DataComponentType<Tank<FluidLike>> fluidTankComponentType() {
        return FluidTank.FLUID_TANK_DATA_COMPONENT;
    }

    @Override
    public @Nullable BlockEntity createCATEntity(BlockPos pos, BlockState state) {
        // Not necessary as fabric can attach storage to block
        return null;
    }

    @Override
    public @NotNull List<GenericAmount<FluidLike>> getCATFluids(Level level, BlockPos pos) {
        return ChestAsTankStorage.getCATFluids(level, pos);
    }
}
