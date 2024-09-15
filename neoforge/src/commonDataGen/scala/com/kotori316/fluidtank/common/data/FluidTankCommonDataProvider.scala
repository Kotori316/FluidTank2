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
import net.neoforged.neoforge.data.event.GatherDataEvent

import java.util.Collections
import scala.annotation.static
import scala.jdk.javaapi.CollectionConverters

object FluidTankCommonDataProvider {
  @static
  @SubscribeEvent
  def onEvent(event: GatherDataEvent): Unit = {
    PlatformConfigAccess.setInstance(() => ConfigData.FOR_TEST)
    FluidTankCommon.LOGGER.info("Start NeoForge common data generation")

    event.getGenerator.addProvider(event.includeServer, new LootTableProvider(event.getGenerator.getPackOutput, Collections.emptySet(),
      CollectionConverters.asJava(Seq(new LootTableProvider.SubProviderEntry(r => new LootSubProvider(r), LootContextParamSets.BLOCK))),
      event.getLookupProvider
    ))
    event.getGenerator.addProvider(event.includeClient, StateAndModelProvider(event.getGenerator, event.getExistingFileHelper))
    event.getGenerator.addProvider(true, PackMetadataGenerator(event.getGenerator.getPackOutput)
      .add(PackMetadataSection.TYPE, PackMetadataSection(Component.literal("FluidTank Resources"), DetectedVersion.BUILT_IN.getPackVersion(PackType.CLIENT_RESOURCES)))
    )
  }
}

@Mod("fluidtank_common_data")
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
class FluidTankCommonDataProvider {

}
