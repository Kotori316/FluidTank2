package com.kotori316.fluidtank

import cats.implicits.catsSyntaxEq
import com.google.gson.{GsonBuilder, JsonObject}
import com.kotori316.fluidtank.config.PlatformConfigAccess
import com.kotori316.fluidtank.item.PlatformItemAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.crafting.{Ingredient, Recipe}
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.{Level, Logger}

import java.security.SecureClassLoader
import scala.jdk.CollectionConverters.CollectionHasAsScala

object DebugLogging {
  val ENABLED: Boolean = PlatformConfigAccess.getInstance().getConfig.debug

  val LOGGER: Logger = {
    class DummyClassLoader extends SecureClassLoader

    val context = Configurator.initialize("fluidtank-config", new DummyClassLoader,
      classOf[FluidTankCommon].getResource("/fluidtank-log4j2.xml").toURI)
    val l = context.getLogger("FluidTankDebug")
    if (!ENABLED) {
      l.setLevel(Level.INFO)
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
      .filter(_.id().getNamespace === FluidTankCommon.modId)
      .map(h => (h.id(), h.value().asInstanceOf[Recipe[?]]))
      .map { case (id, r) => (id, r.getResultItem(server.registryAccess()), ingredientAsMap(r.getIngredients.asScala)) }
      .map { case (id, stack, value) =>
        val location = BuiltInRegistries.ITEM.getKey(stack.getItem)
        s"$id $location x${stack.getCount}(${stack.getTag}) -> ${noPretty.toJson(value)}" }
      .zipWithIndex
      .foreach { case (s, index) => LOGGER.info("{} {}", index + 1, s) }
  }

  private def ingredientAsMap(ingredients: scala.collection.Iterable[Ingredient]): JsonObject = {
    ingredients.map(PlatformItemAccess.getInstance().ingredientToJson)
      .zipWithIndex
      .foldLeft(new JsonObject()) { case (a, (e, i)) => a.add(i.toString, e); a }
  }
}