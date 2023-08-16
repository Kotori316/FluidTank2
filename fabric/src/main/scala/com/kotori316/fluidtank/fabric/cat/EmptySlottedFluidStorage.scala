package com.kotori316.fluidtank.fabric.cat

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

//noinspection UnstableApiUsage
object EmptySlottedFluidStorage extends SingleSlotStorage[FluidVariant] {
  val INSTANCE: SingleSlotStorage[FluidVariant] = this

  override def insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0

  override def extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0

  override def isResourceBlank: Boolean = true

  override def getResource: FluidVariant = FluidVariant.blank()

  override def getAmount: Long = 0

  override def getCapacity: Long = 0

  override def supportsInsertion(): Boolean = false

  override def supportsExtraction(): Boolean = false

  override def nonEmptyIterator(): java.util.Iterator[StorageView[FluidVariant]] = java.util.Collections.emptyIterator()

  override def nonEmptyViews(): java.lang.Iterable[StorageView[FluidVariant]] = java.util.Collections.emptyList()
}
