package com.kotori316.fluidtank.neoforge;

import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.testutil.MCTestInitializer;
import org.junit.jupiter.api.BeforeAll;

public abstract class BeforeMC {
    @BeforeAll
    public static void initialize() {
        MCTestInitializer.setUp(FluidTankCommon.modId, BeforeMC::setup, e -> {
            // make lazy to avoid accessing forge constants before initialization
            var c = MCTestInitializer.getRegisterer(FluidTank.REGISTER_LIST);
            c.accept(e);
        }, FluidTank::registerCapabilities);
    }

    private static void setup() {
        PlatformConfigAccess.setInstance(ConfigData::FOR_TEST);
    }

}
