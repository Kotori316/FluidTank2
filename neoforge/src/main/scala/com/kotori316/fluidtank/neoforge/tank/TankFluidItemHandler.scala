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
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.capability.{IFluidHandler, IFluidHandlerItem}
import org.jetbrains.annotations.VisibleForTesting

import scala.jdk.OptionConverters.RichOptional

class TankFluidItemHandler(tier: Tier, stack: ItemStack) extends TankFluidHandler {

  def getCapability(ignored: Void): IFluidHandlerItem = this

  override def getContainer: ItemStack = stack

  override def getTank: Tank[FluidLike] = {
    val componentPatch = getContainer.getComponentsPatch
    val maybeTank = for {
      blockEntityData <- Option(componentPatch.get(DataComponents.BLOCK_ENTITY_DATA)).flatMap(_.toScala)
      if blockEntityData.contains(TileTank.KEY_TANK)
      customTag = blockEntityData.copyTag()
      tankTag <- Option(customTag.getCompound(TileTank.KEY_TANK))
    } yield TankUtil.load(tankTag)
    maybeTank.getOrElse(Tank(FluidAmountUtil.EMPTY, GenericUnit(tier.getCapacity)))
  }

  override def saveTank(tank: Tank[FluidLike]): Unit = {
    if (tank.isEmpty) {
      // remove tags related to block entity
      // Other mods might add own tags in BlockEntityTag, but remove them as they will cause rendering issue.
      getContainer.remove(DataComponents.BLOCK_ENTITY_DATA)
    } else {
      val tankTag = TankUtil.save(tank)
      val tag = Option(getContainer.getComponentsPatch.get(DataComponents.BLOCK_ENTITY_DATA))
        .flatMap(_.toScala)
        .map(_.copyTag())
        .getOrElse(new CompoundTag())
      tag.put(TileTank.KEY_TANK, tankTag)
      tag.putString(TileTank.KEY_TIER, tier.name())
      PlatformItemAccess.setTileTag(getContainer, tag, FluidTank.TILE_TANK_TYPE.getId.toString)
    }
  }

  @VisibleForTesting
  def fill(fill: FluidAmount, execute: Boolean): Unit = {
    this.fill(fill.toStack, if (execute) IFluidHandler.FluidAction.EXECUTE else IFluidHandler.FluidAction.SIMULATE)
  }
}
