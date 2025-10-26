package com.kotori316.fluidtank.neoforge.test;

import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.testutil.MCTestInitializer;
import org.junit.jupiter.api.BeforeAll;

public abstract class BeforeMC {
    @BeforeAll
    public static void initialize() {
        MCTestInitializer.setUp(BeforeMC::setup);
    }

    private static void setup() {
        PlatformConfigAccess.setInstance(ConfigData::FOR_TEST);
    }

}
