package com.kotori316.fluidtank.neoforge.recipe;

import com.kotori316.fluidtank.recipe.TierRecipe;
import com.kotori316.fluidtank.tank.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class TierRecipeNeoForge extends TierRecipe {
    public static final RecipeSerializer<TierRecipe> SERIALIZER = new Serializer();

    public TierRecipeNeoForge(Tier tier, Ingredient tankItem, Ingredient subItem) {
        super(tier, tankItem, subItem);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public static class Serializer extends SerializerBase {
        public Serializer() {
            // This codec includes forge magic!
            super(Ingredient.CODEC_NONEMPTY);
        }

        @Override
        protected TierRecipe createInstance(Tier tier, Ingredient tankItem, Ingredient subItem) {
            return new TierRecipeNeoForge(tier, tankItem, subItem);
        }

        /*@Override
        public TierRecipe fromJson(ResourceLocation recipeLoc, JsonObject recipeJson, ICondition.IContext context) {
            return super.fromJson(recipeLoc, recipeJson, context);
        }*/
    }

}
