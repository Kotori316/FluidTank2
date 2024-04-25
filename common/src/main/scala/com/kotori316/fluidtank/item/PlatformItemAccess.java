package com.kotori316.fluidtank.item;

import com.google.gson.JsonElement;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidAmountUtil;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.logging.log4j.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlatformItemAccess {
    static PlatformItemAccess getInstance() {
        return PlatformItemAccessHolder.access;
    }

    static void setInstance(PlatformItemAccess access) {
        PlatformItemAccessHolder.access = access;
    }

    @NotNull
    ItemStack getCraftingRemainingItem(ItemStack stack);

    Codec<Ingredient> ingredientCodec();

    DataComponentType<Tank<FluidLike>> fluidTankComponentType();

    static void setTileTag(@NotNull ItemStack stack, @Nullable CompoundTag tileTag) {
        if (tileTag == null || tileTag.isEmpty()) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        } else {
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tileTag));
        }
    }

    static String convertIngredientToString(Ingredient ingredient) {
        return "[%s]".formatted(getInstance().ingredientToJson(ingredient));
    }

    default JsonElement ingredientToJson(Ingredient ingredient) {
        return ingredientCodec().encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow();
    }
}

class PlatformItemAccessHolder {
    @NotNull
    static PlatformItemAccess access = new Default();

    private static class Default implements PlatformItemAccess {
        Lazy<DataComponentType<Tank<FluidLike>>> dataComponentTypeLazy = Lazy.lazy(() -> {
            var builder = DataComponentType.<Tank<FluidLike>>builder()
                .persistent(Tank.codec(FluidAmountUtil.access()));
            return builder.build();
        });

        @Override
        public @NotNull ItemStack getCraftingRemainingItem(ItemStack stack) {
            var remaining = stack.getItem().getCraftingRemainingItem();
            if (remaining == null) return ItemStack.EMPTY;
            else return remaining.getDefaultInstance();
        }

        @Override
        public Codec<Ingredient> ingredientCodec() {
            return Ingredient.CODEC;
        }

        @Override
        public DataComponentType<Tank<FluidLike>> fluidTankComponentType() {
            return dataComponentTypeLazy.get();
        }
    }
}
