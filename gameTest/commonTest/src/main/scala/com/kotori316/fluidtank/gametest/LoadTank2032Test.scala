package com.kotori316.fluidtank.gametest

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.FluidAmountUtil
import com.kotori316.fluidtank.tank.{BlockTank, PlatformTankAccess, Tier, TileTank}
import com.kotori316.testutil.common.TestFunction
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import org.junit.jupiter.api.Assertions.{assertEquals, assertFalse, assertTrue, fail}

import scala.jdk.StreamConverters.*

class LoadTank2032Test {
  private type TankType = TileTank

  private def getTankByTier(tier: Tier): BlockTank = {
    PlatformTankAccess.getInstance().getTankBlockMap.get(tier).get()
  }

  def assumptionWood(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(0, 1, 0)
    val expectedTier = Tier.WOOD
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        helper.assertBlockPresent(getTankByTier(expectedTier), pos)
      })
      .thenExecuteAfter(2, () => {
        val tile = helper.getBlockEntity(pos, classOf[TankType])
        assertEquals(expectedTier, tile.tier)
        assertFalse(tile.getConnection.isDummy)
      }).thenSucceed()
  }

  def woodTypeContents(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(0, 1, 0)
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        val tile = helper.getBlockEntity(pos, classOf[TankType])
        val content = tile.getConnection.getContent.getOrElse(fail("Content is empty!"))
        assertFalse(content.isEmpty)
        assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3000)), content)
      })
      .thenSucceed()
  }

  def assumptionStone(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(0, 1, 1)
    val expectedTier = Tier.STONE
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        helper.assertBlockPresent(getTankByTier(expectedTier), pos)
      })
      .thenExecuteAfter(2, () => {
        val tile = helper.getBlockEntity(pos, classOf[TankType])
        assertEquals(expectedTier, tile.tier)
        assertFalse(tile.getConnection.isDummy)
      }).thenSucceed()
  }

  def stoneTypeContents(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(0, 1, 1)
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        val tile = helper.getBlockEntity(pos, classOf[TankType])
        val content = tile.getConnection.getContent.getOrElse(fail("Content is empty!"))
        assertFalse(content.isEmpty)
        assertEquals(FluidAmountUtil.BUCKET_LAVA.setAmount(GenericUnit.fromForge(24000)), content)

        val tanks = tile.getConnection.getHandler.getTank
        assertEquals(2, tanks.size)
      })
      .thenSucceed()
  }

  def assumptionCopper(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(1, 1, 2)
    val expectedTier = Tier.COPPER
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        helper.assertBlockPresent(getTankByTier(expectedTier), pos)
      })
      .thenExecuteAfter(2, () => {
        val tile = helper.getBlockEntity(pos, classOf[TankType])
        assertEquals(expectedTier, tile.tier)
        assertFalse(tile.getConnection.isDummy)
      }).thenSucceed()
  }

  def copperTypeContents(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(1, 1, 2)
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        val tile = helper.getBlockEntity(pos, classOf[TankType])
        val content = tile.getConnection.getContent.getOrElse(fail("Content is empty!"))
        assertFalse(content.isEmpty)
        assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(30000)), content)

        val tanks = tile.getConnection.getHandler.getTank
        assertEquals(2, tanks.size)
      }).thenSucceed()
  }

  def assumptionStar(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(1, 1, 1)
    val expectedTier = Tier.STAR
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        helper.assertBlockPresent(getTankByTier(expectedTier), pos)
      })
      .thenExecuteAfter(2, () => {
        val tile = helper.getBlockEntity(pos, classOf[TankType])
        assertEquals(expectedTier, tile.tier)
        assertFalse(tile.getConnection.isDummy)
      }).thenSucceed()
  }

  def starTypeContents(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(1, 1, 1)
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        val tile = helper.getBlockEntity(pos, classOf[TankType])
        val content = tile.getConnection.getContent.getOrElse(fail("Content is empty!"))
        assertFalse(content.isEmpty)
        assertTrue(content.contentEqual(FluidAmountUtil.BUCKET_LAVA))

        val tanks = tile.getConnection.getHandler.getTank
        assertEquals(3, tanks.size)
      })
      .thenSucceed()
  }
}

object LoadTank2032Test {
  def tests(batch: String, structure: String): java.util.stream.Stream[TestFunction] = {
    val instance = new LoadTank2032Test
    instance.getClass.getMethods.toSeq
      .filter(m => m.getParameterTypes sameElements Array(classOf[GameTestHelper]))
      .filter(m => m.getReturnType == Void.TYPE)
      .map { m =>
        GameTestFunctions.create(batch, structure, f"LoadTank2032Test_${m.getName}", g => m.invoke(instance, g))
      }
      .asJavaSeqStream
  }
}
