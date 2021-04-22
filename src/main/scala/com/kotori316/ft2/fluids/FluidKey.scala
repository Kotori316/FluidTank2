package com.kotori316.ft2.fluids

import cats.{Hash, Show}
import net.minecraft.fluid.{Fluid, Fluids}
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.fluids.FluidStack

case class FluidKey(fluid: Fluid, nbt: Option[CompoundNBT]) {
  def isEmpty: Boolean = fluid == Fluids.EMPTY

  def isGaseous: Boolean = fluid.getAttributes.isGaseous

  def getRegistryName: String = String.valueOf(fluid.getRegistryName)

  override def toString: String = nbt match {
    case Some(value) => s"FluidKey{fluid=$getRegistryName, nbt=$value}"
    case None => s"FluidKey{fluid=$getRegistryName}"
  }

  def createStack(amount: Int): FluidStack = new FluidStack(fluid, amount, nbt.orNull)
}

object FluidKey {
  implicit val hashFluidKey: Hash[FluidKey] = Hash.fromUniversalHashCode
  implicit val showFluidKey: Show[FluidKey] = Show.fromToString
  final val EMPTY: FluidKey = FluidKey(Fluids.EMPTY, None)
  final val WATER: FluidKey = FluidKey(Fluids.WATER, None)
  final val LAVA: FluidKey = FluidKey(Fluids.LAVA, None)
}
