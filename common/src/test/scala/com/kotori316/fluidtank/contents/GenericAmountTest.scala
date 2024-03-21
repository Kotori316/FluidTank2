package com.kotori316.fluidtank.contents

import com.kotori316.fluidtank.BeforeMC
import net.minecraft.core.component.{DataComponentPatch, DataComponents}
import net.minecraft.world.item.DyeColor
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.{assertEquals, assertNotEquals, assertNotNull, assertTrue}

import scala.language.implicitConversions

class GenericAmountTest extends BeforeMC {

  implicit def int2GenericUnit(a: Int): GenericUnit = GenericUnit.fromForge(a)

  @Nested
  class BasicTest {
    @Test
    def createInstance(): Unit = {
      val amount = GenericAmount("", 0, None)
      assertNotNull(amount)
    }

    @Test
    def add1(): Unit = {
      val a = GenericAmount("a", 1, None)
      assertEquals(GenericAmount("a", 2, None), a + a)
    }

    @Test
    def add2(): Unit = {
      val a = GenericAmount("a", 1, None)
      assertEquals(GenericAmount("a", 3, None), a + a + a)
    }

    @Test
    def addNotSame(): Unit = {
      val a = GenericAmount("a", 1, None)
      val b = GenericAmount("b", 100, None)
      assertEquals(GenericAmount("a", 101, None), a + b)
      assertEquals(GenericAmount("b", 101, None), b + a)
    }

    @Test
    def minus1(): Unit = {
      val a = GenericAmount("a", 1, None)
      val subtract = a - a
      assertTrue(subtract.isEmpty)
      assertEquals("a", subtract.content)
    }

    @Test
    def notEqual1(): Unit = {
      val a = GenericAmount("a", 1, None)
      val b = GenericAmount("b", 1, None)
      assertNotEquals(a, b)
      assertNotEquals(a.##, b.##)
    }

    @Test
    def notEqual2(): Unit = {
      val a = GenericAmount("a", 1, None)
      val b = GenericAmount("a", 2, None)
      assertNotEquals(a, b)
      assertNotEquals(a.##, b.##)
    }

    @Test
    def notEqual3(): Unit = {
      val a = GenericAmount("a", 1, None)
      val b = GenericAmount("a", 1, Option(DataComponentPatch.EMPTY))
      assertNotEquals(a, b)
      assertNotEquals(a.##, b.##)
    }

    @Test
    def equal(): Unit = {
      val a1 = GenericAmount("a", 1, None)
      val a2 = GenericAmount("a", 1, None)
      assertEquals(a1.##, a2.##)
      assertEquals(a1, a2)
    }
  }

  @Nested
  class CycleTest {
    @TestFactory
    def cycleTest(): Array[DynamicNode] = {
      val amounts = Seq(
        GenericAmount("", 0, None),
        GenericAmount("", 100, None),
        GenericAmount("a", 1, None),
        GenericAmount("a", 3, None),
        GenericAmount("b", 1, None),
        GenericAmount("b", 1, Option(DataComponentPatch.EMPTY)),
        GenericAmount("tag", 1, Option(DataComponentPatch.builder().set(DataComponents.BASE_COLOR, DyeColor.BLACK).build())),
      )

      amounts.map(f => DynamicTest.dynamicTest(s"cycle $f", () => {
        val tag = f.getTag
        val reconstructed = implicitly[GenericAccess[String]].read(tag)
        assertEquals(f, reconstructed)
      })).toArray
    }
  }

  @Nested
  class EmptyTest {
    @Test
    def contentEmpty(): Unit = {
      val e = GenericAmount("", 100, None)
      assertTrue(e.isEmpty)
    }

    @Test
    def addEmpty(): Unit = {
      val a = GenericAmount("a", 1, None)
      val e = GenericAmount("", 100, None)
      assertEquals(GenericAmount("a", 101, None), a + e)
      assertEquals(GenericAmount("a", 101, None), e + a)
    }

    @Test
    def minusEmpty(): Unit = {
      val a = GenericAmount("a", 1000, None)
      val e = GenericAmount("", 100, None)
      assertEquals(GenericAmount("a", 900, None), a - e)
      assertEquals(GenericAmount("a", -900, None), e - a)
    }
  }
}
