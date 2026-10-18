package com.kotori316.fluidtank.neoforge.config

import com.kotori316.fluidtank.config.ConfigData
import com.kotori316.fluidtank.tank.Tier
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.common.ModConfigSpec

import java.util.Locale
import scala.jdk.javaapi.FunctionConverters
import scala.util.Try

class FluidTankConfig(serverBuilder: ModConfigSpec.Builder, clientBuilder: ModConfigSpec.Builder) {
  clientBuilder.push("client")
  private final val renderLowerBound: ModConfigSpec.DoubleValue = clientBuilder.comment("The lower bound of tank renderer")
    .comment(s"Default: ${ConfigData.DEFAULT.renderLowerBound}")
    .defineInRange("renderLowerBound", ConfigData.DEFAULT.renderLowerBound, 0d, 1d)
  private final val renderUpperBound: ModConfigSpec.DoubleValue = clientBuilder.comment("The upper bound of tank renderer")
    .comment(s"Default: ${ConfigData.DEFAULT.renderUpperBound}")
    .defineInRange("renderUpperBound", ConfigData.DEFAULT.renderUpperBound, 0d, 1d)
  clientBuilder.pop()

  serverBuilder.push("tank")
  serverBuilder.comment("The capacity of each tanks", "Unit is fabric one, 81000 unit is 1000 mB.").push("capacity")

  private final val capacities: Map[Tier, ModConfigSpec.ConfigValue[String]] = Tier.values()
    .toSeq
    .filter(t => t.isNormalTankTier)
    .map { t =>
      val defaultCapacity = ConfigData.DEFAULT.capacityMap(t)
      t -> serverBuilder.comment(s"Capacity of $t tank", s"Default: $defaultCapacity unit(= ${defaultCapacity / 81} mB)")
        .define[String](t.name().toLowerCase(Locale.ROOT), defaultCapacity.toString(),
          FunctionConverters.asJavaPredicate[AnyRef] {
            case s: String => Try(BigInt(s)).isSuccess
            case _ => false
          })
    }
    .toMap

  serverBuilder.pop()

  private final val debug: ModConfigSpec.BooleanValue = serverBuilder.comment("Debug mode")
    .comment(s"Default: ${ConfigData.DEFAULT.debug || !FMLEnvironment.isProduction}")
    .define("debug", ConfigData.DEFAULT.debug || !FMLEnvironment.isProduction)
  private final val changeItemInCreative: ModConfigSpec.BooleanValue = serverBuilder.comment("True to allow to modify items in player attracting")
    .comment(s"Default: ${ConfigData.DEFAULT.changeItemInCreative}")
    .define("changeItemInCreative", ConfigData.DEFAULT.changeItemInCreative)

  serverBuilder.pop()

  // Here because IDE highlighting is wrong
  private def builtinTierMap: Map[Tier, BigInt] = Tier.values()
    .filterNot(_.isNormalTankTier)
    .map(t => t -> Tier.getDefaultCapacityMap.get(t))
    .toMap

  final val serverSpec: ModConfigSpec = serverBuilder.build()
  final val clientSpec: ModConfigSpec = clientBuilder.build()

  private def getValue[T](spec: ModConfigSpec, value: ModConfigSpec.ConfigValue[T]): T =
    if (spec.isLoaded) value.get() else value.getDefault

  def createConfigData: ConfigData = {
    ConfigData(
      capacityMap = this.capacities.map { case (tier, value) => tier -> BigInt(getValue(serverSpec, value)) } ++ builtinTierMap,
      renderLowerBound = getValue[java.lang.Double](clientSpec, this.renderLowerBound),
      renderUpperBound = getValue[java.lang.Double](clientSpec, this.renderUpperBound),
      debug = getValue[java.lang.Boolean](serverSpec, this.debug),
      changeItemInCreative = getValue[java.lang.Boolean](serverSpec, this.changeItemInCreative)
    )
  }
}
