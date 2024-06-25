package com.kotori316.fluidtank.neoforge.cat

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, FluidLike}
import com.kotori316.fluidtank.neoforge.BeforeMC
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.FluidAmount2FluidStack
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.{ItemStack, Items}
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.wrapper.InvWrapper
import org.junit.jupiter.api.Assertions.{assertEquals, assertTrue}
import org.junit.jupiter.api.{Disabled, Test}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.{Arguments, MethodSource}

@Disabled("Archtectury doesn't support NeoForge JUnit support system")
class EntityChestAsTankTest extends BeforeMC {

  @Test
  def slot(): Unit = {
    val items = new SimpleContainer(Seq.fill(15)(new ItemStack(Items.BUCKET)) *)
    val handler = new EntityChestAsTank.FluidHandlerProxy(new InvWrapper(items))

    assertEquals(15, handler.getTanks)
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.cat.EntityChestAsTankTest#fluids"))
  def fillToBucket(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(Seq.fill(10)(new ItemStack(Items.BUCKET)) *)
    val handler = new EntityChestAsTank.FluidHandlerProxy(new InvWrapper(items))

    val filled = handler.fill(fluid.toStack, IFluidHandler.FluidAction.EXECUTE)
    assertEquals(filledItem.getCount * 1000, filled)
    assertEquals(filledItem.getCount, items.countItem(filledItem.getItem))
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.cat.EntityChestAsTankTest#fluids"))
  def drainFromBucket1(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(Seq.fill(10)(filledItem.copyWithCount(1)) *)
    val handler = new EntityChestAsTank.FluidHandlerProxy(new InvWrapper(items))

    val drained = handler.drain(fluid.toStack, IFluidHandler.FluidAction.EXECUTE)
    assertEquals(filledItem.getCount * 1000, drained.getAmount)
    assertEquals(fluid.content, FluidLike.of(drained.getFluid))

    assertEquals(filledItem.getCount, items.countItem(Items.BUCKET))
    assertEquals(10 - filledItem.getCount, items.countItem(filledItem.getItem))
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.cat.EntityChestAsTankTest#fluids"))
  def drainFromBucket2(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(Seq.fill(10)(filledItem.copyWithCount(1)) *)
    val handler = new EntityChestAsTank.FluidHandlerProxy(new InvWrapper(items))

    val drained = handler.drain(fluid.amount.asForge, IFluidHandler.FluidAction.EXECUTE)
    assertEquals(filledItem.getCount * 1000, drained.getAmount)
    assertEquals(fluid.content, FluidLike.of(drained.getFluid))

    assertEquals(filledItem.getCount, items.countItem(Items.BUCKET))
    assertEquals(10 - filledItem.getCount, items.countItem(filledItem.getItem))
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.cat.EntityChestAsTankTest#fluids"))
  def fillStackedBucket(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(2)
    items.setItem(0, new ItemStack(Items.BUCKET, 2))
    val handler = new EntityChestAsTank.FluidHandlerProxy(new InvWrapper(items))

    val filled = handler.fill(fluid.toStack, IFluidHandler.FluidAction.EXECUTE)
    assertEquals(0, filled)
    assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET, 2), items.getItem(0)))
    assertEquals(0, items.countItem(filledItem.getItem))
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.cat.EntityChestAsTankTest#fluids"))
  def drainStackedBucket1(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(2)
    items.setItem(1, filledItem.copy())
    items.getItem(1).setCount(2)
    val handler = new EntityChestAsTank.FluidHandlerProxy(new InvWrapper(items))

    val drained = handler.drain(fluid.toStack, IFluidHandler.FluidAction.EXECUTE)
    assertTrue(drained.isEmpty)
    assertEquals(0, items.countItem(Items.BUCKET))
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.fluidtank.neoforge.cat.EntityChestAsTankTest#fluids"))
  def drainStackedBucket2(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(2)
    items.setItem(0, filledItem.copy())
    items.getItem(0).setCount(2)
    val handler = new EntityChestAsTank.FluidHandlerProxy(new InvWrapper(items))

    val drained = handler.drain(fluid.amount.asForge, IFluidHandler.FluidAction.EXECUTE)
    assertTrue(drained.isEmpty)
    assertEquals(0, items.countItem(Items.BUCKET))
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
