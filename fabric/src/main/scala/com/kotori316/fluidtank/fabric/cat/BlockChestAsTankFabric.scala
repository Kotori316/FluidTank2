package com.kotori316.fluidtank.fabric.cat

import com.kotori316.fluidtank.cat.{BlockChestAsTank, PlatformChestAsTankAccess}
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fabric.fluid.FabricConverter
import com.kotori316.fluidtank.fluids.PlatformFluidAccess
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.{InteractionHand, InteractionResult}

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Using

class BlockChestAsTankFabric extends BlockChestAsTank {

  override def transferFluid(level: Level, pos: BlockPos, player: Player, hand: InteractionHand, stack: ItemStack): InteractionResult = {
    val storage = FluidStorage.SIDED.find(level, pos, Direction.UP)
    if (storage == null) {
      return InteractionResult.PASS
    }

    def fillChest(): Option[InteractionResult] = {
      for {
        toFill <- Option(PlatformFluidAccess.getInstance().getFluidContained(stack))
        if toFill.nonEmpty
        fillSimulate <- Using(Transaction.openOuter()) { tr =>
          storage.insert(FabricConverter.toVariant(toFill, Fluids.EMPTY), FabricConverter.fabricAmount(toFill), tr)
        }.toOption
        if fillSimulate > 0
        canDrainFromItem = PlatformFluidAccess.getInstance().drainItem(toFill.setAmount(GenericUnit.fromFabric(fillSimulate)), stack, player, hand, false)
        if canDrainFromItem.moved().nonEmpty
        filled <- Using(Transaction.openOuter()) { tr =>
          val toFill = canDrainFromItem.moved()
          val f = storage.insert(FabricConverter.toVariant(toFill, Fluids.EMPTY), FabricConverter.fabricAmount(toFill), tr)
          tr.commit()
          f
        }.toOption
      } yield {
        val transferStack = PlatformFluidAccess.getInstance().drainItem(canDrainFromItem.moved().setAmount(GenericUnit.fromFabric(filled)), stack, player, hand, true)
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
            Using(Transaction.openOuter()) { tr =>
              storage.extract(FabricConverter.toVariant(filled.moved(), Fluids.EMPTY), FabricConverter.fabricAmount(filled.moved()), tr)
              tr.commit()
            }
            InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(filled.toReplace)
          }
        }
        .headOption
    }

    fillChest() orElse fillItem() getOrElse InteractionResult.PASS
  }
}
