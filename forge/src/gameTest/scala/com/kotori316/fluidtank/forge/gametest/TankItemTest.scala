package com.kotori316.fluidtank.forge.gametest

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.forge.FluidTank
import com.kotori316.fluidtank.tank.Tier
import net.minecraft.gametest.framework.{GameTest, GameTestHelper}
import net.minecraft.world.item.{BlockItem, ItemStack}
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.{FluidStack, FluidUtil}
import net.minecraftforge.gametest.{GameTestHolder, PrefixGameTestTemplate}
import org.junit.jupiter.api.Assertions

@GameTestHolder(FluidTankCommon.modId)
@PrefixGameTestTemplate(value = false)
final class TankItemTest {
  @GameTest(template = GameTestUtil.NO_PLACE_STRUCTURE)
  def tankItemHasCap(helper: GameTestHelper): Unit = {
    val stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get)
    val handler = FluidUtil.getFluidHandler(stack)
    Assertions.assertTrue(handler.isPresent)
    helper.succeed()
  }

  @GameTest(template = GameTestUtil.NO_PLACE_STRUCTURE)
  def tankItemCheckNBT(helper: GameTestHelper): Unit = {
    val stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get)
    val handler = FluidUtil.getFluidHandler(stack)
    handler.ifPresent(h => h.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE))
    Assertions.assertNotNull(BlockItem.getBlockEntityData(stack))
    helper.succeed()
  }
}
