package com.kotori316.fluidtank.forge.data

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.config.{ConfigData, PlatformConfigAccess}
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.slf4j.MarkerFactory

@Mod.EventBusSubscriber(modid = FluidTankCommon.modId, bus = Mod.EventBusSubscriber.Bus.MOD)
object FluidTankDataProvider {
  final val MARKER = MarkerFactory.getMarker("FluidTankDataProvider")

  @SubscribeEvent
  def gatherDataEvent(event: GatherDataEvent): Unit = {
    PlatformConfigAccess.setInstance(() => ConfigData.FOR_TEST)
    FluidTankCommon.LOGGER.info(MARKER, "Start data generation")
    // Recipe
    event.getGenerator.addProvider(event.includeServer(), new RecipeForge(event.getGenerator.getPackOutput, event.getLookupProvider))
  }

}
