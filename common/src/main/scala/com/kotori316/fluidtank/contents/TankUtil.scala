package com.kotori316.fluidtank.contents

import com.kotori316.fluidtank.FluidTankCommon
import com.mojang.serialization.{Codec, DataResult}
import net.minecraft.nbt.{CompoundTag, NbtOps}

import java.nio.ByteBuffer

object TankUtil {
  final val KEY_TYPE = "type"

  final val tankTypeCodec: Codec[TankType] = Codec.STRING.flatXmap(name => {
    typeByName(name) match {
      case Some(value) => DataResult.success(value)
      case None => DataResult.error(() => s"Unknown type of tank ($name)")
    }
  }, t => DataResult.success(t.name))

  enum TankType(val name: String) {
    case TANK extends TankType("Tank")
    case CREATIVE_TANK extends TankType("CreativeTank")
    case VOID_TANK extends TankType("VoidTank")
  }

  private def typeByName(name: String): Option[TankType] = {
    TankType.values.find(_.name == name)
  }

  def save[A](tank: Tank[A])(implicit access: GenericAccess[A]): CompoundTag = {
    Tank.codec.encodeStart(NbtOps.INSTANCE, tank)
      .result()
      .flatMap[CompoundTag](_.asCompound())
      .orElse(new CompoundTag())
  }

  def load[A](tag: CompoundTag)(implicit access: GenericAccess[A]): Tank[A] = {
    Tank.codec.parse(NbtOps.INSTANCE, tag)
      .mapOrElse(t => t, { error =>
        // necessary keys are unavailable
        FluidTankCommon.logOnceInMinute("TankUtil.load failed",
          () => s"tag: $tag",
          () => new IllegalArgumentException(error.message()))
        Tank(access.newInstance(access.empty, GenericUnit.ZERO, Option.empty), GenericUnit.ZERO)
      })
  }

  def createTank[A](content: GenericAmount[A], byteBuffer: ByteBuffer, tankType: TankType): Tank[A] = {
    val capacity = GenericUnit.fromByteArray(byteBuffer.array())
    createTank(content, capacity, tankType)
  }

  private def createTank[A](content: GenericAmount[A], capacity: GenericUnit, tankType: TankType): Tank[A] = {
    tankType match {
      case TankType.TANK => Tank(content, capacity)
      case TankType.CREATIVE_TANK => new CreativeTank(content, capacity)
      case TankType.VOID_TANK => new VoidTank(content, capacity)
    }
  }

  def getType(tank: Tank[?]): TankType = {
    tank match {
      case _: CreativeTank[?] => TankType.CREATIVE_TANK
      case _: VoidTank[?] => TankType.VOID_TANK
      case _: Tank[?] => TankType.TANK
      case null => throw new IllegalArgumentException("Unknown type of tank, %s".formatted(tank))
    }
  }
}
