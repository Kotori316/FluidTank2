package com.kotori316.fluidtank.contents

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

import scala.jdk.javaapi.CollectionConverters

class TankOperationsTest {
  def createTank(contentString: String, amount: Long, capacity: Long): Tank[String] = {
    Tank(GenericAmount(contentString, GenericUnit(amount), None), GenericUnit(capacity))
  }

  @Nested
  class FillTest {

    @Test
    def fillToEmpty(): Unit = {
      val tank = createTank("", 0, 1000)
      val op: Operations.TankOperation[String] = Operations.fillOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertEquals(createTank("a", 500, 1000), result)
      assertTrue(rest.isEmpty)
    }

    @Test
    def fillOriginalUnchanged(): Unit = {
      val tank = createTank("", 0, 1000)
      assertTrue(tank.isEmpty)

      val op: Operations.TankOperation[String] = Operations.fillOp(tank)
      op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))

      assertTrue(tank.isEmpty)
    }

    @Test
    def fillToFilledSucceed(): Unit = {
      val tank = createTank("a", 200, 1000)
      val op = Operations.fillOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertEquals(createTank("a", 700, 1000), result)
      assertTrue(rest.isEmpty)
    }

    @Test
    def fillToFilledSucceed2(): Unit = {
      val tank = createTank("a", 200, 1000)
      val op = Operations.fillOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(1000), None))
      assertEquals(createTank("a", 1000, 1000), result)
      assertEquals(GenericAmount("a", GenericUnit(200), None), rest)
    }

    @Test
    def fillToFilledFail(): Unit = {
      val tank = createTank("a", 200, 1000)
      val op = Operations.fillOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("b", GenericUnit(500), None))
      assertEquals(createTank("a", 200, 1000), result)
      assertTrue(rest.nonEmpty)
      assertEquals(GenericAmount("b", GenericUnit(500), None), rest)
    }

    @Test
    def fillToFilledFail2(): Unit = {
      val tank = createTank("a", 1000, 1000)
      val op = Operations.fillOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertEquals(createTank("a", 1000, 1000), result)
      assertTrue(rest.nonEmpty)
      assertEquals(GenericAmount("a", GenericUnit(500), None), rest)
    }

    @Test
    def fillEmpty(): Unit = {
      val tank = createTank("a", 200, 1000)
      val op = Operations.fillOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("", GenericUnit(500), None))
      assertEquals(createTank("a", 200, 1000), result)
      assertEquals(GenericUnit(500), rest.amount)
    }

    @Test
    def fillToEmpty2(): Unit = {
      val tank = createTank("", 200, 1000)
      val op = Operations.fillOp(tank)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertEquals(createTank("a", 700, 1000), result)
      assertTrue(rest.isEmpty)
    }

    @Test
    def fillToEmpty3(): Unit = {
      val tank = createTank("b", 0, 1000)
      val op = Operations.fillOp(tank)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertEquals(createTank("a", 500, 1000), result)
      assertTrue(rest.isEmpty)
    }

    @Test
    def fillVoid(): Unit = {
      val tank = createTank("a", 200, 1000)
      val op = Operations.fillVoidOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertTrue(rest.isEmpty)
      assertEquals(tank, result)
    }
  }

  @Nested
  class DrainTest {
    @TestFactory
    def drainFromEmptyTank1(): java.util.List[DynamicNode] = {
      val tankAmount = Seq(0, 1000)
      val fluids = Seq(
        GenericAmount("a", GenericUnit(500), None),
        GenericAmount("b", GenericUnit(500), None),
        GenericAmount("", GenericUnit(500), None),
        GenericAmount("a", GenericUnit(0), None),
        GenericAmount("", GenericUnit(0), None),
      )
      val tests: Seq[DynamicNode] = for {
        amount <- tankAmount
        a <- fluids
      } yield {
        DynamicTest.dynamicTest(s"$amount $a", () => {
          val tank = createTank("", amount, 1000)
          val op = Operations.drainOp(tank)
          val (_, rest, result) = op.run(DefaultTransferEnv, a)
          assertEquals(a, rest)
          assertEquals(tank, result)
        })
      }

      CollectionConverters.asJava(tests)
    }

    @TestFactory
    def drainFromEmptyTank2(): java.util.List[DynamicNode] = {
      val fluids = Seq(
        GenericAmount("a", GenericUnit(500), None),
        GenericAmount("b", GenericUnit(500), None),
        GenericAmount("", GenericUnit(500), None),
        GenericAmount("a", GenericUnit(0), None),
        GenericAmount("", GenericUnit(0), None),
      )
      val tankContent = Seq("a", "b")
      val tests: Seq[DynamicNode] = for {
        content <- tankContent
        a <- fluids
      } yield {
        DynamicTest.dynamicTest(s"$content $a", () => {
          val tank = createTank(content, 0, 1000)
          val op = Operations.drainOp(tank)
          val (_, rest, result) = op.run(DefaultTransferEnv, a)
          assertEquals(a, rest)
          assertEquals(tank, result)
        })
      }
      CollectionConverters.asJava(tests)
    }

    @Test
    def drainFromEmptyTank3(): Unit = {
      val tank = createTank("", 500, 1000)
      val op = Operations.drainOp(tank)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("content", GenericUnit(200), None))
      assertEquals(GenericAmount("content", GenericUnit(200), None), rest)
      assertEquals(tank, result)
    }

    @Test
    def drain1(): Unit = {
      val tank = createTank("content", 500, 1000)
      val op = Operations.drainOp(tank)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("content", GenericUnit(200), None))
      assertTrue(rest.isEmpty)
      assertEquals(createTank("content", 300, 1000), result, "Expect 300 tank")
    }

    @Test
    def drain2(): Unit = {
      val tank = createTank("content", 500, 1000)
      val op = Operations.drainOp(tank)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("content", GenericUnit(500), None))
      assertTrue(rest.isEmpty)
      assertTrue(result.isEmpty)
      assertEquals(createTank("content", 0, 1000), result, "Expect empty tank")
    }

    @Test
    def drain3(): Unit = {
      val tank = createTank("content", 500, 1000)
      val op = Operations.drainOp(tank)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("content", GenericUnit(800), None))
      assertAll(
        () => assertTrue(rest.nonEmpty),
        () => assertEquals(GenericAmount("content", GenericUnit(300), None), rest),
        () => assertTrue(result.isEmpty),
        () => assertEquals(createTank("content", 0, 1000), result, "Expect empty tank"),
      )
    }

    @TestFactory
    def drainFail(): java.util.List[DynamicNode] = {
      val tank = createTank("content", 500, 1000)
      val op = Operations.drainOp(tank)
      val fluids = Seq(
        GenericAmount("b", GenericUnit(500), None),
        GenericAmount("", GenericUnit(500), None),
      )
      CollectionConverters.asJava(fluids.map(a => DynamicTest.dynamicTest(s"$a", () => {
        val (_, rest, result) = op.run(DefaultTransferEnv, a)
        assertEquals(a, rest)
        assertEquals(tank, result)
      })))
    }
  }

  @Nested
  class CreativeTest {
    @Test
    def fillToEmpty(): Unit = {
      val tank = createTank("", 0, 1000)
      val op = Operations.fillCreativeOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertTrue(rest.isEmpty)
      assertEquals(createTank("a", 1000, 1000), result)
    }

    @Test
    def fillToSameContent(): Unit = {
      val tank = createTank("a", 1, 1000)
      val op = Operations.fillCreativeOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertTrue(rest.isEmpty)
      assertEquals(createTank("a", 1000, 1000), result)
    }

    @Test
    def fillToDifferentContent(): Unit = {
      val tank = createTank("b", 1, 1000)
      val op = Operations.fillCreativeOp(tank)
      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertEquals(GenericAmount("a", GenericUnit(500), None), rest)
      assertEquals(tank, result)
    }

    @Test
    def drainFromEmpty(): Unit = {
      val tank = createTank("", 0, 1000)
      val op = Operations.drainCreativeOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertEquals(GenericAmount("a", GenericUnit(500), None), rest)
      assertEquals(tank, result)
    }

    @Test
    def drainFromSameContent(): Unit = {
      val tank = createTank("a", 1, 1000)
      val op = Operations.drainCreativeOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertTrue(rest.isEmpty)
      assertEquals(tank, result)
    }

    @Test
    def drainFromDifferentContent(): Unit = {
      val tank = createTank("b", 1, 1000)
      val op = Operations.drainCreativeOp(tank)

      val (_, rest, result) = op.run(DefaultTransferEnv, GenericAmount("a", GenericUnit(500), None))
      assertEquals(GenericAmount("a", GenericUnit(500), None), rest)
      assertEquals(tank, result)
    }
  }

  @Nested
  class LogTest {
    @Test
    def fillFluid(): Unit = {
      val tank = createTank("a", 100, 1000)
      val op = tank.fillOp
      val fill = GenericAmount("a", GenericUnit(500), None)
      val (log, rest, newTank) = op.run(DefaultTransferEnv, fill)
      assertTrue(rest.isEmpty)

      log.headOption match {
        case Some(FluidTransferLog.FillFluid(toFill, filled, before, after)) =>
          assertAll(
            () => assertEquals(fill, toFill),
            () => assertEquals(fill, filled),
            () => assertEquals(tank, before),
            () => assertEquals(newTank, after),
          )
        case _ => fail(s"Expect FluidTransferLog.FillFluid but $log")
      }
    }

    @Test
    def fillFailed(): Unit = {
      val tank = createTank("a", 100, 1000)
      val op = tank.fillOp
      val fill = GenericAmount("b", GenericUnit(500), None)
      val (log, rest, newTank) = op.run(DefaultTransferEnv, fill)
      assertTrue(rest.nonEmpty)

      log.headOption match {
        case Some(FluidTransferLog.FillFailed(f, t)) =>
          assertAll(
            () => assertEquals(fill, rest),
            () => assertEquals(fill, f),
            () => assertEquals(tank, newTank),
            () => assertEquals(tank, t),
          )
        case _ => fail(s"Expect FluidTransferLog.FillFailed but $log")
      }
    }

    @Test
    def drainFluid(): Unit = {
      val tank = createTank("a", 100, 1000)
      val op = tank.drainOp
      val drain = GenericAmount("a", GenericUnit(500), None)
      val (log, rest, newTank) = op.run(DefaultTransferEnv, drain)
      assertTrue(rest.nonEmpty)

      log.headOption match {
        case Some(FluidTransferLog.DrainFluid(toDrain, drained, before, after)) =>
          assertAll(
            () => assertEquals(drain, toDrain),
            () => assertEquals(tank.content, drained),
            () => assertEquals(tank, before),
            () => assertEquals(newTank, after),
            () => assertEquals(GenericAmount("a", GenericUnit(400), None), rest)
          )
        case _ => fail(s"Expect FluidTransferLog.DrainFluid but $log")
      }
    }

    @Test
    def drainFailed(): Unit = {
      val tank = createTank("a", 100, 1000)
      val op = tank.drainOp
      val drain = GenericAmount("b", GenericUnit(500), None)
      val (log, rest, newTank) = op.run(DefaultTransferEnv, drain)
      assertTrue(rest.nonEmpty)

      log.headOption match {
        case Some(FluidTransferLog.DrainFailed(f, t)) =>
          assertAll(
            () => assertEquals(drain, rest),
            () => assertEquals(drain, f),
            () => assertEquals(tank, newTank),
            () => assertEquals(tank, t),
          )
        case _ => fail(s"Expect FluidTransferLog.DrainFailed but $log")
      }
    }
  }
}
