package com.kotori316.fluidtank.data

import com.kotori316.fluidtank.tank.Tier
import net.minecraft.advancements.Criterion
import net.minecraft.advancements.critereon.{InventoryChangeTrigger, ItemPredicate}
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.ItemLike

trait IngredientProvider {
  def glass: Ingredient = Ingredient.of(glassTag)

  def glassTag: TagKey[Item]

  def obsidian: Ingredient = Ingredient.of(obsidianTag)

  def obsidianTag: TagKey[Item]

  def subItemOfTank(tier: Tier): TankSubitem

  def tagCondition(recipeOutput: RecipeOutput, tagKey: TagKey[Item]): RecipeOutput
}

case class TankSubitem(subItem: ItemLike | TagKey[Item]) {
  def ingredient: Ingredient = {
    subItem match {
      case i: ItemLike => Ingredient.of(i)
      case tag: TagKey[Item] => Ingredient.of(tag)
    }
  }

  def subItemTrigger: Criterion[InventoryChangeTrigger.TriggerInstance] = {
    subItem match {
      case i: ItemLike => InventoryChangeTrigger.TriggerInstance.hasItems(i)
      case tag: TagKey[Item] => InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tag))
    }
  }

  def conditionedOutput(ip: IngredientProvider, output: RecipeOutput): RecipeOutput = {
    subItem match {
      case _: ItemLike => output
      case tag: TagKey[Item] => ip.tagCondition(output, tag)
    }
  }
}
