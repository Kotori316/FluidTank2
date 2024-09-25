package com.kotori316.fluidtank.fabric.data

import com.kotori316.fluidtank.data.{IngredientProvider, TankSubitem}
import com.kotori316.fluidtank.tank.Tier
import net.fabricmc.fabric.api.resource.conditions.v1.{ResourceCondition, ResourceConditions}
import net.fabricmc.fabric.api.tag.convention.v2.{ConventionalItemTags, TagUtil}
import net.minecraft.core.registries.Registries
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.{ItemTags, TagKey}
import net.minecraft.world.item.{Item, Items}

class IngredientProviderFabric(withCondition: (RecipeOutput, Seq[ResourceCondition]) => RecipeOutput) extends IngredientProvider {

  override def glassTag: TagKey[Item] = ConventionalItemTags.GLASS_BLOCKS

  override def obsidianTag: TagKey[Item] = ConventionalItemTags.OBSIDIANS

  override def tagCondition(recipeOutput: RecipeOutput, tagKey: TagKey[Item]): RecipeOutput = {
    val condition = ResourceConditions.tagsPopulated(tagKey)
    withCondition(recipeOutput, Seq(condition))
  }

  override def subItemOfTank(tier: Tier): TankSubitem = {
    tier match {
      case Tier.WOOD => TankSubitem(ItemTags.LOGS)
      case Tier.STONE => TankSubitem(ConventionalItemTags.STONES)
      case Tier.IRON => TankSubitem(ConventionalItemTags.IRON_INGOTS)
      case Tier.GOLD => TankSubitem(ConventionalItemTags.GOLD_INGOTS)
      case Tier.DIAMOND => TankSubitem(ConventionalItemTags.DIAMOND_GEMS)
      case Tier.EMERALD => TankSubitem(ConventionalItemTags.EMERALD_GEMS)
      case Tier.STAR => TankSubitem(Items.NETHER_STAR)
      case Tier.VOID => TankSubitem(obsidianTag)
      case Tier.COPPER => TankSubitem(ConventionalItemTags.COPPER_INGOTS)
      case Tier.TIN => TankSubitem(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TagUtil.C_TAG_NAMESPACE, "ingots/tin")))
      case Tier.BRONZE => TankSubitem(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TagUtil.C_TAG_NAMESPACE, "ingots/bronze")))
      case Tier.LEAD => TankSubitem(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TagUtil.C_TAG_NAMESPACE, "ingots/lead")))
      case Tier.SILVER => TankSubitem(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TagUtil.C_TAG_NAMESPACE, "ingots/silver")))
      case _ => throw new IllegalArgumentException("Sub item of %s is not found".formatted(tier))
    }
  }
}
