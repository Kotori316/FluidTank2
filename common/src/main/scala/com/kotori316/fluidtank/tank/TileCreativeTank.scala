package com.kotori316.fluidtank.tank

import com.kotori316.fluidtank.contents.CreativeTank
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class TileCreativeTank(t: BlockEntityType[? <: TileTank], p: BlockPos, s: BlockState)
  extends TileTank(Tier.CREATIVE, t, p, s) {
  def this(p: BlockPos, s: BlockState) = {
    this(PlatformTankAccess.getInstance().getCreativeType, p, s)
  }

  setTank(new CreativeTank(this.getTank.content, this.getTank.capacity))
}
