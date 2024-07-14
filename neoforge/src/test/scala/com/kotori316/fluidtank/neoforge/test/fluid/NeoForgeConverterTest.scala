package com.kotori316.fluidtank.neoforge.test.fluid

import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import com.kotori316.fluidtank.neoforge.test.BeforeMC
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.{FluidStack, FluidType}
import org.junit.jupiter.api.{Assertions, DynamicNode, DynamicTest, TestFactory}

class NeoForgeConverterTest extends BeforeMC {
  private val conversion: Map[FluidAmount, FluidStack] = Map(
    FluidAmountUtil.EMPTY -> FluidStack.EMPTY,
    FluidAmountUtil.BUCKET_WATER -> new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME),
    FluidAmountUtil.BUCKET_LAVA -> new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME),
  )

  @TestFactory
  def testToFluidStack(): Array[DynamicNode] = {
    conversion.map { case (amount, stack) => DynamicTest.dynamicTest(amount.toString, () => {
      Assertions.assertTrue(FluidStack.matches(stack, amount.toStack))
    })
    }.toArray
  }

  @TestFactory
  def testToFluidAmount(): Array[DynamicNode] = {
    conversion.map { case (amount, stack) => DynamicTest.dynamicTest(stack.toString, () => {
      Assertions.assertEquals(amount, stack.toAmount)
    })
    }.toArray
  }
}
