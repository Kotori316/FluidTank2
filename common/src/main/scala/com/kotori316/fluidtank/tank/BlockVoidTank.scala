package com.kotori316.fluidtank.tank

import net.minecraft.core.component.DataComponents
import net.minecraft.core.{BlockPos, HolderLookup}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class BlockVoidTank extends BlockTank(Tier.VOID) {

  override protected def createTankItem() = new ItemBlockVoidTank(this)

  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = new TileVoidTank(pos, state)

  // Save tank name only
  override def saveTankNBT(tileEntity: BlockEntity, stack: ItemStack, provider: HolderLookup.Provider): Unit = {
    tileEntity match {
      case tank: TileTank =>
        if (tank.hasCustomName) stack.set(DataComponents.CUSTOM_NAME, tank.getCustomName)
      case _ => // should be unreachable
    }
  }

  override protected def createBlockInstance(): BlockTank = {
    val constructor = getClass.getConstructor()
    constructor.newInstance()
  }
}
