package com.kotori316.fluidtank.fabric.gametest

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fabric.FluidTank
import com.kotori316.fluidtank.fabric.tank.ConnectionStorage
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, PotionType}
import com.kotori316.fluidtank.tank.Tier
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.fabricmc.fabric.api.transfer.v1.fluid.{FluidConstants, FluidStorage, FluidVariant}
import net.fabricmc.fabric.api.transfer.v1.storage.{Storage, StorageUtil}
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.{GameTest, GameTestGenerator, GameTestHelper, TestFunction}
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.material.Fluids
import org.junit.jupiter.api.Assertions.*
import org.junit.platform.commons.support.ReflectionSupport

import java.lang.reflect.Modifier
import scala.jdk.javaapi.CollectionConverters
import scala.util.Using

//noinspection UnstableApiUsage
final class ConnectionStorageTest extends FabricGameTest {
  @GameTestGenerator
  def generator: java.util.List[TestFunction] = {
    val batch = "connection_storage_test"
    val withHelper = getClass.getDeclaredMethods.toSeq
      .filter(m => m.getReturnType == Void.TYPE)
      .filter(m => !m.isAnnotationPresent(classOf[GameTest]))
      .filter(m => (m.getModifiers & Modifier.PRIVATE) == 0)
      .filter(m => m.getParameterTypes.toSeq == Seq(classOf[GameTestHelper]))
      .map { m =>
        val test: java.util.function.Consumer[GameTestHelper] = g => ReflectionSupport.invokeMethod(m, this, g)
        GameTestUtil.create(FluidTankCommon.modId, batch, getClass.getSimpleName + "_" + m.getName,
          test
        )
      }

    CollectionConverters.asJava(withHelper)
  }

  private def getStorage(helper: GameTestHelper, pos: BlockPos) = {
    val storage = FluidStorage.SIDED.find(helper.getLevel, helper.absolutePos(pos), null)
    if (storage == null) GameTestUtil.throwExceptionAt(helper, pos, "Storage must be presented")
    assertInstanceOf(classOf[ConnectionStorage], storage)
  }

  def getInstance(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    helper.setBlock(pos, FluidTank.TANK_MAP.get(Tier.WOOD))
    assertNotNull(getStorage(helper, pos))
    helper.succeed()
  }

  def capacity1(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    val storage = getStorage(helper, pos)
    assertEquals(4 * FluidConstants.BUCKET, storage.getCapacity)
    assertEquals(0, storage.getAmount)
    assertTrue(storage.isResourceBlank)
    assertEquals(FluidVariant.blank, storage.getResource)
    helper.succeed()
  }

  def capacity2(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val storage = getStorage(helper, pos)
    assertEquals(20 * FluidConstants.BUCKET, storage.getCapacity)
    assertEquals(0, storage.getAmount)
    assertTrue(storage.isResourceBlank)
    assertEquals(FluidVariant.blank, storage.getResource)
    helper.succeed()
  }

  def capacity3(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.CREATIVE)
    val storage = getStorage(helper, pos)
    assertEquals(GenericUnit.CREATIVE_TANK, GenericUnit(storage.getCapacity), "Connection capacity must not be over GenericUnit.CREATIVE_TANK")
    helper.succeed()
  }

  def amountWithCreative1(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tank = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.CREATIVE)
    tank.getConnection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)
    val storage = getStorage(helper, pos)
    assertEquals(GenericUnit.CREATIVE_TANK, GenericUnit(storage.getAmount), "Connection capacity must not be over GenericUnit.CREATIVE_TANK")
    helper.succeed()
  }

  def amountWithCreative2(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tank = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above(1), Tier.CREATIVE)
    GameTestUtil.placeTank(helper, pos.above(2), Tier.CREATIVE)
    tank.getConnection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)
    val storage = getStorage(helper, pos)
    assertEquals(GenericUnit.CREATIVE_TANK, GenericUnit(storage.getAmount), "Connection capacity must not be over GenericUnit.CREATIVE_TANK")
    helper.succeed()
  }

  def fillToEmpty(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val storage = getStorage(helper, pos)
    val filled = Using(Transaction.openOuter()) { transaction =>
      val filled = storage.insert(FluidVariant.of(Fluids.WATER), 6 * FluidConstants.BUCKET, transaction)
      transaction.commit()
      filled
    }.get

    assertEquals(6 * FluidConstants.BUCKET, filled)
    val connection = tile.getConnection
    assertEquals(GenericUnit.fromForge(20000), connection.capacity)
    assertEquals(GenericUnit.fromForge(6000), connection.amount)
    assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(6000)), connection.getContent.get)
    helper.succeed()
  }

  def fillToFilled(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val connection = tile.getConnection
    connection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)
    val storage = getStorage(helper, pos)
    val filled = Using(Transaction.openOuter()) { transaction =>
      val filled = storage.insert(FluidVariant.of(Fluids.WATER), 6 * FluidConstants.BUCKET, transaction)
      transaction.commit()
      filled
    }.get

    assertEquals(6 * FluidConstants.BUCKET, filled)
    assertEquals(GenericUnit.fromForge(20000), connection.capacity)
    assertEquals(GenericUnit.fromForge(7000), connection.amount)
    assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(7000)), connection.getContent.get)
    helper.succeed()
  }

  def fillToFilled2(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val connection = tile.getConnection
    connection.getHandler.fill(FluidAmountUtil.BUCKET_LAVA, execute = true)
    val storage = getStorage(helper, pos)
    val filled = Using(Transaction.openOuter()) { transaction =>
      val filled = storage.insert(FluidVariant.of(Fluids.WATER), 6 * FluidConstants.BUCKET, transaction)
      transaction.commit()
      filled
    }.get

    assertEquals(0, filled)
    assertEquals(GenericUnit.fromForge(20000), connection.capacity)
    assertEquals(FluidAmountUtil.BUCKET_LAVA.setAmount(GenericUnit.fromForge(1000)), connection.getContent.get)
    helper.succeed()
  }

  def fillToFilled3(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val connection = tile.getConnection
    connection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)
    val storage = getStorage(helper, pos)
    val filled = Using(Transaction.openOuter()) { transaction =>
      val filled = storage.insert(FluidVariant.of(Fluids.WATER), 40 * FluidConstants.BUCKET, transaction)
      transaction.commit()
      filled
    }.get

    assertEquals(19 * FluidConstants.BUCKET, filled)
    assertEquals(GenericUnit.fromForge(20000), connection.capacity)
    assertEquals(GenericUnit.fromForge(20000), connection.amount)
    assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(20000)), connection.getContent.get)
    helper.succeed()
  }

  def abort1(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val storage = getStorage(helper, pos)
    val filled = Using(Transaction.openOuter()) { transaction =>
      val filled = storage.insert(FluidVariant.of(Fluids.WATER), 6 * FluidConstants.BUCKET, transaction)
      transaction.abort()
      filled
    }.get

    assertEquals(6 * FluidConstants.BUCKET, filled)
    val connection = tile.getConnection
    assertEquals(GenericUnit.fromForge(20000), connection.capacity)
    assertEquals(GenericUnit.fromForge(0), connection.amount)
    assertTrue(connection.getContent.isEmpty)
    helper.succeed()
  }

  def abort2(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val connection = tile.getConnection
    connection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)
    val storage = getStorage(helper, pos)
    val filled = Using(Transaction.openOuter()) { transaction =>
      val filled = storage.insert(FluidVariant.of(Fluids.WATER), 6 * FluidConstants.BUCKET, transaction)
      transaction.abort()
      filled
    }.get

    assertEquals(6 * FluidConstants.BUCKET, filled)
    assertEquals(GenericUnit.fromForge(20000), connection.capacity)
    assertEquals(GenericUnit.fromForge(1000), connection.amount)
    assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(1000)), connection.getContent.get)
    helper.succeed()
  }

  def drainFromEmpty(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val storage = getStorage(helper, pos)
    val drained = Using(Transaction.openOuter()) { transaction =>
      val drained = storage.extract(FluidVariant.of(Fluids.WATER), 6 * FluidConstants.BUCKET, transaction)
      transaction.abort()
      drained
    }.get

    assertEquals(0, drained)
    helper.succeed()
  }

  def drainFromFilled1(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val connection = tile.getConnection
    connection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)
    val storage = getStorage(helper, pos)
    val drained = Using(Transaction.openOuter()) { transaction =>
      val drained = storage.extract(FluidVariant.of(Fluids.WATER), 6 * FluidConstants.BUCKET, transaction)
      transaction.commit()
      drained
    }.get

    assertEquals(FluidConstants.BUCKET, drained)
    assertTrue(connection.getContent.isEmpty)
    helper.succeed()
  }

  def drainFromFilled2(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val connection = tile.getConnection
    connection.getHandler.fill(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(20000)), execute = true)
    val storage = getStorage(helper, pos)
    val drained = Using(Transaction.openOuter()) { transaction =>
      val drained = storage.extract(FluidVariant.of(Fluids.WATER), 19 * FluidConstants.BUCKET, transaction)
      transaction.commit()
      drained
    }.get

    assertEquals(FluidConstants.BUCKET * 19, drained)
    assertEquals(Option.apply(FluidAmountUtil.BUCKET_WATER), connection.getContent)
    helper.succeed()
  }

  def drainFromFilled3(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    GameTestUtil.placeTank(helper, pos.above, Tier.STONE)
    val connection = tile.getConnection
    connection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)
    val storage = getStorage(helper, pos)
    val drained = Using(Transaction.openOuter()) { transaction =>
      val drained = storage.extract(FluidVariant.of(Fluids.LAVA), 6 * FluidConstants.BUCKET, transaction)
      transaction.commit()
      drained
    }.get

    assertEquals(0, drained)
    assertEquals(Option.apply(FluidAmountUtil.BUCKET_WATER), connection.getContent)
    helper.succeed()
  }

  def potionStorage(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    val storage = getStorage(helper, pos)
    tile.getConnection.getHandler.fill(FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BUCKET), execute = true)
    assertTrue(storage.isResourceBlank)
    assertEquals(4 * FluidConstants.BUCKET, storage.getCapacity)
    assertEquals(0, storage.getAmount)
    helper.succeed()
  }

  def fillPotionStorage(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    val storage: Storage[FluidVariant] = getStorage(helper, pos)
    tile.getConnection.getHandler.fill(FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BUCKET), execute = true)
    val filled = StorageUtil.simulateInsert(storage, FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, null)
    assertEquals(0, filled)
    helper.succeed()
  }

  def drainPotionStorage(helper: GameTestHelper): Unit = {
    val pos = BlockPos.ZERO.above
    val tile = GameTestUtil.placeTank(helper, pos, Tier.WOOD)
    val storage: Storage[FluidVariant] = getStorage(helper, pos)
    tile.getConnection.getHandler.fill(FluidAmountUtil.from(PotionType.NORMAL, Potions.INVISIBILITY, GenericUnit.ONE_BUCKET), execute = true)
    val drained = StorageUtil.simulateExtract(storage, FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, null)
    assertEquals(0, drained)
    helper.succeed()
  }
}
