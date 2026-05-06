package com.kotori316.fluidtank.fabric.integration.jei

import com.kotori316.fluidtank.integration.jei.{JeiPluginConstant, TierRecipeCraftingCategoryExtension}
import com.kotori316.fluidtank.recipe.TierRecipe
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration
import mezz.jei.api.{IModPlugin, JeiPlugin}
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

@JeiPlugin
class FluidTankJeiPlugin extends IModPlugin {
  private val LOGGER = LoggerFactory.getLogger(classOf[FluidTankJeiPlugin])

  override def getPluginUid: Identifier = JeiPluginConstant.pluginUid

  override def registerVanillaCategoryExtensions(registration: IVanillaCategoryExtensionRegistration): Unit = {
    LOGGER.info("Registering TierRecipe CategoryExtension for JEI with Fabric")
    super.registerVanillaCategoryExtensions(registration)
    registration.getCraftingCategory.addExtension(classOf[TierRecipe], new TierRecipeCraftingCategoryExtension)
  }
}
