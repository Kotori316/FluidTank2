package com.kotori316.fluidtank.neoforge.data

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.data.Recipe
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.{EventBusSubscriber, Mod}
import net.neoforged.neoforge.data.event.GatherDataEvent

import scala.annotation.static

object FluidTankDataProvider {
  @static
  @SubscribeEvent
  def onEvent(event: GatherDataEvent): Unit = {
    FluidTankCommon.LOGGER.info("Start NeoForge data generation")
    val ingredientProvider = IngredientProviderNeoForge()
    event.getGenerator.addProvider(event.includeServer, new Recipe(ingredientProvider, event.getGenerator.getPackOutput, event.getLookupProvider))
  }
}

@Mod("fluidtank_data")
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
class FluidTankDataProvider {

}
