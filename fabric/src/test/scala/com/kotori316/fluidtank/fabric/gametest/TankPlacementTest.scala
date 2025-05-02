package com.kotori316.fluidtank.fabric.gametest

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fabric.FluidTank
import com.kotori316.fluidtank.fabric.recipe.ModifiableSingleItemStorage
import com.kotori316.fluidtank.fabric.tank.FabricTankItemStorage
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil}
import com.kotori316.fluidtank.gametest.GameTestFunctions
import com.kotori316.fluidtank.tank.{PlatformTankAccess, Tier}
import com.kotori316.testutil.common.TestFunction
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.{Block, Blocks}
import net.minecraft.world.phys.{BlockHitResult, Vec3}
import org.junit.jupiter.api.Assertions.{assertAll, assertEquals}

import java.util.Locale
import scala.jdk.javaapi.CollectionConverters

final class TankPlacementTest {
  private final val BATCH_NAME = "tank_place_test"

  def tests(): java.util.List[TestFunction] = {
    val tests = notRemovedByFluid() ++ tankDrop() ++ placeTankAboveTank()
    CollectionConverters.asJava(tests)
  }

  def notRemovedByFluid(): Seq[TestFunction] = {
    for {
      t <- Tier.values().filterNot(_ == Tier.INVALID).toSeq
      f <- Seq(Blocks.LAVA, Blocks.WATER)
      name = s"${BATCH_NAME}_${t}_${f.getName.getString}".toLowerCase(Locale.ROOT)
    } yield GameTestFunctions.create(BATCH_NAME, "check_water", name, g => notRemovedByFluid(g, t, f))
  }

  private def notRemovedByFluid(helper: GameTestHelper, tier: Tier, fluid: Block): Unit = {
    val pos = new BlockPos(4, 1, 4)
    helper.startSequence
      .thenExecute(() => GameTestUtil.placeTank(helper, pos, tier))
      .thenExecuteAfter(1, () => helper.setBlock(pos.west, fluid))
      .thenIdle(40)
      .thenExecute(() => helper.assertBlockPresent(fluid, pos.west.north))
      .thenExecute(() => helper.assertBlockPresent(PlatformTankAccess.getInstance().getTankBlockMap.get(tier).get(), pos))
      .thenSucceed()
  }

  def tankDrop(): Seq[TestFunction] = {
    val tests = for {
      t <- Seq(Tier.WOOD, Tier.STONE, Tier.STAR)
      f <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
      amount <- Seq(GenericUnit.ONE_BUCKET, GenericUnit.ONE_BOTTLE, GenericUnit.fromForge(2000))
      dropName = s"tank_drop_${t}_${f.content.getKey.getPath}_${amount.asForge}".toLowerCase(Locale.ROOT)
      cloneName = s"tank_clone_${t}_${f.content.getKey.getPath}_${amount.asForge}".toLowerCase(Locale.ROOT)

      test <- Seq(
        GameTestFunctions.create(BATCH_NAME, TestFunction.NO_PLACE_STRUCTURE, dropName, g => testGetTankDrop(g, t, f.setAmount(amount))),
        GameTestFunctions.create(BATCH_NAME, TestFunction.NO_PLACE_STRUCTURE, cloneName, g => testGetTankClone(g, t, f.setAmount(amount))),
      )
    } yield test

    tests
  }

  private def testGetTankDrop(helper: GameTestHelper, tier: Tier, fillContent: FluidAmount): Unit = {
    val pos = BlockPos.ZERO.above()
    val tankTile = GameTestUtil.placeTank(helper, pos, tier)
    tankTile.getConnection.getHandler.fill(fillContent, execute = true)

    val drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel, helper.absolutePos(pos), tankTile, helper.makeMockPlayer(GameType.CREATIVE), ItemStack.EMPTY)
    assertEquals(1, drops.size, "Drop was " + drops)

    val stack = drops.get(0)
    val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
    assertAll(
      () => assertEquals(FluidTank.TANK_MAP.get(tier).itemBlock, stack.getItem),
      () => assertEquals(1, stack.getCount),
      () => assertEquals(fillContent, handler.getTank.content),
    )
    helper.succeed()
  }

  private def testGetTankClone(helper: GameTestHelper, tier: Tier, fillContent: FluidAmount): Unit = {
    val pos = BlockPos.ZERO.above()
    val tankTile = GameTestUtil.placeTank(helper, pos, tier)
    tankTile.getConnection.getHandler.fill(fillContent, execute = true)

    val state = helper.getBlockState(pos)
    val stack = state.getCloneItemStack(
      helper.getLevel, helper.absolutePos(pos), false
    )
    val handler = new FabricTankItemStorage(ModifiableSingleItemStorage.getContext(stack))
    assertAll(
      () => assertEquals(FluidTank.TANK_MAP.get(tier).itemBlock, stack.getItem),
      () => assertEquals(1, stack.getCount),
      () => assertEquals(fillContent, handler.getTank.content),
    )
    helper.succeed()
  }

  def placeTankAboveTank(): Seq[TestFunction] = {
    val tests = for {
      t <- Seq(Tier.WOOD, Tier.STONE, Tier.STAR)
      f <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
      testName = s"tank_place_tank_above_tank_${t}_${f.content.getKey.getPath}".toLowerCase(Locale.ROOT)
    } yield GameTestFunctions.create(BATCH_NAME, TestFunction.EMPTY_STRUCTURE, testName, g => testPlaceTankAboveTank(g, t, f))

    tests
  }

  private def testPlaceTankAboveTank(helper: GameTestHelper, tier: Tier, tankContent: FluidAmount): Unit = {
    val pos = BlockPos.ZERO.above()
    val tankTile = GameTestUtil.placeTank(helper, pos, tier)
    tankTile.getConnection.getHandler.fill(tankContent, execute = true)

    val stack = new ItemStack(PlatformTankAccess.getInstance().getTankBlockMap.get(tier).get())
    val player = helper.makeMockPlayer(GameType.SURVIVAL)
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    val absolutePos = helper.absolutePos(pos)
    helper.useBlock(pos, player, new BlockHitResult(Vec3.atBottomCenterOf(absolutePos.above()), Direction.UP, absolutePos, false))

    helper.assertBlockPresent(PlatformTankAccess.getInstance().getTankBlockMap.get(tier).get(), pos.above())
    assertEquals(2, tankTile.getConnection.getTiles.size)
    assertEquals(Option(tankContent), tankTile.getConnection.getContent)

    helper.succeed()
  }
}
