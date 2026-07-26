package com.kotori316.fluidtank.contents

import cats.implicits.{catsSyntaxGroup, catsSyntaxSemigroup}
import org.junit.jupiter.api.{Assertions, Nested, Test}
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GenericUnitTest {
  @Nested
  class ForgeTest {
    @Test
    def asForge(): Unit = {
      Assertions.assertEquals(1000, GenericUnit.ONE_BUCKET.asForge)
    }

    @Test
    def oneBucketFromForge(): Unit = {
      Assertions.assertEquals(GenericUnit.ONE_BUCKET, GenericUnit.fromForge(1000))
    }

    @Test
    def max(): Unit = {
      val maxUnit = GenericUnit.fromForge(Int.MaxValue)
      Assertions.assertEquals(Int.MaxValue, maxUnit.asForge)
    }

    @Test
    def maxPlus1(): Unit = {
      val overMax = GenericUnit.fromForge(1L + Int.MaxValue)
      Assertions.assertEquals(Int.MaxValue, overMax.asForge)
    }

    @Test
    def maxMinus1(): Unit = {
      val underMax = GenericUnit.fromForge(-1L + Int.MaxValue)
      Assertions.assertEquals(Int.MaxValue - 1, underMax.asForge)
    }
  }

  @Nested
  class FabricTest {
    @Test
    def asFabric(): Unit = {
      Assertions.assertEquals(81000L, GenericUnit.ONE_BUCKET.asFabric)
    }

    @Test
    def oneBucketFromFabric(): Unit = {
      Assertions.assertEquals(GenericUnit.ONE_BUCKET, GenericUnit.fromFabric(81000))
    }

    val MAX_BUCKET = 113_868_790_578_454L

    @Test
    def max(): Unit = {
      val maxUnit = GenericUnit.fromFabric(Long.MaxValue)
      Assertions.assertEquals(Long.MaxValue, maxUnit.asFabric)
    }

    @Test
    def maxPlus1(): Unit = {
      val overMax = GenericUnit(BigInt(Long.MaxValue) + BigInt(1))
      Assertions.assertEquals(Long.MaxValue, overMax.asFabric)
    }

    @Test
    def maxMinus1(): Unit = {
      val underMax = GenericUnit.fromFabric(-1L + Long.MaxValue)
      Assertions.assertEquals(Long.MaxValue - 1, underMax.asFabric)
    }
  }

  @Nested
  class RotationTest {
    @ParameterizedTest
    @ValueSource(longs = Array(
      0L, 81000L, 81000L * 5, 81000L * 10
    ))
    def rotation(amount: Long): Unit = {
      val genericUnit = GenericUnit(BigInt(amount))
      Assertions.assertAll(
        () => Assertions.assertEquals(genericUnit, GenericUnit.fromForge(genericUnit.asForge)),
        () => Assertions.assertEquals(genericUnit, GenericUnit.fromFabric(genericUnit.asFabric)),
        () => Assertions.assertEquals(genericUnit, GenericUnit.fromByteArray(genericUnit.asByteArray)),
      )
    }

    @ParameterizedTest
    @ValueSource(longs = Array(
      0L, 1L, 2L, 10L, 100L, 1000L, Int.MaxValue, Long.MaxValue
    ))
    def rotationByte(amount: Long): Unit = {
      val genericUnit = GenericUnit(BigInt(amount))
      Assertions.assertEquals(genericUnit, GenericUnit.fromByteArray(genericUnit.asByteArray))
    }

    @ParameterizedTest
    @ValueSource(strings = Array(
      "0", "100", "1000000000000000000000000000"
    ))
    def rotationByteFromString(amount: String): Unit = {
      val genericUnit = GenericUnit(BigInt(amount))
      Assertions.assertEquals(genericUnit, GenericUnit.fromByteArray(genericUnit.asByteArray))
    }
  }

  @Test
  def fromEmptyArray(): Unit = {
    val genericUnit = GenericUnit.fromByteArray(Array())
    Assertions.assertEquals(GenericUnit.ZERO, genericUnit)
  }

  @Nested
  class GroupTest {
    @Test
    def add(): Unit = {
      val a = GenericUnit(BigInt(200))
      val b = GenericUnit(BigInt(30))
      Assertions.assertEquals(GenericUnit(BigInt(230)), a |+| b)
    }

    @Test
    def minus(): Unit = {
      val a = GenericUnit(BigInt(200))
      val b = GenericUnit(BigInt(30))
      Assertions.assertEquals(GenericUnit(BigInt(170)), a |-| b)
    }

    @Test
    def inverse1(): Unit = {
      val a = GenericUnit(BigInt(200))
      Assertions.assertEquals(GenericUnit(BigInt(-200)), a.inverse())
    }

    @Test
    def inverse2(): Unit = {
      val a = GenericUnit(BigInt(0))
      Assertions.assertEquals(a, a.inverse())
    }

    @Test
    def multiply(): Unit = {
      val a = GenericUnit(BigInt(200))
      Assertions.assertEquals(GenericUnit(BigInt(200)), a.combineN(1))
      Assertions.assertEquals(GenericUnit(BigInt(400)), a.combineN(2))
      Assertions.assertEquals(GenericUnit(BigInt(600)), a.combineN(3))
    }
  }
}
