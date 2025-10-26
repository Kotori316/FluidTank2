package com.kotori316.fluidtank.neoforge.gametest

import com.kotori316.fluidtank.neoforge.FluidTank
import com.kotori316.fluidtank.tank.Tier
import com.kotori316.testutil.common.TestFunction
import net.minecraft.core.component.DataComponents
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.transfer.access.ItemAccess
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.Transaction
import org.junit.jupiter.api.Assertions

import scala.util.Using

final class TankItemTest {
  def tests(): java.util.List[TestFunction] = {
    GetGameTestMethods.getTests(getClass, this, GetGameTestMethods.DEFAULT_BATCH, TestFunction.NO_PLACE_STRUCTURE)
  }

  def tankItemHasCap(helper: GameTestHelper): Unit = {
    val stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get)
    val handler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM)
    Assertions.assertNotNull(handler)
    helper.succeed()
  }

  def tankItemCheckNbt(helper: GameTestHelper): Unit = {
    val stack = new ItemStack(FluidTank.TANK_MAP.get(Tier.WOOD).get)
    Assertions.assertNull(stack.get(DataComponents.BLOCK_ENTITY_DATA))
    val handler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM)
    Assertions.assertNotNull(handler)
    Using.resource(Transaction.openRoot()) { tx =>
      handler.insert(FluidResource.of(Fluids.WATER), 1000, tx)
      tx.commit()
    }
    Assertions.assertNotNull(stack.get(DataComponents.BLOCK_ENTITY_DATA))
    helper.succeed()
  }
}
