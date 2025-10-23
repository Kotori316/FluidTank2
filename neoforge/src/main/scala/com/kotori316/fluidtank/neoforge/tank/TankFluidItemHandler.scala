package com.kotori316.fluidtank.neoforge.tank

import com.kotori316.fluidtank.contents.{GenericUnit, Tank, TankUtil}
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, FluidLike, fluidAccess}
import com.kotori316.fluidtank.item.PlatformItemAccess
import com.kotori316.fluidtank.neoforge.FluidTank
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import com.kotori316.fluidtank.neoforge.fluid.TankFluidHandler
import com.kotori316.fluidtank.tank.{Tier, TileTank}
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.ProblemReporter
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.TagValueOutput
import net.neoforged.neoforge.transfer.transaction.Transaction
import org.jetbrains.annotations.VisibleForTesting

import scala.jdk.OptionConverters.RichOptional
import scala.util.Using

class TankFluidItemHandler(tier: Tier, stack: ItemStack) extends TankFluidHandler {

  def getContainer: ItemStack = stack

  override def getTank: Tank[FluidLike] = {
    val componentPatch = getContainer.getComponentsPatch
    val maybeTank = for {
      blockEntityData <- Option(componentPatch.get(DataComponents.BLOCK_ENTITY_DATA)).flatMap(_.toScala)
      if blockEntityData.contains(TileTank.KEY_TANK)
      customTag = blockEntityData.copyTagWithoutId()
      tankTag <- customTag.getCompound(TileTank.KEY_TANK).toScala
    } yield TankUtil.load(tankTag)
    maybeTank.getOrElse(Tank(FluidAmountUtil.EMPTY, GenericUnit(tier.getCapacity)))
  }

  override def saveTank(tank: Tank[FluidLike]): Unit = {
    if (tank.isEmpty) {
      // remove tags related to block entity
      // Other mods might add own tags in BlockEntityTag, but remove them as they will cause rendering issue.
      getContainer.remove(DataComponents.BLOCK_ENTITY_DATA)
    } else {
      val tagValueOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING)
      val tag = Option(getContainer.getComponentsPatch.get(DataComponents.BLOCK_ENTITY_DATA))
        .flatMap(_.toScala)
        .map(_.copyTagWithoutId())
        .getOrElse(new CompoundTag())
      tagValueOutput.store(tag)
      tagValueOutput.store(TileTank.KEY_TANK, Tank.codec, tank)
      tagValueOutput.putString(TileTank.KEY_TIER, tier.name())
      PlatformItemAccess.setTileTag(getContainer, tagValueOutput, FluidTank.TILE_TANK_TYPE.get())
    }
  }

  @VisibleForTesting
  def fill(fill: FluidAmount, execute: Boolean): Unit = {
    Using(Transaction.openRoot()) { tx =>
      this.insert(fill.asVariant, fill.amount.asForge, tx)
      if (execute) tx.commit()
    }
  }
}
