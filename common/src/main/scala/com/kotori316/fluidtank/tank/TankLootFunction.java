package com.kotori316.fluidtank.tank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public final class TankLootFunction extends LootItemConditionalFunction {
    public static final String NAME = "content_tank";

    public TankLootFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        var tile = context.getParam(LootContextParams.BLOCK_ENTITY);
        if (stack.getItem() instanceof ItemBlockTank tank) {
            tank.blockTank().saveTankNBT(tile, stack, context.getLevel().registryAccess());
        }
        return stack;
    }

    @Override
    public LootItemFunctionType getType() {
        return PlatformTankAccess.getInstance().getTankLoot();
    }

    public static LootItemConditionalFunction.Builder<?> builder() {
        return LootItemConditionalFunction.simpleBuilder(TankLootFunction::new);
    }

    public static final Codec<TankLootFunction> CODEC = RecordCodecBuilder.create(
        instance -> LootItemConditionalFunction.commonFields(instance).apply(instance, TankLootFunction::new)
    );
}
