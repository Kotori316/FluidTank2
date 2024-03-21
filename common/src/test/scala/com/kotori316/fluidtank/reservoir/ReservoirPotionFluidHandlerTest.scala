package com.kotori316.fluidtank.reservoir

import com.kotori316.fluidtank.BeforeMC
import com.kotori316.fluidtank.contents.{GenericUnit, Tank}
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, FluidLike, PotionType}
import com.kotori316.fluidtank.item.PlatformItemAccess
import com.kotori316.fluidtank.potions.PotionFluidHandler
import com.kotori316.fluidtank.tank.Tier
import net.minecraft.core.component.DataComponentType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.Potions
import org.junit.jupiter.api.Assertions.{assertAll, assertEquals, assertFalse, assertInstanceOf, assertTrue}
import org.junit.jupiter.api.Test

class ReservoirPotionFluidHandlerTest extends BeforeMC {
  val tankComponent: DataComponentType[Tank[FluidLike]] = PlatformItemAccess.getInstance().fluidTankComponentType()
  val reservoir = new ItemReservoir(Tier.WOOD)

  @Test
  def instance(): Unit = {
    val stack = new ItemStack(reservoir)
    val handler = PotionFluidHandler(stack)

    assertTrue(handler.isValidHandler)
    assertInstanceOf(classOf[ReservoirPotionFluidHandler], handler)
  }

  @Test
  def initialEmpty(): Unit = {
    val stack = new ItemStack(reservoir)
    val handler = PotionFluidHandler(stack)

    assertTrue(handler.getContent.isEmpty)
  }

  @Test
  def fillPotion(): Unit = {
    val stack = new ItemStack(reservoir)
    val handler = PotionFluidHandler(stack)
    val toFill = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BUCKET)
    val result = handler.fill(toFill, FluidLike.of(PotionType.NORMAL))

    assertAll(
      () => assertTrue(result.shouldMove()),
      () => assertEquals(toFill, result.moved()),
      () => assertFalse(stack.has(tankComponent)),
      () => assertTrue(result.toReplace.has(tankComponent)),
    )

    val tank = reservoir.getTank(result.toReplace)
    assertEquals(toFill, tank.content)
  }

  @Test
  def fillPotion2(): Unit = {
    val stack = new ItemStack(reservoir)
    val toFill = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BUCKET)
    reservoir.saveTank(stack, Tank(toFill, GenericUnit.fromForge(4000)))
    val handler = PotionFluidHandler(stack)
    val result = handler.fill(toFill, FluidLike.of(PotionType.NORMAL))

    assertAll(
      () => assertTrue(result.shouldMove()),
      () => assertEquals(toFill, result.moved()),
      () => assertTrue(stack.has(tankComponent)),
      () => assertTrue(result.toReplace.has(tankComponent)),
    )

    val tank = reservoir.getTank(result.toReplace)
    assertEquals(toFill.setAmount(GenericUnit.fromForge(2000)), tank.content)
  }

  @Test
  def fillPotion3(): Unit = {
    val stack = new ItemStack(reservoir)
    val toFill = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.fromForge(3000))
    reservoir.saveTank(stack, Tank(toFill, GenericUnit.fromForge(4000)))
    val handler = PotionFluidHandler(stack)
    val result = handler.fill(toFill, FluidLike.of(PotionType.NORMAL))

    assertAll(
      () => assertTrue(result.shouldMove()),
      () => assertEquals(toFill.setAmount(GenericUnit.ONE_BUCKET), result.moved()),
      () => assertTrue(stack.has(tankComponent)),
      () => assertTrue(result.toReplace.has(tankComponent)),
    )

    val tank = reservoir.getTank(result.toReplace)
    assertEquals(toFill.setAmount(GenericUnit.fromForge(4000)), tank.content)
  }

  @Test
  def fillPotion4(): Unit = {
    val stack = new ItemStack(reservoir)
    val toFill = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.fromForge(3000))
    val content = FluidAmountUtil.from(PotionType.NORMAL, Potions.NIGHT_VISION, GenericUnit.fromForge(3000))
    reservoir.saveTank(stack, Tank(content, GenericUnit.fromForge(4000)))
    val handler = PotionFluidHandler(stack)
    val result = handler.fill(toFill, FluidLike.of(PotionType.NORMAL))

    assertAll(
      () => assertFalse(result.shouldMove()),
      () => assertEquals(FluidAmountUtil.EMPTY, result.moved()),
      () => assertTrue(stack.has(tankComponent)),
      () => assertTrue(result.toReplace.has(tankComponent)),
    )

    val tank = reservoir.getTank(result.toReplace)
    assertEquals(content, tank.content)
  }

  @Test
  def drainPotion(): Unit = {
    val stack = new ItemStack(reservoir)
    val toDrain = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BUCKET)
    reservoir.saveTank(stack, Tank(toDrain, GenericUnit.fromForge(4000)))
    assertTrue(stack.has(tankComponent))
    val handler = PotionFluidHandler(stack)
    assertEquals(toDrain, handler.getContent)

    val result = handler.drain(toDrain, FluidLike.of(PotionType.NORMAL))
    assertAll(
      () => assertTrue(result.shouldMove()),
      () => assertEquals(toDrain, result.moved()),
      () => assertFalse(result.toReplace.has(tankComponent)),
    )

    val tank = reservoir.getTank(result.toReplace)
    assertTrue(tank.isEmpty)
  }

  @Test
  def drainPotion2(): Unit = {
    val stack = new ItemStack(reservoir)
    val toDrain = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BUCKET)
    reservoir.saveTank(stack, Tank(toDrain, GenericUnit.fromForge(4000)))
    assertTrue(stack.has(tankComponent))
    val handler = PotionFluidHandler(stack)
    assertEquals(toDrain, handler.getContent)

    val result = handler.drain(toDrain.setAmount(GenericUnit.fromForge(1500)), FluidLike.of(PotionType.NORMAL))
    assertAll(
      () => assertTrue(result.shouldMove()),
      () => assertEquals(toDrain, result.moved()),
      () => assertFalse(result.toReplace.has(tankComponent)),
    )

    val tank = reservoir.getTank(result.toReplace)
    assertTrue(tank.isEmpty)
  }

  @Test
  def drainPotion3(): Unit = {
    val stack = new ItemStack(reservoir)
    val toDrain = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BUCKET)
    reservoir.saveTank(stack, Tank(toDrain, GenericUnit.fromForge(4000)))
    assertTrue(stack.has(tankComponent))
    val handler = PotionFluidHandler(stack)
    assertEquals(toDrain, handler.getContent)

    val result = handler.drain(toDrain.setAmount(GenericUnit.fromForge(500)), FluidLike.of(PotionType.NORMAL))
    assertAll(
      () => assertTrue(result.shouldMove()),
      () => assertEquals(toDrain.setAmount(GenericUnit.fromForge(500)), result.moved()),
      () => assertTrue(result.toReplace.has(tankComponent)),
    )

    val tank = reservoir.getTank(result.toReplace)
    assertEquals(toDrain.setAmount(GenericUnit.fromForge(500)), tank.content)
  }

  @Test
  def drainPotion4(): Unit = {
    val stack = new ItemStack(reservoir)
    val toDrain = FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BUCKET)
    val content = FluidAmountUtil.from(PotionType.NORMAL, Potions.NIGHT_VISION, GenericUnit.ONE_BUCKET)
    reservoir.saveTank(stack, Tank(content, GenericUnit.fromForge(4000)))
    assertTrue(stack.has(tankComponent))
    val handler = PotionFluidHandler(stack)
    assertEquals(content, handler.getContent)

    val result = handler.drain(toDrain, FluidLike.of(PotionType.NORMAL))
    assertAll(
      () => assertFalse(result.shouldMove()),
      () => assertEquals(FluidAmountUtil.EMPTY, result.moved()),
      () => assertTrue(result.toReplace.has(tankComponent)),
    )

    val tank = reservoir.getTank(result.toReplace)
    assertEquals(content, tank.content)
  }
}
