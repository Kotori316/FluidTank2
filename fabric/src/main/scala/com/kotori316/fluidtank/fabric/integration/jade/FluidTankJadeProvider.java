package com.kotori316.fluidtank.fabric.integration.jade;

import com.kotori316.fluidtank.integration.tooltip.TooltipContent;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

class FluidTankJadeProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof TileTank tileTank) {
            var languageSplit = Minecraft.getInstance().getLanguageManager().getSelected().split("_", 2);
            Locale locale;
            if (languageSplit.length == 2) {
                locale = Locale.of(languageSplit[0], languageSplit[1].toUpperCase(Locale.ROOT));
            } else {
                locale = Locale.US;
            }
            var content = TooltipContent.getTooltipTextJava(
                accessor.getServerData(),
                tileTank,
                false,
                false,
                locale
            );
            tooltip.addAll(content);
        }
    }

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
