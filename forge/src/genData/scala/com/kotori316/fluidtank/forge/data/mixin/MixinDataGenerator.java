package com.kotori316.fluidtank.forge.data.mixin;

import net.minecraft.data.DataGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataGenerator.class)
public class MixinDataGenerator {
    /**
     * To prevent a bug(?) that the data generator doesn't finish forever in FluidTank
     */
    @Inject(method = "run", at = @At("RETURN"))
    private void finishMinecraft(CallbackInfo ci) {
        System.exit(0);
    }
}
