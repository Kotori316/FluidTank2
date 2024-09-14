package com.kotori316.fluidtank.fabric.data

import com.kotori316.fluidtank.data.IngredientProvider
import net.fabricmc.fabric.api.resource.conditions.v1.{ResourceCondition, ResourceConditions}
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.tags.TagKey
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.{Item, Items}

class IngredientProviderFabric(withCondition: (RecipeOutput, Seq[ResourceCondition]) => RecipeOutput) extends IngredientProvider {

  override def glassTag: TagKey[Item] = ConventionalItemTags.GLASS_BLOCKS

  override def obsidian: Ingredient = Ingredient.of(Items.OBSIDIAN)

  override def tagCondition(recipeOutput: RecipeOutput, tagKey: TagKey[Item]): RecipeOutput = {
    val condition = ResourceConditions.tagsPopulated(tagKey)
    withCondition(recipeOutput, Seq(condition))
  }
}
