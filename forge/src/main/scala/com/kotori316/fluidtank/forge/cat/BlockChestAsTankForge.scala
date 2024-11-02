package com.kotori316.fluidtank.forge.cat

import com.kotori316.fluidtank.cat.{BlockChestAsTank, PlatformChestAsTankAccess}
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.PlatformFluidAccess
import com.kotori316.fluidtank.forge.fluid.ForgeConverter
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.{InteractionHand, InteractionResult}
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.fluids.capability.IFluidHandler

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.OptionConverters.RichOptional

class BlockChestAsTankForge extends BlockChestAsTank {

  override def transferFluid(level: Level, pos: BlockPos, player: Player, hand: InteractionHand, stack: ItemStack): InteractionResult = {
    val cap = for {
      tile <- Option(level.getBlockEntity(pos))
      t <- tile.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).resolve().toScala
    } yield t

    def fillChest(): Option[InteractionResult] = {
      for {
        storage <- cap
        toFill <- Option(PlatformFluidAccess.getInstance().getFluidContained(stack))
        if toFill.nonEmpty
        fillSimulate = storage.fill(ForgeConverter.toStack(toFill), IFluidHandler.FluidAction.SIMULATE)
        if fillSimulate > 0
        canDrainFromItem = PlatformFluidAccess.getInstance().drainItem(toFill.setAmount(GenericUnit.fromForge(fillSimulate)), stack, player, hand, false)
        if canDrainFromItem.moved().nonEmpty
        filled = storage.fill(ForgeConverter.toStack(toFill), IFluidHandler.FluidAction.EXECUTE)
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
            storage <- cap
            bucketContent <- Option(PlatformFluidAccess.getInstance().getFluidContained(stack))
            if bucketContent.isEmpty || toFill.contentEqual(bucketContent)
            fillSimulate = PlatformFluidAccess.getInstance().fillItem(toFill, stack, player, hand, false).moved
            if fillSimulate.nonEmpty
            filled = PlatformFluidAccess.getInstance().fillItem(fillSimulate, stack, player, hand, true)
          } yield {
            storage.drain(ForgeConverter.toStack(filled.moved()), IFluidHandler.FluidAction.EXECUTE)
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
