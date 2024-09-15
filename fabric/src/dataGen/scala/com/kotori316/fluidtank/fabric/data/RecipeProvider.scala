package com.kotori316.fluidtank.fabric.data

import cats.data.Chain
import cats.implicits.catsSyntaxOptionId
import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.fabric.FluidTank
import com.kotori316.fluidtank.fabric.data.RecipeProvider.*
import com.kotori316.fluidtank.fabric.recipe.IgnoreUnknownTagIngredientFabric
import com.kotori316.fluidtank.recipe.{TierRecipe, TierRecipeBuilder}
import com.kotori316.fluidtank.tank.Tier
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags
import net.minecraft.advancements.Advancement.Builder
import net.minecraft.advancements.critereon.{InventoryChangeTrigger, ItemPredicate}
import net.minecraft.advancements.{Advancement, AdvancementHolder, Criterion}
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.recipes.{RecipeBuilder, RecipeCategory, RecipeOutput, ShapedRecipeBuilder, ShapelessRecipeBuilder}
import net.minecraft.data.{CachedOutput, DataProvider}
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.{ItemTags, TagKey}
import net.minecraft.world.item.crafting.{Ingredient, Recipe}
import net.minecraft.world.item.{Item, Items}

import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters.{MapHasAsScala, SeqHasAsJava}

private[data] final class RecipeProvider(dataOutput: FabricDataOutput, provider: CompletableFuture[HolderLookup.Provider])
  extends DataProvider {

  override def getName: String = s"Recipe of ${FluidTankCommon.modId}"

  override def run(cache: CachedOutput): CompletableFuture[?] = {
    provider.thenCompose { p =>
      val works = getRecipes
        .flatMap { r =>
          val recipeOutput = new RecipeOutputWrapper(dataOutput, p, r.conditions, cache)
          r.recipe.save(recipeOutput)
          recipeOutput.features
        }
      CompletableFuture.allOf(works *)
    }
  }

  private def both(fabricTag: TagKey[Item], forgeTag: String): TagThings = {
    val ft = TagKey.create(Registries.ITEM, ResourceLocation.parse(forgeTag))
    TagThings(
      new IgnoreUnknownTagIngredientFabric(Seq(
        Ingredient.of(fabricTag),
        Ingredient.of(ft)
      ).asJava).toVanilla,
      InventoryChangeTrigger.TriggerInstance.hasItems(
        ItemPredicate.Builder.item().of(fabricTag),
        ItemPredicate.Builder.item().of(ft),
      ),
      PlatformCondition.tagCondition(fabricTag.some, forgeTag.some),
    )
  }

  private def both(fabricTag: String, forgeTag: String): TagThings =
    both(TagKey.create(Registries.ITEM, ResourceLocation.parse(fabricTag)), forgeTag)

  private def itemFallBack(item: Item, neoforgeTag: String, forgeTag: String): Ingredient = {
    new IgnoreUnknownTagIngredientFabric(Seq(
      Ingredient.of(item),
      Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(neoforgeTag))),
      Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(forgeTag))),
    ).asJava).toVanilla
  }

  private def getRecipes: Seq[RecipeWithCondition] = {
    val woodTankBlock = FluidTank.TANK_MAP.get(Tier.WOOD)
    val TagThings(glassItem, glassCriterion, glassCondition) = both(ConventionalItemTags.GLASS_BLOCKS, "forge:glass")
    val woodTank = RecipeWithCondition(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, woodTankBlock)
        .define('x', glassItem)
        .define('p', ItemTags.LOGS)
        .pattern("x x")
        .pattern("xpx")
        .pattern("xxx")
        .unlockedBy("has_glass", glassCriterion)
        .unlockedBy("has_water_bucket", InventoryChangeTrigger.TriggerInstance.hasItems(Items.WATER_BUCKET)),
      Chain.one(glassCondition)
    )
    val obsidianItem = itemFallBack(Items.OBSIDIAN, "c:obsidian", "forge:obsidian")
    val voidTank = RecipeWithCondition(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, FluidTank.BLOCK_VOID_TANK)
        .define('o', obsidianItem)
        .define('t', woodTankBlock)
        .pattern("ooo")
        .pattern("oto")
        .pattern("ooo")
        .unlockedBy("has_wood_tank", InventoryChangeTrigger.TriggerInstance.hasItems(woodTankBlock)),
      Chain.empty
    )
    val normalTanks = Tier.values().filter(_.isNormalTankTier).filterNot(_ == Tier.WOOD)
      .map { t =>
        val tankItem = TierRecipe.Serializer.getIngredientTankForTier(t)
        val itemArr = tankItem.getItems.map(_.getItem())
        val location = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, t.getBlockName)
        getSubItem(t) match {
          case a: Ingredient =>
            val builder = new TierRecipeBuilder(t, tankItem, a)
              .unlockedBy("has_tank", InventoryChangeTrigger.TriggerInstance.hasItems(itemArr *))
              .unlockedBy("has_ingredient", InventoryChangeTrigger.TriggerInstance.hasItems(a.getItems.map(_.getItem) *))
            RecipeWithCondition(builder, Chain.empty)
          case tuple: TagThings =>
            val builder = new TierRecipeBuilder(t, tankItem, tuple.ingredient)
              .unlockedBy("has_tank", InventoryChangeTrigger.TriggerInstance.hasItems(itemArr *))
              .unlockedBy("has_ingredient", tuple.criterion)
            RecipeWithCondition(builder, Chain.one(tuple.condition))
        }
      }

    val reservoirs = FluidTank.RESERVOIR_MAP.asScala.map { (tier, reservoir) =>
      val tank = FluidTank.TANK_MAP.get(tier)
      RecipeWithCondition(
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, reservoir)
          .requires(tank)
          .requires(Items.BUCKET)
          .requires(Items.BUCKET)
          .unlockedBy("has_tank", InventoryChangeTrigger.TriggerInstance.hasItems(tank)),
        Chain.empty,
      )
    }

    val cat = RecipeWithCondition(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, FluidTank.BLOCK_CAT)
        .define('p', Ingredient.of(Items.CHEST, Items.BARREL))
        .define('x', woodTankBlock)
        .pattern("x x")
        .pattern("xpx")
        .pattern("xxx")
        .unlockedBy("has_tank", InventoryChangeTrigger.TriggerInstance.hasItems(woodTankBlock)),
      Chain.empty,
    )

    Seq(
      woodTank, voidTank, cat
    ) ++ normalTanks ++ reservoirs
  }

  private def getSubItem(tier: Tier): Ingredient | TagThings = {
    tier match {
      case Tier.WOOD => Ingredient.of(ItemTags.LOGS)
      case Tier.STONE => both(ConventionalItemTags.STONES, "forge:stone")
      case Tier.IRON => both(ConventionalItemTags.IRON_INGOTS, "forge:ingots/iron")
      case Tier.GOLD => both(ConventionalItemTags.GOLD_INGOTS, "forge:ingots/gold")
      case Tier.DIAMOND => both(ConventionalItemTags.DIAMOND_GEMS, "forge:gems/diamond")
      case Tier.EMERALD => both(ConventionalItemTags.EMERALD_GEMS, "forge:gems/emerald")
      case Tier.STAR => itemFallBack(Items.NETHER_STAR, "c:nether_stars", "forge:nether_stars")
      case Tier.VOID => itemFallBack(Items.OBSIDIAN, "c:obsidian", "forge:obsidian")
      case Tier.COPPER => both(ConventionalItemTags.COPPER_INGOTS, "forge:ingots/copper")
      case Tier.TIN => both("c:ingots/tin", "forge:ingots/tin")
      case Tier.BRONZE => both("c:ingots/bronze", "forge:ingots/bronze")
      case Tier.LEAD => both("c:ingots/lead", "forge:ingots/lead")
      case Tier.SILVER => both("c:ingots/silver", "forge:ingots/silver")
      case _ => throw new IllegalArgumentException("Sub item of %s is not found".formatted(tier))
    }
  }
}

object RecipeProvider {

  private case class RecipeWithCondition(recipe: RecipeBuilder, conditions: Chain[PlatformCondition])

  private case class TagThings(ingredient: Ingredient, criterion: Criterion[InventoryChangeTrigger.TriggerInstance], condition: PlatformCondition)

  private final class RecipeOutputWrapper(output: FabricDataOutput, provider: HolderLookup.Provider, conditions: Chain[PlatformCondition], cache: CachedOutput) extends RecipeOutput {
    private final val recipePathProvider = output.createRegistryElementsPathProvider(Registries.RECIPE)
    private final val advancementPathProvider = output.createRegistryElementsPathProvider(Registries.ADVANCEMENT)
    private final val ops = provider.createSerializationContext(JsonOps.INSTANCE)
    private final val builder = Seq.newBuilder[CompletableFuture[?]]

    override def accept(location: ResourceLocation, recipe: Recipe[?], advancement: AdvancementHolder): Unit = {
      {
        val json = Recipe.CODEC.encodeStart(ops, recipe).getOrThrow().getAsJsonObject
        PlatformCondition.addPlatformConditions(json, conditions)
        val recipePath = recipePathProvider.json(location)
        builder += DataProvider.saveStable(cache, json, recipePath)
      }

      if (advancement != null) {
        val json = Advancement.CODEC.encodeStart(ops, advancement.value()).getOrThrow().getAsJsonObject
        PlatformCondition.addPlatformConditions(json, conditions)
        val path = advancementPathProvider.json(advancement.id())
        builder += DataProvider.saveStable(cache, json, path)
      }
    }

    override def advancement(): Advancement.Builder = Builder.recipeAdvancement.parent(AdvancementHolder(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT, null))

    def features: Seq[CompletableFuture[?]] = builder.result()
  }

}
