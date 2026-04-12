package com.kotori316.fluidtank.data

import com.kotori316.fluidtank.PlatformBaseAccess
import com.kotori316.fluidtank.cat.PlatformChestAsTankAccess
import com.kotori316.fluidtank.recipe.{TierRecipe, TierRecipeBuilder}
import com.kotori316.fluidtank.reservoir.ReservoirRecipeConstant
import com.kotori316.fluidtank.tank.{PlatformTankAccess, Tier}
import net.minecraft.advancements.criterion.InventoryChangeTrigger
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.recipes.{RecipeCategory, RecipeOutput, RecipeProvider, ShapedRecipeBuilder, ShapelessRecipeBuilder}
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.ItemLike

import scala.jdk.StreamConverters.StreamHasToScala

class Recipe(ip: IngredientProvider, recipeOutput: RecipeOutput, registries: HolderLookup.Provider)
  extends RecipeProvider(registries, recipeOutput) {

  given HolderLookup.Provider = registries

  given RecipeOutput = recipeOutput

  override def buildRecipes(): Unit = {
    val woodTankBlock = PlatformTankAccess.getInstance().getTankBlockMap.get(Tier.WOOD).get()
    val voidTankBlock = PlatformTankAccess.getInstance().getTankBlockMap.get(Tier.VOID).get()
    val items = registries.lookupOrThrow(Registries.ITEM)
    ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, woodTankBlock)
      .define('x', ip.glass)
      .define('p', ItemTags.LOGS)
      .pattern("x x")
      .pattern("xpx")
      .pattern("xxx")
      .unlockedBy(ip.glassTag)
      .group(TierRecipe.TANK_RECIPE_GROUP)
      .save(ip.tagCondition(recipeOutput, ip.glassTag))

    ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, voidTankBlock)
      .define('o', ip.obsidian)
      .define('t', woodTankBlock)
      .pattern("ooo")
      .pattern("oto")
      .pattern("ooo")
      .unlockedBy(woodTankBlock)
      .unlockedBy(ip.obsidianTag)
      .group(TierRecipe.TANK_RECIPE_GROUP)
      .save(ip.tagCondition(recipeOutput, ip.obsidianTag))

    for {
      t <- Tier.values().toSeq
      if t.isNormalTankTier
      if t != Tier.WOOD
    } {
      val tankItem = TierRecipe.Serializer.getIngredientTankForTier(t)
      val itemArr: Seq[? <: ItemLike] = TierRecipe.Serializer.getTankForTier(t).toScala(Seq)
      val subItem = ip.subItemOfTank(t)
      TierRecipeBuilder(t, tankItem, subItem.ingredient)
        .unlockedBy("has_tank", InventoryChangeTrigger.TriggerInstance.hasItems(itemArr *))
        .unlockedBy("has_ingredient", subItem.subItemTrigger)
        .save(subItem.conditionedOutput(ip, recipeOutput))
    }

    PlatformTankAccess.getInstance().getReservoirMap.forEach { (tier, reservoir) =>
      val tank = PlatformTankAccess.getInstance().getTankBlockMap.get(tier)
      ShapelessRecipeBuilder.shapeless(items, RecipeCategory.TOOLS, reservoir.get())
        .requires(tank.get())
        .requires(Items.BUCKET)
        .requires(Items.BUCKET)
        .unlockedBy(tank.get())
        .group(ReservoirRecipeConstant.RESERVOIR_RECIPE_GROUP)
        .save(recipeOutput)
    }

    if (PlatformBaseAccess.getInstance().getPlatform != PlatformBaseAccess.Platforms.NEOFORGE) {
      // TODO currently cats in NeoForge is disabled
      ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, PlatformChestAsTankAccess.getInstance().getCATBlock.get())
        .define('p', Ingredient.of(Items.CHEST, Items.BARREL))
        .define('x', woodTankBlock)
        .pattern("x x")
        .pattern("xpx")
        .pattern("xxx")
        .unlockedBy(woodTankBlock)
        .save(recipeOutput)
    }
  }
}
