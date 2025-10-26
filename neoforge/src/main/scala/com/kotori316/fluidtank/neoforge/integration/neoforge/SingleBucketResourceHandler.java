package com.kotori316.fluidtank.neoforge.integration.neoforge;

import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.BucketResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

public final class SingleBucketResourceHandler implements ResourceHandler<FluidResource> {
    private final ItemAccess itemAccess;
    private final BucketResourceHandler internal;

    public SingleBucketResourceHandler(ItemAccess itemAccess) {
        this.itemAccess = itemAccess;
        this.internal = new BucketResourceHandler(itemAccess);
    }

    @Override
    public int size() {
        return this.internal.size();
    }

    @Override
    public FluidResource getResource(int index) {
        return this.internal.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return this.internal.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return this.internal.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return this.internal.isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (this.itemAccess.getAmount() == 0) {
            // Empty?
            return 0;
        }
        var resourceInBucket = this.getResource(index);
        var amountInBucket = this.getAmountAsInt(index);
        if ((amountInBucket == 0 || resourceInBucket.equals(resource)) && this.isValid(index, resource)) {
            // Allowed to insert to this bucket
            var toInsert = Math.min(this.getCapacityAsInt(index, resource) - amountInBucket, amount);
            if (toInsert > 0) {
                var newBucket = this.newBucket(resource, toInsert + amountInBucket);
                if (!newBucket.isEmpty() && this.itemAccess.exchange(newBucket, 1, transaction) == 1) {
                    return toInsert;
                }
            }
        }
        return 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (this.itemAccess.getAmount() != 1) {
            // Avoid extracting from stacked items
            return 0;
        }
        var resourceInBucket = this.getResource(index);
        var amountInBucket = this.getAmountAsInt(index);
        if (resourceInBucket.equals(resource) && amountInBucket > 0) {
            var toExtract = Math.min(amount, amountInBucket);
            var newBucket = this.newBucket(resource, amountInBucket - toExtract);
            if (!newBucket.isEmpty() && this.itemAccess.exchange(newBucket, 1, transaction) == 1) {
                return toExtract;
            }
        }

        return 0;
    }

    /**
     * Copied from {@link BucketResourceHandler#update(ItemResource, int, FluidResource, int)}
     */
    @SuppressWarnings("JavadocReference") // Just reference
    public ItemResource newBucket(FluidResource newResource, int newAmount) {
        if (newAmount == 0) {
            return ItemResource.of(Items.BUCKET);
        } else if (newAmount != FluidType.BUCKET_VOLUME) {
            return ItemResource.EMPTY;
        } else {
            var newStack = newResource.toStack(newAmount);
            return ItemResource.of(newStack.getFluidType().getBucket(newStack));
        }
    }
}
