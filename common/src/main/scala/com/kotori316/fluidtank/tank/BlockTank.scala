package com.kotori316.fluidtank.tank

import cats.implicits.toShow
import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.MCImplicits.showPos
import com.kotori316.fluidtank.fluids.{PlatformFluidAccess, TransferFluid}
import com.mojang.serialization.MapCodec
import net.minecraft.core.component.DataComponents
import net.minecraft.core.{BlockPos, Direction, HolderLookup}
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.{Item, ItemStack}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityTicker, BlockEntityType}
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState, StateDefinition}
import net.minecraft.world.level.block.{Block, EntityBlock}
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.level.{BlockGetter, Level, LevelReader}
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.{CollisionContext, VoxelShape}
import net.minecraft.world.{InteractionHand, InteractionResult, ItemInteractionResult}
import org.jetbrains.annotations.Nullable

import scala.annotation.nowarn

abstract class BlockTank(val tier: Tier) extends Block(BlockBehaviour.Properties.of().strength(1f).dynamicShape().pushReaction(PushReaction.BLOCK).forceSolidOn()) with EntityBlock {

  registerDefaultState(this.getStateDefinition.any.setValue[TankPos, TankPos](TankPos.TANK_POS_PROPERTY, TankPos.SINGLE))
  final val itemBlock: ItemBlockTank = createTankItem()

  protected def createTankItem(): ItemBlockTank = new ItemBlockTank(this)

  protected def createBlockInstance(): BlockTank

  override protected final val codec: MapCodec[BlockTank] = BlockBehaviour.simpleCodec(_ => createBlockInstance())

  override final def asItem(): Item = itemBlock

  override def toString: String = s"Block{${tier.getBlockName}}"

  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = {
    new TileTank(tier, pos, state)
  }

  //noinspection ScalaDeprecation,deprecation
  override final def skipRendering(state: BlockState, adjacentBlockState: BlockState, side: Direction) = true

  override def useWithoutItem(blockState: BlockState, level: Level, pos: BlockPos, player: Player, blockHitResult: BlockHitResult): InteractionResult = {
    level.getBlockEntity(pos) match {
      case tank: TileTank =>
        if (!level.isClientSide) {
          player.displayClientMessage(tank.getConnection.getTextComponent, true)
        }
        InteractionResult.SUCCESS
      case tile =>
        FluidTankCommon.LOGGER.error(FluidTankCommon.MARKER_TANK, "There is not TileTank at {}, but {}", pos.show, tile)
        super.useWithoutItem(blockState, level, pos, player, blockHitResult)
    }
  }

  //noinspection ScalaDeprecation,deprecation
  override def useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): ItemInteractionResult = {
    level.getBlockEntity(pos) match {
      case tank: TileTank =>
        if (!stack.getItem.isInstanceOf[ItemBlockTank]) {
          // Move tank content
          if (PlatformFluidAccess.getInstance().isFluidContainer(stack)) {
            if (!level.isClientSide) {
              /*return*/
              TransferFluid.transferFluid(tank.getConnection, stack, player, hand)
                .map { r => TransferFluid.setItem(player, hand, r, pos); ItemInteractionResult.CONSUME }
                .getOrElse(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
            } else {
              /*return*/
              ItemInteractionResult.sidedSuccess(level.isClientSide)
            }
          } else {
            ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
          }
        } else {
          ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        }
      case tile =>
        FluidTankCommon.LOGGER.error(FluidTankCommon.MARKER_TANK, "There is not TileTank at {}, but {}", pos.show, tile)
        ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }
  }

  override def setPlacedBy(level: Level, pos: BlockPos, state: BlockState, @Nullable entity: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(level, pos, state, entity, stack)
    level.getBlockEntity(pos) match {
      case tank: TileTank => if (!level.isClientSide) tank.onBlockPlacedBy()
      case tile => FluidTankCommon.LOGGER.error(FluidTankCommon.MARKER_TANK, "There is not TileTank at {}, but {}", pos.show, tile)
    }
  }

  //noinspection ScalaDeprecation,deprecation
  override final def hasAnalogOutputSignal(state: BlockState): Boolean = true

  //noinspection ScalaDeprecation,deprecation
  override final def getAnalogOutputSignal(blockState: BlockState, level: Level, pos: BlockPos): Int = {
    level.getBlockEntity(pos) match {
      case tileTank: TileTank => tileTank.getComparatorLevel
      case tile => FluidTankCommon.LOGGER.error(FluidTankCommon.MARKER_TANK, "There is not TileTank at {}, but {}", pos.show, tile); 0
    }
  }

  //noinspection ScalaDeprecation,deprecation
  override final def onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moved: Boolean): Unit = {
    if (!state.is(newState.getBlock)) {
      level.getBlockEntity(pos) match {
        case tank: TileTank => tank.onDestroy()
        case tile => FluidTankCommon.LOGGER.error(FluidTankCommon.MARKER_TANK, "There is not TileTank at {}, but {}", pos.show, tile)
      }
      //noinspection ScalaDeprecation,deprecation
      super.onRemove(state, level, pos, newState, moved): @nowarn
    }
  }

  def saveTankNBT(tileEntity: BlockEntity, stack: ItemStack, provider: HolderLookup.Provider): Unit = {
    tileEntity match {
      case tank: TileTank =>
        if (!tank.getTank.isEmpty) stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tank.saveWithId(provider)))
        if (tank.hasCustomName) stack.set(DataComponents.CUSTOM_NAME, tank.getCustomName)
      case _ => // should be unreachable
    }
  }

  override def getCloneItemStack(level: LevelReader, pos: BlockPos, state: BlockState): ItemStack = {
    val stack = super.getCloneItemStack(level, pos, state)
    saveTankNBT(level.getBlockEntity(pos), stack, level.registryAccess())
    stack
  }

  //noinspection ScalaDeprecation,deprecation
  override def getShape(state: BlockState, worldIn: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = FluidTankCommon.TANK_SHAPE

  override def createBlockStateDefinition(builder: StateDefinition.Builder[Block, BlockState]): Unit = {
    super.createBlockStateDefinition(builder)
    builder.add(TankPos.TANK_POS_PROPERTY)
  }

  override def getTicker[T <: BlockEntity](level: Level, state: BlockState, blockEntityType: BlockEntityType[T]): BlockEntityTicker[T] = {
    if (level.isClientSide) {
      super.getTicker(level, state, blockEntityType)
    } else {
      if (PlatformTankAccess.isTankType(blockEntityType)) {
        (_, _, _, tile) => tile.asInstanceOf[TileTank].onTickLoading()
      } else {
        null
      }
    }
  }
}
