package com.kotori316.fluidtank.neoforge.integration.jade;

import com.kotori316.fluidtank.integration.tooltip.TooltipContent;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

final class FluidTankJadeComponentProvider implements IBlockComponentProvider {
    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof TileTank tileTank) {
            var content = TooltipContent.getTooltipTextJava(
                accessor.getServerData(),
                tileTank,
                config.get(TooltipContent.JADE_CONFIG_SHORT()),
                config.get(TooltipContent.JADE_CONFIG_COMPACT()),
                Minecraft.getInstance().getLocale()
            );
            tooltip.addAll(content);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return TooltipContent.JADE_TOOLTIP_UID();
    }
}
