package com.kotori316.fluidtank.forge.cat;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.contents.GenericUnit;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.FluidLikeKey;
import com.kotori316.fluidtank.forge.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Option;
import scala.math.BigInt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EntityChestAsTank extends BlockEntity {
    public EntityChestAsTank(BlockPos pos, BlockState state) {
        super(FluidTank.TILE_CAT.get(), pos, state);
    }

    @NotNull
    private LazyOptional<FluidHandlerProxy> proxy = LazyOptional.empty();

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (ForgeCapabilities.FLUID_HANDLER == cap) {
            if (!this.proxy.isPresent()) {
                this.proxy = createProxy();
            }
            return this.proxy.cast();
        }
        return super.getCapability(cap, side);
    }

    private LazyOptional<FluidHandlerProxy> createProxy() {
        var facing = getBlockState().getValue(BlockStateProperties.FACING);
        var pos = getBlockPos().relative(facing);
        var handler = Optional.ofNullable(getLevel())
            .map(l -> l.getBlockEntity(pos))
            .flatMap(b -> {
                var o = b.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite());
                // No problem even if o is empty
                o.addListener(h -> this.markInvalid());
                return o.filter(IItemHandlerModifiable.class::isInstance).map(IItemHandlerModifiable.class::cast);
            })
            .or(() -> Optional.ofNullable(HopperBlockEntity.getContainerAt(getLevel(), pos)).map(InvWrapper::new));

        return handler.map(i -> LazyOptional.of(() -> new FluidHandlerProxy(i)))
            .orElse(LazyOptional.empty());
    }

    private void markInvalid() {
        this.proxy.invalidate();
        this.proxy = LazyOptional.empty();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        markInvalid();
    }

    public Optional<List<GenericAmount<FluidLike>>> getFluids() {
        return proxy.map(FluidHandlerProxy::fluids)
            .map(m ->
                m.entrySet().stream().map(e -> e.getKey().toAmount(e.getValue())).toList()
            );
    }

    static class FluidHandlerProxy implements IFluidHandler {

        private final IItemHandlerModifiable inventory;

        FluidHandlerProxy(IItemHandlerModifiable inventory) {
            this.inventory = inventory;
        }

        LazyOptional<IFluidHandlerItem> getHandler(int slot) {
            var stack = this.inventory.getStackInSlot(slot);
            return FluidUtil.getFluidHandler(stack);
        }

        @Override
        public int getTanks() {
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
        public int fill(FluidStack resource, FluidAction fluidAction) {
            if (resource == null || resource.isEmpty()) return 0;
            var rest = resource.copy();

            for (int i = 0; i < getTanks(); i++) {
                var stack = this.inventory.getStackInSlot(i);
                if (stack.isEmpty() || stack.getCount() > 1) continue; // Don't fill to stacked item
                var handlerO = FluidUtil.getFluidHandler(stack);
                if (!handlerO.isPresent()) continue; // Not a fluid container

                var handler = handlerO.orElseThrow(AssertionError::new);
                var filled = handler.fill(rest, fluidAction);
                rest.shrink(filled);
                if (fluidAction.execute()) this.inventory.setStackInSlot(i, handler.getContainer());

                if (rest.isEmpty()) {
                    // Filled all resources, early return
                    return resource.getAmount();
                }
            }
            return resource.getAmount() - rest.getAmount();
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction fluidAction) {
            if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            var rest = resource.copy();

            for (int i = 0; i < getTanks(); i++) {
                var stack = this.inventory.getStackInSlot(i);
                if (stack.isEmpty() || stack.getCount() > 1) continue; // Don't drain from stacked item
                var handlerO = FluidUtil.getFluidHandler(stack);
                if (!handlerO.isPresent()) continue; // Not a fluid container

                var handler = handlerO.orElseThrow(AssertionError::new);
                var drained = handler.drain(rest, fluidAction);
                rest.shrink(drained.getAmount());
                if (fluidAction.execute()) this.inventory.setStackInSlot(i, handler.getContainer());

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
                .collect(Collectors.groupingBy(f -> FluidLikeKey.apply(FluidLike.of(f.getFluid()), Option.apply(f.getTag())),
                    Collectors.reducing(BigInt.apply(0), f -> GenericUnit.asBigIntFromForge(f.getAmount()), BigInt::$plus)));
        }
    }
}
