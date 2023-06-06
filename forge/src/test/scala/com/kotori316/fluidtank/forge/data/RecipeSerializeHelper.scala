package com.kotori316.fluidtank.forge.data

import com.google.gson.JsonObject
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.data.recipes.{FinishedRecipe, RecipeBuilder, SpecialRecipeBuilder}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.{CraftingRecipe, RecipeSerializer}

case class RecipeSerializeHelper(recipe: FinishedRecipe,
                                 conditions: List[PlatformedCondition] = Nil,
                                 saveName: ResourceLocation = null,
                                 advancement: AdvancementSerializeHelper = AdvancementSerializeHelper(),
                                ) {
  def this(c: RecipeBuilder, saveName: ResourceLocation) = {
    this(RecipeSerializeHelper.getConsumeValue(c), saveName = saveName)
  }

  def addCondition(condition: PlatformedCondition): RecipeSerializeHelper =
    copy(conditions = condition :: this.conditions)

  def addTagCondition(ingredientHelper: RecipeIngredientHelper): RecipeSerializeHelper =
    addCondition(PlatformedCondition.Tag(ingredientHelper))

  def build: JsonObject = {
    val o = recipe.serializeRecipe()
    if (conditions.nonEmpty) {
      o.add("conditions", FluidTankDataProvider.makeForgeConditionArray(conditions))
      o.add("fabric:load_conditions", FluidTankDataProvider.makeFabricConditionArray(conditions))
    }
    o
  }

  def location: ResourceLocation = if (saveName == null) recipe.getId else saveName

  def addItemCriterion(item: Item): RecipeSerializeHelper =
    this.copy(advancement = advancement.addItemCriterion(item))

  def addItemCriterion(ingredientHelper: RecipeIngredientHelper): RecipeSerializeHelper =
    this.copy(advancement = advancement.addItemCriterion(ingredientHelper))
}

object RecipeSerializeHelper {
  def by(c: RecipeBuilder, saveName: ResourceLocation = null): RecipeSerializeHelper = new RecipeSerializeHelper(c, saveName)

  def bySpecial(serializer: RecipeSerializer[_ <: CraftingRecipe], recipeId: String, saveName: ResourceLocation = null): RecipeSerializeHelper = {
    val c = SpecialRecipeBuilder.special(serializer)
    var t: FinishedRecipe = null
    c.save(p => t = p, recipeId)
    new RecipeSerializeHelper(t, Nil, saveName)
  }

  private def getConsumeValue(c: RecipeBuilder): FinishedRecipe = {
    val fixed: RecipeBuilder = c.unlockedBy("dummy", RecipeUnlockedTrigger.unlocked(new ResourceLocation("dummy:dummy")))
    var t: FinishedRecipe = null
    fixed.save(p => t = p)
    t
  }

}