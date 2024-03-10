package com.kotori316.fluidtank.fabric.fluid

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, VanillaFluid, VanillaPotion}
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.world.level.material.Fluid

//noinspection UnstableApiUsage
object FabricConverter {
  def toVariant(amount: FluidAmount, fallback: Fluid): FluidVariant = {
    amount.content match {
      case VanillaFluid(fluid) => FluidVariant.of(fluid, amount.componentPatch.getOrElse(DataComponentPatch.EMPTY))
      case VanillaPotion(_) => FluidVariant.of(fallback)
    }
  }

  def fabricAmount(amount: FluidAmount): Long = amount.amount.asFabric

  def fromVariant(variant: FluidVariant, fabricAmount: Long): FluidAmount = {
    FluidAmountUtil.from(variant.getFluid, GenericUnit.fromFabric(fabricAmount), Option(variant.getComponents))
  }
}
