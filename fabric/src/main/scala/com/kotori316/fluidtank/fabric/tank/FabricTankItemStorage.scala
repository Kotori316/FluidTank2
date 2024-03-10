package com.kotori316.fluidtank.fabric.tank

import com.kotori316.fluidtank.contents.*
import com.kotori316.fluidtank.fabric.fluid.FabricTankStorage
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, FluidLike, fluidAccess}
import com.kotori316.fluidtank.tank.{ItemBlockTank, Tier, TileTank}
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.core.component.{DataComponentPatch, DataComponentType, DataComponents}
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData

import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.jdk.OptionConverters.RichOptional

//noinspection UnstableApiUsage
class FabricTankItemStorage(c: ContainerItemContext) extends FabricTankStorage(c) {
  override def getTank: Tank[FluidLike] = {
    val componentPatch = context.getItemVariant.getComponents
    val maybeTank = for {
      blockEntityData <- Option(componentPatch.get(DataComponents.BLOCK_ENTITY_DATA)).flatMap(_.toScala)
      if blockEntityData.contains(TileTank.KEY_TANK)
      customTag = blockEntityData.copyTag()
      tankTag <- Option(customTag.getCompound(TileTank.KEY_TANK))
    } yield TankUtil.load(tankTag)
    maybeTank.getOrElse(Tank(FluidAmountUtil.EMPTY, GenericUnit(getTier.getCapacity)))
  }

  private def getTier: Tier = context.getItemVariant.getItem.asInstanceOf[ItemBlockTank].blockTank.tier

  override def saveTank(newTank: Tank[FluidLike]): ItemVariant = {
    val componentPatch = this.context.getItemVariant.getComponents
    val tileTag = Option(componentPatch.get(DataComponents.BLOCK_ENTITY_DATA))
      .flatMap(_.toScala)
      .map(_.copyTag())
      .getOrElse(new CompoundTag())

    if (newTank.isEmpty) {
      tileTag.remove(TileTank.KEY_TIER)
      tileTag.remove(TileTank.KEY_TANK)
    } else {
      tileTag.putString(TileTank.KEY_TIER, getTier.name())
      tileTag.put(TileTank.KEY_TANK, TankUtil.save(newTank))
    }
    val componentBuilder = DataComponentPatch.builder()
    componentPatch.entrySet().asScala
      .flatMap(e => e.getValue.toScala.map(v => e.getKey.asInstanceOf[DataComponentType[AnyRef]] -> v.asInstanceOf[AnyRef]))
      .foreach { case (k, v) => componentBuilder.set(k, v) }
    if (tileTag.isEmpty) {
      componentBuilder.remove(DataComponents.BLOCK_ENTITY_DATA)
    } else {
      componentBuilder.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tileTag))
    }
    ItemVariant.of(context.getItemVariant.getItem, componentBuilder.build())
  }
}
