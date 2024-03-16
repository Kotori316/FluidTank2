package com.kotori316.fluidtank.fabric.fluid

import com.kotori316.fluidtank.contents.{DefaultTransferEnv, Operations, Tank}
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidLike}
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.{Transaction, TransactionContext}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.material.Fluids
import org.jetbrains.annotations.VisibleForTesting

import scala.util.Using

//noinspection UnstableApiUsage
abstract class FabricTankStorage(protected final val context: ContainerItemContext) extends SingleSlotStorage[FluidVariant] {

  def getTank: Tank[FluidLike]

  def saveTank(newTank: Tank[FluidLike]): ItemVariant

  override def insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = {
    val tank = getTank
    move(tank, tank.fillOp, FabricConverter.fromVariant(resource, maxAmount), transaction)
  }

  override def extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = {
    val tank = getTank
    move(tank, tank.drainOp, FabricConverter.fromVariant(resource, maxAmount), transaction)
  }

  private def move(tank: Tank[FluidLike], op: Operations.TankOperation[FluidLike], fluid: FluidAmount, transaction: TransactionContext): Long = {
    val (_, rest, result) = op.run(DefaultTransferEnv, fluid)
    if (tank != result && this.context.exchange(saveTank(result), 1, transaction) == 1) {
      val inserted = fluid - rest
      inserted.amount.asFabric
    } else {
      // Not inserted
      0
    }
  }

  override def isResourceBlank: Boolean = getTank.content.isContentEmpty

  override def getResource: FluidVariant = FabricConverter.toVariant(getTank.content, Fluids.EMPTY)

  override def getAmount: Long = getTank.content.amount.asFabric

  override def getCapacity: Long = getTank.capacity.asFabric

  @VisibleForTesting
  def fill(amount: FluidAmount): Unit = {
    Using(Transaction.openOuter()) { transaction =>
      val tank = getTank
      move(tank, tank.fillOp, amount, transaction)
      transaction.commit()
    }
  }

  @VisibleForTesting
  def getStack: ItemStack = {
    context.getItemVariant.toStack(context.getAmount.toInt)
  }
}
