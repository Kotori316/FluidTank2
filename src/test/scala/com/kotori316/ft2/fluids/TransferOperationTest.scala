package com.kotori316.ft2.fluids

import cats.syntax.group._
import com.kotori316.ft2.BeforeAllTest
import com.kotori316.ft2.fluids.Operations._
import org.junit.jupiter.api.Assertions.{assertAll, assertEquals, assertFalse, assertTrue}
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import scala.jdk.CollectionConverters._

//noinspection DuplicatedCode It's test!
object TransferOperationTest {
  private[this] final val waterTank = Tank(FluidAmount.BUCKET_WATER.withAmount(0), 16000)
  private[this] final val lavaTank = Tank(FluidAmount.BUCKET_LAVA.withAmount(4000), 16000)

  def normalFluids(): Array[FluidAmount] = Array(FluidAmount.BUCKET_WATER, FluidAmount.BUCKET_LAVA)

  object Fill extends BeforeAllTest {
    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.TransferOperationTest#normalFluids"))
    def fillToEmpty1(fa: FluidAmount): Unit = {
      val fillAction = fillOp(waterTank)
      val x1: Seq[Executable] = {
        val (log, left, tank) = fillAction.run((), fa.withAmount(10000))
        Seq(
          () => assertTrue(log.forall(_.isInstanceOf[FluidTransferLog.FillFluid])),
          () => assertTrue(left.isEmpty),
          () => assertEquals(Tank(fa.withAmount(10000), 16000), tank),
        )
      }
      assertAll(x1: _*)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.TransferOperationTest#normalFluids"))
    def fill2(fa: FluidAmount): Unit = {
      val emptyTank = Tank(FluidAmount.EMPTY, 16000)
      val fillAction = for {
        a <- fillOp(emptyTank)
        b <- fillOp(emptyTank)
      } yield (a, b)

      val (_, left, (a, b)) = fillAction.run((), fa.withAmount(20000))
      assertTrue(left.isEmpty)
      assertEquals(fa.withAmount(16000), a.fluidAmount)
      assertEquals(fa.withAmount(4000), b.fluidAmount)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.TransferOperationTest#normalFluids"))
    def fillListAllSuccess(fa: FluidAmount): Unit = {
      val tanks = List(Tank(FluidAmount.BUCKET_WATER.withAmount(0), 16000), Tank.EMPTY, Tank(FluidAmount.BUCKET_LAVA.withAmount(0), 16000))
      val fillOperation = fillList(tanks)

      val (_, left, a :: b :: c :: Nil) = fillOperation.run((), fa)
      assertAll(
        () => assertTrue(left.isEmpty, s"Left isEmpty, $left"),
        () => assertEquals(Tank(fa.withAmount(1000), 16000), a),
        () => assertEquals(Tank.EMPTY, b),
        () => assertEquals(Tank(FluidAmount.BUCKET_LAVA.withAmount(0), 16000), c),
      )
    }

    @Test
    def fillListAllSuccess2(): Unit = {
      val tanks = List(Tank(FluidAmount.BUCKET_WATER.withAmount(1000), 16000), Tank.EMPTY, Tank(FluidAmount.BUCKET_LAVA.withAmount(1000), 16000))
      val fillOperation = fillList(tanks)
      locally {
        val (_, left, a :: b :: c :: Nil) = fillOperation.run((), FluidAmount.BUCKET_WATER)
        assertAll(
          () => assertTrue(left.isEmpty, s"Left isEmpty, $left"),
          () => assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(2000), 16000), a),
          () => assertEquals(Tank.EMPTY, b, "Second tank isn't touched."),
          () => assertEquals(Tank(FluidAmount.BUCKET_LAVA.withAmount(1000), 16000), c),
        )
      }
      locally {
        val (_, left, a :: b :: c :: Nil) = fillOperation.run((), FluidAmount.BUCKET_LAVA)
        assertAll(
          () => assertTrue(left.isEmpty, s"Left isEmpty, $left"),
          () => assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(1000), 16000), a),
          () => assertTrue(b.isEmpty, "Second tank was tried to fill lava."),
          () => assertEquals(Tank(FluidAmount.BUCKET_LAVA.withAmount(2000), 16000), c),
        )
      }
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.TransferOperationTest#normalFluids"))
    def fillListWaterOnly(fa: FluidAmount): Unit = {
      val tanks = List(Tank(FluidAmount.BUCKET_WATER.withAmount(0), 16000), Tank.EMPTY)
      val fillOperation = fillList(tanks)

      val (_, left, a :: b :: Nil) = fillOperation.run((), fa)
      assertAll(
        () => assertTrue(left.isEmpty, s"Left isEmpty, $left"),
        () => assertEquals(Tank(fa, 16000), a),
        () => assertEquals(Tank.EMPTY, b),
      )
    }

    @Test
    def fillListWaterOnly2(): Unit = {
      val tanks = List(Tank(FluidAmount.BUCKET_WATER, 16000), Tank.EMPTY)
      val fillOperation = fillList(tanks)
      locally {
        val (_, left, a :: b :: Nil) = fillOperation.run((), FluidAmount.BUCKET_WATER)
        assertAll(
          () => assertTrue(left.isEmpty, s"Left isEmpty, $left"),
          () => assertEquals(FluidKey.WATER, left.fluidKey),
          () => assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(2000), 16000), a),
          () => assertEquals(Tank.EMPTY, b),
        )
      }
      locally {
        val (_, left, a :: b :: Nil) = fillOperation.run((), FluidAmount.BUCKET_LAVA)
        assertAll(
          () => assertEquals(FluidAmount.BUCKET_LAVA, left),
          () => assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(1000), 16000), a),
          () => assertTrue(b.isEmpty),
        )
      }
    }

  }

  object FillAll extends BeforeAllTest {

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.TransferOperationTest#normalFluids"))
    def fillAll1(fluidAmount: FluidAmount): Unit = {
      val fillAction = fillAll(List(waterTank, waterTank.copy(capacity = 32000)))
      val (_, left, a :: b :: Nil) = fillAction.run((), fluidAmount.withAmount(1))
      assertAll(
        () => assertTrue(left.isEmpty),
        () => assertEquals(fluidAmount.withAmount(0), left),
        () => assertEquals(waterTank.copy(fluidAmount = fluidAmount.withAmount(waterTank.capacity)), a),
        () => assertEquals(waterTank.copy(fluidAmount = fluidAmount.withAmount(32000), capacity = 32000), b),
        () => assertEquals(Tank(fluidAmount.withAmount(16000), 16000), a),
        () => assertEquals(Tank(fluidAmount.withAmount(32000), 32000), b),
      )
    }

    @Test
    def fillAll3(): Unit = {
      val fillAction = fillAll(List(lavaTank, lavaTank.copy(capacity = 32000)))
      val (_, left, List(a, b)) = fillAction.run((), FluidAmount.BUCKET_WATER.withAmount(1))
      assertAll(
        () => assertEquals(FluidAmount.BUCKET_WATER.withAmount(1), left),
        () => assertEquals(lavaTank, a),
        () => assertEquals(lavaTank.copy(capacity = 32000), b),
      )
    }

  }

  object Drain extends BeforeAllTest {

    @Test
    def drain1(): Unit = {
      val drainAction = drainOp(lavaTank)
      val (log, left, tank) = drainAction.run((), FluidAmount.BUCKET_WATER.withAmount(10000))
      val x1: Seq[Executable] = Seq(
        () => assertTrue(log.forall(_.isInstanceOf[FluidTransferLog.DrainFailed])),
        () => assertEquals(FluidAmount.BUCKET_WATER.withAmount(10000), left),
        () => assertEquals(lavaTank, tank),
      )
      assertAll(x1: _*)
    }

    @Test
    def drain2(): Unit = {
      val drainAction = drainOp(lavaTank)
      val toDrain = FluidAmount.BUCKET_LAVA.withAmount(10000)
      val (log, left, tank) = drainAction.run((), toDrain)
      val drained = toDrain |-| left
      val x2: Seq[Executable] = Seq(
        () => assertTrue(log.forall(_.isInstanceOf[FluidTransferLog.DrainFluid])),
        () => assertEquals(FluidAmount.BUCKET_LAVA.withAmount(6000), left),
        () => assertEquals(Tank(FluidAmount.BUCKET_LAVA.withAmount(0), 16000), tank),
        () => assertEquals(FluidAmount.BUCKET_LAVA * 4, drained)
      )
      assertAll(x2: _*)
    }

    @ParameterizedTest
    @MethodSource(Array("com.kotori316.ft2.fluids.TransferOperationTest#normalFluids"))
    def drainFromEmptyTank(fa: FluidAmount): Unit = {
      val drainAction = drainOp(Tank.EMPTY)
      val (log, left, tank) = drainAction.run((), fa.withAmount(10000))
      val e1: Seq[Executable] = Seq(
        () => assertTrue(log.forall(_.isInstanceOf[FluidTransferLog.DrainFailed]), s"Log=$log"),
        () => assertEquals(fa.withAmount(10000), left),
        () => assertEquals(Tank.EMPTY, tank),
      )
      assertAll(e1: _*)
    }

    @Test
    def drainFromEmptyWaterTank(): Unit = {
      val drainAction = drainOp(waterTank)
      val e1: Seq[Executable] = {
        val (log, left, tank) = drainAction.run((), FluidAmount.BUCKET_WATER.withAmount(10000))
        Seq(
          () => assertTrue(log.forall(_.isInstanceOf[FluidTransferLog.DrainFailed]), s"Log=$log"),
          () => assertEquals(FluidAmount.BUCKET_WATER.withAmount(10000), left),
          () => assertEquals(waterTank, tank),
        )
      }
      val e2: Seq[Executable] = {
        val (log, left, tank) = drainAction.run((), FluidAmount.BUCKET_LAVA.withAmount(10000))
        Seq(
          () => assertTrue(log.forall(_.isInstanceOf[FluidTransferLog.DrainFailed]), s"Log=$log"),
          () => assertEquals(FluidAmount.BUCKET_LAVA.withAmount(10000), left),
          () => assertEquals(waterTank, tank),
        )
      }
      assertAll((e1 ++ e2).asJava)
    }

    @Test
    def drainAll1(): Unit = {
      val tanks = List(Tank(FluidAmount.BUCKET_WATER.withAmount(1000), 16000), Tank.EMPTY, Tank(FluidAmount.BUCKET_LAVA.withAmount(1000), 16000))
      val drainOp = drainList(tanks)
      val x1: Seq[Executable] = {
        val (_, left, a :: b :: c :: Nil) = drainOp.run((), FluidAmount.EMPTY.withAmount(1000))
        Seq(
          () => assertTrue(left.isEmpty, s"Left: $left"),
          () => assertEquals(FluidAmount.BUCKET_WATER.withAmount(0), left, s"Left: $left"),
          () => assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(0), 16000), a),
          () => assertEquals(Tank.EMPTY, b),
          () => assertEquals(Tank(FluidAmount.BUCKET_LAVA.withAmount(1000), 16000), c),
        )
      }
      val x2: Seq[Executable] = {
        val (_, left, a :: b :: c :: Nil) = drainOp.run((), FluidAmount.BUCKET_WATER.withAmount(1000))
        Seq(
          () => assertTrue(left.isEmpty, s"Left: $left"),
          () => assertEquals(FluidAmount.BUCKET_WATER.withAmount(0), left, s"Left: $left"),
          () => assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(0), 16000), a),
          () => assertEquals(Tank.EMPTY, b),
          () => assertEquals(Tank(FluidAmount.BUCKET_LAVA.withAmount(1000), 16000), c),
        )
      }
      val x3: Seq[Executable] = {
        val (_, left, a :: b :: c :: Nil) = drainOp.run((), FluidAmount.BUCKET_LAVA.withAmount(1000))
        Seq(
          () => assertTrue(left.isEmpty, s"Left: $left"),
          () => assertEquals(FluidAmount.BUCKET_LAVA.withAmount(0), left, s"Left: $left"),
          () => assertEquals(Tank(FluidAmount.BUCKET_WATER.withAmount(1000), 16000), a),
          () => assertEquals(Tank.EMPTY, b),
          () => assertEquals(Tank(FluidAmount.BUCKET_LAVA.withAmount(0), 16000), c),
        )
      }
      assertAll((x1 ++ x2 ++ x3).asJava)
    }

  }

  object Util extends BeforeAllTest {
    @Test
    def tankIsEmpty(): Unit = {
      assertAll(
        () => assertTrue(waterTank.isEmpty),
        () => assertFalse(lavaTank.isEmpty),
        () => assertTrue(Tank.EMPTY.isEmpty),
        () => assertFalse(Tank(FluidAmount.BUCKET_WATER, 2000L).isEmpty),
      )
    }
  }

}
