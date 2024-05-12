package com.kotori316.fluidtank.forge.reservoir;

import com.kotori316.fluidtank.forge.render.RenderReservoirItemForge;
import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.Tier;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public final class ItemReservoirForge extends ItemReservoir {
    public ItemReservoirForge(Tier tier) {
        super(tier);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return RenderReservoirItemForge.INSTANCE;
            }
        });
    }

    /*@Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ReservoirFluidHandler(this, stack);
    }*/
}
