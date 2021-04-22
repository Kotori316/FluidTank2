package com.kotori316.ft2.fluids

import cats.data.{Chain, ReaderWriterStateT}
import cats.syntax.eq._
import com.kotori316.ft2.fluids.Operations._
class CreativeTankHandler extends TankHandler {
  setTank(Tank(FluidAmount.EMPTY, Long.MaxValue))

  override def getFillOperation(tank: Tank): TankOperation = {
    if (tank.fluidAmount.isEmpty) {
      // Fill tank.
      super.getFillOperation(tank).map(t => t.copy(t.fluidAmount.withAmount(t.capacity)))
    } else {
      ReaderWriterStateT.applyS { s =>
        if (tank.fluidAmount.fluidKey === s.fluidKey) {
          (Chain(FluidTransferLog.FillAll(s, tank)), FluidAmount.EMPTY, tank)
        } else {
          (Chain(FluidTransferLog.FillFailed(s, tank)), s, tank)
        }
      }
    }
  }

  override def getDrainOperation(tank: Tank): TankOperation =
    if (tank.fluidAmount.isEmpty) {
      super.getDrainOperation(tank).map(_ => tank)
    } else {
      ReaderWriterStateT.applyS { s =>
        if ((tank.fluidAmount.fluidKey === s.fluidKey) || s.fluidKey.isEmpty) {
          (Chain(FluidTransferLog.DrainFluid(s, s, tank, tank)), tank.fluidAmount.withAmount(0L), tank)
        } else {
          (Chain(FluidTransferLog.DrainFailed(s, tank)), s, tank)
        }
      }
    }
}
