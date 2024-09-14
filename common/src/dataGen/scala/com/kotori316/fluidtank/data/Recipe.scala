package com.kotori316.fluidtank.data

import com.kotori316.fluidtank.cat.PlatformChestAsTankAccess
import com.kotori316.fluidtank.tank.{PlatformTankAccess, Tier}
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.{RecipeCategory, RecipeOutput, RecipeProvider, ShapedRecipeBuilder, ShapelessRecipeBuilder}
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient

import java.util.concurrent.CompletableFuture

class Recipe(ip: IngredientProvider, output: PackOutput, registries: CompletableFuture[HolderLookup.Provider])
  extends RecipeProvider(output, registries) {

  override def buildRecipes(recipeOutput: RecipeOutput): Unit = {
    val woodTankBlock = PlatformTankAccess.getInstance().getTankBlockMap.get(Tier.WOOD).get()
    val voidTankBlock = PlatformTankAccess.getInstance().getTankBlockMap.get(Tier.VOID).get()
    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, woodTankBlock)
      .define('x', ip.glass)
      .define('p', ItemTags.LOGS)
      .pattern("x x")
      .pattern("xpx")
      .pattern("xxx")
      .unlockedBy(ip.glassTag)
      .save(ip.tagCondition(recipeOutput, ip.glassTag))

    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, voidTankBlock)
      .define('o', ip.obsidian)
      .define('t', woodTankBlock)
      .pattern("ooo")
      .pattern("oto")
      .pattern("ooo")
      .unlockedBy(woodTankBlock)
      .save(recipeOutput)

    PlatformTankAccess.getInstance().getReservoirMap.forEach { (tier, reservoir) =>
      val tank = PlatformTankAccess.getInstance().getTankBlockMap.get(tier)
      ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, reservoir.get())
        .requires(tank.get())
        .requires(Items.BUCKET)
        .requires(Items.BUCKET)
        .unlockedBy(tank.get())
        .save(recipeOutput)
    }

    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, PlatformChestAsTankAccess.getInstance().getCATBlock().get())
      .define('p', Ingredient.of(Items.CHEST, Items.BARREL))
      .define('x', woodTankBlock)
      .pattern("x x")
      .pattern("xpx")
      .pattern("xxx")
      .unlockedBy(woodTankBlock)
      .save(recipeOutput)
  }
}
