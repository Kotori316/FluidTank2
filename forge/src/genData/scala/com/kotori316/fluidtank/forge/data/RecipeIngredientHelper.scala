package com.kotori316.fluidtank.forge.data

import com.kotori316.fluidtank.forge.recipe.IgnoreUnknownTagIngredient
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.{ItemTags, TagKey}
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.ItemLike

case class RecipeIngredientHelper(forgeIngredient: IgnoreUnknownTagIngredient,
                                  fabricIngredient: Option[IgnoreUnknownTagIngredient],
                                  forgeTagLimit: Option[ResourceLocation],
                                  fabricTagLimit: Option[ResourceLocation],
                                 ) {
  def ingredient: Ingredient = {
    new IgnoreUnknownTagIngredient(java.util.stream.Stream.concat(
      forgeIngredient.getValues.stream(),
      fabricIngredient.map(_.getValues.stream()).getOrElse(java.util.stream.Stream.empty())
    ).toList)
  }
}

object RecipeIngredientHelper {
  def vanillaTag(key: TagKey[Item]): RecipeIngredientHelper = {
    RecipeIngredientHelper(IgnoreUnknownTagIngredient.of(key), None, None, None)
  }

  def item(item: ItemLike): RecipeIngredientHelper = {
    RecipeIngredientHelper(IgnoreUnknownTagIngredient.of(item), None, None, None)
  }

  def bothTag(forgeTag: TagKey[Item], fabricTag: String): RecipeIngredientHelper = {
    val fTag = ItemTags.create(ResourceLocation.parse(fabricTag))
    RecipeIngredientHelper(
      IgnoreUnknownTagIngredient.of(forgeTag), Some(IgnoreUnknownTagIngredient.of(fTag)),
      Some(forgeTag.location()), Some(fTag.location())
    )
  }

  def bothTag(forgeTag: String, fabricTag: String): RecipeIngredientHelper = {
    val forgeKey = ItemTags.create(ResourceLocation.parse(forgeTag))
    val fabricKey = ItemTags.create(ResourceLocation.parse(fabricTag))
    RecipeIngredientHelper(
      IgnoreUnknownTagIngredient.of(forgeKey), Some(IgnoreUnknownTagIngredient.of(fabricKey)),
      Some(forgeKey.location()), Some(fabricKey.location())
    )
  }

  def forgeTagFabricItem(forgeTag: TagKey[Item], fabricItem: ItemLike): RecipeIngredientHelper = {
    RecipeIngredientHelper(
      IgnoreUnknownTagIngredient.of(forgeTag), Some(IgnoreUnknownTagIngredient.of(fabricItem)),
      Some(forgeTag.location()), None
    )
  }
}
