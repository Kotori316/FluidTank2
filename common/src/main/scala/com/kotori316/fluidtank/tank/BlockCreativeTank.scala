package com.kotori316.fluidtank.tank

import net.minecraft.core.{BlockPos, HolderLookup}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class BlockCreativeTank extends BlockTank(Tier.CREATIVE) {
  override protected def createTankItem() = new ItemBlockCreativeTank(this)

  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = new TileCreativeTank(pos, state)

  // Do nothing for Creative Tank
  override def saveTankNBT(tileEntity: BlockEntity, stack: ItemStack, provider: HolderLookup.Provider): Unit = ()

  override protected def createBlockInstance(): BlockTank = {
    val constructor = getClass.getConstructor()
    constructor.newInstance()
  }
}
