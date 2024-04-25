package com.kotori316.fluidtank.neoforge.tank

import com.kotori316.fluidtank.contents.{GenericUnit, Tank, TankUtil}
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, fluidAccess}
import com.kotori316.fluidtank.neoforge.BeforeMC
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import com.kotori316.fluidtank.tank.{Tier, TileTank}
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.{ItemStack, Items}
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.{DynamicTest, Nested, Test, TestFactory}

class TankFluidItemHandlerTest extends BeforeMC {

  @Test
  def create(): Unit = {
    val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
    assertTrue(handler.getContainer.is(Items.APPLE))
  }

  @TestFactory
  def capacity(): Array[DynamicTest] = {
    val tiers = Tier.values().filter(_.isNormalTankTier)
    tiers.map(t => DynamicTest.dynamicTest(t.toString, () => {
      val handler = new TankFluidItemHandler(t, new ItemStack(Items.APPLE))
      assertEquals(GenericUnit(t.getCapacity).asForge, handler.getTankCapacity(0))
    }))
  }

  @Test
  def load(): Unit = {
    val tank = Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(16000))
    val tag = new CompoundTag()
    tag.put(TileTank.KEY_TANK, TankUtil.save(tank))
    tag.putString(TileTank.KEY_TIER, Tier.STONE.name())
    val stack = new ItemStack(Items.APPLE)
    stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag))

    val handler = new TankFluidItemHandler(Tier.STONE, stack)
    assertEquals(tank, handler.getTank)
  }

  @Nested
  class FillTest {
    @Test
    def fillToEmpty(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      assertTrue(handler.getTank.isEmpty)

      val filled = handler.fill(FluidAmountUtil.BUCKET_WATER.toStack, IFluidHandler.FluidAction.SIMULATE)
      assertEquals(1000, filled)
      assertTrue(handler.getTank.isEmpty)
      assertNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
      val filled2 = handler.fill(FluidAmountUtil.BUCKET_WATER.toStack, IFluidHandler.FluidAction.EXECUTE)
      assertEquals(1000, filled2)
      assertEquals(FluidAmountUtil.BUCKET_WATER, handler.getTank.content)
      assertNotNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def fillToEmpty2(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      assertTrue(handler.getTank.isEmpty)

      val filled1 = handler.fill(FluidAmountUtil.BUCKET_WATER.toStack, IFluidHandler.FluidAction.EXECUTE)
      val filled2 = handler.fill(FluidAmountUtil.BUCKET_WATER.toStack, IFluidHandler.FluidAction.EXECUTE)
      assertEquals(1000, filled1)
      assertEquals(1000, filled2)
      assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000)), handler.getTank.content)
      assertNotNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def fillToFilled1(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      handler.saveTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))

      val f1 = handler.fill(FluidAmountUtil.BUCKET_LAVA.toStack, IFluidHandler.FluidAction.SIMULATE)
      assertEquals(0, f1)
      val f2 = handler.fill(FluidAmountUtil.EMPTY.toStack, IFluidHandler.FluidAction.SIMULATE)
      assertEquals(0, f2)
    }

    @Test
    def fillToFilled2(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      handler.saveTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))

      val f1 = handler.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.SIMULATE)
      assertEquals(500, f1)
    }

    @Test
    def fillToFilled3(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      handler.saveTank(Tank(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3800)), GenericUnit.fromForge(4000)))

      val f1 = handler.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.SIMULATE)
      assertEquals(200, f1)
      val f2 = handler.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE)
      assertEquals(200, f2)
      assertEquals(
        FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(4000)),
        handler.getTank.content
      )
    }
  }

  @Nested
  class DrainTest {
    @Test
    def drain1(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      handler.saveTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))

      val d1 = handler.drain(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.SIMULATE)
      assertTrue(FluidStack.matches(d1, new FluidStack(Fluids.WATER, 500)))
      assertEquals(
        FluidAmountUtil.BUCKET_WATER,
        handler.getTank.content
      )

      val d2 = handler.drain(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE)
      assertTrue(FluidStack.matches(d2, new FluidStack(Fluids.WATER, 500)))
      assertEquals(
        FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(500)),
        handler.getTank.content
      )
      assertNotNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def drain2(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      handler.saveTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))
      val d2 = handler.drain(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE)
      assertTrue(FluidStack.matches(d2, new FluidStack(Fluids.WATER, 1000)))

      assertTrue(handler.getTank.isEmpty)
      assertNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def drain3(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      handler.saveTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))
      val d2 = handler.drain(new FluidStack(Fluids.WATER, 1500), IFluidHandler.FluidAction.EXECUTE)
      assertTrue(FluidStack.matches(d2, new FluidStack(Fluids.WATER, 1000)))

      assertTrue(handler.getTank.isEmpty)
      assertNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }

    @Test
    def drainFail(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      handler.saveTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))

      val t1 = handler.drain(new FluidStack(Fluids.LAVA, 1500), IFluidHandler.FluidAction.SIMULATE)
      assertTrue(t1.isEmpty)
      val t2 = handler.drain(FluidStack.EMPTY, IFluidHandler.FluidAction.SIMULATE)
      assertTrue(t2.isEmpty)
    }

    @Test
    def unknownTagAdded(): Unit = {
      val handler = new TankFluidItemHandler(Tier.WOOD, new ItemStack(Items.APPLE))
      handler.saveTank(Tank(FluidAmountUtil.BUCKET_WATER, GenericUnit.fromForge(4000)))
      CustomData.update(DataComponents.BLOCK_ENTITY_DATA, handler.getContainer, tag => tag.putString("unknownTag", "unknownTag"))
      handler.drain(new FluidStack(Fluids.WATER, 1500), IFluidHandler.FluidAction.EXECUTE)

      assertTrue(handler.getTank.isEmpty)
      assertNull(handler.getContainer.get(DataComponents.BLOCK_ENTITY_DATA))
    }
  }
}
