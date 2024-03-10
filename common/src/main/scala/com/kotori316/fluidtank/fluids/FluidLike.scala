package com.kotori316.fluidtank.fluids

import cats.Hash
import com.kotori316.fluidtank.FluidTankCommon
import net.minecraft.core.component.{DataComponentPatch, DataComponents}
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.alchemy.{Potion, PotionContents}
import net.minecraft.world.level.material.{Fluid, Fluids}

import java.util.Locale
import scala.jdk.OptionConverters.RichOptional
import scala.util.Try

sealed trait FluidLike {
  def isGaseous: Boolean

  def getKey: ResourceLocation
}

case class VanillaFluid(fluid: Fluid) extends FluidLike {
  override def isGaseous: Boolean = PlatformFluidAccess.getInstance().isGaseous(fluid)

  override def getKey: ResourceLocation = BuiltInRegistries.FLUID.getKey(fluid)
}

case class VanillaPotion(potionType: PotionType) extends FluidLike {
  override def isGaseous: Boolean = false

  override def getKey: ResourceLocation = {
    new ResourceLocation(FluidTankCommon.modId, ("potion_" + potionType.name()).toLowerCase(Locale.ROOT))
  }

  def getVanillaPotionName(nbt: Option[DataComponentPatch]): Component = {
    val potion = nbt.map(_.get(DataComponents.POTION_CONTENTS)).filter(_ != null).flatMap(_.toScala).getOrElse(PotionContents.EMPTY)
    val prefix = potionType.getItem.getDescriptionId + ".effect."
    Component.translatable(Potion.getName(potion.potion(), prefix))
  }
}

object FluidLike {
  implicit final val hashFluidLike: Hash[FluidLike] = Hash.fromUniversalHashCode

  final val FLUID_EMPTY = VanillaFluid(Fluids.EMPTY)
  final val FLUID_WATER = VanillaFluid(Fluids.WATER)
  final val FLUID_LAVA = VanillaFluid(Fluids.LAVA)

  final val POTION_NORMAL = VanillaPotion(PotionType.NORMAL)
  final val POTION_SPLASH = VanillaPotion(PotionType.SPLASH)
  final val POTION_LINGERING = VanillaPotion(PotionType.LINGERING)

  private final val FLUID_CACHE: scala.collection.mutable.Map[Fluid, VanillaFluid] = scala.collection.mutable.Map(
    Fluids.EMPTY -> FLUID_EMPTY,
    Fluids.WATER -> FLUID_WATER,
    Fluids.LAVA -> FLUID_LAVA,
  )
  private final val POTION_CACHE: scala.collection.mutable.Map[PotionType, VanillaPotion] = scala.collection.mutable.Map(
    PotionType.NORMAL -> POTION_NORMAL,
    PotionType.SPLASH -> POTION_SPLASH,
    PotionType.LINGERING -> POTION_LINGERING,
  )

  def of(fluid: Fluid): VanillaFluid = {
    FLUID_CACHE.getOrElseUpdate(fluid, VanillaFluid(fluid))
  }

  def of(potionType: PotionType): VanillaPotion = {
    POTION_CACHE.getOrElseUpdate(potionType, VanillaPotion(potionType))
  }

  def asFluid(fluidLike: FluidLike, fallback: Fluid): Fluid = {
    fluidLike match {
      case VanillaFluid(fluid) => fluid
      case VanillaPotion(_) => fallback
    }
  }

  def fromResourceLocation(key: ResourceLocation): FluidLike = {
    if (key.getNamespace == FluidTankCommon.modId && key.getPath.startsWith("potion_")) {
      val potionType = Try(PotionType.valueOf(key.getPath.substring(7).toUpperCase(Locale.ROOT)))
      potionType.map(FluidLike.of).getOrElse {
        FluidTankCommon.LOGGER.error(FluidTankCommon.MARKER_FLUID_LIKE, "[FluidLike] Get unknown potion type {}", key)
        FLUID_EMPTY
      }
    } else {
      // this is a fluid
      val fluid = Option(key).filter(BuiltInRegistries.FLUID.containsKey)
        .map(BuiltInRegistries.FLUID.get)
      fluid.map(FluidLike.of).getOrElse {
        FluidTankCommon.LOGGER.error(FluidTankCommon.MARKER_FLUID_LIKE, "[FluidLike] Get unknown fluid type {}", key)
        FLUID_EMPTY
      }
    }
  }
}
