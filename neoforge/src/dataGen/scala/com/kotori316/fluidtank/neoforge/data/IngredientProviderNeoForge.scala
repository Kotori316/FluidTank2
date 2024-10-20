package com.kotori316.fluidtank.neoforge.data

import com.kotori316.fluidtank.data.{IngredientProvider, TankSubitem}
import com.kotori316.fluidtank.tank.Tier
import net.minecraft.core.registries.Registries
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.{ItemTags, TagKey}
import net.minecraft.world.item.Item
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.conditions.{NotCondition, TagEmptyCondition}

class IngredientProviderNeoForge extends IngredientProvider {

  override def glassTag: TagKey[Item] = Tags.Items.GLASS_BLOCKS

  override def obsidianTag: TagKey[Item] = Tags.Items.OBSIDIANS

  override def tagCondition(recipeOutput: RecipeOutput, tagKey: TagKey[Item]): RecipeOutput = {
    recipeOutput.withConditions(NotCondition(TagEmptyCondition(tagKey)))
  }

  private final val C_TAG_NAMESPACE = "c"

  override def subItemOfTank(tier: Tier): TankSubitem = {
    tier match {
      case Tier.WOOD => TankSubitem(ItemTags.LOGS)
      case Tier.STONE => TankSubitem(Tags.Items.STONES)
      case Tier.IRON => TankSubitem(Tags.Items.INGOTS_IRON)
      case Tier.GOLD => TankSubitem(Tags.Items.INGOTS_GOLD)
      case Tier.DIAMOND => TankSubitem(Tags.Items.GEMS_DIAMOND)
      case Tier.EMERALD => TankSubitem(Tags.Items.GEMS_EMERALD)
      case Tier.STAR => TankSubitem(Tags.Items.NETHER_STARS)
      case Tier.VOID => TankSubitem(obsidianTag)
      case Tier.COPPER => TankSubitem(Tags.Items.INGOTS_COPPER)
      case Tier.TIN => TankSubitem(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(C_TAG_NAMESPACE, "ingots/tin")))
      case Tier.BRONZE => TankSubitem(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(C_TAG_NAMESPACE, "ingots/bronze")))
      case Tier.LEAD => TankSubitem(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(C_TAG_NAMESPACE, "ingots/lead")))
      case Tier.SILVER => TankSubitem(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(C_TAG_NAMESPACE, "ingots/silver")))
      case _ => throw new IllegalArgumentException("Sub item of %s is not found".formatted(tier))
    }
  }
}
