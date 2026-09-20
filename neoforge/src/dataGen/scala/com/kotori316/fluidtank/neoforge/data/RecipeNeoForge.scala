package com.kotori316.fluidtank.neoforge.data

import com.kotori316.fluidtank.data.Recipe
import net.minecraft.core.Registry
import net.minecraft.core.registries.{MultiRegistryBootstrap, Registries}
import net.minecraft.resources.ResourceKey

import java.util.Set as JSet

object RecipeNeoForge extends MultiRegistryBootstrap {
  override def requestedRegistries(): JSet[ResourceKey[? <: Registry[?]]] =
    JSet.of(Registries.RECIPE, Registries.ADVANCEMENT)

  override def run(registries: MultiRegistryBootstrap.BootstrapGetter): Unit = {
    val recipeBootstrap = registries.get(Registries.RECIPE)
    val advancementBootstrap = registries.get(Registries.ADVANCEMENT)
    val ip = new IngredientProviderNeoForge(recipeBootstrap.lookup(Registries.ITEM))
    new Recipe(ip, recipeBootstrap, advancementBootstrap).buildRecipes()
  }
}
