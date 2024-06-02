package com.kotori316.fluidtank.forge.data

import com.google.gson.{JsonElement, JsonObject}
import com.mojang.serialization.JsonOps
import net.minecraft.Util
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.data.recipes.{RecipeBuilder, RecipeOutput}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Recipe

case class RecipeSerializeHelper(recipe: Recipe[?],
                                 conditions: List[PlatformedCondition] = Nil,
                                 location: ResourceLocation,
                                 advancement: AdvancementSerializeHelper = AdvancementSerializeHelper(),
                                ) {
  def this(c: RecipeBuilder, location: ResourceLocation) = {
    this(RecipeSerializeHelper.getConsumeValue(c), location = location)
  }

  def addCondition(condition: PlatformedCondition): RecipeSerializeHelper =
    copy(conditions = condition :: this.conditions)

  def addTagCondition(ingredientHelper: RecipeIngredientHelper): RecipeSerializeHelper =
    addCondition(PlatformedCondition.Tag(ingredientHelper))

  def build: JsonObject = {
    val o = Util.getOrThrow(Recipe.CODEC.encodeStart(JsonOps.INSTANCE, this.recipe).map(_.getAsJsonObject), s => new IllegalStateException(s))
    FluidTankDataProvider.addPlatformConditions(o, conditions)
    o
  }

  def addItemCriterion(item: Item): RecipeSerializeHelper =
    this.copy(advancement = advancement.addItemCriterion(item))

  def addItemCriterion(ingredientHelper: RecipeIngredientHelper): RecipeSerializeHelper =
    this.copy(advancement = advancement.addItemCriterion(ingredientHelper))
}

object RecipeSerializeHelper {
  def by(c: RecipeBuilder, location: ResourceLocation): RecipeSerializeHelper = new RecipeSerializeHelper(c, location)

  private def getConsumeValue(c: RecipeBuilder): Recipe[?] = {
    val fixed: RecipeBuilder = c.unlockedBy("dummy", RecipeUnlockedTrigger.unlocked(ResourceLocation.parse("dummy:dummy")))
    var t: Recipe[?] = null
    fixed.save(new RecipeOutput {
      override def accept(arg: ResourceLocation, arg2: Recipe[?], arg3: ResourceLocation, jsonElement: JsonElement): Unit = {
        t = arg2
      }
      override def advancement(): Advancement.Builder = Advancement.Builder.recipeAdvancement()
    })
    t
  }

}
