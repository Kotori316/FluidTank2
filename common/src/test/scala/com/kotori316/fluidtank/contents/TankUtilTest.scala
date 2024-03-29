package com.kotori316.fluidtank.contents

import com.kotori316.fluidtank.BeforeMC
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, fluidAccess}
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.{Assertions, Nested, Test}

class TankUtilTest {
  private def cycle[A](tank: Tank[A])(implicit access: GenericAccess[A]): Unit = {
    val tag = TankUtil.save(tank)
    val reconstructed = TankUtil.load[A](tag)
    Assertions.assertEquals(tank, reconstructed)
    Assertions.assertEquals(tank.getClass, reconstructed.getClass)
  }

  @Nested
  class StringTankTest extends BeforeMC {
    @Test
    def normal(): Unit = {
      val tank = Tank(GenericAmount("a", GenericUnit.fromForge(100), None), GenericUnit.fromForge(1000))
      cycle(tank)
    }

    @Test
    def voidTank(): Unit = {
      val tank = VoidTank(gaString)
      cycle(tank)
    }

    @Test
    def creativeTank(): Unit = {
      val tank = new CreativeTank(GenericAmount("a", GenericUnit.fromForge(1000), None), GenericUnit.fromForge(1000))
      cycle(tank)
    }

    @Test
    def parseNullTag(): Unit = {
      val tank = Assertions.assertDoesNotThrow(() => TankUtil.load(null)(gaString))
      Assertions.assertTrue(tank.isEmpty)
      Assertions.assertEquals(GenericUnit.ZERO, tank.content.amount)
      Assertions.assertEquals(GenericUnit.ZERO, tank.capacity)
    }

    @Test
    def parseEmptyTag(): Unit = {
      val tank = Assertions.assertDoesNotThrow(() => TankUtil.load(new CompoundTag())(gaString))
      Assertions.assertTrue(tank.isEmpty)
      Assertions.assertEquals(GenericUnit.ZERO, tank.content.amount)
      Assertions.assertEquals(GenericUnit.ZERO, tank.capacity)
    }
  }

  @Nested
  class FluidTankTest extends BeforeMC {
    @Test
    def normal(): Unit = {
      val tank = Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(1000))
      cycle(tank)
    }

    @Test
    def voidTank(): Unit = {
      val tank = VoidTank(fluidAccess)
      cycle(tank)
    }

    @Test
    def creativeTank(): Unit = {
      val tank = new CreativeTank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(1000))
      cycle(tank)
    }
  }
}
