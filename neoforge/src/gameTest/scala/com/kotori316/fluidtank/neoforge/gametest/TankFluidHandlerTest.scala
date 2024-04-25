package com.kotori316.fluidtank.neoforge.gametest

import cats.implicits.catsSyntaxSemigroup
import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, PotionType}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.FluidAmount2FluidStack
import com.kotori316.fluidtank.neoforge.gametest.GetGameTestMethods.assertEqualHelper
import com.kotori316.fluidtank.neoforge.tank.TileTankNeoForge
import com.kotori316.fluidtank.tank.Tier
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.{GameTestGenerator, GameTestHelper, TestFunction}
import net.minecraft.world.item.alchemy.Potions
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction
import net.neoforged.neoforge.gametest.GameTestHolder
import org.junit.jupiter.api.Assertions.{assertDoesNotThrow, assertEquals, assertNotNull, assertTrue}

@GameTestHolder(FluidTankCommon.modId)
class TankFluidHandlerTest {
  private final val BATCH = "defaultBatch"

  @GameTestGenerator
  def generator(): java.util.List[TestFunction] = {
    GetGameTestMethods.getTests(getClass, this, BATCH)
  }

  private def getTankCapability(helper: GameTestHelper, pos: BlockPos, tile: TileTankNeoForge): IFluidHandler = {
    val h = assertDoesNotThrow(() => helper.getLevel.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null, tile, null))
    assertNotNull(h)
    h
  }

  def testGetCapability(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]

    getTankCapability(helper, basePos, tile)
    helper.succeed()
  }

  def capacity(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]

    val cap = getTankCapability(helper, basePos, tile)
    assertEquals(4000, cap.getTankCapacity(0))
    helper.succeed()
  }

  def amount1(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    tile.getConnection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)

    val cap = getTankCapability(helper, basePos, tile)
    assertEquals(FluidAmountUtil.BUCKET_WATER, NeoForgeConverter.toAmount(cap.getFluidInTank(0)))
    helper.succeed()
  }

  def fillSimulate1(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    val handler = getTankCapability(helper, basePos, tile)

    val filled = handler.fill(FluidAmountUtil.BUCKET_WATER.toStack, FluidAction.SIMULATE)
    assertEquals(1000, filled)
    assertEquals(GenericUnit.ZERO, tile.getConnection.amount)
    helper.succeed()
  }

  def fillExecute1(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    TankTest.placeTank(helper, basePos.above(), Tier.STONE)
    val handler = getTankCapability(helper, basePos, tile)

    val toFill = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(20000))
    val filled = handler.fill(toFill.toStack, FluidAction.EXECUTE)
    assertEquals(20000, filled)
    assertEqualHelper(Option(toFill), tile.getConnection.getContent)
    helper.succeed()
  }

  def potionTank(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    TankTest.placeTank(helper, basePos.above(), Tier.STONE)
    tile.getConnection.getHandler.fill(FluidAmountUtil.from(PotionType.SPLASH, Potions.NIGHT_VISION, GenericUnit.ONE_BUCKET), execute = true)

    val handler = getTankCapability(helper, basePos, tile)
    assertEquals(20000, handler.getTankCapacity(0))
    assertTrue(handler.getFluidInTank(0).isEmpty)
    helper.succeed()
  }

  def potionTank2(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    TankTest.placeTank(helper, basePos.above(), Tier.STONE)
    val content = FluidAmountUtil.from(PotionType.SPLASH, Potions.NIGHT_VISION, GenericUnit.ONE_BUCKET.combineN(3))
    tile.getConnection.getHandler.fill(content, execute = true)

    val handler = getTankCapability(helper, basePos, tile)
    val filled = handler.fill(FluidAmountUtil.BUCKET_WATER.toStack, FluidAction.SIMULATE)
    assertEquals(0, filled)
    assertEqualHelper(Option(content), tile.getConnection.getContent)
    helper.succeed()
  }

  def potionTank3(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    TankTest.placeTank(helper, basePos.above(), Tier.STONE)
    val content = FluidAmountUtil.from(PotionType.SPLASH, Potions.NIGHT_VISION, GenericUnit.ONE_BUCKET.combineN(3))
    tile.getConnection.getHandler.fill(content, execute = true)

    val handler = getTankCapability(helper, basePos, tile)
    val drained = handler.drain(FluidAmountUtil.BUCKET_WATER.toStack, FluidAction.SIMULATE)
    assertTrue(drained.isEmpty)
    assertEqualHelper(Option(content), tile.getConnection.getContent)
    helper.succeed()
  }
}
