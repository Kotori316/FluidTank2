package com.kotori316.fluidtank

import cats.implicits.catsSyntaxEq
import com.google.gson.{GsonBuilder, JsonObject}
import com.kotori316.fluidtank.config.PlatformConfigAccess
import com.kotori316.fluidtank.item.PlatformItemAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.display.SlotDisplayContext
import net.minecraft.world.item.crafting.{Ingredient, Recipe}
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.{Level, Logger}

import java.security.SecureClassLoader
import scala.jdk.CollectionConverters.CollectionHasAsScala

object DebugLogging {
  val ENABLED: Boolean = PlatformConfigAccess.getInstance().getConfig.debug

  val LOGGER: Logger = {
    class DummyClassLoader extends SecureClassLoader

    val configUri = classOf[FluidTankCommon].getResource("/fluidtank-log4j2.xml").toURI
    val context = Configurator.initialize("fluidtank-config", new DummyClassLoader, configUri)
    val l: Logger = if (context == null) {
      FluidTankCommon.LOGGER.error("Failed to initialize DebugLogging.LOGGER. See the log for detail. Uri: {}, Context: {}", configUri, context)
      org.apache.logging.log4j.LogManager.getLogger("FluidTankDebug")
    } else {
      val logger = context.getLogger("FluidTankDebug")
      if (!ENABLED) {
        // default is debug
        logger.setLevel(Level.INFO)
      } else {
        logger.debug("Logger Configuration URI: {}, Context: {}", configUri, context)
      }
      logger
    }
    l
  }

  def initialLog(server: MinecraftServer): Unit = {
    // Config
    LOGGER.info("Config {}", new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
      .toJson(PlatformConfigAccess.getInstance().getConfig.createJson))
    // Recipes fo FluidTank
    val noPretty = new GsonBuilder().disableHtmlEscaping().create()
    server.getRecipeManager.getRecipes.asScala
      .filter(_.id().location().getNamespace === FluidTankCommon.modId)
      .map(h => (h.id(), h.value().asInstanceOf[Recipe[?]]))
      .map { case (id, r) => (id, getResultItem(r, server), ingredientAsMap(r.placementInfo().ingredients().asScala)) }
      .map { case (id, stack, value) =>
        val location = BuiltInRegistries.ITEM.getKey(stack.getItem)
        s"$id $location x${stack.getCount}(tag: ${stack.getComponents}) -> ${noPretty.toJson(value)}"
      }
      .zipWithIndex
      .foreach { case (s, index) => LOGGER.info("{} {}", index + 1, s) }
  }

  private def ingredientAsMap(ingredients: scala.collection.Iterable[Ingredient]): JsonObject = {
    ingredients.map(PlatformItemAccess.getInstance().ingredientToJson)
      .zipWithIndex
      .foldLeft(new JsonObject()) { case (a, (e, i)) => a.add(i.toString, e); a }
  }

  private def getResultItem(recipe: Recipe[?], server: MinecraftServer): ItemStack = {
    recipe.display().asScala
      .headOption
      .map(d => d.result())
      .map(d => d.resolveForFirstStack(SlotDisplayContext.fromLevel(server.overworld())))
      .getOrElse(ItemStack.EMPTY)
  }
}
