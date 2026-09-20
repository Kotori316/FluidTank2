package com.kotori316.fluidtank.fabric.data

import com.kotori316.fluidtank.data.Recipe
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.advancements.Advancement
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.Recipe as MCRecipe

import java.util.concurrent.CompletableFuture

class RecipeFabric(output: FabricPackOutput, registries: CompletableFuture[HolderLookup.Provider])
  extends FabricRecipeProvider(output, registries) {

  override def getRecipeIdentifier(identifier: Identifier): Identifier = identifier

  override def createRecipeProvider(registryLookup: HolderLookup.Provider, recipeBootstrap: BootstrapContext[MCRecipe[?]], advancementBootstrap: BootstrapContext[Advancement]): RecipeProvider = {
    val ip = new IngredientProviderFabric(recipeBootstrap.lookup(Registries.ITEM), (o, c) => this.withConditions(o, c *))
    new Recipe(ip, recipeBootstrap, advancementBootstrap)
  }

  override def getName: String = getClass.getSimpleName
}
