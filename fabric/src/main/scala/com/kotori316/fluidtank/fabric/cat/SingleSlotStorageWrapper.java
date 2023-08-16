package com.kotori316.fluidtank.fabric.cat;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

@SuppressWarnings("UnstableApiUsage")
class SingleSlotStorageWrapper implements SingleSlotStorage<FluidVariant> {
    private final Storage<FluidVariant> storage;

    SingleSlotStorageWrapper(Storage<FluidVariant> storage) {
        this.storage = storage;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return this.storage.insert(resource, maxAmount, transaction);
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        return this.storage.extract(resource, maxAmount, transaction);
    }

    @Override
    public boolean isResourceBlank() {
        for (StorageView<FluidVariant> view : this.storage) {
            if (!view.isResourceBlank()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public FluidVariant getResource() {
        for (StorageView<FluidVariant> view : this.storage) {
            if (!view.isResourceBlank()) {
                return view.getResource();
            }
        }
        return FluidVariant.blank();
    }

    @Override
    public long getAmount() {
        for (StorageView<FluidVariant> view : this.storage) {
            if (!view.isResourceBlank()) {
                return view.getAmount();
            }
        }
        return 0;
    }

    @Override
    public long getCapacity() {
        for (StorageView<FluidVariant> view : this.storage) {
            if (!view.isResourceBlank()) {
                return view.getCapacity();
            }
        }
        return 0;
    }
}
