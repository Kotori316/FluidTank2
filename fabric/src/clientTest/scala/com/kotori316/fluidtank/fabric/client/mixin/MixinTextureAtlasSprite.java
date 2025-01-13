package com.kotori316.fluidtank.fabric.client.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlasSprite.class)
public abstract class MixinTextureAtlasSprite {
    @Inject(method = "createTicker", at = @At("HEAD"), cancellable = true)
    private void createTicker(CallbackInfoReturnable<TextureAtlasSprite.Ticker> cir) {
        cir.setReturnValue(null);
    }
}
