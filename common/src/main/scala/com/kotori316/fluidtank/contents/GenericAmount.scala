package com.kotori316.fluidtank.contents

import cats.implicits.{catsSyntaxEq, catsSyntaxGroup, catsSyntaxSemigroup}
import cats.{Hash, Show}
import com.kotori316.fluidtank.MCImplicits.*
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.CompoundTag

import java.util.Objects
import scala.annotation.targetName
import scala.math.Ordering.Implicits.infixOrderingOps

case class GenericAmount[ContentType](content: ContentType, amount: GenericUnit, componentPatch: Option[DataComponentPatch])
                                     (implicit access: GenericAccess[ContentType], contentHash: Hash[ContentType]) {
  final def setAmount(newAmount: GenericUnit): GenericAmount[ContentType] = new GenericAmount[ContentType](this.content, newAmount, this.componentPatch)

  final def isContentEmpty: Boolean = access.isEmpty(this.content)

  final def isEmpty: Boolean = isContentEmpty || this.amount.value <= 0

  final def nonEmpty: Boolean = !isEmpty

  final def getTag: CompoundTag = access.write(this)

  @targetName("operatorAdd")
  final def +(that: GenericAmount[ContentType]): GenericAmount[ContentType] = {
    val added = this.amount |+| that.amount
    if (this.isEmpty) that.setAmount(added)
    else this.setAmount(added)
  }

  final def add(that: GenericAmount[ContentType]): GenericAmount[ContentType] = this + that

  @targetName("operatorMinus")
  final def -(that: GenericAmount[ContentType]): GenericAmount[ContentType] = {
    val subtracted = this.amount |-| that.amount
    if (this.isEmpty) that.setAmount(subtracted)
    else this.setAmount(subtracted)
  }

  final def contentEqual(that: GenericAmount[ContentType]): Boolean =
    this.content === that.content && this.componentPatch === that.componentPatch

  final def createEmpty: GenericAmount[ContentType] = GenericAmount(access.empty, GenericUnit.ZERO, Option.empty)

  final def isGaseous: Boolean = access.isGaseous(this.content)

  final def hasOneBucket: Boolean = this.amount >= GenericUnit.ONE_BUCKET

  final def hasOneBottle: Boolean = this.amount >= GenericUnit.ONE_BOTTLE

  override final def equals(obj: Any): Boolean = obj match {
    case that: GenericAmount[?] =>
      val c = this.access.classTag
      that.content match {
        case c(content) => this.content === content && this.componentPatch === that.componentPatch && this.amount.value === that.amount.value
        case _ => false
      }
    case _ => false
  }

  override def hashCode(): Int = Objects.hash(this.content, this.amount.value, this.componentPatch)

  override def toString: String = s"GenericAmount{content=${access.asString(content)}, amount=${amount.value}, component=$componentPatch}"

  private def show: String = {
    val key = access.getKey(this.content)
    val amount = this.amount.asDisplay
    s"{$key, $amount mB, componentMap=$componentPatch}"
  }
}

object GenericAmount {
  implicit final def showGenericAmount[A]: Show[GenericAmount[A]] = _.show
}
