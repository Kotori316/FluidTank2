package com.kotori316.fluidtank.neoforge.data

import com.kotori316.fluidtank.data.IngredientProvider
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.conditions.{NotCondition, TagEmptyCondition}

class IngredientProviderNeoForge extends IngredientProvider {

  override def glassTag: TagKey[Item] = Tags.Items.GLASS_BLOCKS

  override def obsidian: Ingredient = Ingredient.of(Tags.Items.OBSIDIANS)

  override def tagCondition(recipeOutput: RecipeOutput, tagKey: TagKey[Item]): RecipeOutput = {
    recipeOutput.withConditions(NotCondition(TagEmptyCondition(tagKey)))
  }
}
