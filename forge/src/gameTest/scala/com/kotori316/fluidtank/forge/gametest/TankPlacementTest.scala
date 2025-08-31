package com.kotori316.fluidtank.forge.gametest

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.tank.Tier
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.{GameTestGenerator, GameTestHelper, TestFunction}
import net.minecraft.world.level.block.{Block, Blocks}
import net.minecraftforge.gametest.{GameTestHolder, PrefixGameTestTemplate}

import java.util.Locale
import scala.jdk.javaapi.CollectionConverters

@GameTestHolder(FluidTankCommon.modId)
@PrefixGameTestTemplate(value = false)
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
}
