package com.kotori316.fluidtank.tank;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

public final class TankLootFunction extends LootItemConditionalFunction {
    public static final String NAME = "content_tank";

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public TankLootFunction(Optional<Holder<LootItemCondition>> condition) {
        super(condition);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        var tile = context.getOptional(LootContextParams.BLOCK_ENTITY);
        if (stack.getItem() instanceof ItemBlockTank tank) {
            tank.blockTank().saveTankNBT(tile, stack, context.getLevel().registryAccess());
        }
        return stack;
    }

    @Override
    public MapCodec<TankLootFunction> codec() {
        return CODEC;
    }

    public static LootItemConditionalFunction.Builder<?> builder() {
        return LootItemConditionalFunction.simpleBuilder(TankLootFunction::new);
    }

    public static final MapCodec<TankLootFunction> CODEC = RecordCodecBuilder.mapCodec(
        instance -> LootItemConditionalFunction.commonFields(instance).apply(instance, TankLootFunction::new)
    );
}
