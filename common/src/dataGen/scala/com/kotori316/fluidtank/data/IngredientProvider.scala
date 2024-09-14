package com.kotori316.fluidtank.data

import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient

trait IngredientProvider {
  def glass: Ingredient = Ingredient.of(glassTag)

  def glassTag: TagKey[Item]

  def obsidian: Ingredient

  def tagCondition(recipeOutput: RecipeOutput, tagKey: TagKey[Item]): RecipeOutput
}
