package com.kotori316.fluidtank.cat

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.fluids.PlatformFluidAccess
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.Registries
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.network.chat.Component
import net.minecraft.resources.{Identifier, ResourceKey}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState, StateDefinition}
import net.minecraft.world.level.block.{Block, EntityBlock, Mirror, Rotation}
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.{InteractionHand, InteractionResult}

import scala.jdk.javaapi.CollectionConverters

abstract class BlockChestAsTank extends Block(BlockBehaviour.Properties.of()
  .strength(0.7f).pushReaction(PushReaction.BLOCK).forceSolidOn()
  .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FluidTankCommon.modId, BlockChestAsTank.NAME)))
)
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
      if (!level.isClientSide()) {
        val fluids = PlatformChestAsTankAccess.getInstance().getCATFluids(level, pos)
        if (fluids.isEmpty) {
          player.sendSystemMessage(Component.translatable("chat.fluidtank.cat_empty"))
        } else {
          player.sendSystemMessage(Component.translatable("chat.fluidtank.cat_fluid"))
        }
        for (f <- CollectionConverters.asScala(fluids)) {
          val message = Component.literal("[")
            .append(PlatformFluidAccess.getInstance().getDisplayName(f).copy().withStyle(ChatFormatting.AQUA))
            .append("]")
            .append(" " + f.amount.asDisplay + " mB")
          player.sendSystemMessage(message)
        }
      }
      InteractionResult.SUCCESS
    } else {
      InteractionResult.PASS
    }
  }

  override def useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult = {
    if (PlatformFluidAccess.getInstance().isFluidContainer(stack)) {
      if (!level.isClientSide()) {
        transferFluid(level, pos, player, hand, stack)
      } else {
        InteractionResult.SUCCESS_SERVER
      }
    } else {
      super.useItemOn(stack, state, level, pos, player, hand, hit)
    }
  }

  override def rotate(state: BlockState, rotation: Rotation): BlockState = {
    state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)))
  }

  override def mirror(state: BlockState, mirror: Mirror): BlockState = {
    this.rotate(state, mirror.getRotation(state.getValue(BlockStateProperties.FACING)))
  }

  def transferFluid(level: Level, pos: BlockPos, player: Player, hand: InteractionHand, stack: ItemStack): InteractionResult
}

object BlockChestAsTank {
  final val NAME = "chest_as_tank"
}
