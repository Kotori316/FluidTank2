package com.kotori316.fluidtank.contents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import scala.jdk.javaapi.OptionConverters;

import java.nio.ByteBuffer;

final class CodecHelper {
    static <T> Codec<GenericAmount<T>> createGenericAmountCodec(GenericAccess<T> access) {
        return RecordCodecBuilder.create(instance -> {
            var t = instance.group(
                ResourceLocation.CODEC.fieldOf(access.KEY_CONTENT()).forGetter(a -> access.getKey(a.content())),
                Codec.BYTE_BUFFER.fieldOf(access.KEY_AMOUNT_GENERIC()).forGetter(a -> ByteBuffer.wrap(a.amount().toByteArray())),
                DataComponentPatch.CODEC.optionalFieldOf(access.KEY_COMPONENT()).forGetter(a ->
                    OptionConverters.toJava(a.componentPatch())
                )
            );
            return t.apply(instance, (a, b, c) ->
                access.newInstance(access.fromKey(a), GenericUnit.fromByteArray(b.array()), OptionConverters.toScala(c))
            );
        });
    }

    static <T> Codec<Tank<T>> createTankCodec(GenericAccess<T> access) {
        return RecordCodecBuilder.create(instance ->
            instance.group(
                access.codec().fieldOf(access.KEY_CONTENT()).forGetter(Tank::content),
                Codec.BYTE_BUFFER.fieldOf(access.KEY_AMOUNT_GENERIC()).forGetter(a -> ByteBuffer.wrap(a.capacity().toByteArray())),
                TankUtil.tankTypeCodec().fieldOf(TankUtil.KEY_TYPE()).forGetter(TankUtil::getType)
            ).apply(instance, TankUtil::createTank)
        );
    }
}
