package com.kotori316.fluidtank.forge.data

import com.google.gson.{JsonArray, JsonElement, JsonNull, JsonObject}
import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.config.{ConfigData, PlatformConfigAccess}
import com.mojang.serialization.JsonOps
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraftforge.common.crafting.conditions.{AndCondition, ICondition, TrueCondition}
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.slf4j.MarkerFactory

import java.util.Collections
import scala.jdk.OptionConverters.RichOptional
import scala.jdk.javaapi.CollectionConverters

@Mod.EventBusSubscriber(modid = FluidTankCommon.modId, bus = Mod.EventBusSubscriber.Bus.MOD)
object FluidTankDataProvider {
  final val MARKER = MarkerFactory.getMarker("FluidTankDataProvider")

  @SubscribeEvent
  def gatherDataEvent(event: GatherDataEvent): Unit = {
    PlatformConfigAccess.setInstance(() => ConfigData.FOR_TEST)
    FluidTankCommon.LOGGER.info(MARKER, "Start data generation")
    // Loot table
    event.getGenerator.addProvider(event.includeServer(), new LootTableProvider(
      event.getGenerator.getPackOutput,
      Collections.emptySet(),
      CollectionConverters.asJava(Seq(new LootTableProvider.SubProviderEntry(p => new LootSubProvider(p), LootContextParamSets.BLOCK))),
      event.getLookupProvider,
    ))
    // State and model
    event.getGenerator.addProvider(event.includeClient(), new StateAndModelProvider(event.getGenerator, event.getExistingFileHelper))
    // Recipe
    event.getGenerator.addProvider(event.includeServer(), new RecipeProvider(event.getGenerator.getPackOutput))
  }

  def addPlatformConditions(obj: JsonObject, conditions: List[PlatformedCondition]): Unit = {
    if (conditions.nonEmpty) {
      obj.add(ICondition.DEFAULT_FIELD, FluidTankDataProvider.makeForgeConditionArray(conditions))
      obj.add("fabric:load_conditions", FluidTankDataProvider.makeFabricConditionArray(conditions))
      // See net.neoforged.neoforge.common.conditions.ConditionalOps#DEFAULT_CONDITIONS_KEY
      obj.add("neoforge:conditions", FluidTankDataProvider.makeNeoForgeConditionArray(conditions))
    }
  }

  def makeForgeConditionArray(conditions: List[PlatformedCondition]): JsonElement = {
    val oneCondition = conditions.flatMap(_.forgeCondition) match {
      case head :: Nil => head
      case Nil => TrueCondition.INSTANCE
      case c => new AndCondition(CollectionConverters.asJava(c))
    }
    ICondition.CODEC.encodeStart(JsonOps.INSTANCE, oneCondition).result()
      .toScala
      .getOrElse(JsonNull.INSTANCE)
  }

  def makeFabricConditionArray(conditions: List[PlatformedCondition]): JsonArray = {
    conditions.flatMap(_.fabricCondition).foldLeft(new JsonArray) { case (a, c) => a.add(c); a }
  }

  def makeNeoForgeConditionArray(conditions: List[PlatformedCondition]): JsonArray = {
    conditions.flatMap(_.neoForgeCondition).foldLeft(new JsonArray) { case (a, c) => a.add(c); a }
  }
}
