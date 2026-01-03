package com.kotori316.fluidtank.fabric.integration.jade;

import com.kotori316.fluidtank.integration.tooltip.TooltipContent;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

final class FluidTankJadeComponentProvider implements IBlockComponentProvider {
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
    public Identifier getUid() {
        return TooltipContent.JADE_TOOLTIP_UID();
    }
}
