package com.kotori316.fluidtank.neoforge.cat;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.FluidLikeKey;
import com.kotori316.fluidtank.neoforge.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import scala.math.BigInt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EntityChestAsTank extends BlockEntity {
    public EntityChestAsTank(BlockPos pos, BlockState state) {
        super(FluidTank.TILE_CAT.get(), pos, state);
    }

    @Nullable
    private FluidHandlerProxy proxy = null;

    @Nullable
    public IFluidHandler getCapability(Direction ignored) {
        if (!(getLevel() instanceof ServerLevel)) return null;
        if (this.proxy == null) {
            this.proxy = createProxy();
        }
        return proxy;
    }

    @Nullable
    private FluidHandlerProxy createProxy() {
        var facing = getBlockState().getValue(BlockStateProperties.FACING);
        var pos = getBlockPos().relative(facing);
        var cache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, (ServerLevel) Objects.requireNonNull(getLevel()), pos, facing.getOpposite(), () -> true, () -> this.proxy = null);
        if (cache.getCapability() instanceof IItemHandlerModifiable) {
            return new FluidHandlerProxy(cache);
        }
        return null;
    }

    public Optional<List<GenericAmount<FluidLike>>> getFluids() {
        return Optional.ofNullable(getCapability(null))
            .filter(FluidHandlerProxy.class::isInstance)
            .map(FluidHandlerProxy.class::cast)
            .map(FluidHandlerProxy::fluids)
            .map(m ->
                m.entrySet().stream().map(e -> e.getKey().toAmount(e.getValue())).toList()
            );
    }

    static class FluidHandlerProxy implements IFluidHandler {

        private final Supplier<IItemHandler> cache;

        FluidHandlerProxy(BlockCapabilityCache<IItemHandler, ?> cache) {
            this.cache = cache::getCapability;
        }

        @VisibleForTesting
        FluidHandlerProxy(IItemHandlerModifiable handler) {
            this.cache = () -> handler;
        }

        Optional<IFluidHandlerItem> getHandler(int slot) {
            return Optional.ofNullable(cache.get())
                .map(i -> i.getStackInSlot(slot))
                .flatMap(FluidUtil::getFluidHandler);
        }

        @Override
        public int getTanks() {
            var inventory = cache.get();
            if (inventory == null) return 0;
            return inventory.getSlots();
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int i) {
            return getHandler(i).map(h -> h.getFluidInTank(0)).orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int i) {
            return getHandler(i).map(h -> h.getTankCapacity(0)).orElse(0);
        }

        @Override
        public boolean isFluidValid(int i, @NotNull FluidStack fluidStack) {
            return getHandler(i).map(h -> h.isFluidValid(0, fluidStack)).orElse(false);
        }

        @Override
        public int fill(@Nullable FluidStack resource, FluidAction fluidAction) {
            if (resource == null || resource.isEmpty()) return 0;
            var t = cache.get();
            if (!(t instanceof IItemHandlerModifiable inventory)) return 0;
            var rest = resource.copy();

            for (int i = 0; i < getTanks(); i++) {
                var stack = inventory.getStackInSlot(i);
                if (stack.isEmpty() || stack.getCount() > 1) continue; // Don't fill to stacked item
                var handlerO = FluidUtil.getFluidHandler(stack);
                if (handlerO.isEmpty()) continue; // Not a fluid container

                var handler = handlerO.orElseThrow(AssertionError::new);
                var filled = handler.fill(rest, fluidAction);
                rest.shrink(filled);
                if (fluidAction.execute()) inventory.setStackInSlot(i, handler.getContainer());

                if (rest.isEmpty()) {
                    // Filled all resources, early return
                    return resource.getAmount();
                }
            }
            return resource.getAmount() - rest.getAmount();
        }

        @Override
        public @NotNull FluidStack drain(@Nullable FluidStack resource, FluidAction fluidAction) {
            if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            var t = cache.get();
            if (!(t instanceof IItemHandlerModifiable inventory)) return FluidStack.EMPTY;
            var rest = resource.copy();

            for (int i = 0; i < getTanks(); i++) {
                var stack = inventory.getStackInSlot(i);
                if (stack.isEmpty() || stack.getCount() > 1) continue; // Don't drain from stacked item
                var handlerO = FluidUtil.getFluidHandler(stack);
                if (handlerO.isEmpty()) continue; // Not a fluid container

                var handler = handlerO.orElseThrow(AssertionError::new);
                var drained = handler.drain(rest, fluidAction);
                rest.shrink(drained.getAmount());
                if (fluidAction.execute()) inventory.setStackInSlot(i, handler.getContainer());

                if (rest.isEmpty()) {
                    // Drained all resources, early return
                    return resource;
                }
            }
            rest.setAmount(resource.getAmount() - rest.getAmount());
            return rest;
        }

        @Override
        public @NotNull FluidStack drain(int amount, FluidAction fluidAction) {
            if (amount <= 0) return FluidStack.EMPTY;

            var toDrain = IntStream.range(0, getTanks())
                .mapToObj(this::getFluidInTank)
                .filter(Predicate.not(FluidStack::isEmpty))
                .findFirst()
                .map(FluidStack::copy);

            return toDrain.map(f -> {
                f.setAmount(amount);
                return this.drain(f, fluidAction);
            }).orElse(FluidStack.EMPTY);
        }

        Map<FluidLikeKey, BigInt> fluids() {
            return IntStream.range(0, getTanks())
                .mapToObj(this::getFluidInTank)
                .filter(Predicate.not(FluidStack::isEmpty))
                .collect(Collectors.groupingBy(f -> FluidLikeKey.apply(FluidLike.of(f.getFluid()), f.getComponentsPatch()),
                    Collectors.reducing(BigInt.apply(0), f -> GenericUnit.asBigIntFromForge(f.getAmount()), BigInt::$plus)));
        }
    }

    @VisibleForTesting
    public static IFluidHandler getProxy(IItemHandlerModifiable handler) {
        return new FluidHandlerProxy(handler);
    }
}
