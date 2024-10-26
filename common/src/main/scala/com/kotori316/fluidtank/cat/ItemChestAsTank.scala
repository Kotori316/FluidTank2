package com.kotori316.fluidtank.cat

import com.kotori316.fluidtank.FluidTankCommon
import net.minecraft.core.registries.Registries
import net.minecraft.resources.{ResourceKey, ResourceLocation}
import net.minecraft.world.item.{BlockItem, Item}
import net.minecraft.world.level.block.Block

class ItemChestAsTank(b: Block) extends BlockItem(b, new Item.Properties()
  .useBlockDescriptionPrefix()
  .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, BlockChestAsTank.NAME)))) {

}
