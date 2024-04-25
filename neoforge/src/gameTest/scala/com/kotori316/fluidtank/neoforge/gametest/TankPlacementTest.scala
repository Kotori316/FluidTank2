package com.kotori316.fluidtank.neoforge.gametest

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil}
import com.kotori316.fluidtank.neoforge.FluidTank
import com.kotori316.fluidtank.neoforge.tank.TankFluidItemHandler
import com.kotori316.fluidtank.tank.Tier
import com.kotori316.testutil.GameTestUtil
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.gametest.framework.{GameTestGenerator, GameTestHelper, TestFunction}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.{Block, Blocks}
import net.minecraft.world.phys.{BlockHitResult, Vec3}
import net.neoforged.neoforge.gametest.GameTestHolder
import org.junit.jupiter.api.Assertions.{assertAll, assertEquals}

import java.util.Locale
import scala.jdk.javaapi.CollectionConverters

@GameTestHolder(FluidTankCommon.modId)
final class TankPlacementTest {
  private final val BATCH_NAME = "tank_place_test"

  @GameTestGenerator
  def notRemovedByFluid(): java.util.List[TestFunction] = {
    CollectionConverters.asJava(for {
      t <- Tier.values().filterNot(_ == Tier.INVALID).toSeq
      f <- Seq(Blocks.LAVA, Blocks.WATER)
      name = s"${BATCH_NAME}_${t}_${f.getName.getString}".toLowerCase(Locale.ROOT)
    } yield GameTestUtil.createWithStructure(FluidTankCommon.modId, BATCH_NAME,
      name, "check_water", g => notRemovedByFluid(g, t, f)))
  }

  private def notRemovedByFluid(helper: GameTestHelper, tier: Tier, fluid: Block): Unit = {
    val pos = new BlockPos(4, 2, 4)
    helper.startSequence
      .thenExecute(() => TankTest.placeTank(helper, pos, tier))
      .thenExecuteAfter(1, () => helper.setBlock(pos.west, fluid))
      .thenIdle(40)
      .thenExecute(() => helper.assertBlockPresent(fluid, pos.west.north))
      .thenExecute(() => helper.assertBlockPresent(TankTest.getBlock(tier).get, pos))
      .thenSucceed()
  }

  @GameTestGenerator
  def tankDrop(): java.util.List[TestFunction] = {
    val tests = for {
      t <- Seq(Tier.WOOD, Tier.STONE, Tier.STAR)
      f <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
      amount <- Seq(GenericUnit.ONE_BUCKET, GenericUnit.ONE_BOTTLE, GenericUnit.fromForge(2000))
      dropName = s"tank_drop_${t}_${f.content.getKey.getPath}_${amount.asForge}".toLowerCase(Locale.ROOT)
      cloneName = s"tank_clone_${t}_${f.content.getKey.getPath}_${amount.asForge}".toLowerCase(Locale.ROOT)

      test <- Seq(
        GameTestUtil.create(FluidTankCommon.modId, BATCH_NAME, dropName, g => testGetTankDrop(g, t, f.setAmount(amount))),
        GameTestUtil.create(FluidTankCommon.modId, BATCH_NAME, cloneName, g => testGetTankClone(g, t, f.setAmount(amount))),
      )
    } yield test

    CollectionConverters.asJava(tests)
  }

  private def testGetTankDrop(helper: GameTestHelper, tier: Tier, fillContent: FluidAmount): Unit = {
    val pos = BlockPos.ZERO.above()
    val tankTile = TankTest.placeTank(helper, pos, tier)
    tankTile.getConnection.getHandler.fill(fillContent, execute = true)

    val drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel, helper.absolutePos(pos), tankTile, helper.makeMockPlayer(GameType.CREATIVE), ItemStack.EMPTY)
    assertEquals(1, drops.size, "Drop was " + drops)

    val stack = drops.get(0)
    val handler = new TankFluidItemHandler(tier, stack)
    assertAll(
      () => assertEquals(FluidTank.TANK_MAP.get(tier).get().itemBlock, stack.getItem),
      () => assertEquals(1, stack.getCount),
      () => assertEquals(fillContent, handler.getTank.content),
    )
    helper.succeed()
  }

  private def testGetTankClone(helper: GameTestHelper, tier: Tier, fillContent: FluidAmount): Unit = {
    val pos = BlockPos.ZERO.above()
    val tankTile = TankTest.placeTank(helper, pos, tier)
    tankTile.getConnection.getHandler.fill(fillContent, execute = true)

    val stack = helper.getBlockState(pos).getCloneItemStack(
      new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(pos)), Direction.UP, helper.absolutePos(pos), true), helper.getLevel, helper.absolutePos(pos), helper.makeMockPlayer(GameType.CREATIVE)
    )
    val handler = new TankFluidItemHandler(tier, stack)
    assertAll(
      () => assertEquals(FluidTank.TANK_MAP.get(tier).get().itemBlock, stack.getItem),
      () => assertEquals(1, stack.getCount),
      () => assertEquals(fillContent, handler.getTank.content),
    )
    helper.succeed()
  }
}
