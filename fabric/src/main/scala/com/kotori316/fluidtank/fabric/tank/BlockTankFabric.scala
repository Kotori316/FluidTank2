package com.kotori316.fluidtank.fabric.tank

import com.kotori316.fluidtank.tank.{BlockTank, Tier}
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class BlockTankFabric(t: Tier) extends BlockTank(t) {
  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = {
    new TileTankFabric(t, pos, state)
  }
}
