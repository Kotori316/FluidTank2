package com.kotori316.fluidtank.neoforge.tank

import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, FluidConnection}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.{SnapshotJournal, TransactionContext}

class ConnectionHandler(connection: FluidConnection) extends SnapshotJournal[FluidAmount] with ResourceHandler[FluidResource] {
  override def createSnapshot(): FluidAmount = connection.getContent.getOrElse(FluidAmountUtil.EMPTY)

  override def revertToSnapshot(snapshot: FluidAmount): Unit = connection.getHandler.set(snapshot)

  override def size: Int = 1

  override def getResource(i: Int): FluidResource = connection.getContent.map(_.asVariant).getOrElse(FluidResource.EMPTY)

  override def getAmountAsLong(i: Int): Long = connection.getContent.map(_.amount.asNeoForge).getOrElse(0L)

  override def getCapacityAsLong(index: Int, resource: FluidResource): Long = connection.capacity.asNeoForge

  override def isValid(index: Int, resource: FluidResource): Boolean = true

  override def insert(index: Int, resource: FluidResource, amount: Int, transaction: TransactionContext): Int = {
    val toFill = NeoForgeConverter.toAmount(resource, amount)

    updateSnapshots(transaction)

    val filled = this.connection.getHandler.fill(toFill, execute = true)
    filled.amount.asForge
  }

  override def extract(index: Int, resource: FluidResource, amount: Int, transaction: TransactionContext): Int = {
    val toDrain = NeoForgeConverter.toAmount(resource, amount)

    updateSnapshots(transaction)

    val drained = this.connection.getHandler.drain(toDrain, execute = true)
    drained.amount.asForge
  }
}
