package com.kotori316.fluidtank.tank

import com.kotori316.fluidtank.contents.VoidTank
import com.kotori316.fluidtank.fluids.fluidAccess
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class TileVoidTank(t: BlockEntityType[? <: TileTank], p: BlockPos, s: BlockState)
  extends TileTank(Tier.CREATIVE, t, p, s) {
  def this(p: BlockPos, s: BlockState) = {
    this(PlatformTankAccess.getInstance().getVoidType, p, s)
  }

  setTank(VoidTank.apply)

  override def getVisualTank: VisualTank = throw new UnsupportedOperationException("Visual tank for Void tank is not needed as void tank never has content.")
}
