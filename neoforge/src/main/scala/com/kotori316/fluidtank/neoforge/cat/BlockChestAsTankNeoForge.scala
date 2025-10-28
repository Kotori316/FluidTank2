package com.kotori316.fluidtank.neoforge.cat

import com.kotori316.fluidtank.cat.{BlockChestAsTank, PlatformChestAsTankAccess}
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, PlatformFluidAccess}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.{InteractionHand, InteractionResult}
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.Transaction

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Using

class BlockChestAsTankNeoForge extends BlockChestAsTank {

  private def fillSimulate(storage: ResourceHandler[FluidResource], toFill: FluidAmount, stack: ItemStack, player: Player, hand: InteractionHand): Option[PlatformFluidAccess.TransferStack] = {
    val fillSimulate = Using.resource(Transaction.openRoot()) { tx =>
      storage.insert(toFill.asVariant, toFill.amount.asForge, tx)
    }
    if (fillSimulate > 0) {
      Option(PlatformFluidAccess.getInstance().drainItem(toFill.setAmount(GenericUnit.fromForge(fillSimulate)), stack, player, hand, false))
    } else {
      Option.empty
    }
  }

  private def fill(storage: ResourceHandler[FluidResource], toFill: FluidAmount, toDrain: FluidAmount, stack: ItemStack, player: Player, hand: InteractionHand): Option[PlatformFluidAccess.TransferStack] = {
    val filled = Using.resource(Transaction.openRoot()) { tx =>
      val r = storage.insert(toFill.asVariant, toFill.amount.asForge, tx)
      tx.commit()
      r
    }
    Option(PlatformFluidAccess.getInstance().drainItem(toDrain.setAmount(GenericUnit.fromForge(filled)), stack, player, hand, true))
  }

  private def drain(storage: ResourceHandler[FluidResource], toDrain: FluidAmount): Unit = {
    Using.resource(Transaction.openRoot()) { tx =>
      storage.extract(toDrain.asVariant, toDrain.amount.asForge, tx)
      tx.commit()
    }
  }

  override def transferFluid(level: Level, pos: BlockPos, player: Player, hand: InteractionHand, stack: ItemStack): InteractionResult = {
    val storage = level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP)
    if (storage == null) {
      return InteractionResult.PASS
    }

    def fillChest(): Option[InteractionResult] = {
      for {
        toFill <- Option(PlatformFluidAccess.getInstance().getFluidContained(stack))
        if toFill.nonEmpty
        canDrainFromItem <- fillSimulate(storage, toFill, stack, player, hand)
        if canDrainFromItem.moved().nonEmpty
        transferStack <- fill(storage, toFill, canDrainFromItem.moved(), stack, player, hand)
      } yield {
        val drainedItem: ItemStack = transferStack.toReplace
        InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(drainedItem)
      }
    }

    def fillItem(): Option[InteractionResult] = {
      val fluidContents = PlatformChestAsTankAccess.getInstance().getCATFluids(level, pos).asScala

      fluidContents
        .flatMap { toFill =>
          for {
            bucketContent <- Option(PlatformFluidAccess.getInstance().getFluidContained(stack))
            if bucketContent.isEmpty || toFill.contentEqual(bucketContent)
            fillSimulate = PlatformFluidAccess.getInstance().fillItem(toFill, stack, player, hand, false).moved
            if fillSimulate.nonEmpty
            filled = PlatformFluidAccess.getInstance().fillItem(fillSimulate, stack, player, hand, true)
          } yield {
            drain(storage, filled.moved())
            InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(filled.toReplace)
          }
        }
        .headOption
    }

    val result = fillChest() orElse fillItem() getOrElse InteractionResult.PASS
    result match {
      case success: InteractionResult.Success if !player.hasInfiniteMaterials =>
        player.setItemInHand(hand, success.heldItemTransformedTo())
      case _ =>
    }
    result
  }
}
