package com.kotori316.fluidtank.fabric.cat;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.fabric.FluidTank;
import com.kotori316.fluidtank.fabric.fluid.FabricConverter;
import com.kotori316.fluidtank.fluids.FluidLike;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("UnstableApiUsage")
public class ChestAsTankStorage implements SlottedStorage<FluidVariant> {

    private final SlottedStorage<ItemVariant> items;

    ChestAsTankStorage(SlottedStorage<ItemVariant> items) {
        this.items = items;
    }

    @Override
    public int getSlotCount() {
        return items.getSlotCount();
    }

    @Override
    public SingleSlotStorage<FluidVariant> getSlot(int slot) {
        var itemStorage = items.getSlot(slot);
        return getFluidVariantSingleSlotStorage(itemStorage);
    }

    private StorageView<FluidVariant> getSlotAsView(int slot) {
        return getSlot(slot);
    }

    @NotNull
    private static SingleSlotStorage<FluidVariant> getFluidVariantSingleSlotStorage(SingleSlotStorage<ItemVariant> itemStorage) {
        var count = itemStorage.getAmount();
        if (count != 1) return EmptySlottedFluidStorage.INSTANCE();
        var stack = itemStorage.getResource().toStack();
        var fluid = FluidStorage.ITEM.find(stack, ContainerItemContext.ofSingleSlot(itemStorage));
        if (fluid == null) return EmptySlottedFluidStorage.INSTANCE();

        if (fluid instanceof SingleSlotStorage<FluidVariant> s) return s;
        else return new SingleSlotStorageWrapper(fluid);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        long toFill = maxAmount;
        for (int i = 0; i < getSlotCount(); i++) {
            var storage = getSlot(i);
            if (!storage.supportsInsertion()) continue;

            long filled = storage.insert(resource, toFill, transaction);
            toFill -= filled;
            if (toFill <= 0) {
                return maxAmount;
            }
        }
        return maxAmount - toFill;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        long toDrain = maxAmount;
        for (int i = 0; i < getSlotCount(); i++) {
            var storage = getSlot(i);
            if (!storage.supportsExtraction()) continue;

            long drained = storage.extract(resource, toDrain, transaction);
            toDrain -= drained;
            if (toDrain <= 0) {
                return maxAmount;
            }
        }
        return maxAmount - toDrain;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return IntStream.range(0, getSlotCount())
            .mapToObj(this::getSlotAsView)
            .iterator();
    }

    public static void register() {
        FluidStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, context) -> {
            var facing = state.getValue(BlockStateProperties.FACING);
            var itemStorage = ItemStorage.SIDED.find(world, pos.relative(facing), facing.getOpposite());
            if (itemStorage instanceof SlottedStorage<ItemVariant> s) return new ChestAsTankStorage(s);
            else return null;
        }, FluidTank.BLOCK_CAT);
    }

    public static List<GenericAmount<FluidLike>> getCATFluids(Level level, BlockPos pos) {
        if (FluidStorage.SIDED.find(level, pos, null) instanceof ChestAsTankStorage storage) {
            return IntStream.range(0, storage.getSlotCount())
                .mapToObj(storage::getSlot)
                .filter(Predicate.not(StorageView::isResourceBlank))
                .collect(Collectors.groupingBy(StorageView::getResource, Collectors.summingLong(StorageView::getAmount)))
                .entrySet()
                .stream()
                .map(e -> FabricConverter.fromVariant(e.getKey(), e.getValue()))
                .toList();
        } else {
            return List.of();
        }
    }
}
