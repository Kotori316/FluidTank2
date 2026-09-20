package com.kotori316.fluidtank.neoforge.data

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.config.{ConfigData, PlatformConfigAccess}
import net.minecraft.core.RegistrySetBuilder
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.{EventBusSubscriber, Mod}
import net.neoforged.neoforge.data.event.GatherDataEvent

import java.util.Set as JSet
import scala.annotation.static

object FluidTankDataProvider {
  @static
  @SubscribeEvent
  def onEvent(event: GatherDataEvent.Client): Unit = {
    PlatformConfigAccess.setInstance(() => ConfigData.FOR_TEST)
    FluidTankCommon.LOGGER.info("Start NeoForge data generation")
    val builder = new RegistrySetBuilder().add(RecipeNeoForge)
    event.createReloadableRegistryObjects(builder, JSet.of(FluidTankCommon.modId)) // manually set the target mod id
  }
}

@Mod("fluidtank_data")
@EventBusSubscriber()
class FluidTankDataProvider {

}
