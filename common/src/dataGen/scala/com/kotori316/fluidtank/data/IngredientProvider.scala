package com.kotori316.fluidtank.data

import com.kotori316.fluidtank.tank.Tier
import net.minecraft.advancements.Criterion
import net.minecraft.advancements.critereon.{InventoryChangeTrigger, ItemPredicate}
import net.minecraft.core.HolderGetter
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.ItemLike

trait IngredientProvider {
  given itemRegistry: HolderGetter[Item] = scala.compiletime.deferred

  def glass: Ingredient = Ingredient.of(itemRegistry.getOrThrow(glassTag))

  def glassTag: TagKey[Item]

  def obsidian: Ingredient = Ingredient.of(itemRegistry.getOrThrow(obsidianTag))

  def obsidianTag: TagKey[Item]

  def subItemOfTank(tier: Tier): TankSubitem

  def tagCondition(recipeOutput: RecipeOutput, tagKey: TagKey[Item]): RecipeOutput
}

case class TankSubitem(subItem: ItemLike | TagKey[Item])(using itemRegistry: HolderGetter[Item]) {
  def ingredient: Ingredient = {
    subItem match {
      case i: ItemLike => Ingredient.of(i)
      case tag: TagKey[Item] => Ingredient.of(itemRegistry.getOrThrow(tag))
    }
  }

  def subItemTrigger: Criterion[InventoryChangeTrigger.TriggerInstance] = {
    subItem match {
      case i: ItemLike => InventoryChangeTrigger.TriggerInstance.hasItems(i)
      case tag: TagKey[Item] => InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(itemRegistry, tag))
    }
  }

  def conditionedOutput(ip: IngredientProvider, output: RecipeOutput): RecipeOutput = {
    subItem match {
      case _: ItemLike => output
      case tag: TagKey[Item] => ip.tagCondition(output, tag)
    }
  }
}
