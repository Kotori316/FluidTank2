package com.kotori316.fluidtank.fluids

import com.kotori316.fluidtank.config.PlatformConfigAccess
import net.minecraft.core.BlockPos
import net.minecraft.sounds.{SoundEvent, SoundSource}
import net.minecraft.util.Prediction
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

object TransferFluid {
  case class Result(stack: ItemStack, sound: Option[SoundEvent], shouldMove: Boolean)

  def transferFluid(connection: FluidConnection, playerStack: ItemStack, player: Player, hand: InteractionHand): Option[Result] = {
    tryFillTank(connection, playerStack, player, hand) orElse tryFillContainer(connection, playerStack, player, hand)
  }

  private def tryFillTank(connection: FluidConnection, playerStack: ItemStack, player: Player, hand: InteractionHand): Option[Result] = {
    val toFill = PlatformFluidAccess.getInstance().getFluidContained(playerStack)
    if (toFill.isEmpty) return None

    val fillSimulate = connection.getHandler.fill(toFill, execute = false)
    if (fillSimulate.isEmpty) return None

    val canDrainFromItem = PlatformFluidAccess.getInstance().drainItem(fillSimulate, playerStack, player, hand, false)
    if (canDrainFromItem.toReplace.isEmpty) return None

    // Assuming canDrainFromItem.getLeft.amount <= fillSimulate.amount
    // Also, tank can accept any fluid whose amount <= fillSimulate.amount
    val filled: FluidAmount = connection.getHandler.fill(canDrainFromItem.moved, execute = true)
    val transferStack = PlatformFluidAccess.getInstance().drainItem(filled, playerStack, player, hand, true)
    val drainedItem: ItemStack = transferStack.toReplace
    Option(Result(drainedItem, Option(PlatformFluidAccess.getInstance().getEmptySound(filled)), transferStack.shouldMove))
  }

  private def tryFillContainer(connection: FluidConnection, playerStack: ItemStack, player: Player, hand: InteractionHand): Option[Result] = {
    val toFillOption = connection.getContent
    if (toFillOption.isEmpty) return None

    val toFill = toFillOption.get
    val bucketContent = PlatformFluidAccess.getInstance().getFluidContained(playerStack)
    if (bucketContent.nonEmpty && !toFill.contentEqual(bucketContent)) return None

    val fillSimulate = PlatformFluidAccess.getInstance().fillItem(toFill, playerStack, player, hand, false).moved
    if (fillSimulate.isEmpty) return None

    val filled = PlatformFluidAccess.getInstance().fillItem(fillSimulate, playerStack, player, hand, true)
    connection.getHandler.drain(filled.moved, execute = true)
    Option(Result(filled.toReplace, Option(PlatformFluidAccess.getInstance().getFillSound(filled.moved)), filled.shouldMove))
  }

  def setItem(player: Player, hand: InteractionHand, result: Result, blockPos: BlockPos): Unit = {
    if (result.shouldMove && shouldMoveItem(player)) {
      // set item
      if (player.getItemInHand(hand).getCount == 1) {
        // replace
        player.setItemInHand(hand, result.stack)
      } else {
        // give
        player.getItemInHand(hand).shrink(1)
        if (!player.addItem(result.stack)) {
          player.drop(result.stack, false, Prediction.SERVER_ONLY)
        }
      }
    }
    // Sound, the player in parameter means "except", so passing null
    result.sound.foreach(s => player.level.playSound(null, blockPos, s, SoundSource.BLOCKS, 1f, 1f))
  }

  def shouldMoveItem(player: Player): Boolean = {
    PlatformConfigAccess.getInstance().getConfig.changeItemInCreative || !(player.isCreative || player.getAbilities.instabuild)
  }
}
