package com.kotori316.fluidtank.forge.gametest

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.forge.FluidTank
import net.minecraft.gametest.framework.{GameTest, GameTestHelper}
import net.minecraftforge.api.distmarker.Dist.*
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.gametest.{GameTestHolder, PrefixGameTestTemplate}
import org.junit.jupiter.api.Assertions.assertEquals

@GameTestHolder(FluidTankCommon.modId)
@PrefixGameTestTemplate(value = false)
final class SideProxyTest {
  @GameTest(template = GameTestUtil.NO_PLACE_STRUCTURE)
  def checkProxyClass(helper: GameTestHelper): Unit = {
    val clazz = FMLEnvironment.dist match {
      case DEDICATED_SERVER => Class.forName("com.kotori316.fluidtank.forge.SideProxy$ServerProxy")
      case CLIENT => Class.forName("com.kotori316.fluidtank.forge.SideProxy$ClientProxy")
    }
    assertEquals(clazz, FluidTank.proxy.getClass)
    helper.succeed()
  }
}
