package com.kotori316.fluidtank.forge.fluid

import cats.implicits.catsSyntaxOptionId
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, VanillaFluid, VanillaPotion}
import net.minecraft.core.component.{DataComponentPatch, DataComponents}
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import net.minecraftforge.fluids.FluidStack

import scala.jdk.OptionConverters.RichOptional

object ForgeConverter {
  def toStack(amount: FluidAmount): FluidStack = {
    amount.content match {
      case VanillaFluid(fluid) => new FluidStack(fluid, amount.amount.asForge, component2FluidStackTag(amount.componentPatch))
      case VanillaPotion(_) => FluidStack.EMPTY
    }
  }

  def toAmount(stack: FluidStack): FluidAmount = {
    FluidAmountUtil.from(stack.getRawFluid, GenericUnit.fromForge(stack.getAmount), fluidStackTag2Component(stack.getTag))
  }

  implicit final class FluidAmount2FluidStack(private val a: FluidAmount) extends AnyVal {
    def toStack: FluidStack = ForgeConverter.toStack(a)
  }

  implicit final class FluidStack2FluidAmount(private val stack: FluidStack) extends AnyVal {
    def toAmount: FluidAmount = ForgeConverter.toAmount(stack)
  }

  def fluidStackTag2Component(tag: CompoundTag): Option[DataComponentPatch] = {
    if (tag == null || tag.isEmpty) {
      Option.empty
    } else {
      val components = DataComponentPatch.builder()
        .set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        .build()
      components.some
    }
  }

  def component2FluidStackTag(component: Option[DataComponentPatch]): CompoundTag = {
    val data = for {
      c <- component
      d = c.get(DataComponents.CUSTOM_DATA) // The Optional is Nullable!
      if d != null
      dd <- d.toScala
    } yield dd.copyTag()
    data.orNull
  }
}
