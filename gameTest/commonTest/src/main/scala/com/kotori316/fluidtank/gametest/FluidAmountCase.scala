package com.kotori316.fluidtank.gametest

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, PotionType}
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

import java.util.Locale

enum FluidType {
  case Fluid(content: Identifier)
  case Potion(potionType: PotionType, content: Identifier)

  def amounts: Seq[GenericUnit] = this match {
    case _: Fluid => Seq(GenericUnit.fromForge(500), GenericUnit.ONE_BUCKET, GenericUnit.fromForge(2000))
    case _: Potion => Seq(GenericUnit.ONE_BOTTLE, GenericUnit.ONE_BUCKET, GenericUnit.fromForge(2000))
  }

  def keyPath: String = this match {
    case Fluid(id) => id.getPath
    case Potion(potionType, _) => s"potion_${potionType.name().toLowerCase(Locale.ROOT)}"
  }

  def toFluidAmount(unit: GenericUnit): FluidAmount = this match {
    case Fluid(content) =>
      FluidAmountUtil.from(BuiltInRegistries.FLUID.getValue(content), unit)
    case Potion(potionType, content) =>
      val holder = BuiltInRegistries.POTION.get(content).orElseThrow()
      FluidAmountUtil.from(potionType, holder, unit)
  }
}

case class FluidAmountCase(fluidType: FluidType, unit: GenericUnit) {
  def toFluidAmount: FluidAmount = fluidType.toFluidAmount(unit)

  def keyPath: String = fluidType.keyPath
}
