package com.kotori316.fluidtank.neoforge.gametest

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil}
import com.kotori316.fluidtank.gametest.GameTestFunctions
import com.kotori316.fluidtank.neoforge.FluidTank
import com.kotori316.fluidtank.neoforge.cat.EntityChestAsTank
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import com.kotori316.fluidtank.neoforge.tank.TankFluidItemHandler
import com.kotori316.fluidtank.tank.Tier
import com.kotori316.testutil.common.TestFunction
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.{Item, ItemStack, Items}
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.block.{Block, Rotation}
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.access.ItemAccess
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.Transaction
import org.junit.jupiter.api.Assertions.{assertDoesNotThrow, assertEquals, assertNotNull, assertTrue}

import java.util.Locale
import scala.jdk.CollectionConverters.{BufferHasAsJava, ListHasAsScala}
import scala.jdk.OptionConverters.RichOptional
import scala.util.Using

//noinspection ScalaUnusedSymbol,DuplicatedCode
class CatTest {
  private final val BATCH = GetGameTestMethods.DEFAULT_BATCH

  def tests(): java.util.List[TestFunction] = {
    val all = generator().asScala ++ fillMore() ++ fillFail() ++ drainWater() ++ drainClonedTank() ++ drainDroppedTank()
    all.asJava
  }

  private def drainClonedTank(): Seq[TestFunction] = {
    Seq(GameTestFunctions.create(BATCH, TestFunction.EMPTY_STRUCTURE, "cat_test_drain_cloned_tank", g => drainClonedTank(g)))
  }

  private def drainDroppedTank(): Seq[TestFunction] = {
    Seq(GameTestFunctions.create(BATCH, TestFunction.EMPTY_STRUCTURE, "cat_test_drain_dropped_tank", g => drainDroppedTank(g)))
  }

  private def drainClonedTank(helper: GameTestHelper): Unit = {
    val tier = Tier.WOOD
    val pos = BlockPos.ZERO.above()
    val toDrain = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit(tier.getCapacity))
    val tankTile = TankTest.placeTank(helper, pos, tier)
    tankTile.getConnection.getHandler.fill(toDrain, execute = true)
    val stack = helper.getBlockState(pos).getCloneItemStack(helper.absolutePos(pos), helper.getLevel, true, helper.makeMockPlayer(GameType.CREATIVE))
    val handler = new TankFluidItemHandler(tier, ItemAccess.forStack(stack))
    assertEquals(toDrain.amount.asForge, handler.getTank.amount.asForge)
    Using.resource(Transaction.openRoot()) { tx =>
      val drained = handler.extract(toDrain.asVariant, toDrain.amount.asForge, tx)
      assertEquals(toDrain.amount.asForge, drained)
      tx.commit()
    }
    val drainedStack = handler.getContainer
    val freshStack = new ItemStack(FluidTank.TANK_MAP.get(tier).get())
    assertTrue(handler.getTank.isEmpty, s"Tank should be empty but ${handler.getTank}")
    assertTrue(
      ItemStack.isSameItemSameComponents(freshStack, drainedStack),
      s"Drained tank must stack with a fresh tank. fresh=${freshStack.getComponents}, drained=${drainedStack.getComponents}"
    )
    helper.succeed()
  }

  private def drainDroppedTank(helper: GameTestHelper): Unit = {
    val tier = Tier.WOOD
    val pos = BlockPos.ZERO.above()
    val toDrain = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit(tier.getCapacity))
    val tankTile = TankTest.placeTank(helper, pos, tier)
    tankTile.getConnection.getHandler.fill(toDrain, execute = true)
    val drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel, helper.absolutePos(pos), tankTile, helper.makeMockPlayer(GameType.CREATIVE), ItemStack.EMPTY)
    assertEquals(1, drops.size, s"Drop was $drops")
    val stack = drops.get(0)
    val handler = new TankFluidItemHandler(tier, ItemAccess.forStack(stack))
    assertEquals(toDrain.amount.asForge, handler.getTank.amount.asForge)
    Using.resource(Transaction.openRoot()) { tx =>
      val drained = handler.extract(toDrain.asVariant, toDrain.amount.asForge, tx)
      assertEquals(toDrain.amount.asForge, drained)
      tx.commit()
    }
    val drainedStack = handler.getContainer
    val freshStack = new ItemStack(FluidTank.TANK_MAP.get(tier).get())
    assertTrue(handler.getTank.isEmpty, s"Tank should be empty but ${handler.getTank}")
    assertTrue(
      ItemStack.isSameItemSameComponents(freshStack, drainedStack),
      s"Drained tank must stack with a fresh tank. fresh=${freshStack.getComponents}, drained=${drainedStack.getComponents}"
    )
    helper.succeed()
  }

  def generator(): java.util.List[TestFunction] = {
    GetGameTestMethods.getTests(getClass, this, BATCH, "cat_test")
  }

  def testGetFluids(helper: GameTestHelper): Unit = {
    val pos = new BlockPos(2, 1, 2)
    val cat = helper.getBlockEntity(pos, classOf[EntityChestAsTank])

    val fluids = cat.getFluids.toScala.toSeq.flatMap(_.asScala)
    assertTrue(fluids.contains(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000))),
      s"CAT should recognize fluids, $fluids")
    assertTrue(fluids.contains(FluidAmountUtil.BUCKET_LAVA.setAmount(GenericUnit.fromForge(3000))),
      s"CAT should recognize fluids, $fluids")

    helper.succeed()
  }

  private def getHandler(helper: GameTestHelper): ResourceHandler[FluidResource] = {
    val pos = new BlockPos(2, 1, 2)
    val cat = helper.getBlockEntity(pos, classOf[EntityChestAsTank])

    val handler = assertDoesNotThrow(() => helper.getLevel.getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), null))
    assertNotNull(handler)
    handler
  }

  def fillLava(helper: GameTestHelper): Unit = {
    val handler = getHandler(helper)
    val fluid = FluidAmountUtil.BUCKET_LAVA
    val filled: Int = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.insert(fluid.asVariant, fluid.amount.asForge, tx)
      tx.commit()
      d
    }
    assertEquals(1000, filled)

    val chest = HopperBlockEntity.getContainerOrHandlerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)), Direction.UP)
    assertNotNull(chest.container())

    assertEquals(4, chest.container().countItem(Items.LAVA_BUCKET), "Lava Bucket count")

    helper.succeed()
  }

  def fillWater(helper: GameTestHelper): Unit = {
    val handler = getHandler(helper)
    val fluid = FluidAmountUtil.BUCKET_WATER
    val filled: Int = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.insert(fluid.asVariant, fluid.amount.asForge, tx)
      tx.commit()
      d
    }
    assertEquals(1000, filled)

    val chest = HopperBlockEntity.getContainerOrHandlerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)), Direction.UP)
    assertNotNull(chest.container())

    assertEquals(3, chest.container().countItem(Items.WATER_BUCKET))

    helper.succeed()
  }

  private def fillMore(): Seq[TestFunction] = {
    val t = for {
      rot <- Rotation.values().toSeq
      kind <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
      amount <- 2000 to 4000 by 1000
      fluid = kind.setAmount(GenericUnit.fromForge(amount))
      bucket = if (kind.contentEqual(FluidAmountUtil.BUCKET_WATER)) Items.WATER_BUCKET else Items.LAVA_BUCKET
      count = if (kind.contentEqual(FluidAmountUtil.BUCKET_WATER)) 4 else 5
    } yield {
      GameTestFunctions.create(
        BATCH,
        "cat_test",
        s"cat_test_${kind.content.getKey.getPath}_${amount}_${rot.name()}".toLowerCase(Locale.ROOT),
        g => fillMore(g, fluid, count, bucket),
      )
    }
    t
  }

  private def fillMore(helper: GameTestHelper, fluid: FluidAmount, expectItemCount: Int, expectItem: Item): Unit = {
    val handler = getHandler(helper)

    val filled: Int = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.insert(fluid.asVariant, fluid.amount.asForge, tx)
      tx.commit()
      d
    }
    assertEquals(2000, filled, "Filled amount doesn't match to expectation")

    val chest = HopperBlockEntity.getContainerOrHandlerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)), Direction.UP)
    assertNotNull(chest.container())

    assertEquals(expectItemCount, chest.container().countItem(expectItem), "Item count")
    helper.succeed()
  }

  private def fillFail(): Seq[TestFunction] = {
    val t = for {
      kind <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
      a <- Seq(0, 500, 999)
      amount = GenericUnit.fromForge(a)
      fluid = kind.setAmount(amount)
    } yield GameTestFunctions.create(
      BATCH,
      "cat_test",
      s"cat_test_fill_fail_${fluid.content.getKey.getPath}_$a".toLowerCase(Locale.ROOT),
      g => fillFail(g, fluid)
    )
    t
  }

  private def fillFail(helper: GameTestHelper, amount: FluidAmount): Unit = {
    val filled: Int = Using.resource(Transaction.openRoot()) { tx =>
      val d = getHandler(helper).insert(amount.asVariant, amount.amount.asForge, tx)
      tx.close()
      d
    }
    assertEquals(0, filled)

    helper.succeed()
  }

  def fillSimulate(helper: GameTestHelper): Unit = {
    val toFill = FluidAmountUtil.BUCKET_WATER
    val handler = getHandler(helper)
    val filled = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.insert(toFill.asVariant, toFill.amount.asForge, tx)
      tx.close()
      d
    }

    val chest = HopperBlockEntity.getContainerOrHandlerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)), Direction.UP)
    assertNotNull(chest.container())
    assertEquals(2, chest.container().countItem(Items.BUCKET))
    assertEquals(2, chest.container().countItem(Items.WATER_BUCKET))

    helper.succeed()
  }

  private def drainWater(): Seq[TestFunction] = {
    val t = for {
      a <- 1000 to 3000 by 500
    } yield {
      val waterBucket = math.max(2 - a / 1000, 0)
      val emptyBucket = 4 - waterBucket
      val drained = math.min(1000 * (a / 1000), 2000)
      val toDrain = FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(a))
      GameTestFunctions.create(
        BATCH,
        "cat_test",
        s"cat_test_drain_${toDrain.content.getKey.getPath}_$a".toLowerCase(Locale.ROOT),
        g => drainWater(g, toDrain, waterBucket, emptyBucket, drained),
      )
    }
    t
  }

  private def drainWater(helper: GameTestHelper, toDrain: FluidAmount, filledBucket: Int, emptyBucket: Int, drainedAmount: Int): Unit = {
    val handler = getHandler(helper)
    val drained = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.extract(toDrain.asVariant, toDrain.amount.asForge, tx)
      tx.commit()
      d
    }
    assertEquals(drainedAmount, drained)

    val chest = HopperBlockEntity.getContainerOrHandlerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)), Direction.UP)
    assertNotNull(chest.container())
    assertEquals(emptyBucket, chest.container().countItem(Items.BUCKET))
    assertEquals(filledBucket, chest.container().countItem(Items.WATER_BUCKET))
    helper.succeed()
  }

  def drainLava(helper: GameTestHelper): Unit = {
    val toDrain = FluidAmountUtil.BUCKET_LAVA
    val handler = getHandler(helper)
    val drained = Using.resource(Transaction.openRoot()) { tx =>
      val d = handler.extract(toDrain.asVariant, toDrain.amount.asForge, tx)
      tx.commit()
      d
    }
    assertEquals(toDrain.amount.asForge, drained)

    val chest = HopperBlockEntity.getContainerOrHandlerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)), Direction.UP)
    assertNotNull(chest.container())
    assertEquals(3, chest.container().countItem(Items.BUCKET))
    assertEquals(2, chest.container().countItem(Items.LAVA_BUCKET))
    helper.succeed()
  }

  def drain1000Simulate(helper: GameTestHelper): Unit = {
    val toDrain = FluidAmountUtil.BUCKET_LAVA
    val handler = getHandler(helper)
    val drained = Using.resource(Transaction.openRoot()) { tx =>
      handler.extract(toDrain.asVariant, toDrain.amount.asForge, tx)
    }
    assertEquals(toDrain.amount.asForge, drained)

    val chest = HopperBlockEntity.getContainerOrHandlerAt(helper.getLevel, helper.absolutePos(new BlockPos(3, 1, 2)), Direction.UP)
    assertNotNull(chest.container())
    assertEquals(2, chest.container().countItem(Items.BUCKET))
    assertEquals(2, chest.container().countItem(Items.WATER_BUCKET))
    assertEquals(3, chest.container().countItem(Items.LAVA_BUCKET))
    helper.succeed()
  }
}
