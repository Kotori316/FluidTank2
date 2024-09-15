package com.kotori316.fluidtank.tank

import com.kotori316.fluidtank.config.PlatformConfigAccess
import com.kotori316.fluidtank.contents.GenericUnit
import org.junit.jupiter.api.Assertions.{assertEquals, assertThrows}
import org.junit.jupiter.api.{AfterEach, BeforeEach, Test}

class TierConfigTest {
  private var configInstance: PlatformConfigAccess = scala.compiletime.uninitialized

  @BeforeEach
  def before(): Unit = {
    this.configInstance = PlatformConfigAccess.getInstance()
  }

  @AfterEach
  def after(): Unit = {
    PlatformConfigAccess.setInstance(this.configInstance)
  }

  @Test
  def modifyTierCapacity(): Unit = {
    val config = PlatformConfigAccess.getInstance().getConfig
      .copy(capacityMap = Map(Tier.WOOD -> GenericUnit.ONE_BUCKET.value))
    PlatformConfigAccess.setInstance(() => config)

    assertEquals(GenericUnit.ONE_BUCKET.value, Tier.WOOD.getCapacity)
  }

  @Test
  def invalidCapacityMap(): Unit = {
    val config = PlatformConfigAccess.getInstance().getConfig
      .copy(capacityMap = Map(Tier.WOOD -> GenericUnit.ONE_BUCKET.value))
    PlatformConfigAccess.setInstance(() => config)

    assertThrows(classOf[IllegalStateException], () => Tier.STONE.getCapacity)
  }
}
