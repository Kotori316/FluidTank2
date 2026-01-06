package com.kotori316.fluidtank.fabric.tank

import com.kotori316.fluidtank.fabric.FluidTank
import com.kotori316.fluidtank.fabric.fluid.FabricConverter
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, FluidConnection, VanillaFluid, VanillaPotion}
import com.kotori316.fluidtank.tank.TileTank
import net.fabricmc.fabric.api.transfer.v1.fluid.{FluidStorage, FluidVariant}
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.world.level.material.Fluids

//noinspection UnstableApiUsage
class ConnectionStorage(private val connection: FluidConnection) extends SnapshotParticipant[FluidAmount] with SingleSlotStorage[FluidVariant] {
  override def createSnapshot(): FluidAmount = connection.getContent.getOrElse(FluidAmountUtil.EMPTY)

  override def readSnapshot(snapshot: FluidAmount): Unit = connection.getHandler.set(snapshot)

  override def isResourceBlank: Boolean = {
    connection.getContent.forall { f =>
      f.content match {
        case _: VanillaFluid => f.isEmpty
        case _: VanillaPotion => true
      }
    }
  }

  override def getResource: FluidVariant = {
    val fluid = connection.getContent.getOrElse(FluidAmountUtil.EMPTY)
    FabricConverter.toVariant(fluid, Fluids.EMPTY)
  }

  override def getAmount: Long = {
    connection.getContent.map { f =>
      f.content match {
        case _: VanillaFluid => connection.amount.asFabric
        case _: VanillaPotion => 0
      }
    }.getOrElse(0)
  }

  override def getCapacity: Long = connection.capacity.asFabric

  /**
   * @return the inserted amount
   */
  override def insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = {
    val toFill = FabricConverter.fromVariant(resource, maxAmount)

    updateSnapshots(transaction)

    val filled = this.connection.getHandler.fill(toFill, execute = true)
    filled.amount.asFabric
  }

  /**
   * @return the extracted amount
   */
  override def extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = {
    val toDrain = FabricConverter.fromVariant(resource, maxAmount)

    updateSnapshots(transaction)

    val drained = this.connection.getHandler.drain(toDrain, execute = true)
    drained.amount.asFabric
  }

  override def onFinalCommit(): Unit = {
    super.onFinalCommit()
    connection.getTiles.foreach(_.setChanged())
  }
}

//noinspection UnstableApiUsage
object ConnectionStorage {
  def register(): Unit = {
    FluidStorage.SIDED.registerForBlockEntities((entity, _) =>
      entity match {
        case tank: TileTank => new ConnectionStorage(tank.getConnection)
        case _ => null
      },
      FluidTank.TILE_TANK_TYPE, FluidTank.TILE_CREATIVE_TANK_TYPE, FluidTank.TILE_VOID_TANK_TYPE)
  }
}
