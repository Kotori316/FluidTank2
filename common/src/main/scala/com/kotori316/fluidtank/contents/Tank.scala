package com.kotori316.fluidtank.contents

import com.mojang.serialization.Codec

import java.util.Objects

class Tank[A](val content: GenericAmount[A], val capacity: GenericUnit) {
  final def amount: GenericUnit = content.amount

  final def isEmpty: Boolean = content.isEmpty

  final def hasContent: Boolean = !isEmpty

  override final def hashCode(): Int = Objects.hash(content, capacity.value)

  override final def equals(obj: Any): Boolean = obj match {
    case t: Tank[?] => this.content == t.content && this.capacity.value == t.capacity.value
    case _ => false
  }

  override final def toString: String = {
    val className = getClass.getSimpleName
    s"$className{content=$content, capacity=${capacity.value}}"
  }

  def copy(content: GenericAmount[A] = this.content, capacity: GenericUnit = this.capacity): Tank[A] = {
    if (this.getClass == classOf[Tank[A]]) {
      new Tank(content, capacity)
    } else {
      throw new NotImplementedError("Child classes of Tank must override copy method")
    }
  }

  // overridable!
  def fillOp: Operations.TankOperation[A] = Operations.fillOp(this)

  // overridable!
  def drainOp: Operations.TankOperation[A] = Operations.drainOp(this)
}

object Tank {
  def apply[A](content: GenericAmount[A], capacity: GenericUnit): Tank[A] = new Tank(content, capacity)

  def codec[A](implicit access: GenericAccess[A]): Codec[Tank[A]] = {
    CodecHelper.createTankCodec(access)
  }
}
