package com.kotori316.fluidtank.neoforge;

import com.kotori316.fluidtank.FluidTankCommon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = FluidTankCommon.modId, dist = Dist.CLIENT)
public final class FluidTankClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FluidTankClient.class);

    public FluidTankClient(ModContainer container) {
        LOGGER.debug("Registering extension point for Config");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
