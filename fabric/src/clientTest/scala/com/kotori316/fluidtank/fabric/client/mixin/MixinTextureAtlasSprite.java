package com.kotori316.fluidtank.fabric.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlasSprite.class)
public abstract class MixinTextureAtlasSprite {
    @Inject(method = "createAnimationState", at = @At("HEAD"), cancellable = true)
    private void createTicker(GpuBufferSlice buffer, int size, CallbackInfoReturnable<SpriteContents.AnimationState> cir) {
        cir.setReturnValue(null);
    }

    @Inject(method = "isAnimated", at = @At("HEAD"), cancellable = true)
    private void isAnimated(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
