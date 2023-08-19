package com.kotori316.fluidtank.fabric.cat

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fabric.BeforeMC
import com.kotori316.fluidtank.fabric.fluid.FabricConverter
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil}
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.{ItemStack, Items}
import net.minecraft.world.level.material.Fluids
import org.junit.jupiter.api.Assertions.{assertEquals, assertTrue}
import org.junit.jupiter.api.{DynamicContainer, DynamicNode, DynamicTest, TestFactory}

import scala.jdk.javaapi.CollectionConverters
import scala.util.Using
import scala.util.chaining.scalaUtilChainingOps

//noinspection UnstableApiUsage
final class ChestAsTankStorageTest extends BeforeMC {

  @TestFactory
  def generator(): java.util.List[DynamicNode] = CollectionConverters.asJava(for {
    (f, i) <- Seq(
      (FluidAmountUtil.BUCKET_WATER, Items.WATER_BUCKET),
      (FluidAmountUtil.BUCKET_LAVA, Items.LAVA_BUCKET),
    )
    amount <- 1000 to 10000 by 500
    fillCount = amount / 1000
    ff = f.setAmount(GenericUnit.fromForge(amount))
    stack = new ItemStack(i, fillCount)
  } yield DynamicContainer.dynamicContainer(s"$ff", CollectionConverters.asJava(Seq(
    DynamicTest.dynamicTest("fillToBucket", () => fillToBucket(ff, stack.copy())),
    DynamicTest.dynamicTest("drainFromBucket1", () => drainFromBucket1(ff, stack.copy())),
    DynamicTest.dynamicTest("fillStackedBucket", () => fillStackedBucket(ff, stack.copy())),
    DynamicTest.dynamicTest("drainStackedBucket1", () => drainStackedBucket1(ff, stack.copy())),
  ))))

  private def fillToBucket(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(Seq.fill(10)(new ItemStack(Items.BUCKET)) *)
    val handler = new ChestAsTankStorage(InventoryStorage.of(items, null))

    Using(Transaction.openOuter()) { tr =>
      val filled = handler.insert(
        FabricConverter.toVariant(fluid, Fluids.EMPTY),
        FabricConverter.fabricAmount(fluid),
        tr,
      )
      assertEquals(filledItem.getCount * FluidConstants.BUCKET, filled)
      tr.commit()
    }
    assertEquals(filledItem.getCount, items.countItem(filledItem.getItem))
  }

  private def drainFromBucket1(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(Seq.fill(10)(filledItem.copy().tap(_.setCount(1))) *)
    val handler = new ChestAsTankStorage(InventoryStorage.of(items, null))

    Using(Transaction.openOuter()) { tr =>
      val drained = handler.extract(
        FabricConverter.toVariant(fluid, Fluids.EMPTY),
        FabricConverter.fabricAmount(fluid),
        tr,
      )
      assertEquals(filledItem.getCount * FluidConstants.BUCKET, drained)
      tr.commit()
    }

    assertEquals(filledItem.getCount, items.countItem(Items.BUCKET))
    assertEquals(10 - filledItem.getCount, items.countItem(filledItem.getItem))
  }

  private def fillStackedBucket(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(2)
    items.setItem(0, new ItemStack(Items.BUCKET, 2))
    val handler = new ChestAsTankStorage(InventoryStorage.of(items, null))

    Using(Transaction.openOuter()) { tr =>
      val filled = handler.insert(
        FabricConverter.toVariant(fluid, Fluids.EMPTY),
        FabricConverter.fabricAmount(fluid),
        tr,
      )
      assertEquals(0, filled)
      tr.commit()
    }

    assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET, 2), items.getItem(0)))
    assertEquals(0, items.countItem(filledItem.getItem))
  }

  private def drainStackedBucket1(fluid: FluidAmount, filledItem: ItemStack): Unit = {
    val items = new SimpleContainer(2)
    items.setItem(1, filledItem.copy().tap(_.setCount(2)))
    val handler = new ChestAsTankStorage(InventoryStorage.of(items, null))

    Using(Transaction.openOuter()) { tr =>
      val drained = handler.extract(
        FabricConverter.toVariant(fluid, Fluids.EMPTY),
        FabricConverter.fabricAmount(fluid),
        tr,
      )
      assertEquals(0, drained)
      tr.commit()
    }

    assertEquals(0, items.countItem(Items.BUCKET))
  }
}
