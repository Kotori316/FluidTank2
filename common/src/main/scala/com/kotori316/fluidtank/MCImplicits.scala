package com.kotori316.fluidtank

import cats.{Hash, Show}
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.storage.{TagValueOutput, ValueInput}

object MCImplicits {
  implicit final val showPos: Show[BlockPos] = pos => s"(${pos.getX}, ${pos.getY}, ${pos.getZ})"
  implicit final val hashCompoundTag: Hash[CompoundTag] = Hash.fromUniversalHashCode
  implicit final val hashFluid: Hash[Fluid] = Hash.fromUniversalHashCode
  implicit final val hashDataComponentPatch: Hash[DataComponentPatch] = Hash.fromUniversalHashCode

  extension (valueInput: ValueInput) {
    def stringConvert[T](key: String, func: String => T, defaultValue: T): T = {
      val string = valueInput.getString(key)
      string.map[T](s => func(s)).orElse(defaultValue)
    }
  }

  extension (tagValueOutput: TagValueOutput) {
    def merge(compoundTag: CompoundTag): Unit = {
      compoundTag.forEach((key, value) => {
        tagValueOutput.store(key, ExtraCodecs.NBT, value)
      })
    }
  }
}
