package com.kotori316.fluidtank.fabric.integration.ae2;

import net.fabricmc.loader.api.FabricLoader;

public class AE2FluidTankIntegration {
    public static void onAPIAvailable() {
        if (FabricLoader.getInstance().isModLoaded("ae2"))
            AE2Capability.register();
    }

}

class AE2Capability {
    static void register() {
        /*MEStorage.SIDED.registerForBlockEntities(
                (blockEntity, context) -> {
                    if (blockEntity instanceof TileTank tank) return new TankMEStorage(tank);
                    else return null;
                },
                PlatformTankAccess.getInstance().getNormalType(),
                PlatformTankAccess.getInstance().getVoidType(),
                PlatformTankAccess.getInstance().getCreativeType()
        );*/
    }
}
