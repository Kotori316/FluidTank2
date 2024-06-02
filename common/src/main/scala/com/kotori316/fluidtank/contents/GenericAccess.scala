package com.kotori316.fluidtank.contents

import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.{CompoundTag, NbtOps, Tag as NbtTag}
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
    val tag = new CompoundTag()

    tag.putString(KEY_CONTENT, getKey(amount.content).toString)
    tag.putByteArray(KEY_AMOUNT_GENERIC, amount.amount.asByteArray)
    amount.componentPatch.foreach(t => tag.put(KEY_TAG, DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, t).getOrThrow()))

    tag
  }

  def read(tag: CompoundTag): GenericAmount[A] = {
    val key = ResourceLocation.parse(
      if (tag.contains(KEY_CONTENT)) tag.getString(KEY_CONTENT)
      else tag.getString(KEY_FLUID)
    )
    val content = fromKey(key)
    val amount: GenericUnit = {
      if (tag.contains(KEY_AMOUNT_GENERIC, NbtTag.TAG_BYTE_ARRAY)) GenericUnit.fromByteArray(tag.getByteArray(KEY_AMOUNT_GENERIC))
      else if (tag.contains(KEY_FABRIC_AMOUNT)) GenericUnit.fromFabric(tag.getLong(KEY_FABRIC_AMOUNT))
      else GenericUnit.fromForge(tag.getLong(KEY_FORGE_AMOUNT))
    }
    val component: Option[DataComponentPatch] = Option.when(tag.contains(KEY_TAG))(tag.getCompound(KEY_TAG))
      .map(t => DataComponentPatch.CODEC.decode(NbtOps.INSTANCE, t).map(_.getFirst).getOrThrow())
    newInstance(content, amount, component)
  }

  def classTag: ClassTag[A]
}
