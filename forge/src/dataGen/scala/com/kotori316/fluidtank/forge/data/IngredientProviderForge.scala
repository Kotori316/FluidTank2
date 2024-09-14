package com.kotori316.fluidtank.forge.data

import com.kotori316.fluidtank.data.IngredientProvider
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import net.minecraftforge.common.Tags
import net.minecraftforge.common.crafting.conditions.{NotCondition, TagEmptyCondition}

class IngredientProviderForge extends IngredientProvider {

  override def glassTag: TagKey[Item] = Tags.Items.GLASS

  override def obsidian: Ingredient = Ingredient.of(Tags.Items.OBSIDIAN)

  override def tagCondition(recipeOutput: RecipeOutput, tagKey: TagKey[Item]): RecipeOutput = {
    recipeOutput match {
      case recipe: CollectRecipe => recipe.withCondition(Seq(new NotCondition(new TagEmptyCondition(tagKey))))
      case _ => recipeOutput
    }
  }
}
