package com.kotori316.ft2.fluids

import cats.data.{Chain, ReaderWriterStateT}
import cats.syntax.eq._
import cats.syntax.group._
import cats.syntax.semigroupk._
import cats.{Applicative, Foldable, Id, Monad, MonoidK}
import net.minecraft.fluid.{Fluid, Fluids}
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler

object Operations {
  type TankOperation = ReaderWriterStateT[Id, Unit, Chain[FluidTransferLog], FluidAmount, Tank]
  type ListTankOperation[F[_]] = ReaderWriterStateT[Id, Unit, Chain[FluidTransferLog], FluidAmount, F[Tank]]

  def fillOp(tank: Tank): TankOperation = ReaderWriterStateT { case (_, s) =>
    if (s.fluidKey.fluid === Fluids.EMPTY || s.amount === 0L) {
      // Nothing to fill, skip.
      (Chain(FluidTransferLog.Empty(s, tank)), s, tank)
    } else if (tank.fluidAmount.isEmpty || (tank.fluidAmount.fluidKey === s.fluidKey)) {
      val filledAmount = (tank.capacity |-| tank.amount) min s.amount
      val filledStack = s.copy(amount = filledAmount)
      val newTank = tank.copy(tank.fluidAmount |+| filledStack)
      (Chain(FluidTransferLog.FillFluid(s, filledStack, tank, newTank)), s |-| filledStack, newTank)
    } else {
      (Chain(FluidTransferLog.FillFailed(s, tank)), s, tank)
    }
  }

  def drainOp(tank: Tank): TankOperation = if (tank.isEmpty) ReaderWriterStateT.applyS(s => Monad[Id].pure((Chain(FluidTransferLog.DrainFailed(s, tank)), s, tank)))
  else ReaderWriterStateT { case (_, s) =>
    if (s.amount === 0L) {
      // Nothing to drain.
      (Chain(FluidTransferLog.Empty(s, tank)), s, tank)
    } else if (s.fluidKey.fluid === Fluids.EMPTY || (s.fluidKey === tank.fluidAmount.fluidKey)) {
      val drainAmount = tank.amount min s.amount
      val drainedStack = tank.fluidAmount.copy(amount = drainAmount)
      val newTank = tank.copy(tank.fluidAmount.copy(amount = tank.amount |-| drainAmount))
      val restToDrain = if (drainedStack.nonEmpty) s |-| drainedStack else s
      (Chain(FluidTransferLog.DrainFluid(s, drainedStack, tank, newTank)), restToDrain, newTank)
    } else {
      (Chain(FluidTransferLog.DrainFailed(s, tank)), s, tank)
    }
  }

  def opList[F[+_]](opList: F[TankOperation])(implicit applicative: Applicative[F], F: Foldable[F], monoidK: MonoidK[F]): ListTankOperation[F] = {
    val initialState: ListTankOperation[F] = ReaderWriterStateT.applyS(f => Monad[Id].pure((Chain.empty, f, monoidK.empty)))
    F.foldLeft(opList, initialState) { (s, op) =>
      s.flatMap(filledTankList => op.map(t => filledTankList <+> applicative.pure(t)))
    }
  }

  def fillList[F[+_]](tanks: F[Tank])(implicit applicative: Applicative[F], F: Foldable[F], monoidK: MonoidK[F]): ListTankOperation[F] =
    opList(applicative.map(tanks)(fillOp))

  def fillAll[F[+_]](tanks: F[Tank])(implicit applicative: Applicative[F], F: Foldable[F], monoidK: MonoidK[F]): ListTankOperation[F] = {
    val op: Tank => TankOperation = t =>
      for {
        before <- ReaderWriterStateT.get[Id, Unit, Chain[FluidTransferLog], FluidAmount]
        _ <- ReaderWriterStateT.modify[Id, Unit, Chain[FluidTransferLog], FluidAmount](f => f.withAmount(t.capacity))
        y <- fillOp(t)
        rest <- ReaderWriterStateT.get[Id, Unit, Chain[FluidTransferLog], FluidAmount]
        r = if (rest.amount < t.capacity) rest.withAmount(0) else before
        _ <- ReaderWriterStateT.set[Id, Unit, Chain[FluidTransferLog], FluidAmount](r)
      } yield y
    opList(applicative.map(tanks)(op))
  }

  def drainList[F[+_]](tanks: F[Tank])(implicit applicative: Applicative[F], F: Foldable[F], monoidK: MonoidK[F]): ListTankOperation[F] =
    opList(applicative.map(tanks)(drainOp))

  final object EmptyTankHandler extends TankHandler {
    override def setTank(newTank: Tank): Unit = ()

    override def getFluidInTank(tank: Int): FluidStack = FluidStack.EMPTY

    override protected def outputLog(logs: Chain[FluidTransferLog], action: IFluidHandler.FluidAction): Unit = ()

    override def getFillOperation(tank: Tank): TankOperation = ReaderWriterStateT.applyS { s =>
      Monad[Id].pure(Chain(FluidTransferLog.FillFailed(s, tank)), s, tank)
    }

    override def getDrainOperation(tank: Tank): TankOperation = ReaderWriterStateT.applyS { s =>
      Monad[Id].pure(Chain(FluidTransferLog.DrainFailed(s, tank)), s, tank)
    }

    override def toString: String = "EmptyTankHandler"
  }

  class VoidTankHandler extends TankHandler {
    override def setTank(newTank: Tank): Unit = ()

    override def getFluidInTank(tank: Int): FluidStack = FluidStack.EMPTY

    override def getFillOperation(tank: Tank): TankOperation = ReaderWriterStateT.applyS { s =>
      Monad[Id].pure(Chain(FluidTransferLog.FillAll(s, tank)), FluidAmount.EMPTY, tank)
    }

    override def getDrainOperation(tank: Tank): TankOperation = ReaderWriterStateT.applyS { s =>
      Monad[Id].pure(Chain(FluidTransferLog.Empty(s, tank)), s, tank)
    }

    override def toString: String = "VoidTankHandler"
  }

  private implicit val fluidEq: cats.Eq[Fluid] = cats.Eq.fromUniversalEquals
}
