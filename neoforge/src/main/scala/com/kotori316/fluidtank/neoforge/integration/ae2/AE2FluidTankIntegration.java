package com.kotori316.fluidtank.neoforge.integration.ae2;
/*
import appeng.api.AECapabilities;
import com.kotori316.fluidtank.neoforge.FluidTank;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class AE2FluidTankIntegration {
    public static void onAPIAvailable(IEventBus modBus) {
        if (ModList.get().isLoaded("ae2")) {
            modBus.register(new AE2Capability());
        }
    }
}

class AE2Capability {
    @SubscribeEvent
    public void attachCapability(RegisterCapabilitiesEvent event) {
        Stream.of(FluidTank.TILE_TANK_TYPE, FluidTank.TILE_CREATIVE_TANK_TYPE, FluidTank.TILE_VOID_TANK_TYPE)
            .map(Supplier::get)
            .forEach(t -> event.registerBlockEntity(AECapabilities.ME_STORAGE, t, AE2Capability::create));
    }

    private static TankMEStorage create(TileTank tank, Direction ignored) {
        return new TankMEStorage(tank);
    }
}*/
