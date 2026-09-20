package com.kotori316.fluidtank.data

import net.minecraft.advancements.Advancement
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.recipes.{RecipeBuilder, RecipeOutput}
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Recipe as MCRecipe
import net.minecraft.world.level.ItemLike

extension [T <: RecipeBuilder](builder: T) {
  def unlockedBy(item: ItemLike)(using recipeBootstrap: BootstrapContext[MCRecipe[?]], advancementBootstrap: BootstrapContext[Advancement], recipeOutput: RecipeOutput): T = {
    val name = BuiltInRegistries.ITEM.getKey(item.asItem()).getPath
    builder.unlockedBy(s"has_$name", RecipeProviderAccess.hasItem(item, RecipeProviderAccess(recipeBootstrap, advancementBootstrap)))
    builder
  }

  def unlockedBy(tag: TagKey[Item])(using recipeBootstrap: BootstrapContext[MCRecipe[?]], advancementBootstrap: BootstrapContext[Advancement], recipeOutput: RecipeOutput): T = {
    val name = tag.location().getPath
    builder.unlockedBy(s"has_$name", RecipeProviderAccess.hasTag(tag, RecipeProviderAccess(recipeBootstrap, advancementBootstrap)))
    builder
  }
}
