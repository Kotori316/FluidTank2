package com.kotori316.fluidtank.contents

import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.{DynamicNode, DynamicTest, Nested, Test, TestFactory}

import scala.jdk.javaapi.CollectionConverters

class ListTankOperationsTest {

  @Nested
  class FillTest {
    @Test
    def fillToEmpty1(): Unit = {
      val tanks = createTanks(("", 0, 1000), ("", 0, 1000))
      val op = Operations.fillList(tanks)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("content", GenericUnit(1500), None))
      assertTrue(rest.isEmpty)
      val expected = createTanks(("content", 1000, 1000), ("content", 500, 1000))
      assertEquals(expected, result)
    }

    @Test
    def fillToEmpty2(): Unit = {
      val tanks = createTanks(("", 0, 1000), ("", 0, 1000))
      val op = Operations.fillList(tanks)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("content", GenericUnit(2500), None))
      assertTrue(rest.nonEmpty)
      assertEquals(GenericAmount("content", GenericUnit(500), None), rest)
      val expected = createTanks(("content", 1000, 1000), ("content", 1000, 1000))
      assertEquals(expected, result)
    }

    @Test
    def fillToEmpty3(): Unit = {
      val tanks = createTanks(("a", 0, 1000), ("b", 0, 1000))
      val op = Operations.fillList(tanks)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("content", GenericUnit(1500), None))
      assertTrue(rest.isEmpty)
      val expected = createTanks(("content", 1000, 1000), ("content", 500, 1000))
      assertEquals(expected, result)
    }

    @TestFactory
    def fillFail(): java.util.List[DynamicNode] = {
      val tanks = createTanks(("", 0, 1000), ("", 0, 1000))
      val op = Operations.fillList(tanks)
      val fluids = Seq(
        GenericAmount("", GenericUnit(0), None),
        GenericAmount("", GenericUnit(1000), None),
        GenericAmount("a", GenericUnit(0), None),
      )
      CollectionConverters.asJava(fluids.map(a => DynamicTest.dynamicTest(a.toString, () => {
        val (_, rest, result) = op.run(DefaultTransferEnv, a)
        assertEquals(a, rest)
        assertEquals(tanks, result)
      })))
    }

    @Test
    def fillFail2(): Unit = {
      val tanks = createTanks(("a", 100, 1000), ("b", 100, 1000))
      val op = Operations.fillList(tanks)
      val toFill = GenericAmount("content", GenericUnit(2500), None)
      val (_, rest, result) = op.run(DefaultTransferEnv, toFill)
      assertEquals(toFill, rest)
      assertEquals(tanks, result)
    }

    @Test
    def fillFilled1(): Unit = {
      val tanks = createTanks(("a", 100, 1000), ("b", 200, 1000))
      val op = Operations.fillList(tanks)
      val toFill = GenericAmount("a", GenericUnit(200), None)
      val (_, rest, result) = op.run(DefaultTransferEnv, toFill)
      assertTrue(rest.isEmpty)
      assertEquals(createTanks(("a", 300, 1000), ("b", 200, 1000)), result)
    }

    @Test
    def fillFilled2(): Unit = {
      val tanks = createTanks(("a", 100, 1000), ("b", 200, 1000))
      val op = Operations.fillList(tanks)
      val toFill = GenericAmount("b", GenericUnit(200), None)
      val (_, rest, result) = op.run(DefaultTransferEnv, toFill)
      assertTrue(rest.isEmpty)
      assertEquals(createTanks(("a", 100, 1000), ("b", 400, 1000)), result)
    }
  }

  @Nested
  class DrainTest {
    @Test
    def drain1(): Unit = {
      val tanks = createTanks(("a", 200, 1000), ("a", 1000, 1000))
      val op = Operations.drainList(tanks)
      locally {
        val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(100), None))
        assertTrue(rest.isEmpty)
        assertEquals(createTanks(("a", 100, 1000), ("a", 1000, 1000)), result)
      }
      locally {
        val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
        assertTrue(rest.isEmpty)
        assertEquals(createTanks(("a", 0, 1000), ("a", 700, 1000)), result)
      }
    }

    @Test
    def drain2(): Unit = {
      val tanks = createTanks(("a", 200, 1000), ("a", 1000, 1000))
      val op = Operations.drainList(tanks)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(1500), None))
      assertEquals(GenericAmount("a", GenericUnit(300), None), rest)
      assertEquals(createTanks(("a", 0, 1000), ("a", 0, 1000)), result)
    }

    @Test
    def drain3(): Unit = {
      val tanks = createTanks(("a", 500, 1000), ("b", 1000, 1000))
      val op = Operations.drainList(tanks)
      locally {
        val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(100), None))
        assertTrue(rest.isEmpty)
        assertEquals(createTanks(("a", 400, 1000), ("b", 1000, 1000)), result)
      }
      locally {
        val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("b", GenericUnit(100), None))
        assertTrue(rest.isEmpty)
        assertEquals(createTanks(("a", 500, 1000), ("b", 900, 1000)), result)
      }
      locally {
        val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(1500), None))
        assertEquals(GenericAmount("a", GenericUnit(1000), None), rest)
        assertEquals(createTanks(("a", 0, 1000), ("b", 1000, 1000)), result)
      }
      locally {
        val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("b", GenericUnit(1500), None))
        assertEquals(GenericAmount("b", GenericUnit(500), None), rest)
        assertEquals(createTanks(("a", 500, 1000), ("b", 0, 1000)), result)
      }
    }

    @TestFactory
    def drainEmpty(): java.util.List[DynamicNode] = {
      val tanks = createTanks(("a", 500, 1000), ("b", 1000, 1000))
      val op = Operations.drainList(tanks)
      val drainFluids = Seq(
        GenericAmount("a", GenericUnit(0), None),
        GenericAmount("b", GenericUnit(0), None),
        GenericAmount("", GenericUnit(0), None),
        GenericAmount("", GenericUnit(1000), None),
      )
      CollectionConverters.asJava(drainFluids.map(f => DynamicTest.dynamicTest(f"Drain $f", () => {
        val (_, rest, result) = op.run(DefaultTransferEnv, f)
        assertEquals(f, rest)
        assertEquals(tanks, result)
      })))
    }
  }
}
