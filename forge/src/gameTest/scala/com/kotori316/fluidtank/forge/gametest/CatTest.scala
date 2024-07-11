package com.kotori316.fluidtank.forge.gametest

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil}
import com.kotori316.fluidtank.forge.cat.EntityChestAsTank
import com.kotori316.fluidtank.forge.fluid.ForgeConverter.{FluidAmount2FluidStack, FluidStack2FluidAmount}
import com.kotori316.testutil.GameTestUtil
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.{GameTestGenerator, GameTestHelper, TestFunction}
import net.minecraft.world.item.{Item, Items}
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.fluids.capability.IFluidHandler
import org.junit.jupiter.api.Assertions.{assertEquals, assertInstanceOf, assertNotNull, assertTrue}

import java.util.Locale
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.jdk.OptionConverters.RichOptional

//noinspection ScalaUnusedSymbol,DuplicatedCode
// @GameTestHolder(FluidTankCommon.modId)
class CatTest {
  private final val BATCH = "defaultBatch"

  @GameTestGenerator
  def generator(): java.util.List[TestFunction] = {
    GetGameTestMethods.getTests(getClass, this, BATCH, "cat_test")
  }

  def testGetFluids(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(2, 2, 2)
    val cat = assertInstanceOf(classOf[EntityChestAsTank], helper.getBlockEntity(pos))

    cat.getCapability(ForgeCapabilities.FLUID_HANDLER)
    val fluids = cat.getFluids.toScala.toSeq.flatMap(_.asScala)
    assertTrue(fluids.contains(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000))),
      s"CAT should recognize fluids, $fluids")
    assertTrue(fluids.contains(FluidAmountUtil.BUCKET_LAVA.setAmount(GenericUnit.fromForge(3000))),
      s"CAT should recognize fluids, $fluids")

    helper.succeed()
  }

  private def getHandler(helper: GameTestHelper): IFluidHandler = {
    val pos = new BlockPos(2, 2, 2)
    val cat = assertInstanceOf(classOf[EntityChestAsTank], helper.getBlockEntity(pos))

    cat.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(() => {
      GameTestUtil.throwExceptionAt(helper, pos, "CAT must create proxy handler")
      new AssertionError()
    })
  }

  def fillLava(helper: GameTestHelper): Unit = {
    val handler: IFluidHandler = getHandler(helper)

    val filled = handler.fill(FluidAmountUtil.BUCKET_LAVA.toStack, IFluidHandler.FluidAction.EXECUTE)
    assertEquals(1000, filled)

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 2, 2)))
    assertNotNull(chest)

    assertEquals(4, chest.countItem(Items.LAVA_BUCKET))

    helper.succeed()
  }

  def fillWater(helper: GameTestHelper): Unit = {
    val handler = getHandler(helper)

    val filled = handler.fill(FluidAmountUtil.BUCKET_WATER.toStack, IFluidHandler.FluidAction.EXECUTE)
    assertEquals(1000, filled)

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 2, 2)))
    assertNotNull(chest)

    assertEquals(3, chest.countItem(Items.WATER_BUCKET))

    helper.succeed()
  }

  @GameTestGenerator
  def fillMore(): java.util.List[TestFunction] = {
    val t = for {
      rot <- Rotation.values().toSeq
      kind <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
      amount <- 2000 to 4000 by 1000
      fluid = kind.setAmount(GenericUnit.fromForge(amount))
      bucket = if (kind.contentEqual(FluidAmountUtil.BUCKET_WATER)) Items.WATER_BUCKET else Items.LAVA_BUCKET
      count = if (kind.contentEqual(FluidAmountUtil.BUCKET_WATER)) 4 else 5
    } yield {
      new TestFunction(FluidTankCommon.modId + "." + BATCH,
        s"cat_test_${kind.content.getKey.getPath}_${amount}_${rot.name()}".toLowerCase(Locale.ROOT),
        FluidTankCommon.modId + ":" + FluidTankCommon.modId + "." + "cat_test", rot, 100, 0, true,
        g => fillMore(g, fluid, count, bucket))
    }
    t.asJava
  }

  private def fillMore(helper: GameTestHelper, fluid: FluidAmount, expectItemCount: Int, expectItem: Item): Unit = {
    try {
      val handler = getHandler(helper)

      val filled = handler.fill(fluid.toStack, IFluidHandler.FluidAction.EXECUTE)
      assertEquals(2000, filled)

      val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 2, 2)))
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

  @GameTestGenerator
  def fillFail(): java.util.List[TestFunction] = {
    val t = for {
      kind <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
      a <- Seq(0, 500, 999)
      amount = GenericUnit.fromForge(a)
      fluid = kind.setAmount(amount)
    } yield GameTestUtil.createWithStructure(FluidTankCommon.modId, BATCH,
      s"cat_test_fill_fail_${fluid.content.getKey.getPath}_$a".toLowerCase(Locale.ROOT),
      "cat_test", g => fillFail(g, fluid)
    )
    t.asJava
  }

  private def fillFail(helper: GameTestHelper, amount: FluidAmount): Unit = {
    val filled: Int = getHandler(helper).fill(amount.toStack, IFluidHandler.FluidAction.SIMULATE)
    assertEquals(0, filled)

    helper.succeed()
  }

  def fillSimulate(helper: GameTestHelper): Unit = {
    val toFill = FluidAmountUtil.BUCKET_WATER
    val handler = getHandler(helper)
    val filled = handler.fill(toFill.toStack, IFluidHandler.FluidAction.SIMULATE)

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 2, 2)))
    assertEquals(2, chest.countItem(Items.BUCKET))
    assertEquals(2, chest.countItem(Items.WATER_BUCKET))

    helper.succeed()
  }

  @GameTestGenerator
  def drainWater(): java.util.List[TestFunction] = {
    val t = for {
      a <- 1000 to 3000 by 500
    } yield {
      val waterBucket = math.max(2 - a / 1000, 0)
      val emptyBucket = 4 - waterBucket
      val drained = math.min(1000 * (a / 1000), 2000)
      val toDrain = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(a))
      GameTestUtil.createWithStructure(FluidTankCommon.modId, BATCH, s"cat_test_drain_${toDrain.content.getKey.getPath}_$a",
        "cat_test", g => drainWater(g, toDrain, waterBucket, emptyBucket, drained))
    }
    t.asJava
  }

  private def drainWater(helper: GameTestHelper, toDrain: FluidAmount, filledBucket: Int, emptyBucket: Int, drainedAmount: Int): Unit = {
    val handler = getHandler(helper)
    val drained = handler.drain(toDrain.toStack, IFluidHandler.FluidAction.EXECUTE)
    assertEquals(toDrain.setAmount(GenericUnit.fromForge(drainedAmount)), drained.toAmount)

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 2, 2)))
    assertEquals(emptyBucket, chest.countItem(Items.BUCKET))
    assertEquals(filledBucket, chest.countItem(Items.WATER_BUCKET))
    helper.succeed()
  }

  def drainLava(helper: GameTestHelper): Unit = {
    val handler = getHandler(helper)
    val drained = handler.drain(FluidAmountUtil.BUCKET_LAVA.toStack, IFluidHandler.FluidAction.EXECUTE)
    assertEquals(FluidAmountUtil.BUCKET_LAVA, drained.toAmount)

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 2, 2)))
    assertEquals(3, chest.countItem(Items.BUCKET))
    assertEquals(2, chest.countItem(Items.LAVA_BUCKET))
    helper.succeed()
  }

  def drain1000(helper: GameTestHelper): Unit = {
    val toDrain = FluidAmountUtil.BUCKET_LAVA
    val handler = getHandler(helper)
    val drained = handler.drain(1000, IFluidHandler.FluidAction.SIMULATE)
    assertEquals(toDrain, drained.toAmount)

    val chest = HopperBlockEntity.getContainerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 2, 2)))
    assertEquals(2, chest.countItem(Items.BUCKET))
    assertEquals(2, chest.countItem(Items.WATER_BUCKET))
    assertEquals(3, chest.countItem(Items.LAVA_BUCKET))
    helper.succeed()
  }
}
