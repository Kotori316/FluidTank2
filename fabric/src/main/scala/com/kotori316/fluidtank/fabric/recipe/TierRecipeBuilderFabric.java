package com.kotori316.fluidtank.fabric.recipe;

import com.kotori316.fluidtank.tank.Tier;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TierRecipeBuilderFabric implements RecipeBuilder {
    private final RecipeCategory category = RecipeCategory.DECORATIONS;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    private final TierRecipeFabric recipe;

    public TierRecipeBuilderFabric(Tier tier, Ingredient tankItem, Ingredient subItem) {
        this.recipe = new TierRecipeFabric(tier, tankItem, subItem);
    }

    @Override
    public TierRecipeBuilderFabric unlockedBy(String name, Criterion<?> criterion) {
        criteria.put(name, criterion);
        return this;
    }

    @Override
    public TierRecipeBuilderFabric group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return recipe.getResultItem(RegistryAccess.EMPTY).getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        Advancement.Builder builder = recipeOutput.advancement().addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id)).rewards(AdvancementRewards.Builder.recipe(id)).requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(builder::addCriterion);
        recipeOutput.accept(id, this.recipe, builder.build(id.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }
}
