package com.kotori316.fluidtank.neoforge.integration.top;

import com.kotori316.fluidtank.FluidTankCommon;
import mcjty.theoneprobe.api.ITheOneProbe;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;

import java.util.function.Function;

public final class FluidTankTopPlugin {
    private static final String TOP_ID = "theoneprobe";

    public static void sendIMC() {
        if (ModList.get().isLoaded(TOP_ID))
            Sender.internalSendIMC();
    }

    private static class Sender implements Function<ITheOneProbe, Void> {

        private static void internalSendIMC() {
            boolean registered = InterModComms.sendTo(FluidTankCommon.modId, FluidTankTopPlugin.TOP_ID, "getTheOneProbe", Sender::new);
            if (registered) {
                FluidTankCommon.LOGGER.info(FluidTankCommon.INITIALIZATION, "Registered TheOneProbe Plugin");
            }
        }

        @Override
        public Void apply(ITheOneProbe register) {
            register.registerProvider(FluidTankTopProvider$.MODULE$);
            return null;
        }
    }
}
