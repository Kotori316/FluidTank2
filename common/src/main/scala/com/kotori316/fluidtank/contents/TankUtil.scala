package com.kotori316.fluidtank.contents

import com.kotori316.fluidtank.FluidTankCommon
import com.mojang.serialization.{Codec, DataResult}
import net.minecraft.nbt.CompoundTag

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
    val tag = new CompoundTag()
    tag.putString(KEY_TYPE, getType(tank).name)
    tag.put(access.KEY_CONTENT, access.write(tank.content))
    tag.putByteArray(access.KEY_AMOUNT_GENERIC, tank.capacity.asByteArray)
    tag
  }

  def load[A](tag: CompoundTag)(implicit access: GenericAccess[A]): Tank[A] = {
    if (tag != null && tag.contains(KEY_TYPE) && tag.contains(access.KEY_CONTENT) && tag.contains(access.KEY_AMOUNT_GENERIC)) {
      val tankType = typeByName(tag.getString(KEY_TYPE)).getOrElse(throw new IllegalArgumentException(s"Unknown type of tank (${tag.getString(KEY_TYPE)})"))
      val content = access.read(tag.getCompound(access.KEY_CONTENT))
      val capacity = GenericUnit.fromByteArray(tag.getByteArray(access.KEY_AMOUNT_GENERIC))
      createTank(content, capacity, tankType)
    } else {
      // necessary keys are unavailable
      FluidTankCommon.logOnceInMinute("TankUtil.load No keys",
        () => s"tag: $tag",
        () => new IllegalArgumentException("Not all required tag are present: " + tag))
      Tank(access.newInstance(access.empty, GenericUnit.ZERO, Option.empty), GenericUnit.ZERO)
    }
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
