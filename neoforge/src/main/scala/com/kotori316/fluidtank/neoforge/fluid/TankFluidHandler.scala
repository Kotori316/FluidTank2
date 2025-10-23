package com.kotori316.fluidtank.neoforge.fluid

import com.kotori316.fluidtank.contents.Operations.TankOperation
import com.kotori316.fluidtank.contents.{DefaultTransferEnv, Tank}
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidLike}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.{SnapshotJournal, TransactionContext}

abstract class TankFluidHandler extends SnapshotJournal[Tank[FluidLike]] with ResourceHandler[FluidResource] {
  def getTank: Tank[FluidLike]

  def saveTank(newTank: Tank[FluidLike]): Unit

  override final def createSnapshot(): Tank[FluidLike] = getTank

  override final def revertToSnapshot(snapshot: Tank[FluidLike]): Unit = saveTank(snapshot)

  final def getCapability(ignored: Void): TankFluidHandler = this

  override def size(): Int = 1

  override def getCapacityAsLong(index: Int, resource: FluidResource): Long = getTank.capacity.asNeoForge

  override def getResource(index: Int): FluidResource = getTank.content.asVariant

  override def getAmountAsLong(index: Int): Long = getTank.content.amount.asNeoForge

  override def isValid(index: Int, resource: FluidResource): Boolean = true

  override def insert(index: Int, resource: FluidResource, amount: Int, transaction: TransactionContext): Int = {
    opInternal(getTank.fillOp, NeoForgeConverter.toAmount(resource, amount), transaction)
  }

  override def extract(index: Int, resource: FluidResource, amount: Int, transaction: TransactionContext): Int = {
    if (getTank.isEmpty) {
      0
    } else {
      opInternal(getTank.drainOp, NeoForgeConverter.toAmount(resource, amount), transaction)
    }
  }

  private def opInternal(op: TankOperation[FluidLike], fluid: FluidAmount, transaction: TransactionContext): Int = {
    val (_, rest, newTank) = op.run(DefaultTransferEnv, fluid)
    updateSnapshots(transaction)
    saveTank(newTank)
    fluid.amount.asForge - rest.amount.asForge
  }
}
