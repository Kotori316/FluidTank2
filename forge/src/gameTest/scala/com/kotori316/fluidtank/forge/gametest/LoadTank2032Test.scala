package com.kotori316.fluidtank.forge.gametest

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.FluidAmountUtil
import com.kotori316.fluidtank.forge.FluidTank
import com.kotori316.fluidtank.forge.tank.TileTankForge
import com.kotori316.fluidtank.tank.Tier
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.{GameTestGenerator, GameTestHelper, TestFunction}
import net.minecraftforge.gametest.GameTestHolder
import org.junit.jupiter.api.Assertions.*

//noinspection DuplicatedCode,ScalaUnusedSymbol
@GameTestHolder(FluidTankCommon.modId)
class LoadTank2032Test {
  private final val BATCH = "loadTank2032"
  private type TankType = TileTankForge

  @GameTestGenerator
  def generator(): java.util.List[TestFunction] = {
    GetGameTestMethods.getTests(getClass, this, BATCH, "load_20_3_tanks")
  }

  def assumptionWood(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(0, 1, 0)
    val expectedTier = Tier.WOOD
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        helper.assertBlockPresent(FluidTank.TANK_MAP.get(expectedTier).get(), pos)
      })
      .thenExecuteAfter(2, () => {
        val tile = assertInstanceOf(classOf[TankType], helper.getBlockEntity(pos))
        assertEquals(expectedTier, tile.tier)
        assertFalse(tile.getConnection.isDummy)
      }).thenSucceed()
  }

  def woodTypeContents(helper: GameTestHelper): Unit = {
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        val tile = assertInstanceOf(classOf[TankType], helper.getBlockEntity(new BlockPos(0, 1, 0)))
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
        helper.assertBlockPresent(FluidTank.TANK_MAP.get(expectedTier).get(), pos)
      })
      .thenExecuteAfter(2, () => {
        val tile = assertInstanceOf(classOf[TankType], helper.getBlockEntity(pos))
        assertEquals(expectedTier, tile.tier)
        assertFalse(tile.getConnection.isDummy)
      }).thenSucceed()
  }

  def stoneTypeContents(helper: GameTestHelper): Unit = {
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        val tile = assertInstanceOf(classOf[TankType], helper.getBlockEntity(new BlockPos(0, 1, 1)))
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
        helper.assertBlockPresent(FluidTank.TANK_MAP.get(expectedTier).get(), pos)
      })
      .thenExecuteAfter(2, () => {
        val tile = assertInstanceOf(classOf[TankType], helper.getBlockEntity(pos))
        assertEquals(expectedTier, tile.tier)
        assertFalse(tile.getConnection.isDummy)
      }).thenSucceed()
  }

  def copperTypeContents(helper: GameTestHelper): Unit = {
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        val tile = assertInstanceOf(classOf[TankType], helper.getBlockEntity(new BlockPos(1, 1, 2)))
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
        helper.assertBlockPresent(FluidTank.TANK_MAP.get(expectedTier).get(), pos)
      })
      .thenExecuteAfter(2, () => {
        val tile = assertInstanceOf(classOf[TankType], helper.getBlockEntity(pos))
        assertEquals(expectedTier, tile.tier)
        assertFalse(tile.getConnection.isDummy)
      }).thenSucceed()
  }

  def starTypeContents(helper: GameTestHelper): Unit = {
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        val tile = assertInstanceOf(classOf[TankType], helper.getBlockEntity(new BlockPos(1, 1, 1)))
        val content = tile.getConnection.getContent.getOrElse(fail("Content is empty!"))
        assertFalse(content.isEmpty)
        assertTrue(content.contentEqual(FluidAmountUtil.BUCKET_LAVA))

        val tanks = tile.getConnection.getHandler.getTank
        assertEquals(3, tanks.size)
      })
      .thenSucceed()
  }
}
