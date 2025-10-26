package com.kotori316.fluidtank.neoforge.gametest

import cats.implicits.catsSyntaxSemigroup
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, PotionType}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import com.kotori316.fluidtank.neoforge.gametest.GetGameTestMethods.assertEqualHelper
import com.kotori316.fluidtank.neoforge.tank.TileTankNeoForge
import com.kotori316.fluidtank.tank.Tier
import com.kotori316.testutil.common.TestFunction
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.alchemy.Potions
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.Transaction
import org.junit.jupiter.api.Assertions.{assertDoesNotThrow, assertEquals, assertNotNull, assertTrue}

import scala.util.Using

class TankFluidHandlerTest {
  private final val BATCH = GetGameTestMethods.DEFAULT_BATCH

  def generator(): java.util.List[TestFunction] = {
    GetGameTestMethods.getTests(getClass, this, BATCH)
  }

  private def getTankCapability(helper: GameTestHelper, pos: BlockPos, tile: TileTankNeoForge): ResourceHandler[FluidResource] = {
    val h = assertDoesNotThrow(() => helper.getLevel.getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), null, tile, null))
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
    assertEquals(4000, cap.getCapacityAsLong(0, FluidResource.EMPTY))
    helper.succeed()
  }

  def amount1(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    tile.getConnection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)

    val cap = getTankCapability(helper, basePos, tile)
    assertEquals(FluidAmountUtil.BUCKET_WATER, NeoForgeConverter.toAmount(cap.getResource(0), cap.getAmountAsLong(0)))
    helper.succeed()
  }

  def fillSimulate1(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    val handler = getTankCapability(helper, basePos, tile)
    assertEquals(4000, handler.getCapacityAsLong(0, FluidResource.EMPTY))

    val filled: Int = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.insert(FluidAmountUtil.BUCKET_WATER.asVariant, FluidAmountUtil.BUCKET_WATER.amount.asForge, tx)
      tx.close()
      d
    }
    assertEquals(1000, filled)
    assertEquals(GenericUnit.ZERO, tile.getConnection.amount)
    helper.succeed()
  }

  def fillExecute1(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    TankTest.placeTank(helper, basePos.above(), Tier.STONE)
    val handler = getTankCapability(helper, basePos, tile)
    assertEquals(20000, handler.getCapacityAsLong(0, FluidResource.EMPTY))

    val toFill = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(20000))
    val filled: Int = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.insert(toFill.asVariant, toFill.amount.asForge, tx)
      tx.commit()
      d
    }
    assertEquals(20000, filled)
    assertEqualHelper(Option(toFill), tile.getConnection.getContent)
    helper.succeed()
  }

  def drainSimulate1(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    val handler = getTankCapability(helper, basePos, tile)
    assertEquals(4000, handler.getCapacityAsLong(0, FluidResource.EMPTY))
    val content = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3000))
    tile.getConnection.getHandler.fill(content, execute = true)
    assertEquals(3000L, handler.getAmountAsLong(0))

    val toDrain = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000))
    val drained = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.extract(toDrain.asVariant, toDrain.amount.asForge, tx)
      tx.close()
      d
    }
    assertEquals(toDrain.amount.asForge, drained)
    assertEquals(3000L, handler.getAmountAsLong(0))
    helper.succeed()
  }

  def drainExecute1(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    val handler = getTankCapability(helper, basePos, tile)
    assertEquals(4000, handler.getCapacityAsLong(0, FluidResource.EMPTY))
    val content = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3000))
    tile.getConnection.getHandler.fill(content, execute = true)
    assertEquals(3000L, handler.getAmountAsLong(0))

    val toDrain = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000))
    val drained = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.extract(toDrain.asVariant, toDrain.amount.asForge, tx)
      tx.commit()
      d
    }
    assertEquals(toDrain.amount.asForge, drained)
    assertEquals(1000L, handler.getAmountAsLong(0))
    helper.succeed()
  }

  def potionTank(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    TankTest.placeTank(helper, basePos.above(), Tier.STONE)
    tile.getConnection.getHandler.fill(FluidAmountUtil.from(PotionType.SPLASH, Potions.NIGHT_VISION, GenericUnit.ONE_BUCKET), execute = true)

    val handler = getTankCapability(helper, basePos, tile)
    assertEquals(20000, handler.getCapacityAsLong(0, FluidResource.EMPTY))
    assertTrue(handler.getResource(0).isEmpty)
    helper.succeed()
  }

  def potionTank2(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above()
    val tile = TankTest.placeTank(helper, basePos, Tier.WOOD).asInstanceOf[TileTankNeoForge]
    TankTest.placeTank(helper, basePos.above(), Tier.STONE)
    val content = FluidAmountUtil.from(PotionType.SPLASH, Potions.NIGHT_VISION, GenericUnit.ONE_BUCKET.combineN(3))
    tile.getConnection.getHandler.fill(content, execute = true)

    val handler = getTankCapability(helper, basePos, tile)
    val filled: Int = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.extract(FluidAmountUtil.BUCKET_WATER.asVariant, FluidAmountUtil.BUCKET_WATER.amount.asForge, tx)
      tx.close()
      d
    }
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
    val drained = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.extract(FluidAmountUtil.BUCKET_WATER.asVariant, FluidAmountUtil.BUCKET_WATER.amount.asForge, tx)
      tx.commit()
      d
    }
    assertEquals(0, drained)
    assertEqualHelper(Option(content), tile.getConnection.getContent)
    helper.succeed()
  }
}
