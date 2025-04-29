package com.kotori316.fluidtank.contents

import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.{CompoundTag, NbtOps}
import net.minecraft.resources.ResourceLocation

import scala.reflect.ClassTag

trait GenericAccess[A] {
  final val KEY_FLUID = "fluid"
  final val KEY_CONTENT = "content"
  final val KEY_FORGE_AMOUNT = "amount"
  final val KEY_FABRIC_AMOUNT = "fabric_amount"
  final val KEY_AMOUNT_GENERIC = "amount_generic"
  final val KEY_TAG = "tag"
  final val KEY_COMPONENT = "component"

  def isEmpty(a: A): Boolean

  def isGaseous(a: A): Boolean

  def getKey(a: A): ResourceLocation

  def fromKey(key: ResourceLocation): A

  def asString(a: A): String = getKey(a).toString

  def empty: A

  def newInstance(content: A, amount: GenericUnit, componentMap: Option[DataComponentPatch]): GenericAmount[A]

  private val codecInstance = CodecHelper.createGenericAmountCodec(this)

  def codec: Codec[GenericAmount[A]] = codecInstance

  def write(amount: GenericAmount[A]): CompoundTag = {
    this.codec.encodeStart(NbtOps.INSTANCE, amount)
      .result()
      .flatMap[CompoundTag](_.asCompound())
      .orElse(new CompoundTag())
  }

  def read(tag: CompoundTag): GenericAmount[A] = {
    this.codec.parse(NbtOps.INSTANCE, tag).result().orElseGet(() => newInstance(empty, GenericUnit.ZERO, Option.empty))
  }

  def classTag: ClassTag[A]
}
