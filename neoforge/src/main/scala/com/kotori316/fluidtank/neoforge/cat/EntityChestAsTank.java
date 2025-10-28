package com.kotori316.fluidtank.neoforge.cat;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.fluids.FluidLikeKey;
import com.kotori316.fluidtank.neoforge.FluidTank;
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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
    public ResourceHandler<FluidResource> getCapability(Direction ignored) {
        if (!(getLevel() instanceof ServerLevel)) return null;
        if (this.proxy == null) {
            this.proxy = createProxy();
        }
        return proxy;
    }

    private FluidHandlerProxy createProxy() {
        var facing = getBlockState().getValue(BlockStateProperties.FACING);
        var pos = getBlockPos().relative(facing);
        var cache = BlockCapabilityCache.create(Capabilities.Item.BLOCK, (ServerLevel) Objects.requireNonNull(getLevel()), pos, facing.getOpposite(), () -> true, () -> this.proxy = null);
        return new FluidHandlerProxy(cache);
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

    static class FluidHandlerProxy implements ResourceHandler<FluidResource> {

        private final Supplier<ResourceHandler<ItemResource>> cache;

        FluidHandlerProxy(BlockCapabilityCache<ResourceHandler<ItemResource>, ?> cache) {
            this.cache = cache::getCapability;
        }

        @VisibleForTesting
        FluidHandlerProxy(ResourceHandler<ItemResource> handler) {
            this.cache = () -> handler;
        }

        @Override
        public int size() {
            var inventory = cache.get();
            if (inventory == null) return 0;
            return inventory.size();
        }

        @Override
        public FluidResource getResource(int index) {
            return getHandler(index).map(h -> h.getResource(0)).orElse(FluidResource.EMPTY);
        }

        @Override
        public long getAmountAsLong(int index) {
            return getHandler(index).map(h -> h.getAmountAsLong(0)).orElse(0L);
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return getHandler(index).map(h -> h.getCapacityAsLong(0, resource)).orElse(0L);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return getHandler(index).filter(h -> h.isValid(0, resource)).isPresent();
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return getHandler(index)
                .map(h -> h.insert(resource, amount, transaction))
                .orElse(0);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return getHandler(index)
                .map(h -> h.extract(resource, amount, transaction))
                .orElse(0);
        }

        Optional<ResourceHandler<FluidResource>> getHandler(int slot) {
            return Optional.ofNullable(cache.get())
                .map(i -> ItemAccess.forHandlerIndexStrict(i, slot))
                .map(i -> i.getCapability(Capabilities.Fluid.ITEM));
        }

        Map<FluidLikeKey, BigInt> fluids() {
            return IntStream.range(0, size())
                .mapToObj(i -> NeoForgeConverter.toAmount(this.getResource(i), this.getAmountAsLong(i)))
                .filter(Predicate.not(GenericAmount::isEmpty))
                .collect(Collectors.groupingBy(f -> FluidLikeKey.apply(f.content(), f.componentPatch()),
                    Collectors.reducing(BigInt.apply(0), GenericAmount::amount, BigInt::$plus)));
        }
    }

    @VisibleForTesting
    public static ResourceHandler<FluidResource> getProxy(ResourceHandler<ItemResource> handler) {
        return new FluidHandlerProxy(handler);
    }
}
