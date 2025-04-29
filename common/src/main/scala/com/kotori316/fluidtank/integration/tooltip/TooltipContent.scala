package com.kotori316.fluidtank.integration.tooltip

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, PlatformFluidAccess}
import com.kotori316.fluidtank.tank.{Tier, TileTank, TileVoidTank}
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.chaining.scalaUtilChainingOps

object TooltipContent {
  final val JADE_TOOLTIP_UID = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "jade_plugin")
  final val TOP_TOOLTIP_UID = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "top_plugin")
  final val JADE_CONFIG_SHORT = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "short_info")
  final val JADE_CONFIG_COMPACT = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "compact_number")
  private final val KEY_TIER = TileTank.KEY_TIER
  private final val KEY_FLUID = "fluid"
  private final val KEY_CAPACITY = "capacity"
  private final val KEY_COMPARATOR = "comparator"
  private final val KEY_CREATIVE = "hasCreative"

  final def addServerData(compoundTag: CompoundTag, entity: BlockEntity): Unit = {
    entity match {
      case voidTank: TileVoidTank =>
        compoundTag.putString(KEY_TIER, voidTank.tier.toString)
      case tank: TileTank =>
        compoundTag.putString(KEY_TIER, tank.tier.toString)
        compoundTag.put(KEY_FLUID, tank.getConnection.getContent.getOrElse(FluidAmountUtil.EMPTY).getTag)
        compoundTag.putBoolean(KEY_CREATIVE, tank.getConnection.hasCreative)
        if (!tank.getConnection.hasCreative) {
          compoundTag.putLong(KEY_CAPACITY, tank.getConnection.capacity.asDisplay)
          compoundTag.putInt(KEY_COMPARATOR, tank.getConnection.getComparatorLevel)
        }
      case _ =>
    }
  }

  final def getTooltipTextJava(tankData: CompoundTag, tank: TileTank, isShort: Boolean, isCompact: Boolean, locale: Locale): java.util.List[Component] = {
    getTooltipText(
      tier = tank.tier,
      fluid = FluidAmountUtil.fromTag(tankData.getCompoundOrEmpty(KEY_FLUID)),
      capacity = tankData.getLongOr(KEY_CAPACITY, 0),
      comparator = tankData.getIntOr(KEY_COMPARATOR, 0),
      hasCreative = tankData.getBooleanOr(KEY_CREATIVE, false),
      isShort = isShort,
      isCompact = isCompact,
      locale = locale
    ).asJava
  }

  final def getTooltipText(tier: Tier, fluid: FluidAmount, capacity: Long, comparator: Int, hasCreative: Boolean,
                           isShort: Boolean, isCompact: Boolean, locale: Locale): Seq[Component] = {
    val numberFormat: Long => String = if (isCompact) {
      val formatter = NumberFormat.getCompactNumberInstance(locale, NumberFormat.Style.SHORT)
        .tap(_.setMinimumFractionDigits(1))
        .tap(_.setRoundingMode(RoundingMode.DOWN))
      (n: Long) => formatter.format(n)
    } else {
      (n: Long) => n.toString
    }
    val fluidName = if (fluid.nonEmpty) {
      PlatformFluidAccess.getInstance().getDisplayName(fluid)
    } else {
      Component.translatable("chat.fluidtank.empty")
    }

    if (isShort) {
      if (tier == Tier.VOID) Seq.empty
      else if (hasCreative) {
        Seq(fluidName)
      } else {
        Seq(Component.translatable("fluidtank.waila.short",
          fluidName,
          numberFormat(fluid.amount.asDisplay),
          numberFormat(capacity),
        ))
      }
    } else {
      if (tier == Tier.VOID) {
        Seq.empty
      } else if (hasCreative) {
        Seq(
          fluidName,
        )
      } else {
        Seq(
          fluidName,
          Component.translatable("fluidtank.waila.amount", numberFormat(fluid.amount.asDisplay)),
          Component.translatable("fluidtank.waila.capacity", numberFormat(capacity)),
          Component.translatable("fluidtank.waila.comparator", comparator),
        )
      }
    }
  }
}
