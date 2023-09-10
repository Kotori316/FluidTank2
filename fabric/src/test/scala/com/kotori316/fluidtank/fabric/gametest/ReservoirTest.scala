package com.kotori316.fluidtank.fabric.gametest

import cats.implicits.catsSyntaxSemigroup
import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.{GenericUnit, Tank}
import com.kotori316.fluidtank.fabric.BeforeMC.{assertEqualHelper, assertEqualStack}
import com.kotori316.fluidtank.fabric.FluidTank
import com.kotori316.fluidtank.fabric.gametest.TankTest.placeTank
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, PotionType, VanillaFluid, VanillaPotion}
import com.kotori316.fluidtank.tank.Tier
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.{GameTestGenerator, GameTestHelper, TestFunction}
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.Potions
import org.junit.jupiter.api.Assertions.{assertEquals, assertTrue}

import scala.jdk.javaapi.CollectionConverters

//noinspection ScalaUnusedSymbol,DuplicatedCode
class ReservoirTest extends FabricGameTest {
  private final val BATCH = "defaultBatch"
  private final val WOOD_RESERVOIR = FluidTank.RESERVOIR_MAP.get(Tier.WOOD)

  @GameTestGenerator
  def generator(): java.util.List[TestFunction] = {
    GetGameTestMethods.getTests(getClass, this, BATCH)
  }

  private def createReservoirStack(amount: FluidAmount): ItemStack = {
    val stack = new ItemStack(WOOD_RESERVOIR)
    val tank = Tank(amount, GenericUnit(WOOD_RESERVOIR.tier.getCapacity))
    WOOD_RESERVOIR.saveTank(stack, tank)
    stack
  }

  @GameTestGenerator
  def fillTank(): java.util.List[TestFunction] = CollectionConverters.asJava(
    for {
      f <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
        ++ PotionType.values().map(p => FluidAmountUtil.from(p, Potions.POISON, GenericUnit.ONE_BUCKET))
      amount <- f.content match {
        case _: VanillaFluid => Seq(GenericUnit.fromForge(500), GenericUnit.ONE_BUCKET, GenericUnit.fromForge(2000))
        case _: VanillaPotion => Seq(GenericUnit.ONE_BOTTLE, GenericUnit.ONE_BUCKET, GenericUnit.fromForge(2000))
      }
      fluid = f.setAmount(amount)
      initial <- Seq(FluidAmountUtil.EMPTY, fluid)
    } yield GameTestUtil.create(FluidTankCommon.modId, BATCH,
      s"ReservoirTestFillTank_${initial.content.getKey.getPath}_${fluid.content.getKey.getPath}_${fluid.amount.asForge}", g => {
        val basePos = BlockPos.ZERO.above
        val tile = placeTank(g, basePos, Tier.WOOD)
        tile.getConnection.getHandler.fill(initial, execute = true)
        val stack = createReservoirStack(fluid)
        val player = g.makeMockSurvivalPlayer()
        player.setItemInHand(InteractionHand.MAIN_HAND, stack)

        g.useBlock(basePos, player)

        assertEquals(fluid + initial, tile.getTank.content)
        val modified = WOOD_RESERVOIR.getTank(player.getItemInHand(InteractionHand.MAIN_HAND))
        assertTrue(modified.isEmpty)

        g.succeed()
      })
  )

  def fillTank1(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above
    val tile = placeTank(helper, basePos, Tier.WOOD)
    val stack = createReservoirStack(FluidAmountUtil.BUCKET_WATER)
    val player = helper.makeMockSurvivalPlayer()
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    helper.useBlock(basePos, player)

    assertEquals(FluidAmountUtil.BUCKET_WATER, tile.getTank.content)
    val modified = WOOD_RESERVOIR.getTank(player.getItemInHand(InteractionHand.MAIN_HAND))
    assertTrue(modified.isEmpty)

    helper.succeed()
  }

  def fillTank2(helper: GameTestHelper): Unit = {
    val basePos = BlockPos.ZERO.above
    val tile = placeTank(helper, basePos, Tier.WOOD)
    tile.getConnection.getHandler.fill(FluidAmountUtil.BUCKET_WATER, execute = true)
    val stack = createReservoirStack(FluidAmountUtil.BUCKET_WATER)
    val player = helper.makeMockSurvivalPlayer()
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)

    helper.useBlock(basePos, player)

    assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.ONE_BUCKET.combineN(2)), tile.getTank.content)
    val modified = WOOD_RESERVOIR.getTank(player.getItemInHand(InteractionHand.MAIN_HAND))
    assertTrue(modified.isEmpty)

    helper.succeed()
  }

  @GameTestGenerator
  def fillTankFail(): java.util.List[TestFunction] = CollectionConverters.asJava(
    for {
      fluid <- Seq(FluidAmountUtil.BUCKET_WATER)
        ++ PotionType.values().map(p => FluidAmountUtil.from(p, Potions.POISON, GenericUnit.ONE_BUCKET))
      initial <- Seq(FluidAmountUtil.BUCKET_LAVA, FluidAmountUtil.from(PotionType.SPLASH, Potions.WATER, GenericUnit.ONE_BUCKET))
    } yield GameTestUtil.create(FluidTankCommon.modId, BATCH,
      s"ReservoirTestFillTankFail_${initial.content.getKey.getPath}_${fluid.content.getKey.getPath}_${fluid.amount.asForge}", g => {
        val basePos = BlockPos.ZERO.above
        val tile = placeTank(g, basePos, Tier.WOOD)
        tile.getConnection.getHandler.fill(initial, execute = true)
        val stack = createReservoirStack(fluid)
        val player = g.makeMockSurvivalPlayer()
        player.setItemInHand(InteractionHand.MAIN_HAND, stack)

        g.useBlock(basePos, player)

        assertEquals(initial, tile.getTank.content)
        val modified = WOOD_RESERVOIR.getTank(player.getItemInHand(InteractionHand.MAIN_HAND))
        assertEquals(fluid, modified.content)
        assertEqualStack(stack, player.getItemInHand(InteractionHand.MAIN_HAND))

        g.succeed()
      })
  )

  @GameTestGenerator
  def drainTank(): java.util.List[TestFunction] = CollectionConverters.asJava(
    for {
      f <- Seq(FluidAmountUtil.BUCKET_WATER, FluidAmountUtil.BUCKET_LAVA)
        ++ PotionType.values().map(p => FluidAmountUtil.from(p, Potions.POISON, GenericUnit.ONE_BUCKET))
      amount <- f.content match {
        case _: VanillaFluid => Seq(GenericUnit.fromForge(500), GenericUnit.ONE_BUCKET, GenericUnit.fromForge(2000))
        case _: VanillaPotion => Seq(GenericUnit.ONE_BOTTLE, GenericUnit.ONE_BUCKET, GenericUnit.fromForge(2000))
      }
      fluid = f.setAmount(amount)
    } yield GameTestUtil.create(FluidTankCommon.modId, BATCH,
      s"ReservoirTestDrainTank_${fluid.content.getKey.getPath}_${fluid.amount.asForge}", g => {
        val basePos = BlockPos.ZERO.above
        val tile = placeTank(g, basePos, Tier.WOOD)
        tile.getConnection.getHandler.fill(fluid.setAmount(GenericUnit.fromForge(4000)), execute = true)
        val stack = createReservoirStack(fluid)
        val player = g.makeMockSurvivalPlayer()
        player.setItemInHand(InteractionHand.MAIN_HAND, stack)

        g.useBlock(basePos, player)

        assertEquals(fluid, tile.getTank.content)
        val modified = WOOD_RESERVOIR.getTank(player.getItemInHand(InteractionHand.MAIN_HAND))
        assertEqualHelper(fluid.setAmount(GenericUnit.fromForge(4000)), modified.content)

        g.succeed()
      })
  )
}
