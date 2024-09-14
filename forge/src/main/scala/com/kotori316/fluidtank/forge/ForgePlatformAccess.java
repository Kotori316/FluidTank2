package com.kotori316.fluidtank.forge;

import com.kotori316.fluidtank.PlatformAccess;
import com.kotori316.fluidtank.cat.BlockChestAsTank;
import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.VanillaFluid;
import com.kotori316.fluidtank.fluids.VanillaPotion;
import com.kotori316.fluidtank.forge.cat.EntityChestAsTank;
import com.kotori316.fluidtank.forge.fluid.ForgeConverter;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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
        var potionHandler = PotionFluidHandler.apply(stack);
        if (potionHandler.isValidHandler()) {
            return potionHandler.getContent();
        }
        var fromHandler = FluidUtil.getFluidContained(stack)
            .map(ForgeConverter::toAmount)
            .orElse(FluidAmountUtil.EMPTY());
        if (fromHandler.nonEmpty()) {
            return fromHandler;
        }
        if (stack.getItem() instanceof BucketItem bucketItem) {
            var fluid = getBucketContent(bucketItem);
            if (Fluids.EMPTY.equals(fluid)) {
                return FluidAmountUtil.EMPTY();
            }
            return FluidAmountUtil.from(fluid, GenericUnit.ONE_BUCKET());
        }
        return FluidAmountUtil.EMPTY();
    }

    @Override
    public boolean isFluidContainer(ItemStack stack) {
        return FluidUtil.getFluidHandler(stack).isPresent() ||
            PotionFluidHandler.apply(stack).isValidHandler() ||
            stack.getItem() instanceof BucketItem;
    }

    @Override
    public Component getDisplayName(GenericAmount<FluidLike> amount) {
        if (amount.content() instanceof VanillaFluid) {
            return ForgeConverter.toStack(amount).getDisplayName();
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
        return FluidUtil.getFluidHandler(ItemHandlerHelper.copyStackWithSize(fluidContainer, 1))
            .map(h -> {
                int filledAmount = h.fill(ForgeConverter.toStack(toFill), IFluidHandler.FluidAction.EXECUTE);
                return new TransferStack(toFill.setAmount(GenericUnit.fromForge(filledAmount)), h.getContainer());
            })
            .orElseGet(() -> {
                if (toFill.content() instanceof VanillaFluid vanillaFluid && fluidContainer.getItem() == Items.BUCKET && toFill.hasOneBucket()) {
                    // Just for vanilla bucket
                    var filledItem = vanillaFluid.fluid().getBucket().getDefaultInstance();
                    var filledAmount = toFill.setAmount(GenericUnit.ONE_BUCKET());
                    return new TransferStack(filledAmount, filledItem);
                }
                return new TransferStack(FluidAmountUtil.EMPTY(), fluidContainer);
            });
    }

    @Override
    public @NotNull TransferStack drainItem(GenericAmount<FluidLike> toDrain, ItemStack fluidContainer, Player player, InteractionHand hand, boolean execute) {
        if (toDrain.content() instanceof VanillaPotion v) {
            var potionHandler = PotionFluidHandler.apply(fluidContainer);
            return potionHandler.drain(toDrain, v);
        }
        return FluidUtil.getFluidHandler(ItemHandlerHelper.copyStackWithSize(fluidContainer, 1))
            .map(h -> {
                var drained = h.drain(ForgeConverter.toStack(toDrain), IFluidHandler.FluidAction.EXECUTE);
                return new TransferStack(ForgeConverter.toAmount(drained), h.getContainer());
            })
            .orElseGet(() -> {
                var content = getFluidContained(fluidContainer);
                if (fluidContainer.getItem() instanceof BucketItem &&
                    toDrain.content() instanceof VanillaFluid &&
                    toDrain.hasOneBucket() && content.hasOneBucket() && toDrain.contentEqual(content)) {
                    var drainedItem = Items.BUCKET.getDefaultInstance();
                    var drainedAmount = toDrain.setAmount(GenericUnit.ONE_BUCKET());
                    return new TransferStack(drainedAmount, drainedItem);
                }
                return new TransferStack(FluidAmountUtil.EMPTY(), fluidContainer);
            });
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
