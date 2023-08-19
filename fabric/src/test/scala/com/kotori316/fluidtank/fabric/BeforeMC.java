package com.kotori316.fluidtank.fabric;

import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BeforeMC {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    @BeforeAll
    public static void setup() {
        if (!initialized.getAndSet(true)) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            PlatformConfigAccess.setInstance(ConfigData::DEFAULT);
        }
    }

    public static void assertEqualHelper(Object expected, Object actual) {
        Assertions.assertEquals(expected, actual, "Expected: %s, Actual: %s".formatted(expected, actual));
    }
}
