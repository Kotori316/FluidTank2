package com.kotori316.fluidtank.fabric.integration.jei

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.integration.jei.{JeiPluginConstant, TierRecipeCraftingCategoryExtension}
import com.kotori316.fluidtank.recipe.TierRecipe
import mezz.jei.api.constants.RecipeTypes
import mezz.jei.api.registration.{IRecipeRegistration, IVanillaCategoryExtensionRegistration}
import mezz.jei.api.{IModPlugin, JeiPlugin}
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes
import net.minecraft.core.registries.Registries
import net.minecraft.resources.{Identifier, ResourceKey}
import net.minecraft.world.item.crafting.display.SlotDisplay
import net.minecraft.world.item.crafting.{CraftingRecipe, RecipeHolder, RecipeType}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}

@JeiPlugin
class FluidTankJeiPlugin extends IModPlugin {
  private val LOGGER = LoggerFactory.getLogger(classOf[FluidTankJeiPlugin])

  private var recipeMap: Option[SynchronizedRecipes] = None
  ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) => recipeMap = Option(recipes))

  override def getPluginUid: Identifier = JeiPluginConstant.pluginUid

  override def registerVanillaCategoryExtensions(registration: IVanillaCategoryExtensionRegistration): Unit = {
    LOGGER.info("Registering TierRecipe CategoryExtension for JEI with Fabric")
    super.registerVanillaCategoryExtensions(registration)
    registration.getCraftingCategory.addExtension(classOf[TierRecipe], new TierRecipeCraftingCategoryExtension)
  }

  override def registerRecipes(registration: IRecipeRegistration): Unit = {
    super.registerRecipes(registration)
    val tierRecipes = this.recipeMap.iterator
      .flatMap(_.getAllOfType(RecipeType.CRAFTING).asScala)
      .map(_.value())
      .collect { case r: TierRecipe => r }
      .map(createJeiShapedRecipe(registration, _))
      .toSeq

    LOGGER.info("Registering TierRecipe recipes for JEI with Fabric, count: {}", tierRecipes.size)
    registration.addRecipes(RecipeTypes.CRAFTING, tierRecipes.asJava)
  }

  private def createJeiShapedRecipe(registration: IRecipeRegistration, r: TierRecipe): RecipeHolder[CraftingRecipe] = {
    val builder = registration.getVanillaRecipeFactory.createShapedRecipeBuilder(r.category(), new SlotDisplay.ItemStackSlotDisplay(r.result))
    val recipe = builder
      .group(r.group())
      .pattern("tst")
      .pattern("s s")
      .pattern("tst")
      .define('t', r.getTankItem)
      .define('s', r.getSubItem)
      .build()
    val key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(FluidTankCommon.modId, s"jei_dummy_${r.getTier.name.toLowerCase}"))
    new RecipeHolder(key, recipe)
  }
}
