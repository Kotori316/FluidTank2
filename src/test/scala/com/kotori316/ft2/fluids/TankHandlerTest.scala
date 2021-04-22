package com.kotori316.ft2.fluids

import com.kotori316.ft2.BeforeAllTest
import com.kotori316.ft2.fluids.Operations._
import net.minecraft.fluid.Fluids
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import org.junit.jupiter.api.Assertions.{assertAll, assertEquals, assertTrue}
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.api.{DisplayName, Test}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import scala.util.chaining.scalaUtilChainingOps

//noinspection DuplicatedCode It's a test!
object TankHandlerTest {
  def nonEmptyFluidKeys: Array[FluidKey] = FluidAmountTest.fluidKeys().filter(!_.isEmpty)

  object NormalFill extends BeforeAllTest {
    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.TankHandlerTest#nonEmptyFluidKeys"))
    def fillToEmpty1(key: FluidKey): Unit = {
      val tank = TankHandler(4000L)
      val filled = tank.fill(key.createStack(4000), IFluidHandler.FluidAction.EXECUTE)
      assertEquals(4000, filled)
      assertEquals(FluidAmount(key, 4000), tank.getTank.fluidAmount)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.TankHandlerTest#nonEmptyFluidKeys"))
    def fillToEmpty2(key: FluidKey): Unit = {
      val tank = TankHandler(Tank(FluidAmount(FluidKey(Fluids.EMPTY, nbt = Option(new CompoundNBT().tap(_.putBoolean("h", true)))), 0), 4000))
      val filled = tank.fill(key.createStack(4000), IFluidHandler.FluidAction.EXECUTE)
      assertEquals(4000, filled)
      assertEquals(FluidAmount(key, 4000), tank.getTank.fluidAmount)
    }


    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys"))
    def fillWaterToEmpty2(key: FluidKey): Unit = {
      val tank = TankHandler(Tank(FluidAmount(key, 0), 4000L))
      locally {
        val filled = tank.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.SIMULATE)
        assertEquals(4000, filled)
      }
      locally {
        val filled = tank.fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.SIMULATE)
        assertEquals(4000, filled)
      }
      locally {
        val filled = tank.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE)
        assertEquals(4000, filled)
        assertEquals(FluidAmount.BUCKET_WATER.withAmount(4000), tank.getTank.fluidAmount)
      }
    }

    @Test
    def fillEmptyToEmpty(): Unit = {
      val tank = TankHandler(4000L)
      val filled = tank.fill(new FluidStack(Fluids.EMPTY, 4000), IFluidHandler.FluidAction.EXECUTE)
      assertEquals(0, filled)
      assertEquals(FluidAmount.EMPTY, tank.getTank.fluidAmount)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.TankHandlerTest#nonEmptyFluidKeys"))
    def fillSimulation(key: FluidKey): Unit = {
      val tank = TankHandler(4000L)
      val before = tank.getTank

      val filled = tank.fill(key.createStack(4000), IFluidHandler.FluidAction.SIMULATE)
      assertEquals(4000, filled)
      assertEquals(FluidAmount.EMPTY, tank.getTank.fluidAmount, "Simulation must not change the content.")
      assertEquals(before, tank.getTank)
    }

    @Test
    def fillWaterToWater(): Unit = {
      val tank = new TankHandler
      tank.setTank(Tank(FluidAmount.BUCKET_WATER, 16000L))

      val filled = tank.fill(new FluidStack(Fluids.WATER, 3000), IFluidHandler.FluidAction.EXECUTE)
      assertEquals(3000, filled)
      assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(4000L), 16000L), tank.getTank)
    }

    @Test
    def fillWaterToWater2(): Unit = {
      val tank = new TankHandler
      tank.setTank(Tank(FluidAmount.BUCKET_WATER, 16000L)) // Water 1000, Air 15000

      val filled = tank.fill(new FluidStack(Fluids.WATER, 17000), IFluidHandler.FluidAction.EXECUTE)
      assertEquals(15000, filled)
      assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(16000L), 16000L), tank.getTank)
    }

    @Test
    def fillFail1(): Unit = {
      val tank = new TankHandler
      val before = Tank(FluidAmount.BUCKET_WATER, 16000L)
      tank.setTank(before)

      {
        val filled = tank.fill(new FluidStack(Fluids.LAVA, 3000), IFluidHandler.FluidAction.SIMULATE)
        assertEquals(0, filled)
        assertEquals(before, tank.getTank)
      }
      {
        val filled = tank.fill(new FluidStack(Fluids.LAVA, 3000), IFluidHandler.FluidAction.EXECUTE)
        assertEquals(0, filled)
        assertEquals(before, tank.getTank)
      }
    }

    @Test
    def fillFail2(): Unit = {
      val tank = new TankHandler
      val before = Tank(FluidAmount.BUCKET_WATER.withAmount(16000L), 16000L)
      tank.setTank(before)

      val filled = tank.fill(new FluidStack(Fluids.WATER, 17000), IFluidHandler.FluidAction.EXECUTE)
      assertEquals(0, filled)
      assertEquals(before, tank.getTank)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys"))
    def fillFail3(key: FluidKey): Unit = {
      val before = Tank(FluidAmount(FluidKey(Fluids.WATER, Option(new CompoundNBT().tap(_.putInt("f", -10)))), 6000), 16000)
      val tankHandler = TankHandler(before)

      val filled = tankHandler.fill(key.createStack(1000), IFluidHandler.FluidAction.SIMULATE)
      assertEquals(0, filled)
      assertEquals(before, tankHandler.getTank)
    }
  }

  object NormalDrain extends BeforeAllTest {
    @Test
    def drainEmpty(): Unit = {
      val tank = TankHandler(4000L)
      val drained = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE)
      assertTrue(drained.isEmpty)
      assertTrue(tank.getTank.fluidAmount.isEmpty)
    }

    @Test
    @DisplayName("Drain 1000mB of Water from Water tank.")
    def drainNormal1(): Unit = {
      val tank = new TankHandler
      tank.setTank(Tank(FluidAmount.BUCKET_WATER, 16000L))

      val drained = tank.drain(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE)
      assertEquals(FluidAmount.fromStack(new FluidStack(Fluids.WATER, 1000)), FluidAmount.fromStack(drained))
      assertTrue(tank.getTank.fluidAmount.isEmpty)
    }

    @Test
    @DisplayName("Drain 1000mB from Water tank.")
    def drainNormal2(): Unit = {
      val tank = new TankHandler
      tank.setTank(Tank(FluidAmount.BUCKET_WATER, 16000L))

      val drained = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE)
      assertEquals(FluidAmount.BUCKET_WATER, FluidAmount.fromStack(drained))
      assertEquals(FluidAmount.fromStack(new FluidStack(Fluids.WATER, 1000)), FluidAmount.fromStack(drained))
      assertTrue(tank.getTank.fluidAmount.isEmpty)
    }

    @Test
    @DisplayName("Drain 1000mB of Empty from Water tank.")
    def drainNormal3(): Unit = {
      val tank = new TankHandler
      tank.setTank(Tank(FluidAmount.BUCKET_WATER.withAmount(4000L), 16000L))

      val drained = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE)
      assertEquals(FluidAmount.fromStack(new FluidStack(Fluids.WATER, 1000)), FluidAmount.fromStack(drained))
      assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(3000), 16000), tank.getTank)
    }

    @Test
    def drainFail1(): Unit = {
      val tank = TankHandler(4000L)
      val before = tank.getTank
      locally {
        val drained = tank.drain(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE)
        assertTrue(drained.isEmpty)
        assertEquals(before, tank.getTank)
      }
      locally {
        val drained = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE)
        assertTrue(drained.isEmpty)
        assertEquals(before, tank.getTank)
      }
    }

    @Test
    def drainFail2(): Unit = {
      val tank = new TankHandler
      val before = Tank(FluidAmount.BUCKET_WATER.withAmount(4000L), 16000L)
      tank.setTank(before)

      val drained = tank.drain(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE)
      assertTrue(drained.isEmpty)
      assertEquals(before, tank.getTank)
    }

  }

  object SpecialHandlers extends BeforeAllTest {
    @Test
    def emptyHandler(): Unit = {
      val h: TankHandler = EmptyTankHandler
      locally {
        val filled = h.fill(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.SIMULATE)
        assertEquals(0, filled)
      }
      locally {
        val filled = h.fill(FluidAmount.BUCKET_LAVA.toStack, IFluidHandler.FluidAction.SIMULATE)
        assertEquals(0, filled)
      }
      locally {
        val drained = h.drain(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.SIMULATE)
        assertTrue(drained.isEmpty)
      }
      locally {
        val drained = h.drain(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.SIMULATE)
        assertTrue(drained.isEmpty)
      }
      locally {
        val drained = h.drain(1000, IFluidHandler.FluidAction.SIMULATE)
        assertTrue(drained.isEmpty)
      }
    }

    @Test
    def voidHandler(): Unit = {
      val h: TankHandler = new VoidTankHandler
      locally {
        val filled = h.fill(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.SIMULATE)
        assertEquals(1000, filled)
      }
      locally {
        val filled = h.fill(FluidAmount.BUCKET_LAVA.toStack, IFluidHandler.FluidAction.SIMULATE)
        assertEquals(1000, filled)
      }
      locally {
        val drained = h.drain(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.SIMULATE)
        assertTrue(drained.isEmpty)
      }
      locally {
        val drained = h.drain(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.SIMULATE)
        assertTrue(drained.isEmpty)
      }
      locally {
        val drained = h.drain(1000, IFluidHandler.FluidAction.SIMULATE)
        assertTrue(drained.isEmpty)
      }
    }

    @Test
    def fillCreativeHandler(): Unit = {
      val h: TankHandler = new CreativeTankHandler
      val filled = h.fill(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.SIMULATE)
      assertEquals(FluidAmount.BUCKET_WATER.amount, filled)
      assertTrue(h.getTank.fluidAmount.isEmpty)

      val filled2 = h.fill(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.EXECUTE)
      assertEquals(FluidAmount.BUCKET_WATER.amount, filled2)
      assertEquals(FluidAmount.BUCKET_WATER.fluidKey, h.getTank.fluidAmount.fluidKey)
      assertEquals(Long.MaxValue, h.getTank.fluidAmount.amount)
    }

    @Test
    def drainCreativeHandler(): Unit = {
      val h: TankHandler = new CreativeTankHandler
      assertAll(
        () => assertTrue(h.drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty),
        () => assertTrue(h.drain(FluidAmount.EMPTY.toStack, IFluidHandler.FluidAction.SIMULATE).isEmpty),
        () => assertTrue(h.drain(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.SIMULATE).isEmpty),
      )
      h.fill(FluidAmount.BUCKET_WATER.toStack, IFluidHandler.FluidAction.EXECUTE)
      assertAll(
        Range(1, 10).map(i => Math.pow(10, i).toLong).map(FluidAmount.BUCKET_WATER.withAmount)
          .map[Executable](f => () => assertEquals(f, FluidAmount.fromStack(h.drain(f.toStack, IFluidHandler.FluidAction.SIMULATE)), s"Drain $f Simulation")): _*
      )
      assertAll(
        Range(1, 10).map(i => Math.pow(10, i).toLong).map(FluidAmount.BUCKET_WATER.withAmount)
          .map[Executable](f => () => assertEquals(f, FluidAmount.fromStack(h.drain(f.toStack, IFluidHandler.FluidAction.EXECUTE)), s"Drain $f Execution")): _*
      )
      assertAll(
        Range(1, 10).map(i => Math.pow(10, i).toLong).map(FluidAmount.BUCKET_LAVA.withAmount)
          .map[Executable](f => () => assertTrue(FluidAmount.fromStack(h.drain(f.toStack, IFluidHandler.FluidAction.SIMULATE)).isEmpty, s"Drain $f Simulation")): _*
      )
    }

    @Test
    @DisplayName("Drain Empty from Creative")
    def drainEmptyFromCreativeHandler(): Unit = {
      val h: TankHandler = new CreativeTankHandler
      h.fill(FluidAmount.BUCKET_WATER, IFluidHandler.FluidAction.EXECUTE)
      assertEquals(FluidAmount.BUCKET_WATER, h.drain(FluidAmount.EMPTY.withAmount(1000L), IFluidHandler.FluidAction.SIMULATE))
      assertTrue(h.getTank.amount > 0)
    }

    @Test
    def emptyCreativeHandler(): Unit = {
      val h: TankHandler = new CreativeTankHandler
      assertAll(
        () => assertEquals(0, h.fill(FluidStack.EMPTY, IFluidHandler.FluidAction.SIMULATE), "Filling EMPTY"),
        () => assertEquals(0, h.fill(FluidStack.EMPTY, IFluidHandler.FluidAction.EXECUTE), "Filling EMPTY"),
        () => assertTrue(h.getTank.fluidAmount.isEmpty, "Fill with 0 cause no changes."),
        () => assertEquals(FluidAmount.EMPTY, FluidAmount.fromStack(h.drain(FluidStack.EMPTY, IFluidHandler.FluidAction.SIMULATE)), "Drain empty fluid to get EMPTY."),
        () => assertEquals(FluidAmount.EMPTY, FluidAmount.fromStack(h.drain(0, IFluidHandler.FluidAction.SIMULATE)), "Drain 0 fluid to get EMPTY."),
      )
    }

  }
}
