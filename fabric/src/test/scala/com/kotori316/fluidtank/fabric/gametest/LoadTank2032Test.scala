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

import scala.jdk.javaapi.CollectionConverters

class LoadTank2032Test extends FabricGameTest {
  private final val BATCH = "defaultBatch"

  @GameTestGenerator
  def generator(): java.util.List[TestFunction] = {
    val withHelper = getClass.getDeclaredMethods.toSeq
      .filter(m => m.getReturnType == Void.TYPE)
      .filter(m => !m.isAnnotationPresent(classOf[GameTest]))
      .filter(m => m.getParameterTypes.toSeq == Seq(classOf[GameTestHelper]))
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
    val pos = new BlockPos(0, 2, 0)
    helper.assertBlockPresent(FluidTank.TANK_MAP.get(Tier.WOOD), pos)
    val tile = assertInstanceOf(classOf[TileTankFabric], helper.getBlockEntity(pos))
    assertEquals(Tier.WOOD, tile.tier)
    assertFalse(tile.getConnection.isDummy)
    helper.succeed()
  }

  def woodTypeContents(helper: GameTestHelper): Unit = {
    val tile = assertInstanceOf(classOf[TileTankFabric], helper.getBlockEntity(new BlockPos(0, 2, 0)))
    val content = tile.getConnection.getContent.getOrElse(fail("Content is empty!"))
    assertFalse(content.isEmpty)
    assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(3000)), content)
    helper.succeed()
  }

  def assumptionStone(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(0, 2, 1)
    helper.assertBlockPresent(FluidTank.TANK_MAP.get(Tier.STONE), pos)
    val tile = assertInstanceOf(classOf[TileTankFabric], helper.getBlockEntity(pos))
    assertEquals(Tier.STONE, tile.tier)
    assertFalse(tile.getConnection.isDummy)
    helper.succeed()
  }

  def stoneTypeContents(helper: GameTestHelper): Unit = {
    val tile = assertInstanceOf(classOf[TileTankFabric], helper.getBlockEntity(new BlockPos(0, 2, 1)))
    val content = tile.getConnection.getContent.getOrElse(fail("Content is empty!"))
    assertFalse(content.isEmpty)
    assertEquals(FluidAmountUtil.BUCKET_LAVA.setAmount(GenericUnit.fromForge(24000)), content)

    val tanks = tile.getConnection.getHandler.getTank
    assertEquals(2, tanks.size)
    helper.succeed()
  }

  def assumptionCopper(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(1, 2, 2)
    helper.assertBlockPresent(FluidTank.TANK_MAP.get(Tier.COPPER), pos)
    val tile = assertInstanceOf(classOf[TileTankFabric], helper.getBlockEntity(pos))
    assertEquals(Tier.COPPER, tile.tier)
    assertFalse(tile.getConnection.isDummy)
    assertFalse(tile.getConnection.hasCreative)
    helper.succeed()
  }

  def copperTypeContents(helper: GameTestHelper): Unit = {
    val tile = assertInstanceOf(classOf[TileTankFabric], helper.getBlockEntity(new BlockPos(1, 2, 2)))
    val content = tile.getConnection.getContent.getOrElse(fail("Content is empty!"))
    assertFalse(content.isEmpty)
    assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(30000)), content)

    val tanks = tile.getConnection.getHandler.getTank
    assertEquals(2, tanks.size)
    helper.succeed()
  }

  def assumptionStar(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(1, 2, 1)
    helper.assertBlockPresent(FluidTank.TANK_MAP.get(Tier.STAR), pos)
    val tile = assertInstanceOf(classOf[TileTankFabric], helper.getBlockEntity(pos))
    assertEquals(Tier.STAR, tile.tier)
    assertFalse(tile.getConnection.isDummy)
    assertTrue(tile.getConnection.hasCreative)
    assertFalse(tile.getConnection.hasVoid)
    helper.succeed()
  }

  def starTypeContents(helper: GameTestHelper): Unit = {
    val tile = assertInstanceOf(classOf[TileTankFabric], helper.getBlockEntity(new BlockPos(1, 2, 1)))
    val content = tile.getConnection.getContent.getOrElse(fail("Content is empty!"))
    assertFalse(content.isEmpty)
    assertTrue(content.contentEqual(FluidAmountUtil.BUCKET_LAVA))

    val tanks = tile.getConnection.getHandler.getTank
    assertEquals(3, tanks.size)
    helper.succeed()
  }
}
