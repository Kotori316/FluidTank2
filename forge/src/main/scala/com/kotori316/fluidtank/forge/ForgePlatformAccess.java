package com.kotori316.fluidtank.forge;

import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fluids.*;
import com.kotori316.fluidtank.forge.cat.EntityChestAsTank;
import com.kotori316.fluidtank.forge.fluid.ForgeConverter;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ForgePlatformAccess implements PlatformAccess {

    @Override
    public boolean isGaseous(Fluid fluid) {
        return fluid.getFluidType().isLighterThanAir();
    }

    @Override
    @NotNull
    public Fluid getBucketContent(BucketItem bucketItem) {
        return bucketItem.getFluid();
    }

    @Override
    @NotNull
    public GenericAmount<FluidLike> getFluidContained(ItemStack stack) {
        if (stack.getItem() instanceof PotionItem potionItem) {
            var potionFluid = FluidLike.of(PotionType.fromItemUnsafe(potionItem));
            return FluidAmountUtil.from(potionFluid, GenericUnit.ONE_BOTTLE(), Option.apply(stack.getTag()));
        } else if (stack.is(Items.GLASS_BOTTLE)) {
            return FluidAmountUtil.EMPTY();
        }
        return FluidUtil.getFluidContained(stack)
            .map(ForgeConverter::toAmount)
            .orElse(FluidAmountUtil.EMPTY());
    }

    @Override
    public boolean isFluidContainer(ItemStack stack) {
        return FluidUtil.getFluidHandler(stack).isPresent() ||
            stack.getItem() instanceof PotionItem ||
            stack.is(Items.GLASS_BOTTLE);
    }

    @Override
    public Component getDisplayName(GenericAmount<FluidLike> amount) {
        if (amount.content() instanceof VanillaFluid) {
            return ForgeConverter.toStack(amount).getDisplayName();
        } else if (amount.content() instanceof VanillaPotion vanillaPotion) {
            return vanillaPotion.getVanillaPotionName(amount.nbt());
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public @NotNull TransferStack fillItem(GenericAmount<FluidLike> toFill, ItemStack fluidContainer, Player player, InteractionHand hand, boolean execute) {
        if (toFill.content() instanceof VanillaPotion vanillaPotion) {
            // Potion
            if (fluidContainer.is(Items.GLASS_BOTTLE) && toFill.hasOneBottle()) {
                var filledItem = PotionUtils.setPotion(
                    new ItemStack(vanillaPotion.potionType().getItem()),
                    OptionConverters.toJava(toFill.nbt()).map(PotionUtils::getPotion).orElse(Potions.EMPTY)
                );
                var filledAmount = toFill.setAmount(GenericUnit.ONE_BOTTLE());
                return new TransferStack(filledAmount, filledItem);
            } else {
                // fail
                return new TransferStack(FluidAmountUtil.EMPTY(), fluidContainer);
            }
        }
        return FluidUtil.getFluidHandler(ItemHandlerHelper.copyStackWithSize(fluidContainer, 1))
            .map(h -> {
                int filledAmount = h.fill(ForgeConverter.toStack(toFill), IFluidHandler.FluidAction.EXECUTE);
                return new TransferStack(toFill.setAmount(GenericUnit.fromForge(filledAmount)), h.getContainer());
            })
            .orElse(new TransferStack(FluidAmountUtil.EMPTY(), fluidContainer));
    }

    @Override
    public @NotNull TransferStack drainItem(GenericAmount<FluidLike> toDrain, ItemStack fluidContainer, Player player, InteractionHand hand, boolean execute) {
        if (toDrain.content() instanceof VanillaPotion) {
            var content = getFluidContained(fluidContainer);
            if (toDrain.hasOneBottle() && content.hasOneBottle() && toDrain.contentEqual(content)) {
                var drainedItem = Items.GLASS_BOTTLE.getDefaultInstance();
                var drainedAmount = toDrain.setAmount(GenericUnit.ONE_BOTTLE());
                return new TransferStack(drainedAmount, drainedItem);
            } else {
                // Nothing drained
                return new TransferStack(FluidAmountUtil.EMPTY(), fluidContainer, false);
            }
        }
        return FluidUtil.getFluidHandler(ItemHandlerHelper.copyStackWithSize(fluidContainer, 1))
            .map(h -> {
                var drained = h.drain(ForgeConverter.toStack(toDrain), IFluidHandler.FluidAction.EXECUTE);
                return new TransferStack(ForgeConverter.toAmount(drained), h.getContainer());
            })
            .orElse(new TransferStack(FluidAmountUtil.EMPTY(), fluidContainer));
    }

    @Override
    public @Nullable SoundEvent getEmptySound(GenericAmount<FluidLike> fluid) {
        return FluidLike.asFluid(fluid.content(), Fluids.WATER).getFluidType().getSound(ForgeConverter.toStack(fluid), SoundActions.BUCKET_EMPTY);
    }

    @Override
    public @Nullable SoundEvent getFillSound(GenericAmount<FluidLike> fluid) {
        return FluidLike.asFluid(fluid.content(), Fluids.WATER).getFluidType().getSound(ForgeConverter.toStack(fluid), SoundActions.BUCKET_FILL);
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
    public LootItemFunctionType getTankLoot() {
        return FluidTank.TANK_LOOT_FUNCTION;
    }

    @Override
    public Map<Tier, Supplier<? extends BlockTank>> getTankBlockMap() {
        return Stream.concat(FluidTank.TANK_MAP.entrySet().stream(), Stream.of(Map.entry(Tier.CREATIVE, FluidTank.BLOCK_CREATIVE_TANK), Map.entry(Tier.VOID, FluidTank.BLOCK_VOID_TANK)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.getCraftingRemainingItem();
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
