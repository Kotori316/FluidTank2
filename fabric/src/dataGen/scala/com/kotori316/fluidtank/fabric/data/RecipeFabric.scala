package com.kotori316.fluidtank.fabric.data

import com.kotori316.fluidtank.data.Recipe
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.recipes.{RecipeOutput, RecipeProvider}
import net.minecraft.resources.Identifier

import java.util.concurrent.CompletableFuture

class RecipeFabric(output: FabricDataOutput, registries: CompletableFuture[HolderLookup.Provider])
  extends FabricRecipeProvider(output, registries) {

  override def getRecipeIdentifier(identifier: Identifier): Identifier = identifier

  override def createRecipeProvider(registryLookup: HolderLookup.Provider, exporter: RecipeOutput): RecipeProvider = {
    val ip = new IngredientProviderFabric(registryLookup.lookupOrThrow(Registries.ITEM), (o, c) => this.withConditions(o, c *))
    new Recipe(ip, exporter, registryLookup)
  }

  override def getName: String = getClass.getSimpleName
}
