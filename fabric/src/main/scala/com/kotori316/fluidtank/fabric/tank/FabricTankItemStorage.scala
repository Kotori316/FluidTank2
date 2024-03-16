package com.kotori316.fluidtank.fabric.tank

import com.kotori316.fluidtank.contents.*
import com.kotori316.fluidtank.fabric.FluidTank
import com.kotori316.fluidtank.fabric.fluid.FabricTankStorage
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, FluidLike, fluidAccess}
import com.kotori316.fluidtank.tank.{ItemBlockTank, Tier, TileTank}
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.nbt.{CompoundTag, Tag}
import net.minecraft.world.item.BlockItem

import scala.jdk.CollectionConverters.CollectionHasAsScala

//noinspection UnstableApiUsage
class FabricTankItemStorage(c: ContainerItemContext) extends FabricTankStorage(c) {

  override def getTank: Tank[FluidLike] = {
    val tag = context.getItemVariant.getNbt
    if (tag == null || !tag.contains(BlockItem.BLOCK_ENTITY_TAG, Tag.TAG_COMPOUND)) {
      Tank(FluidAmountUtil.EMPTY, GenericUnit(getTier.getCapacity))
    } else {
      TankUtil.load(tag.getCompound(BlockItem.BLOCK_ENTITY_TAG).getCompound(TileTank.KEY_TANK))
    }
  }

  private def getTier: Tier = context.getItemVariant.getItem.asInstanceOf[ItemBlockTank].blockTank.tier

  override def saveTank(newTank: Tank[FluidLike]): ItemVariant = {
    val itemTag = this.context.getItemVariant.copyOrCreateNbt()
    val tileTag = if (itemTag.contains(BlockItem.BLOCK_ENTITY_TAG, Tag.TAG_COMPOUND)) itemTag.getCompound(BlockItem.BLOCK_ENTITY_TAG) else new CompoundTag()
    if (newTank.isEmpty) {
      tileTag.remove(TileTank.KEY_TIER)
      tileTag.remove(TileTank.KEY_TANK)
    } else {
      tileTag.putString(TileTank.KEY_TIER, getTier.name())
      tileTag.put(TileTank.KEY_TANK, TankUtil.save(newTank))
    }
    if (tileTag.isEmpty) {
      itemTag.remove(BlockItem.BLOCK_ENTITY_TAG)
    } else {
      itemTag.put(BlockItem.BLOCK_ENTITY_TAG, tileTag)
    }
    ItemVariant.of(context.getItemVariant.getItem, if (itemTag.isEmpty) null else itemTag)
  }
}

object FabricTankItemStorage {
  def register(): Unit = {
    FluidStorage.ITEM.registerForItems((_, context) => new FabricTankItemStorage(context),
      FluidTank.TANK_MAP.values().asScala.toSeq *)
  }
}
