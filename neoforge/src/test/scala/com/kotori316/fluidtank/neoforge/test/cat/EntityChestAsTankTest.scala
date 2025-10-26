package com.kotori316.fluidtank.neoforge.test.cat

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import com.kotori316.fluidtank.neoforge.test.{BeforeMC, TestMod}
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.{ItemStack, Items}
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper
import org.junit.jupiter.api.Assertions.{assertEquals, assertTrue}
import org.junit.jupiter.api.{Disabled, Test}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.{Arguments, MethodSource}

@Disabled("Implementing")
class EntityChestAsTankTest extends BeforeMC {

  @Test
  def slot(): Unit = {
    val items = new SimpleContainer(Seq.fill(15)(new ItemStack(Items.BUCKET)) *)
    val handler = TestMod.getCatHandler(VanillaContainerWrapper.of(items))

    assertEquals(15, handler.size())
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.test.cat.EntityChestAsTankTest#fluids"))
  def fillToBucket(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(Seq.fill(10)(new ItemStack(Items.BUCKET)) *)
    val handler = TestMod.getCatHandler(VanillaContainerWrapper.of(items))

    TestMod.inTransaction { tx =>
      val filled = handler.insert(fluid.asVariant, fluid.amount.asForge, tx)
      assertEquals(filledItem.getCount * 1000, filled)
      assertEquals(filledItem.getCount, items.countItem(filledItem.getItem))
    }
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.test.cat.EntityChestAsTankTest#fluids"))
  def drainFromBucket1(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(Seq.fill(10)(filledItem.copyWithCount(1)) *)
    val handler = TestMod.getCatHandler(VanillaContainerWrapper.of(items))

    TestMod.inTransaction { tx =>
      val drained = handler.extract(fluid.asVariant, fluid.amount.asForge, tx)
      assertEquals(filledItem.getCount * 1000, drained)

      assertEquals(filledItem.getCount, items.countItem(Items.BUCKET))
      assertEquals(10 - filledItem.getCount, items.countItem(filledItem.getItem))
    }
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.test.cat.EntityChestAsTankTest#fluids"))
  def fillStackedBucket(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(2)
    items.setItem(0, new ItemStack(Items.BUCKET, 2))
    val handler = TestMod.getCatHandler(VanillaContainerWrapper.of(items))

    TestMod.inTransaction { tx =>
      val filled = handler.insert(fluid.asVariant, fluid.amount.asForge, tx)
      assertEquals(0, filled)
      assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET, 2), items.getItem(0)))
      assertEquals(0, items.countItem(filledItem.getItem))
    }
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.test.cat.EntityChestAsTankTest#fluids"))
  def drainStackedBucket1(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(2)
    items.setItem(1, filledItem.copy())
    items.getItem(1).setCount(2)
    val handler = TestMod.getCatHandler(VanillaContainerWrapper.of(items))

    TestMod.inTransaction { tx =>
      val drained = handler.extract(fluid.asVariant, fluid.amount.asForge, tx)
      assertEquals(0, drained)
      assertEquals(0, items.countItem(Items.BUCKET))
    }
  }
}

object EntityChestAsTankTest {
  def fluids(): Array[Arguments] = {
    for {
      (f, i) <- Array(
        (FluidAmountUtil.BUCKET_WATER, Items.WATER_BUCKET),
        (FluidAmountUtil.BUCKET_LAVA, Items.LAVA_BUCKET),
      )
      amount <- 1000 to 10000 by 500
      fillCount = amount / 1000
    } yield Arguments.of(f.setAmount(GenericUnit.fromForge(amount)), new ItemStack(i, fillCount))
  }
}
