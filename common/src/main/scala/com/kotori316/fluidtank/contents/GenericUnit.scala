package com.kotori316.fluidtank.contents

import cats.Hash
import cats.kernel.CommutativeGroup

class GenericUnit private(val value: BigInt) extends AnyVal {
  def asFabric: Long = {
    if (this.value > GenericUnit.LONG_MAX) {
      Long.MaxValue
    } else {
      this.value.longValue
    }
  }

  def asForge: Int = {
    val inForge = GenericUnit.asForgeFromBigInt(this.value)
    if (inForge > GenericUnit.INT_MAX) {
      Int.MaxValue
    } else {
      inForge.intValue
    }
  }

  def asNeoForge: Long = asDisplay

  /**
   * Returns forge unit amount
   */
  def asDisplay: Long = {
    val inForge = GenericUnit.asForgeFromBigInt(this.value)
    if (inForge > GenericUnit.LONG_MAX) {
      Long.MaxValue
    } else {
      inForge.longValue
    }
  }

  def asByteArray: Array[Byte] = this.value.toByteArray

  def asForgeDouble: Double = {
    this.value.doubleValue / 81d
  }
}

object GenericUnit {
  private final val FABRIC_ONE_BUCKET = 81000
  private final val FORGE_ONE_BUCKET = 1000
  private final val LONG_MAX = BigInt(Long.MaxValue)
  private final val INT_MAX = BigInt(Int.MaxValue)
  final val ZERO: GenericUnit = new GenericUnit(BigInt(0))
  final val ONE_BUCKET: GenericUnit = new GenericUnit(BigInt(FABRIC_ONE_BUCKET))
  final val ONE_BOTTLE: GenericUnit = new GenericUnit(BigInt(FABRIC_ONE_BUCKET / 3))
  final val MAX: GenericUnit = new GenericUnit(GenericUnit.LONG_MAX * BigInt(FABRIC_ONE_BUCKET))
  final val CREATIVE_TANK: GenericUnit = fromForge(100_000_000_000_000L * 1000)

  def fromFabric(value: Long): GenericUnit = new GenericUnit(BigInt(value))

  def fromForge(value: Long): GenericUnit = new GenericUnit(asBigIntFromForge(value))

  def asBigIntFromForge(value: Long): BigInt = BigInt(value * 81)

  @inline
  def asForgeFromBigInt(value: BigInt): BigInt = value / 81

  def fromByteArray(value: Array[Byte]): GenericUnit = new GenericUnit(BigInt(value))

  /**
   * Get [[GenericUnit]] from [[BigInt]]
   *
   * @param value formatted for fabric unit. From forge, use [[asBigIntFromForge]] to convert.
   */
  def apply(value: BigInt): GenericUnit = new GenericUnit(value)

  implicit final val groupGenericUnit: CommutativeGroup[GenericUnit] = new GroupGenericUnit
  implicit final val orderingGenericUnit: Ordering[GenericUnit] = Ordering.by(_.value)
  implicit final val hashGenericUnit: Hash[GenericUnit] = Hash.by(_.value)

  private class GroupGenericUnit extends CommutativeGroup[GenericUnit] {
    override def inverse(a: GenericUnit): GenericUnit = new GenericUnit(-a.value)

    override def empty: GenericUnit = GenericUnit.ZERO

    override def combine(x: GenericUnit, y: GenericUnit): GenericUnit = new GenericUnit(x.value + y.value)

    override def remove(x: GenericUnit, y: GenericUnit): GenericUnit = new GenericUnit(x.value - y.value)

    override def combineN(a: GenericUnit, n: Int): GenericUnit = new GenericUnit(a.value * n)
  }

}
