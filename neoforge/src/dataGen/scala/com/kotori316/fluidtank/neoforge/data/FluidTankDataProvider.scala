package com.kotori316.fluidtank.neoforge.data

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.config.{ConfigData, PlatformConfigAccess}
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.{EventBusSubscriber, Mod}
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent

import scala.annotation.static

object FluidTankDataProvider {
  @static
  @SubscribeEvent
  def onEvent(event: GatherDataRegistryEntriesEvent): Unit = {
    PlatformConfigAccess.setInstance(() => ConfigData.FOR_TEST)
    FluidTankCommon.LOGGER.info("Start NeoForge data registry entry generation")
    event.gatherFor(FluidTankCommon.modId) // manually set the target mod id
      .add(RecipeNeoForge)
  }
}

@Mod("fluidtank_data")
@EventBusSubscriber()
class FluidTankDataProvider {

}
