package com.kotori316.fluidtank.fabric.gametest

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fabric.FluidTank
import com.kotori316.fluidtank.fabric.tank.TileTankFabric
import com.kotori316.fluidtank.fluids.FluidAmountUtil
import com.kotori316.fluidtank.tank.Tier
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.{GameTest, GameTestGenerator, GameTestHelper, TestFunction}
import org.junit.jupiter.api.Assertions.{assertEquals, assertFalse, assertInstanceOf, assertTrue, fail}
import org.junit.platform.commons.support.ReflectionSupport

import java.lang.reflect.Modifier
import scala.jdk.javaapi.CollectionConverters

class LoadTank2032Test extends FabricGameTest {
  private final val BATCH = "defaultBatch"
  private type TankType = TileTankFabric

  @GameTestGenerator
  def generator(): java.util.List[TestFunction] = {
    val withHelper = getClass.getDeclaredMethods.toSeq
      .filter(m => m.getReturnType == Void.TYPE)
      .filter(m => !m.isAnnotationPresent(classOf[GameTest]))
      .filter(m => m.getParameterTypes.toSeq == Seq(classOf[GameTestHelper]))
      .filter(m => (Modifier.PRIVATE & m.getModifiers) == 0)
      .map { m =>
        val test: java.util.function.Consumer[GameTestHelper] = g => ReflectionSupport.invokeMethod(m, this, g)
        GameTestUtil.createWithStructure(FluidTankCommon.modId, BATCH, getClass.getSimpleName + "_" + m.getName,
          "load_20_3_tanks",
          test
        )
      }

    CollectionConverters.asJava(withHelper)
  }

  def assumptionWood(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(0, 1, 0)
    val expectedTier = Tier.WOOD
    helper.startSequence()
      .thenExecuteAfter(2, () => {
        helper.assertBlockPresent(FluidTank.TANK_MAP.get(expectedTier), pos)
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
        helper.assertBlockPresent(FluidTank.TANK_MAP.get(expectedTier), pos)
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
        helper.assertBlockPresent(FluidTank.TANK_MAP.get(expectedTier), pos)
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
        helper.assertBlockPresent(FluidTank.TANK_MAP.get(expectedTier), pos)
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
