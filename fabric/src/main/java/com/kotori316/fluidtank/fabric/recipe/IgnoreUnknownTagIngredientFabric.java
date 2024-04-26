package com.kotori316.fluidtank.fabric.recipe;

import com.kotori316.fluidtank.FluidTankCommon;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.AnyIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class IgnoreUnknownTagIngredientFabric extends AnyIngredient {
    public static final ResourceLocation NAME = new ResourceLocation(FluidTankCommon.modId, "ignore_unknown_tag_ingredient");
    public static final CustomIngredientSerializer<IgnoreUnknownTagIngredientFabric> SERIALIZER = new Serializer();

    public IgnoreUnknownTagIngredientFabric(List<Ingredient> bases) {
        super(bases);
    }

    IgnoreUnknownTagIngredientFabric(Ingredient ingredient) {
        this(List.of(ingredient));
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    List<Ingredient> getBase() {
        return this.ingredients;
    }

    static final class Serializer implements CustomIngredientSerializer<IgnoreUnknownTagIngredientFabric> {
        static final MapCodec<IgnoreUnknownTagIngredientFabric> ALLOW_EMPTY = createCodec(true);
        static final MapCodec<IgnoreUnknownTagIngredientFabric> NON_EMPTY = createCodec(false);
        static final StreamCodec<RegistryFriendlyByteBuf, IgnoreUnknownTagIngredientFabric> STREAM_CODEC =
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list())
                .map(IgnoreUnknownTagIngredientFabric::new, IgnoreUnknownTagIngredientFabric::getBase);

        @Override
        public ResourceLocation getIdentifier() {
            return NAME;
        }

        @Override
        public MapCodec<IgnoreUnknownTagIngredientFabric> getCodec(boolean allowEmpty) {
            return allowEmpty ? ALLOW_EMPTY : NON_EMPTY;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, IgnoreUnknownTagIngredientFabric> getPacketCodec() {
            return STREAM_CODEC;
        }

        static MapCodec<IgnoreUnknownTagIngredientFabric> createCodec(boolean allowEmpty) {
            var base = allowEmpty ? Ingredient.CODEC : Ingredient.CODEC_NONEMPTY;

            return RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                    base.listOf().fieldOf("values").forGetter(IgnoreUnknownTagIngredientFabric::getBase),
                    Codec.STRING.fieldOf("type").forGetter(o -> NAME.toString()) // just for neoforge
                ).apply(instance, (t1, t2) -> new IgnoreUnknownTagIngredientFabric(t1))
            );
        }

    }
}
