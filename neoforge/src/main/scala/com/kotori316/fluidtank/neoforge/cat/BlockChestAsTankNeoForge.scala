package com.kotori316.fluidtank.neoforge.cat

import com.kotori316.fluidtank.cat.{BlockChestAsTank, PlatformChestAsTankAccess}
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.PlatformFluidAccess
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.{InteractionHand, InteractionResult}
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.fluids.capability.IFluidHandler

import scala.jdk.CollectionConverters.CollectionHasAsScala

class BlockChestAsTankNeoForge extends BlockChestAsTank {

  override def transferFluid(level: Level, pos: BlockPos, player: Player, hand: InteractionHand, stack: ItemStack): InteractionResult = {
    val storage = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP)
    if (storage == null) {
      return InteractionResult.PASS
    }

    def fillChest(): Option[InteractionResult] = {
      for {
        toFill <- Option(PlatformFluidAccess.getInstance().getFluidContained(stack))
        if toFill.nonEmpty
        fillSimulate = storage.fill(NeoForgeConverter.toStack(toFill), IFluidHandler.FluidAction.SIMULATE)
        if fillSimulate > 0
        canDrainFromItem = PlatformFluidAccess.getInstance().drainItem(toFill.setAmount(GenericUnit.fromForge(fillSimulate)), stack, player, hand, false)
        if canDrainFromItem.moved().nonEmpty
        filled = storage.fill(NeoForgeConverter.toStack(toFill), IFluidHandler.FluidAction.EXECUTE)
      } yield {
        val transferStack = PlatformFluidAccess.getInstance().drainItem(canDrainFromItem.moved().setAmount(GenericUnit.fromForge(filled)), stack, player, hand, true)
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
            storage.drain(NeoForgeConverter.toStack(filled.moved()), IFluidHandler.FluidAction.EXECUTE)
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
