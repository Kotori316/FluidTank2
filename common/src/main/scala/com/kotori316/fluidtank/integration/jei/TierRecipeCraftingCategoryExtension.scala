package com.kotori316.fluidtank.integration.jei

import com.kotori316.fluidtank.recipe.TierRecipe
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.display.{ShapedCraftingRecipeDisplay, SlotDisplay}

import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
 * This class is used to remove the "Shapreless" icon from TierRecipe guides.
 * The default implementation of [[ICraftingCategoryExtension]] treats only [[net.minecraft.world.item.crafting.ShapedRecipe]] and JEI class
 * as shaped, and all other recipes are treated as shapless.
 */
class TierRecipeCraftingCategoryExtension extends ICraftingCategoryExtension[TierRecipe] {

  /**
   * Logic is copied from mezz.jei.library.plugins.vanilla.crafting.CraftingCategoryExtension#getIngredients
   * Written in Scala syntax.
   */
  override def getIngredients(recipeHolder: RecipeHolder[TierRecipe]): java.util.List[SlotDisplay] = {
    recipeHolder
      .value()
      .display()
      .asScala
      .headOption
      .collect { case display: ShapedCraftingRecipeDisplay => display.ingredients() }
      .getOrElse(java.util.List.of())
  }

  override def getWidth(recipeHolder: RecipeHolder[TierRecipe]): Int = 3

  override def getHeight(recipeHolder: RecipeHolder[TierRecipe]): Int = 3

  override def isHandled(recipe: RecipeHolder[TierRecipe]): Boolean = true
}
