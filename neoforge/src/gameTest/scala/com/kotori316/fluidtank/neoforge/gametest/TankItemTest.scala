package com.kotori316.fluidtank.neoforge.gametest

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.neoforge.FluidTank
import com.kotori316.fluidtank.tank.Tier
import com.kotori316.testutil.GameTestUtil
import net.minecraft.core.component.DataComponents
import net.minecraft.gametest.framework.{GameTestGenerator, GameTestHelper, TestFunction}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.fluids.{FluidStack, FluidUtil}
import net.neoforged.neoforge.gametest.GameTestHolder
import org.junit.jupiter.api.Assertions

@GameTestHolder(FluidTankCommon.modId)
final class TankItemTest {
  @GameTestGenerator
  def generator(): java.util.List[TestFunction] = {
    GetGameTestMethods.getTests(getClass, this, GetGameTestMethods.DEFAULT_BATCH, GameTestUtil.NO_PLACE_STRUCTURE)
  }

  def tankItemHasCap(helper: GameTestHelper): Unit = {
    val stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get)
    val handler = FluidUtil.getFluidHandler(stack)
    Assertions.assertTrue(handler.isPresent)
    helper.succeed()
  }

  def tankItemCheckNBT(helper: GameTestHelper): Unit = {
    val stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get)
    val handler = FluidUtil.getFluidHandler(stack)
    handler.ifPresent(h => h.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE))
    Assertions.assertNotNull(stack.get(DataComponents.BLOCK_ENTITY_DATA))
    helper.succeed()
  }
}
