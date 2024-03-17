package com.kotori316.fluidtank.forge.config

import com.kotori316.fluidtank.config.ConfigData
import com.kotori316.fluidtank.tank.Tier
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.loading.FMLLoader

import java.util.Locale
import scala.jdk.javaapi.FunctionConverters
import scala.util.Try

class FluidTankConfig(builder: ForgeConfigSpec.Builder) {
  builder.push("client")
  private final val renderLowerBound: ForgeConfigSpec.DoubleValue = builder.comment("The lower bound of tank renderer")
    .comment(s"Default: ${ConfigData.DEFAULT.renderLowerBound}")
    .defineInRange("renderLowerBound", ConfigData.DEFAULT.renderLowerBound, 0d, 1d)
  private final val renderUpperBound: ForgeConfigSpec.DoubleValue = builder.comment("The upper bound of tank renderer")
    .comment(s"Default: ${ConfigData.DEFAULT.renderUpperBound}")
    .defineInRange("renderUpperBound", ConfigData.DEFAULT.renderUpperBound, 0d, 1d)
  builder.pop()

  builder.push("tank")
  builder.comment("The capacity of each tanks", "Unit is fabric one, 81000 unit is 1000 mB.").push("capacity")

  private final val capacities: Map[Tier, ForgeConfigSpec.ConfigValue[String]] = Tier.values().toSeq.map { t =>
    val defaultCapacity = ConfigData.DEFAULT.capacityMap(t)
    t -> builder.comment(s"Capacity of $t", s"Default: $defaultCapacity unit(= ${defaultCapacity / 81} mB)")
      .define[String](t.name().toLowerCase(Locale.ROOT), defaultCapacity.toString(),
        FunctionConverters.asJavaPredicate[AnyRef] {
          case s: String => Try(BigInt(s)).isSuccess
          case _ => false
        })
  }.toMap

  builder.pop()

  private final val debug: ForgeConfigSpec.BooleanValue = builder.comment("Debug mode")
    .comment(s"Default: ${ConfigData.DEFAULT.debug || !FMLLoader.isProduction}")
    .define("debug", ConfigData.DEFAULT.debug || !FMLLoader.isProduction)
  private final val changeItemInCreative: ForgeConfigSpec.BooleanValue = builder.comment("True to allow to modify items in player attracting")
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
