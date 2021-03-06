package com.kotori316.ft2.fluids

import cats.data.Chain
import cats.syntax.group._
import com.kotori316.ft2.fluids.Operations._
import com.kotori316.ft2.utils.FT2Utils
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler

class TankHandler extends IFluidHandler {

  private[this] final var tank: Tank = Tank.EMPTY

  def setTank(newTank: Tank): Unit = {
    this.tank = newTank
    onContentsChanged()
  }

  def initCapacity(capacity: Long): Unit = {
    val newTank = getTank.copy(capacity = capacity)
    // Not to use setter to avoid NPE of connection.
    this.tank = newTank
  }

  def getTank: Tank = tank

  def onContentsChanged(): Unit = ()

  override def getTanks: Int = 1

  override def getFluidInTank(tank: Int): FluidStack =
    getDrainOperation(this.tank).runS((), FluidAmount.EMPTY.withAmount(this.tank.capacity)).toStack

  override def getTankCapacity(tank: Int): Int = FT2Utils.toInt(this.tank.capacity)

  // Discards current state.
  override def isFluidValid(tank: Int, stack: FluidStack): Boolean = true

  def getFillOperation(tank: Tank): TankOperation = fillOp(tank)

  def getDrainOperation(tank: Tank): TankOperation = drainOp(tank)

  override final def fill(resource: FluidStack, action: IFluidHandler.FluidAction): Int = {
    FT2Utils.toInt(fill(FluidAmount.fromStack(resource), action).amount)
  }

  private final def action(op: TankOperation, resource: FluidAmount, action: IFluidHandler.FluidAction): FluidAmount = {
    val (log, left, newTank) = op.run((), resource)
    val moved: FluidAmount = left.inverse() |+| resource // Here change order to avoid returning empty.
    if (action.execute())
      setTank(newTank)
    outputLog(log, action)
    moved
  }

  final def fill(resource: FluidAmount, action: IFluidHandler.FluidAction): FluidAmount = {
    this.action(getFillOperation(this.tank), resource, action)
  }

  final def drain(toDrain: FluidAmount, action: IFluidHandler.FluidAction): FluidAmount = {
    this.action(getDrainOperation(this.tank), toDrain, action)
  }

  override final def drain(resource: FluidStack, action: IFluidHandler.FluidAction): FluidStack = drain(FluidAmount.fromStack(resource), action).toStack

  override final def drain(maxDrain: Int, action: IFluidHandler.FluidAction): FluidStack = drain(FluidAmount.EMPTY.withAmount(maxDrain), action).toStack

  protected def outputLog(logs: Chain[FluidTransferLog], action: IFluidHandler.FluidAction): Unit = {
    //    if (Utils.isInDev) {
    //      FluidTank.LOGGER.debug(ModObjects.MARKER_TankHandler, logs.mkString_(action.toString + " ", ", ", ""))
    //    }
  }

  override def toString: String = s"${getClass.getName} with $tank"
}

object TankHandler {
  def apply(capacity: Long): TankHandler = {
    val h = new TankHandler
    h.setTank(h.getTank.copy(capacity = capacity))
    h
  }

  def apply(tank: Tank): TankHandler = {
    val h = new TankHandler()
    h setTank tank
    h
  }
}
