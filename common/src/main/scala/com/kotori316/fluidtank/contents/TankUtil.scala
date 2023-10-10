package com.kotori316.fluidtank.contents

import com.kotori316.fluidtank.FluidTankCommon
import net.minecraft.nbt.CompoundTag

object TankUtil {
  private final val KEY_TYPE = "type"
  private final val TYPE_TANK = "Tank"
  private final val TYPE_CREATIVE_TANK = "CreativeTank"
  private final val TYPE_VOID_TANK = "VoidTank"

  def save[A](tank: Tank[A])(implicit access: GenericAccess[A]): CompoundTag = {
    val tag = new CompoundTag()
    tag.putString(KEY_TYPE, getType(tank))
    tag.put(access.KEY_CONTENT, access.write(tank.content))
    tag.putByteArray(access.KEY_AMOUNT_GENERIC, tank.capacity.asByteArray)
    tag
  }

  def load[A](tag: CompoundTag)(implicit access: GenericAccess[A]): Tank[A] = {
    if (tag != null && tag.contains(KEY_TYPE) && tag.contains(access.KEY_CONTENT) && tag.contains(access.KEY_AMOUNT_GENERIC)) {
      val tankType = tag.getString(KEY_TYPE)
      val content = access.read(tag.getCompound(access.KEY_CONTENT))
      val capacity = GenericUnit.fromByteArray(tag.getByteArray(access.KEY_AMOUNT_GENERIC))

      tankType match {
        case TYPE_TANK => Tank(content, capacity)
        case TYPE_CREATIVE_TANK => new CreativeTank(content, capacity)
        case TYPE_VOID_TANK => new VoidTank(content, capacity)
        case _ => throw new IllegalArgumentException("Unknown type of tank for %s, %s".formatted(content, tag))
      }
    } else {
      // necessary keys are unavailable
      FluidTankCommon.logOnceInMinute("TankUtil.load No keys",
        () => s"tag: $tag",
        () => new IllegalArgumentException("Not all required tag are present: " + tag))
      Tank(access.newInstance(access.empty, GenericUnit.ZERO, Option.empty), GenericUnit.ZERO)
    }
  }

  private def getType(tank: Tank[?]): String = {
    tank match {
      case _: CreativeTank[?] => TYPE_CREATIVE_TANK
      case _: VoidTank[?] => TYPE_VOID_TANK
      case _: Tank[?] => TYPE_TANK
      case null => throw new IllegalArgumentException("Unknown type of tank, %s".formatted(tank.getClass))
    }
  }
}
