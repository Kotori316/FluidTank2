package com.kotori316.fluidtank.forge.integration.ae2;
/*
import appeng.api.storage.MEStorage;
import com.kotori316.fluidtank.FluidTankCommon;
import com.kotori316.fluidtank.tank.TileTank;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AE2FluidTankIntegration {
    public static void onAPIAvailable() {
        if (ModList.get().isLoaded("ae2"))
            MinecraftForge.EVENT_BUS.register(new AE2FluidTankIntegration());
    }

    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, "attach_ae2");

    @SubscribeEvent
    public void attachCapability(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof TileTank tank) {
            event.addCapability(LOCATION, new AE2Capability(tank));
        }
    }
}

class AE2Capability implements ICapabilityProvider {
    private static final Capability<MEStorage> ME_STORAGE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    private final LazyOptional<MEStorage> accessorLazyOptional;

    AE2Capability(TileTank tank) {
        this.accessorLazyOptional = LazyOptional.of(() -> new TankMEStorage(tank));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction arg) {
        return ME_STORAGE_CAPABILITY.orEmpty(capability, this.accessorLazyOptional);
    }
}*/
