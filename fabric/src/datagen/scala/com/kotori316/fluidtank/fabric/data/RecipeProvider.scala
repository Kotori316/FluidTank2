package com.kotori316.fluidtank.fabric.data

import cats.data.Ior
import cats.implicits.{catsSyntaxIorId, catsSyntaxOptionId}
import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.fabric.FluidTank
import com.kotori316.fluidtank.fabric.recipe.{IgnoreUnknownTagIngredientFabric, TierRecipeBuilderFabric}
import com.kotori316.fluidtank.recipe.TierRecipe
import com.kotori316.fluidtank.tank.Tier
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags
import net.minecraft.advancements.Advancement.Builder
import net.minecraft.advancements.critereon.{InventoryChangeTrigger, ItemPredicate}
import net.minecraft.advancements.{Advancement, AdvancementHolder, Criterion}
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput.Target
import net.minecraft.data.recipes.{RecipeBuilder, RecipeCategory, RecipeOutput, ShapedRecipeBuilder, ShapelessRecipeBuilder}
import net.minecraft.data.{CachedOutput, DataProvider}
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.{ItemTags, TagKey}
import net.minecraft.world.item.crafting.{Ingredient, Recipe}
import net.minecraft.world.item.{Item, Items}

import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters.{MapHasAsScala, SeqHasAsJava}

class RecipeProvider(output: FabricDataOutput, provider: CompletableFuture[HolderLookup.Provider])
  extends DataProvider {
  private val recipePathProvider = output.createPathProvider(Target.DATA_PACK, "recipes")
  private val advancementPathProvider = output.createPathProvider(Target.DATA_PACK, "advancements")

  case class RecipeWithCondition(recipe: RecipeBuilder, conditions: Seq[PlatformCondition])

  class RecipeOutputWrapper(provider: HolderLookup.Provider, conditions: Seq[PlatformCondition], cache: CachedOutput) extends RecipeOutput {
    private val ops = provider.createSerializationContext(JsonOps.INSTANCE)
    private val builder = Seq.newBuilder[CompletableFuture[?]]

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

  override def getName: String = s"Recipe of ${FluidTankCommon.modId}"

  override def run(cache: CachedOutput): CompletableFuture[?] = {
    provider.thenCompose { p =>
      val works = getRecipes
        .flatMap { r =>
          val output = new RecipeOutputWrapper(p, r.conditions, cache)
          r.recipe.save(output)
          output.features
        }
      CompletableFuture.allOf(works *)
    }
  }

  def both(fabricTag: TagKey[Item], forgeTag: String): (Ingredient, Criterion[InventoryChangeTrigger.TriggerInstance], PlatformCondition) = {
    val ft = TagKey.create(Registries.ITEM, new ResourceLocation(forgeTag))
    (
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

  private def both(fabricTag: String, forgeTag: String): (Ingredient, Criterion[InventoryChangeTrigger.TriggerInstance], PlatformCondition) =
    both(TagKey.create(Registries.ITEM, new ResourceLocation(fabricTag)), forgeTag)

  def itemFallBack(item: Item, neoforgeTag: String, forgeTag: String): Ingredient = {
    new IgnoreUnknownTagIngredientFabric(Seq(
      Ingredient.of(item),
      Ingredient.of(TagKey.create(Registries.ITEM, new ResourceLocation(neoforgeTag))),
      Ingredient.of(TagKey.create(Registries.ITEM, new ResourceLocation(forgeTag))),
    ).asJava).toVanilla
  }

  private def getRecipes: Seq[RecipeWithCondition] = {
    val woodTankBlock = FluidTank.TANK_MAP.get(Tier.WOOD)
    val (glassItem, glassCriterion, glassCondition) = both(ConventionalItemTags.GLASS_BLOCKS, "forge:glass")
    val woodTank = RecipeWithCondition(
      ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, woodTankBlock)
        .define('x', glassItem)
        .define('p', ItemTags.LOGS)
        .pattern("x x")
        .pattern("xpx")
        .pattern("xxx")
        .unlockedBy("has_glass", glassCriterion)
        .unlockedBy("has_water_bucket", InventoryChangeTrigger.TriggerInstance.hasItems(Items.WATER_BUCKET)),
      Seq(glassCondition)
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
      Seq.empty
    )
    val normalTanks = Tier.values().filter(_.isNormalTankTier).filterNot(_ == Tier.WOOD)
      .map { t =>
        val tankItem = TierRecipe.SerializerBase.getIngredientTankForTier(t)
        val itemArr = tankItem.getItems.map(_.getItem())
        val location = new ResourceLocation(FluidTankCommon.modId, t.getBlockName)
        getSubItem(t) match
          case Ior.Left(a) =>
            val builder = new TierRecipeBuilderFabric(t, tankItem, a)
              .unlockedBy("has_tank", InventoryChangeTrigger.TriggerInstance.hasItems(itemArr *))
              .unlockedBy("has_ingredient", InventoryChangeTrigger.TriggerInstance.hasItems(a.getItems.map(_.getItem) *))
            RecipeWithCondition(builder, Seq.empty)
          case Ior.Right((ingredient, criterion, condition)) =>
            val builder = new TierRecipeBuilderFabric(t, tankItem, ingredient)
              .unlockedBy("has_tank", InventoryChangeTrigger.TriggerInstance.hasItems(itemArr *))
              .unlockedBy("has_ingredient", criterion)
            RecipeWithCondition(builder, Seq(condition))
          case _ => throw NotImplementedError()
      }

    val reservoirs = FluidTank.RESERVOIR_MAP.asScala.map { (tier, reservoir) =>
      val tank = FluidTank.TANK_MAP.get(tier)
      RecipeWithCondition(
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, reservoir)
          .requires(tank)
          .requires(Items.BUCKET)
          .requires(Items.BUCKET)
          .unlockedBy("has_tank", InventoryChangeTrigger.TriggerInstance.hasItems(tank)),
        Seq.empty,
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
      Seq.empty,
    )

    Seq(
      woodTank, voidTank, cat
    ) ++ normalTanks ++ reservoirs
  }

  private def getSubItem(tier: Tier): Ior[Ingredient, (Ingredient, Criterion[InventoryChangeTrigger.TriggerInstance], PlatformCondition)] = {
    tier match {
      case Tier.WOOD => Ingredient.of(ItemTags.LOGS).leftIor
      case Tier.STONE => both(ConventionalItemTags.STONES, "forge:stone").rightIor
      case Tier.IRON => both(ConventionalItemTags.IRON_INGOTS, "forge:ingots/iron").rightIor
      case Tier.GOLD => both(ConventionalItemTags.GOLD_INGOTS, "forge:ingots/gold").rightIor
      case Tier.DIAMOND => both(ConventionalItemTags.DIAMOND_GEMS, "forge:gems/diamond").rightIor
      case Tier.EMERALD => both(ConventionalItemTags.EMERALD_GEMS, "forge:gems/emerald").rightIor
      case Tier.STAR => itemFallBack(Items.NETHER_STAR, "c:nether_stars", "forge:nether_stars").leftIor
      case Tier.VOID => itemFallBack(Items.OBSIDIAN, "c:obsidian", "forge:obsidian").leftIor
      case Tier.COPPER => both(ConventionalItemTags.COPPER_INGOTS, "ingots/copper").rightIor
      case Tier.TIN => both("c:ingots/tin", "forge:ingots/tin").rightIor
      case Tier.BRONZE => both("c:ingots/bronze", "forge:ingots/bronze").rightIor
      case Tier.LEAD => both("c:ingots/lead", "forge:ingots/lead").rightIor
      case Tier.SILVER => both("c:ingots/silver", "forge:ingots/silver").rightIor
      case _ => throw new IllegalArgumentException("Sub item of %s is not found".formatted(tier))
    }
  }
}
