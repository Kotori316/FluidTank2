package com.kotori316.fluidtank.cat

import com.kotori316.fluidtank.fluids.PlatformFluidAccess
import net.minecraft.ChatFormatting
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState, StateDefinition}
import net.minecraft.world.level.block.{Block, EntityBlock, Mirror, Rotation}
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.BlockHitResult

import scala.jdk.javaapi.CollectionConverters

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

  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = PlatformChestAsTankAccess.getInstance().createCATEntity(pos, state)

  override def useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult = {
    if (!player.isCrouching) {
      if (!level.isClientSide) {
        val fluids = PlatformChestAsTankAccess.getInstance().getCATFluids(level, pos)
        if (fluids.isEmpty) {
          player.displayClientMessage(Component.translatable("chat.fluidtank.cat_empty"), false)
        } else {
          player.displayClientMessage(Component.translatable("chat.fluidtank.cat_fluid"), false)
        }
        for (f <- CollectionConverters.asScala(fluids)) {
          val message = Component.literal("[")
            .append(PlatformFluidAccess.getInstance().getDisplayName(f).copy().withStyle(ChatFormatting.AQUA))
            .append("]")
            .append(" " + f.amount.asDisplay + " mB")
          player.displayClientMessage(message, false)
        }
      }
      InteractionResult.sidedSuccess(level.isClientSide)
    } else {
      InteractionResult.PASS
    }
  }

  //noinspection ScalaDeprecation,deprecation
  override def rotate(state: BlockState, rotation: Rotation): BlockState = {
    state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)))
  }

  //noinspection ScalaDeprecation,deprecation
  override def mirror(state: BlockState, mirror: Mirror): BlockState = {
    this.rotate(state, mirror.getRotation(state.getValue(BlockStateProperties.FACING)))
  }
}

object BlockChestAsTank {
  final val NAME = "chest_as_tank"
}
