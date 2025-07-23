package com.kotori316.fluidtank.fabric.integration.jade;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.TileTank;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(FluidTankCommon.modId)
public final class FluidTankJadePlugin implements IWailaPlugin {
    public FluidTankJadePlugin() {
        FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Registering {}", getClass().getSimpleName());
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        IWailaPlugin.super.register(registration);
        registration.registerBlockDataProvider(new FluidTankJadeProvider(), TileTank.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        IWailaPlugin.super.registerClient(registration);
        registration.registerBlockComponent(new FluidTankJadeComponentProvider(), BlockTank.class);
    }
}
