package com.kotori316.fluidtank.cat

import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState, StateDefinition}
import net.minecraft.world.level.block.{Block, EntityBlock}
import net.minecraft.world.level.material.PushReaction

class BlockChestAsTank extends Block(BlockBehaviour.Properties.of()
  .strength(0.7f).pushReaction(PushReaction.BLOCK).forceSolidOn())
  with EntityBlock {

  registerDefaultState(getStateDefinition.any().setValue(BlockStateProperties.FACING, Direction.NORTH))

  override def createBlockStateDefinition(builder: StateDefinition.Builder[Block, BlockState]): Unit = {
    super.createBlockStateDefinition(builder)
    builder.add(BlockStateProperties.FACING)
  }

  override def getStateForPlacement(context: BlockPlaceContext): BlockState = {
    val facing = context.getClickedFace.getOpposite
    defaultBlockState().setValue(BlockStateProperties.FACING, facing)
  }

  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = null
}

object BlockChestAsTank {
  final val NAME = "chest_as_tank"
}
