package com.kotori316.ft2.fluids

import cats.{Group, Hash}
import com.kotori316.ft2.utils.FT2Utils
import net.minecraftforge.fluids.{FluidAttributes, FluidStack}

case class FluidAmount(fluidKey: FluidKey, amount: Long) {
  def isEmpty: Boolean = fluidKey.isEmpty || amount <= 0

  def nonEmpty: Boolean = !isEmpty

  def toStack: FluidStack = if (fluidKey.isEmpty) FluidStack.EMPTY else fluidKey.createStack(FT2Utils.toInt(amount))

  def withAmount(amount: Long): FluidAmount = {
    if (amount != this.amount) this.copy(amount = amount)
    else this
  }

  def *(n: Int): FluidAmount = Group[FluidAmount].combineN(this, n)
}

object FluidAmount {
  final val EMPTY: FluidAmount = FluidAmount(FluidKey.EMPTY, 0)
  final val BUCKET_WATER: FluidAmount = FluidAmount(FluidKey.WATER, FluidAttributes.BUCKET_VOLUME)
  final val BUCKET_LAVA: FluidAmount = FluidAmount(FluidKey.LAVA, FluidAttributes.BUCKET_VOLUME)

  final def fromStack(stack: FluidStack): FluidAmount = {
    val key = FluidKey(stack.getRawFluid, Option(stack.getTag))
    FluidAmount(fluidKey = key, amount = stack.getAmount)
  }

  implicit val hashFluidAmount: Hash[FluidAmount] = Hash.fromUniversalHashCode
  implicit val groupFluidAmount: Group[FluidAmount] = new Group[FluidAmount] {
    override def empty: FluidAmount = EMPTY

    override def combine(x: FluidAmount, y: FluidAmount): FluidAmount = {
      if (x.amount == 0 && !y.fluidKey.isEmpty) y
      else if (y.amount == 0) x
      else {
        val original = if (x.fluidKey.isEmpty) y else x
        original.copy(amount = x.amount + y.amount)
      }
    }

    override def combineN(a: FluidAmount, n: Int): FluidAmount = a.withAmount(amount = a.amount * n)

    override def inverse(a: FluidAmount): FluidAmount = a.withAmount(amount = -a.amount)

    override def remove(a: FluidAmount, b: FluidAmount): FluidAmount = {
      if (a.amount == 0 && !b.fluidKey.isEmpty) inverse(b)
      else if (b.amount == 0) a
      else {
        val original = if (a.fluidKey.isEmpty) b else a
        original.copy(amount = a.amount - b.amount)
      }
    }

  }
}
