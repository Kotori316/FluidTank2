package com.kotori316.fluidtank.fluids

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.config.PlatformConfigAccess
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.config.Configurator

import java.security.SecureClassLoader

private[fluids] object DebugLogging {
  val ENABLED: Boolean = PlatformConfigAccess.getInstance().getConfig.debug

  val LOGGER: Logger = {
    class DummyClassLoader extends SecureClassLoader

    if (ENABLED) {
      val context = Configurator.initialize("fluidtank-config", new DummyClassLoader,
        classOf[FluidTankCommon].getResource("/fluidtank-log4j2.xml").toURI)
      context.getLogger("FluidTankDebug")
    } else {
      null
    }
  }
}
