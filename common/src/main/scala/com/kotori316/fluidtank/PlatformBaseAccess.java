package com.kotori316.fluidtank;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public interface PlatformBaseAccess {
    static PlatformBaseAccess getInstance() {
        return PlatformBaseAccessHolder.access;
    }

    static void setInstance(PlatformBaseAccess access) {
        PlatformBaseAccessHolder.access = access;
    }

    enum Platforms {
        FABRIC,
        NEOFORGE,
        UNKNOWN,
    }

    @NotNull
    Platforms getPlatform();
}

class PlatformBaseAccessHolder {
    @NotNull
    static PlatformBaseAccess access = new Default();

    @ApiStatus.Internal
    private static class Default implements PlatformBaseAccess {
        @Override
        @NotNull
        public Platforms getPlatform() {
            return Platforms.UNKNOWN;
        }
    }
}
