package com.kotori316.fluidtank.neoforge.config;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class NeoForgePlatformConfigAccess implements PlatformConfigAccess {
    private FluidTankConfig tankConfig;
    private ConfigData cached;

    public NeoForgePlatformConfigAccess() {
    }

    public FluidTankConfig setupConfig() {
        this.tankConfig = new FluidTankConfig(new ModConfigSpec.Builder(), new ModConfigSpec.Builder());
        return this.tankConfig;
    }

    @Override
    public ConfigData getConfig() {
        if (cached == null) {
            cached = this.tankConfig.createConfigData();
        }
        return cached;
    }

    @SubscribeEvent
    public void onConfigLoad(ModConfigEvent.Loading event) {
        invalidate("Load", event);
    }

    @SubscribeEvent
    public void onConfigReload(ModConfigEvent.Reloading event) {
        invalidate("Reload", event);
    }

    @SubscribeEvent
    public void onConfigUnload(ModConfigEvent.Unloading event) {
        invalidate("Unload", event);
    }

    private void invalidate(String action, ModConfigEvent event) {
        if (cached != null) {
            // ignore changes in initial setup
            FluidTankCommon.LOGGER.debug("{} FluidTank config {}",
                action, event.getConfig().getFileName());
        }
        cached = null;
    }
}
