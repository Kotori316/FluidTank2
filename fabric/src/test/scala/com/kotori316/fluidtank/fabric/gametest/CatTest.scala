package com.kotori316.fluidtank.fabric.gametest

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fabric.cat.ChestAsTankStorage
import com.kotori316.fluidtank.fabric.fluid.FabricConverter
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil}
import com.kotori316.fluidtank.gametest.GameTestFunctions
import com.kotori316.testutil.common.TestFunction
import net.fabricmc.fabric.api.transfer.v1.fluid.{FluidConstants, FluidStorage, FluidVariant}
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.{Item, Items}
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.material.Fluids
import org.junit.jupiter.api.Assertions.{assertEquals, assertInstanceOf, assertNotNull, assertTrue}
import org.junit.platform.commons.support.ReflectionSupport

import java.lang.reflect.Modifier
import scala.jdk.javaapi.CollectionConverters
import scala.util.Using

//noinspection UnstableApiUsage
final class CatTest {
  private final val BATCH = "cat_test"

  def tests: java.util.List[TestFunction] = {
    val testFunctions = generator ++ fillMore() ++ fillFail() ++ drainWater()
    CollectionConverters.asJava(testFunctions)
  }

  def generator: Seq[TestFunction] = {
    val withHelper = getClass.getDeclaredMethods.toSeq
      .filter(m => m.getReturnType == Void.TYPE)
      .filter(m => (m.getModifiers & Modifier.PRIVATE) == 0)
      .filter(m => m.getParameterTypes.toSeq == Seq(classOf[GameTestHelper]))
      .map { m =>
        val test: java.util.function.Consumer[GameTestHelper] = g => ReflectionSupport.invokeMethod(m, this, g)
        GameTestFunctions.create(BATCH, "cat_test", getClass.getSimpleName + "_" + m.getName, test)
      }

    withHelper
  }

  private def getStorage(helper: GameTestHelper) = {
    val pos = new BlockPos(2, 1, 2)
    val storage = FluidStorage.SIDED.find(helper.getLevel, helper.absolutePos(pos), null)
    if (storage == null) GameTestUtil.throwExceptionAt(helper, pos, "Storage must be presented")
    assertInstanceOf(classOf[ChestAsTankStorage], storage)
  }

  def testGetFluids(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(2, 1, 2)
    val fluids = CollectionConverters.asScala(ChestAsTankStorage.getCATFluids(helper.getLevel, helper.absolutePos(pos)))
    assertTrue(fluids.contains(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000))),
      s"CAT should recognize fluids, $fluids")
    assertTrue(fluids.contains(FluidAmountUtil.BUCKET_LAVA.setAmount(GenericUnit.fromForge(3000))),
      s"CAT should recognize fluids, $fluids")

    helper.succeed()
  }

  def fillLava(helper: GameTestHelper): Unit = {
    val handler = getStorage(helper)

    Using(Transaction.openOuter()) { tr =>
      val filled = handler.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tr)
      assertEquals(FluidConstants.BUCKET, filled)
      tr.commit()
    }

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)))
    assertNotNull(chest)

    assertEquals(4, chest.countItem(Items.LAVA_BUCKET))

    helper.succeed()
  }

  def fillWater(helper: GameTestHelper): Unit = {
    val handler = getStorage(helper)

    Using(Transaction.openOuter()) { tr =>
      val filled = handler.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tr)
      assertEquals(FluidConstants.BUCKET, filled)
      tr.commit()
    }

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)))
    assertNotNull(chest)

    assertEquals(3, chest.countItem(Items.WATER_BUCKET))

    helper.succeed()
  }

  def fillMore(): Seq[TestFunction] = {
    val t = for {
      rot <- Rotation.values().toSeq
      kind <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
      amount <- 2000 to 4000 by 1000
      fluid = kind.setAmount(GenericUnit.fromForge(amount))
      bucket = if (kind.contentEqual(FluidAmountUtil.BUCKET_WATER)) Items.WATER_BUCKET else Items.LAVA_BUCKET
      count = if (kind.contentEqual(FluidAmountUtil.BUCKET_WATER)) 4 else 5
    } yield {
      GameTestFunctions.create(BATCH, "cat_test", s"cat_test_${kind.content.getKey.getPath}_${amount}_${rot.name()}", g => fillMore(g, fluid, count, bucket))
    }
    t
  }

  private def fillMore(helper: GameTestHelper, fluid: FluidAmount, expectItemCount: Int, expectItem: Item): Unit = {
    try {
      val handler = getStorage(helper)

      Using(Transaction.openOuter()) { tr =>
        val filled = handler.insert(FabricConverter.toVariant(fluid, Fluids.EMPTY), FabricConverter.fabricAmount(fluid), tr)
        assertEquals(2 * FluidConstants.BUCKET, filled)
        tr.commit()
      }

      val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)))
      assertNotNull(chest)

      assertEquals(expectItemCount, chest.countItem(expectItem))
      helper.succeed()
    } catch {
      case e: AssertionError =>
        val ee = new RuntimeException(e.getMessage)
        ee.addSuppressed(e)
        throw ee
    }
  }

  def fillFail(): Seq[TestFunction] = {
    for {
      kind <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
      a <- Seq(0, 500, 999)
      amount = GenericUnit.fromForge(a)
      fluid = kind.setAmount(amount)
    } yield GameTestFunctions.create(BATCH, "cat_test", s"cat_test_fill_fail_${fluid.content.getKey.getPath}_$a", g => fillFail(g, fluid))
  }

  private def fillFail(helper: GameTestHelper, amount: FluidAmount): Unit = {
    val storage = getStorage(helper)
    Using(Transaction.openOuter()) { tr =>
      val filled = storage.insert(FabricConverter.toVariant(amount, Fluids.EMPTY), FabricConverter.fabricAmount(amount), tr)
      assertEquals(0, filled)
    }
    helper.succeed()
  }

  def fillSimulate(helper: GameTestHelper): Unit = {
    val toFill = FluidAmountUtil.BUCKET_WATER
    val handler = getStorage(helper)

    Using(Transaction.openOuter()) { tr =>
      val filled = handler.insert(FabricConverter.toVariant(toFill, Fluids.EMPTY), FabricConverter.fabricAmount(toFill), tr)
      assertEquals(FluidConstants.BUCKET, filled)
      tr.abort()
    }

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)))
    assertEquals(2, chest.countItem(Items.BUCKET))
    assertEquals(2, chest.countItem(Items.WATER_BUCKET))

    helper.succeed()
  }

  def drainWater(): Seq[TestFunction] = {
    for {
      a <- 1000 to 3000 by 500
    } yield {
      val waterBucket = math.max(2 - a / 1000, 0)
      val emptyBucket = 4 - waterBucket
      val drained = math.min(1000 * (a / 1000), 2000)
      val toDrain = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(a))
      GameTestFunctions.create(BATCH, "cat_test", s"cat_test_drain_${toDrain.content.getKey.getPath}_$a", g => drainWater(g, toDrain, waterBucket, emptyBucket, drained))
    }
  }

  private def drainWater(helper: GameTestHelper, toDrain: FluidAmount, filledBucket: Int, emptyBucket: Int, drainedAmount: Int): Unit = {
    val handler = getStorage(helper)

    Using(Transaction.openOuter()) { tr =>
      val drained = handler.extract(FabricConverter.toVariant(toDrain, Fluids.EMPTY), FabricConverter.fabricAmount(toDrain), tr)
      assertEquals(drainedAmount * 81, drained)
      tr.commit()
    }

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)))
    assertEquals(emptyBucket, chest.countItem(Items.BUCKET))
    assertEquals(filledBucket, chest.countItem(Items.WATER_BUCKET))
    helper.succeed()
  }

  def drainWater2(helper: GameTestHelper): Unit = {
    val toDrain = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000))
    val handler = getStorage(helper)

    Using(Transaction.openOuter()) { tr =>
      val drained = handler.extract(FabricConverter.toVariant(toDrain, Fluids.EMPTY), FabricConverter.fabricAmount(toDrain), tr)
      assertEquals(FabricConverter.fabricAmount(toDrain), drained)
      tr.commit()
    }

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)))
    assertEquals(4, chest.countItem(Items.BUCKET))
    assertEquals(0, chest.countItem(Items.WATER_BUCKET))
    helper.succeed()
  }

  def drainLava(helper: GameTestHelper): Unit = {
    val toDrain = FluidAmountUtil.BUCKET_LAVA
    val handler = getStorage(helper)

    Using(Transaction.openOuter()) { tr =>
      val drained = handler.extract(FabricConverter.toVariant(toDrain, Fluids.EMPTY), FabricConverter.fabricAmount(toDrain), tr)
      assertEquals(FabricConverter.fabricAmount(toDrain), drained)
      tr.commit()
    }

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)))
    assertEquals(3, chest.countItem(Items.BUCKET))
    assertEquals(2, chest.countItem(Items.LAVA_BUCKET))
    helper.succeed()
  }

}
