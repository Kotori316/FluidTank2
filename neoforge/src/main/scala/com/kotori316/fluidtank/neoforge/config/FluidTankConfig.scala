package com.kotori316.fluidtank.neoforge.config

import com.kotori316.fluidtank.config.ConfigData
import com.kotori316.fluidtank.tank.Tier
import net.neoforged.neoforge.common.ModConfigSpec

import java.util.Locale
import scala.jdk.javaapi.FunctionConverters
import scala.util.Try

class FluidTankConfig(builder: ModConfigSpec.Builder) {
  builder.push("client")
  private final val renderLowerBound: ModConfigSpec.DoubleValue = builder.comment("The lower bound of tank renderer")
    .comment(s"Default: ${ConfigData.DEFAULT.renderLowerBound}")
    .defineInRange("renderLowerBound", ConfigData.DEFAULT.renderLowerBound, 0d, 1d)
  private final val renderUpperBound: ModConfigSpec.DoubleValue = builder.comment("The upper bound of tank renderer")
    .comment(s"Default: ${ConfigData.DEFAULT.renderUpperBound}")
    .defineInRange("renderUpperBound", ConfigData.DEFAULT.renderUpperBound, 0d, 1d)
  builder.pop()

  builder.push("tank")
  builder.comment("The capacity of each tanks", "Unit is fabric one, 1000 mB is 81000 unit.").push("capacity")

  private final val capacities: Map[Tier, ModConfigSpec.ConfigValue[String]] = Tier.values().toSeq.map { t =>
    val defaultCapacity = ConfigData.DEFAULT.capacityMap(t)
    t -> builder.comment(s"Capacity of $t", s"Default: ${defaultCapacity / 81} mB(= $defaultCapacity unit)")
      .define[String](t.name().toLowerCase(Locale.ROOT), defaultCapacity.toString(),
        FunctionConverters.asJavaPredicate[AnyRef] {
          case s: String => Try(BigInt(s)).isSuccess
          case _ => false
        })
  }.toMap

  builder.pop()

  private final val debug: ModConfigSpec.BooleanValue = builder.comment("Debug mode")
    .comment(s"Default: ${ConfigData.DEFAULT.debug}")
    .define("debug", ConfigData.DEFAULT.debug)
  private final val changeItemInCreative: ModConfigSpec.BooleanValue = builder.comment("True to allow to modify items in player attracting")
    .comment(s"Default: ${ConfigData.DEFAULT.changeItemInCreative}")
    .define("changeItemInCreative", ConfigData.DEFAULT.changeItemInCreative)

  builder.pop()

  def createConfigData: ConfigData = {
    ConfigData(
      capacityMap = this.capacities.map { case (tier, value) => tier -> BigInt(value.get()) },
      renderLowerBound = this.renderLowerBound.get(),
      renderUpperBound = this.renderUpperBound.get(),
      debug = this.debug.get(),
      changeItemInCreative = this.changeItemInCreative.get()
    )
  }
}
