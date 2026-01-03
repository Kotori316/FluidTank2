package com.kotori316.fluidtank.neoforge.integration.top

/*
import com.kotori316.fluidtank.fluids.FluidAmountUtil
import com.kotori316.fluidtank.integration.tooltip.TooltipContent
import com.kotori316.fluidtank.tank.TileTank
import mcjty.theoneprobe.api.{IProbeHitData, IProbeInfo, IProbeInfoProvider, ProbeMode}
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

import java.util.Locale

object FluidTankTopProvider extends IProbeInfoProvider {
  override def getID: Identifier = TooltipContent.TOP_TOOLTIP_UID

  override def addProbeInfo(probeMode: ProbeMode, probeInfo: IProbeInfo, player: Player, level: Level, blockState: BlockState, hitData: IProbeHitData): Unit = {
    level.getBlockEntity(hitData.getPos) match {
      case tileTank: TileTank =>
        val connection = tileTank.getConnection
        val texts = TooltipContent.getTooltipText(
          tier = tileTank.tier,
          fluid = connection.getContent.getOrElse(FluidAmountUtil.EMPTY),
          capacity = connection.capacity.asDisplay,
          comparator = connection.getComparatorLevel,
          hasCreative = connection.hasCreative,
          isShort = false,
          isCompact = false,
          locale = Locale.US
        )
        texts.foreach(probeInfo.text)
      case _ =>
    }
  }
}*/
