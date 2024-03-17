package com.kotori316.fluidtank.fabric.config

import cats.data.{Ior, IorNec, NonEmptyChain}
import cats.implicits.catsSyntaxFoldableOps0
import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.config.FluidTankConfig.E
import com.kotori316.fluidtank.config.{ConfigData, FluidTankConfig, PlatformConfigAccess}
import net.fabricmc.loader.api.FabricLoader

class FabricPlatformConfigAccess extends PlatformConfigAccess {
  private final val configData: ConfigData = {
    val config: IorNec[E, ConfigData] = FluidTankConfig.loadFile(FabricLoader.getInstance().getConfigDir, "fluidtank-common.json",
      forTest = FabricLoader.getInstance().isDevelopmentEnvironment)
    config match {
      case Ior.Right(a: ConfigData) => a
      case Ior.Both(e: NonEmptyChain[E], partial: ConfigData) => handleMigration(e, partial)
      case Ior.Left(a: NonEmptyChain[E]) => handleMigration(a, ConfigData.DEFAULT)
    }
  }

  private def handleMigration(e: NonEmptyChain[E], partial: ConfigData): ConfigData = {
    if (e.contains(FluidTankConfig.FileNotFound)
      || e.exists(_.isInstanceOf[FluidTankConfig.KeyNotFound]
      || e.exists(_.isInstanceOf[FluidTankConfig.InvalidValue]))) {
      FluidTankConfig.createFile(FabricLoader.getInstance().getConfigDir, "fluidtank-common.json", partial)
      FluidTankCommon.LOGGER.warn("Created valid config file.")
    }
    FluidTankCommon.LOGGER.warn("Get error in loading config, using partial value. Errors: {}", e.mkString_(", "))
    partial
  }

  override def getConfig: ConfigData = configData

}
