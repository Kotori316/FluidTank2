package com.kotori316.fluidtank

import cats.implicits.catsSyntaxEq
import com.google.gson.{GsonBuilder, JsonObject}
import com.kotori316.fluidtank.config.PlatformConfigAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.slf4j.{Log4jLogger, Log4jMarkerFactory}
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger

import java.io.{BufferedReader, InputStreamReader}
import java.security.SecureClassLoader
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Using

object DebugLogging {
  val ENABLED: Boolean = PlatformConfigAccess.getInstance().getConfig.debug

  val LOGGER: Logger = {
    class DummyClassLoader extends SecureClassLoader

    val context = Configurator.initialize("fluidtank-config", new DummyClassLoader,
      classOf[FluidTankCommon].getResource("/fluidtank-log4j2.xml").toURI)
    if (context == null) {
      FluidTankCommon.LOGGER.error("Failed to initialize fluidtank-log4j2.xml")

      {
        val stream = classOf[FluidTankCommon].getResourceAsStream("/fluidtank-log4j2.xml")
        if (stream != null) {
          Using(new BufferedReader(new InputStreamReader(stream))) { r =>
            val text = r.lines().collect(java.util.stream.Collectors.joining(System.lineSeparator()))
            FluidTankCommon.LOGGER.error("Log context for {} is unavailable. log-config: {}", FluidTankCommon.modId, text)
          }
        } else {
          FluidTankCommon.LOGGER.error("Failed to load fluidtank-log4j2.xml")
        }
      }
      NOPLogger.NOP_LOGGER
    } else {
      val l = context.getLogger("FluidTankDebug")
      if (!ENABLED) {
        l.setLevel(Level.INFO)
      }
      FluidTankCommon.LOGGER.info("Successfully initialized debug logger for {}, level: {}", FluidTankCommon.modId, l.getLevel)
      new Log4jLogger(new Log4jMarkerFactory(), l, "FluidTankDebug")
    }
  }

  def initialLog(server: MinecraftServer): Unit = {
    // Config
    LOGGER.info("Config {}", new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
      .toJson(PlatformConfigAccess.getInstance().getConfig.createJson))
    // Recipes fo FluidTank
    val noPretty = new GsonBuilder().disableHtmlEscaping().create()
    server.getRecipeManager.getRecipes.asScala
      .filter(_.getId.getNamespace === FluidTankCommon.modId)
      .map(r => (r.getId, r.getResultItem(server.registryAccess()), r.getIngredients.asScala.map(_.toJson).zipWithIndex.foldLeft(new JsonObject()) { case (a, (e, i)) => a.add(i.toString, e); a }))
      .map { case (id, stack, value) => s"$id ${BuiltInRegistries.ITEM.getKey(stack.getItem)} x${stack.getCount}(tag: ${stack.getTag}) -> ${noPretty.toJson(value)}" }
      .zipWithIndex
      .foreach { case (s, index) => LOGGER.info("{} {}", index + 1, s) }
  }
}
