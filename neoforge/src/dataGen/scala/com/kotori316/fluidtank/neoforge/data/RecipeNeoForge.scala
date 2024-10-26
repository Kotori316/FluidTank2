package com.kotori316.fluidtank.neoforge.data

import com.kotori316.fluidtank.data.Recipe
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.{RecipeOutput, RecipeProvider}

import java.util.concurrent.CompletableFuture

class RecipeNeoForge(output: PackOutput, p: CompletableFuture[HolderLookup.Provider]) extends RecipeProvider.Runner(output, p) {

  override def createRecipeProvider(provider: HolderLookup.Provider, recipeOutput: RecipeOutput): RecipeProvider = {
    val ip = new IngredientProviderNeoForge(provider)
    new Recipe(ip, recipeOutput, provider)
  }

  override def getName: String = getClass.getSimpleName
}
