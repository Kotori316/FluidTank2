package com.kotori316.fluidtank.config

import com.google.gson.JsonObject
import com.kotori316.fluidtank.tank.Tier

import java.util.Locale
import scala.jdk.javaapi.CollectionConverters

case class ConfigData
(
  capacityMap: Map[Tier, BigInt],
  renderLowerBound: Double,
  renderUpperBound: Double,
  debug: Boolean,
  changeItemInCreative: Boolean,
) {
  def createJson: JsonObject = {
    val json = new JsonObject
    json.addProperty("renderLowerBound", renderLowerBound)
    json.addProperty("renderUpperBound", renderUpperBound)
    json.addProperty("debug", debug)
    json.addProperty("changeItemInCreative", changeItemInCreative)

    val capacities = new JsonObject
    capacityMap.foreach { case (tier, int) =>
      capacities.addProperty(tier.name().toLowerCase(Locale.ROOT), int.toString())
    }
    json.add("capacities", capacities)
    json
  }
}

object ConfigData {
  final val DEFAULT: ConfigData = ConfigData(
    capacityMap = CollectionConverters.asScala(Tier.getDefaultCapacityMap).toMap,
    renderLowerBound = 0.001d,
    renderUpperBound = 1d - 0.001d,
    debug = false,
    changeItemInCreative = false,
  )

  final val FOR_TEST: ConfigData = DEFAULT.copy(debug = true)
}
