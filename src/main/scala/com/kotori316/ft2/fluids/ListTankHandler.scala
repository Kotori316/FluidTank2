package com.kotori316.ft2.fluids

import cats.Align
import cats.data.Chain
import cats.syntax.eq._
import cats.syntax.foldable._
import cats.syntax.group._
import com.kotori316.ft2.fluids.Operations._
import com.kotori316.ft2.utils.FT2Utils
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler

class ListTankHandler(tankHandlers: Chain[TankHandler], limitOneFluid: Boolean) extends IFluidHandler {
  def this(t: Chain[TankHandler]) = {
    this(t, false)
  }

  def getTankList: Chain[Tank] = tankHandlers.map(_.getTank)

  override def getTanks: Int = 1

  override def getFluidInTank(tank: Int): FluidStack = {
    // Drain from bottom tank.
    val drainOps: Chain[TankOperation] = tankHandlers.map(t => t.getDrainOperation(t.getTank))
    val drained = this.action(opList(drainOps), FluidAmount.EMPTY.withAmount(getSumOfCapacity), IFluidHandler.FluidAction.SIMULATE)
    drained.toStack
  }

  def getSumOfCapacity: Long = tankHandlers.map(_.getTank.capacity).combineAll

  override def getTankCapacity(tank: Int): Int = FT2Utils.toInt(getSumOfCapacity)

  override def isFluidValid(tank: Int, stack: FluidStack): Boolean = true

  override final def fill(resource: FluidStack, action: IFluidHandler.FluidAction): Int = {
    FT2Utils.toInt(fill(FluidAmount.fromStack(resource), action).amount)
  }

  protected def action(op: ListTankOperation[Chain], resource: FluidAmount, action: IFluidHandler.FluidAction): FluidAmount = {
    val (log, left, newTanks) = op.run((), resource)
    val moved: FluidAmount = left.inverse() |+| resource // Here change order to avoid returning empty.
    if (action.execute())
      updateTanks(newTanks)
    outputLog(log, action)
    moved
  }

  def fill(resource: FluidAmount, action: IFluidHandler.FluidAction): FluidAmount = {
    if (limitOneFluid) {
      val fluidInTank = tankHandlers.headOption.map(_.getTank.fluidAmount).filter(_.nonEmpty)
      if (!fluidInTank.forall(_.fluidKey === resource.fluidKey)) {
        return FluidAmount.EMPTY
      }
    }
    if (resource.fluidKey.isGaseous) {
      // Fill from upper
      val fillOps: Chain[TankOperation] = tankHandlers.map(t => t.getFillOperation(t.getTank)).reverse
      this.action(opList(fillOps).map(_.reverse), resource, action)
    } else {
      // Fill from bottom tank
      val fillOps: Chain[TankOperation] = tankHandlers.map(t => t.getFillOperation(t.getTank))
      this.action(opList(fillOps), resource, action)
    }
  }

  def drain(toDrain: FluidAmount, action: IFluidHandler.FluidAction): FluidAmount = {
    if (toDrain.fluidKey.isGaseous || getTankList.lastOption.exists(_.fluidAmount.fluidKey.isGaseous)) {
      // Drain from bottom tank.
      val drainOps: Chain[TankOperation] = tankHandlers.map(t => t.getDrainOperation(t.getTank))
      this.action(opList(drainOps), toDrain, action)
    } else {
      // Drain from upper tank.
      val drainOps: Chain[TankOperation] = tankHandlers.map(t => t.getDrainOperation(t.getTank)).reverse
      this.action(opList(drainOps).map(_.reverse), toDrain, action)
    }
  }

  override final def drain(resource: FluidStack, action: IFluidHandler.FluidAction): FluidStack = drain(FluidAmount.fromStack(resource), action).toStack

  override final def drain(maxDrain: Int, action: IFluidHandler.FluidAction): FluidStack = drain(FluidAmount.EMPTY.withAmount(maxDrain), action).toStack

  protected def outputLog(logs: Chain[FluidTransferLog], action: IFluidHandler.FluidAction): Unit = {
    //    if (Utils.isInDev) {
    //      FluidTank.LOGGER.debug(ModObjects.MARKER_ListTankHandler, logs.mkString_(action.toString + " ", ", ", ""))
    //    }
  }

  protected def updateTanks(newTanks: Chain[Tank]): Unit = {
    Align[Chain].zipAll(tankHandlers, newTanks, EmptyTankHandler, Tank.EMPTY)
      .iterator.foreach { case (handler, tank) => handler.setTank(tank) }
  }

  override def toString: String = s"${getClass.getName}{$tankHandlers}"
}
