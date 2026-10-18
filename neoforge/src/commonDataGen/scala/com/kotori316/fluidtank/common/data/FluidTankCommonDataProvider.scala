package com.kotori316.fluidtank.common.data

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.config.{ConfigData, PlatformConfigAccess}
import net.minecraft.DetectedVersion
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.data.metadata.PackMetadataGenerator
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.pack.PackMetadataSection
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.{EventBusSubscriber, Mod}
import net.neoforged.neoforge.data.event.{GatherDataEvent, GatherDataRegistryEntriesEvent}

import scala.annotation.static

object FluidTankCommonDataProvider {
  @static
  @SubscribeEvent
  def onEvent(event: GatherDataEvent.Client): Unit = {
    PlatformConfigAccess.setInstance(() => ConfigData.FOR_TEST)
    FluidTankCommon.LOGGER.info("Start NeoForge common data generation")

    event.addProvider(StateAndModelProvider(event.getGenerator.getPackOutput))
    event.addProvider(PackMetadataGenerator(event.getGenerator.getPackOutput)
      .add(PackMetadataSection.CLIENT_TYPE, PackMetadataSection(Component.literal("FluidTank Resources"), DetectedVersion.BUILT_IN.packVersion(PackType.CLIENT_RESOURCES).minorRange()))
    )
  }

  @static
  @SubscribeEvent
  def onRegistryEntryEvent(event: GatherDataRegistryEntriesEvent): Unit = {
    FluidTankCommon.LOGGER.info("Start NeoForge common data registry entry generation")
    event.gatherFor(FluidTankCommon.modId)
      .lootTable(new LootTableProvider.SubProviderEntry(r => new LootSubProvider(r), LootContextParamSets.BLOCK))
  }
}

@Mod("fluidtank_common_data")
@EventBusSubscriber()
class FluidTankCommonDataProvider {

}
