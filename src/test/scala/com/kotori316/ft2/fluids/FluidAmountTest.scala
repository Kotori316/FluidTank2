package com.kotori316.ft2.fluids

import cats.syntax.eq._
import cats.syntax.group._
import com.kotori316.ft2.BeforeAllTest
import net.minecraft.fluid.Fluids
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.fluids.FluidAttributes
import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.{MethodSource, ValueSource}

import scala.util.chaining._

object FluidAmountTest {
  def empties(): Array[FluidAmount] = fluidKeys().map(k => FluidAmount(k, 0L))

  def fluidKeys(): Array[FluidKey] = {
    val nbt = Option(new CompoundNBT().tap(_.putInt("b", 6)))
    Array(FluidKey.WATER, FluidKey.LAVA, FluidKey.EMPTY,
      FluidKey.WATER.copy(nbt = nbt), FluidKey.LAVA.copy(nbt = nbt), FluidKey.EMPTY.copy(nbt = nbt),
    )
  }

  def fluidKeys2(): Array[Object] = fluidKeys()
    .combinations(2)
    .map(_.toArray)
    .toArray

  object EmptyTest extends BeforeAllTest {
    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#empties"))
    def emptyTest(maybeEmpty: FluidAmount): Unit = {
      assertTrue(maybeEmpty.isEmpty)
    }

  }

  object EqualTest extends BeforeAllTest {
    @Test
    def emptyEqual(): Unit = {
      val a = FluidAmount(FluidKey.EMPTY, 0)
      val b = FluidAmount(FluidKey.EMPTY, 1000)
      assertNotEquals(a, b)
    }

    @Test
    def preDefined(): Unit = {
      assertNotEquals(FluidAmount.BUCKET_WATER, FluidAmount.BUCKET_LAVA)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys"))
    def equalTest1(key: FluidKey): Unit = {
      val a = FluidAmount(key, 5000L)
      val b = FluidAmount(key, 5000L)
      assertEquals(a, b)
      assertTrue(a === b)
      assertFalse(a =!= b)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys"))
    def equalTest2(key: FluidKey): Unit = {
      val a = FluidAmount(key, 1000L)
      val b = FluidAmount(key, 5000L)
      assertNotEquals(a, b)
      assertFalse(a === b)
      assertTrue(a =!= b)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys2"))
    def equalTest3(key1: FluidKey, key2: FluidKey): Unit = {
      val a = FluidAmount(key1, 1000L)
      val b = FluidAmount(key2, 1000L)
      assertTrue(a =!= b)
      assertNotEquals(a, b)
    }
  }

  object UtilTest extends BeforeAllTest {
    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys"))
    def withAmount1(key: FluidKey): Unit = {
      val a = FluidAmount(key, 1000L)
      val b = a.withAmount(2000)
      assertEquals(2000, b.amount)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys"))
    def withAmount2(key: FluidKey): Unit = {
      val a = FluidAmount(key, 1000L)
      val b = a.withAmount(1000)
      assertTrue(a eq b)
    }
  }

  object GroupTest extends BeforeAllTest {
    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys"))
    def adder1(key: FluidKey): Unit = {
      val a = FluidAmount(key, FluidAttributes.BUCKET_VOLUME)
      assertAll(
        () => assertEquals(FluidAttributes.BUCKET_VOLUME, a.amount),
        () => assertEquals(FluidAttributes.BUCKET_VOLUME * 2, (a |+| a).amount),
        () => assertEquals(FluidAttributes.BUCKET_VOLUME * 3, (a |+| a |+| a).amount),
        () => assertEquals(FluidAttributes.BUCKET_VOLUME, a.amount),
      )
    }

    @Test
    def adder2(): Unit = {
      val bucketWater = FluidAmount.BUCKET_WATER
      assertEquals(bucketWater.withAmount(2000), bucketWater |+| bucketWater)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#empties"))
    def addEmpty(empty: FluidAmount): Unit = {
      val executions: Seq[Executable] = for {
        f <- Seq(
          FluidAmount.BUCKET_WATER,
          FluidAmount.BUCKET_LAVA,
          FluidAmount.BUCKET_WATER.withAmount(500),
          FluidAmount.BUCKET_LAVA.withAmount(1500)
        )
        b <- Seq(true, false)
      }
      yield () => assertEquals(f, if (b) f |+| empty else empty |+| f)
      assertAll(executions: _*)
    }

    @Test
    def addEmptyFluid(): Unit = {
      assertAll(
        () => assertEquals(FluidAmount.BUCKET_WATER.withAmount(2000), FluidAmount.BUCKET_WATER |+| FluidAmount.EMPTY.withAmount(1000)),
        () => assertEquals(FluidAmount.BUCKET_LAVA.withAmount(2000), FluidAmount.BUCKET_LAVA |+| FluidAmount.EMPTY.withAmount(1000)),
        () => assertEquals(FluidAmount.EMPTY.withAmount(1000), FluidAmount.EMPTY |+| FluidAmount.EMPTY.withAmount(1000)),
        () => assertEquals(FluidAmount.EMPTY.withAmount(1500), FluidAmount.EMPTY.withAmount(500) |+| FluidAmount.EMPTY.withAmount(1000)),
        () => assertEquals(FluidAmount.BUCKET_WATER.withAmount(2000), FluidAmount.EMPTY.withAmount(1000) |+| FluidAmount.BUCKET_WATER),
      )
    }

    @ParameterizedTest
    @ValueSource(ints = Array(0, -500, -1000, -5000, -7000))
    def addEmpty2(amount: Int): Unit = {
      val result = FluidAmount.BUCKET_WATER.withAmount(amount) |+| FluidAmount.EMPTY.withAmount(5000)
      assertEquals(FluidAmount.BUCKET_WATER.withAmount(5000 + amount), result)
    }

    @ParameterizedTest
    @ValueSource(ints = Array(0, 500, 1000, 5000, 7000))
    def removeEmpty1(amount: Int): Unit = {
      val result = FluidAmount.BUCKET_WATER.withAmount(amount) |-| FluidAmount.EMPTY.withAmount(5000)
      assertEquals(FluidAmount.BUCKET_WATER.withAmount(amount - 5000), result)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys"))
    def combine1(key: FluidKey): Unit = {
      val a = FluidAmount(key, FluidAttributes.BUCKET_VOLUME)
      assertAll(
        () => assertEquals(FluidAttributes.BUCKET_VOLUME, a.amount),
        () => assertEquals(FluidAttributes.BUCKET_VOLUME * 2, a.combineN(2).amount),
        () => assertEquals(FluidAttributes.BUCKET_VOLUME * 3, a.combineN(3).amount),
        () => assertEquals(FluidAttributes.BUCKET_VOLUME, a.amount),
      )
    }

    @Test
    def addWaterAndLava(): Unit = {
      val wl = FluidAmount.BUCKET_WATER |+| FluidAmount.BUCKET_LAVA
      assertTrue(FluidAmount.BUCKET_WATER.withAmount(FluidAttributes.BUCKET_VOLUME * 2) === wl)
      assertEquals(Fluids.WATER, wl.fluidKey.fluid)
      assertTrue(FluidKey.WATER === wl.fluidKey)
    }

    @Test
    def addLavaAndWater(): Unit = {
      val wl = FluidAmount.BUCKET_LAVA |+| FluidAmount.BUCKET_WATER
      assertTrue(FluidAmount.BUCKET_LAVA.withAmount(FluidAttributes.BUCKET_VOLUME * 2) === wl)
      assertEquals(Fluids.LAVA, wl.fluidKey.fluid)
      assertTrue(FluidKey.LAVA === wl.fluidKey)
    }

    @Test
    def remove1(): Unit = {
      val a = FluidAmount.BUCKET_WATER * 2
      val a2 = FluidAmount.BUCKET_WATER * 3
      val b = FluidAmount.BUCKET_WATER
      assertAll(
        () => assertEquals(FluidAmount.BUCKET_WATER, a |-| b),
        () => assertEquals(FluidAmount.BUCKET_WATER.withAmount(2000), a2 |-| b),
      )
    }

    @Test
    def remove2(): Unit = {
      val a = FluidAmount.BUCKET_WATER * 2
      val b = FluidAmount.BUCKET_LAVA
      assertEquals(FluidAmount.BUCKET_WATER, a |-| b)
    }

    @Test
    def remove3(): Unit = {
      val a = FluidAmount.BUCKET_LAVA * 5
      val b = FluidAmount.BUCKET_WATER * 2
      assertEquals(FluidAmount.BUCKET_LAVA * 3, cats.Group[FluidAmount].remove(a, b))
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#empties"))
    def removeEmpty(empty: FluidAmount): Unit = {
      val executions: Seq[Executable] = for {
        f <- Seq(
          FluidAmount.BUCKET_WATER,
          FluidAmount.BUCKET_LAVA,
          FluidAmount.BUCKET_WATER.withAmount(500),
          FluidAmount.BUCKET_LAVA.withAmount(1500)
        )
      }
      yield () => assertEquals(f, f |-| empty)
      assertAll(executions: _*)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.FluidAmountTest#fluidKeys"))
    def removeItself(key: FluidKey): Unit = {
      val a = FluidAmount(key, 1000)
      val subtracted = a |-| a
      assertTrue(subtracted.isEmpty)
      assertEquals(key, subtracted.fluidKey)
      assertEquals(0, subtracted.amount)
    }

    @Test
    def removeAndPlus(): Unit = {
      val a = FluidAmount.BUCKET_LAVA * 5
      val b = FluidAmount.BUCKET_WATER * 2
      assertEquals(b, a |-| a |+| b)
    }
  }
}
