package com.kotori316.fluidtank.fabric.integration.jade;

import com.kotori316.fluidtank.integration.tooltip.TooltipContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.TooltipPosition;

final class FluidTankJadeProvider implements IServerDataProvider<BlockAccessor> {

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor accessor) {
        TooltipContent.addServerData(compoundTag, accessor.getBlockEntity());
    }

    @Override
    public ResourceLocation getUid() {
        return TooltipContent.JADE_TOOLTIP_UID();
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.BODY;
    }
}
