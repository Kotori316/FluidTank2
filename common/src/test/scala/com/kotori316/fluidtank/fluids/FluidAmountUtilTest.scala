package com.kotori316.fluidtank.fluids

import com.kotori316.fluidtank.BeforeMC
import com.kotori316.fluidtank.contents.{GenericAmount, GenericUnit, gaString}
import net.minecraft.world.item.alchemy.{PotionContents, Potions}
import net.minecraft.world.item.{Item, ItemStack, Items}
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class FluidAmountUtilTest extends BeforeMC {

  @Test
  def waterBucket(): Unit = {
    val water = FluidAmountUtil.BUCKET_WATER
    assertEquals(GenericUnit.ONE_BUCKET, water.amount)
    assertFalse(water.isEmpty)
  }

  @TestFactory
  def notEqual(): Array[DynamicNode] = {
    val fluids = Seq(
      FluidAmountUtil.EMPTY,
      FluidAmountUtil.BUCKET_WATER,
      FluidAmountUtil.BUCKET_LAVA,
      FluidAmountUtil.fromItem(PotionContents.createItemStack(Items.POTION, Potions.WATER)),
      FluidAmountUtil.fromItem(PotionContents.createItemStack(Items.POTION, Potions.INVISIBILITY)),
      FluidAmountUtil.fromItem(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER)),
    )
    fluids.combinations(2).map(s =>
      DynamicTest.dynamicTest(s"$s", () =>
        assertNotEquals(s.head, s(1))
      )).toArray
  }

  //noinspection AssertBetweenInconvertibleTypes
  @Test
  def notEqual2(): Unit = {
    val a = GenericAmount("a", GenericUnit.fromFabric(1), None)
    assertNotEquals(a, "a")
    assertNotEquals(a, FluidAmountUtil.BUCKET_WATER)
  }

  @TestFactory
  def empty(): Array[DynamicNode] = {
    Seq(
      DynamicTest.dynamicTest("empty fluid", () => assertTrue(FluidAmountUtil.EMPTY.isEmpty)),
      DynamicTest.dynamicTest("amount 1000, empty fluid", () => assertTrue(FluidAmountUtil.EMPTY.setAmount(GenericUnit.ONE_BUCKET).isEmpty)),
      DynamicTest.dynamicTest("amount 0, lava", () => assertTrue(FluidAmountUtil.BUCKET_LAVA.setAmount(GenericUnit.ZERO).isEmpty)),
      DynamicTest.dynamicTest("amount 0, water", () => assertTrue(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.ZERO).isEmpty)),
    ).toArray
  }

  @TestFactory
  def cycle(): Array[DynamicNode] = {
    val fluids = Seq(
      FluidAmountUtil.EMPTY,
      FluidAmountUtil.BUCKET_WATER,
      FluidAmountUtil.BUCKET_LAVA,
    )
    fluids.map(f => DynamicTest.dynamicTest(s"cycle $f", () => {
      val tag = f.getTag
      val reconstructed = FluidAmountUtil.fromTag(tag)
      assertEquals(f, reconstructed)
    })).toArray
  }

  @Nested
  class FromItemTest {
    @Test
    def fromEmptyBucket(): Unit = {
      val fluid = FluidAmountUtil.fromItem(new ItemStack(Items.BUCKET))
      assertEquals(FluidAmountUtil.EMPTY, fluid)
    }

    @Test
    def fromWaterBucket(): Unit = {
      val fluid = FluidAmountUtil.fromItem(new ItemStack(Items.WATER_BUCKET))
      assertEquals(FluidAmountUtil.BUCKET_WATER, fluid)
    }

    @Test
    def fromLavaBucket(): Unit = {
      val fluid = FluidAmountUtil.fromItem(new ItemStack(Items.LAVA_BUCKET))
      assertEquals(FluidAmountUtil.BUCKET_LAVA, fluid)
    }

    @Test
    def fromWaterBottle(): Unit = {
      val fluid = FluidAmountUtil.fromItem(PotionContents.createItemStack(Items.POTION, Potions.WATER))
      assertEquals(FluidLike.POTION_NORMAL, fluid.content)
      assertTrue(fluid.componentPatch.nonEmpty)
      assertEquals(GenericUnit.ONE_BOTTLE, fluid.amount)
    }
  }

  @Nested
  class FillPotionTest {

    @Test
    def fillBucket(): Unit = {
      val fluid = FluidAmountUtil.fromItem(PotionContents.createItemStack(Items.POTION, Potions.WATER))

      val filled = PlatformFluidAccess.getInstance().fillItem(fluid, Items.BUCKET.getDefaultInstance, null, null, true)
      assertTrue(filled.moved().isEmpty)
      assertFalse(filled.shouldMove())
      assertEquals(Items.BUCKET, filled.toReplace.getItem)
    }

    @TestFactory
    def fillBottle(): Array[DynamicNode] = {
      PotionType.values()
        .map(p => DynamicTest.dynamicTest(s"fillBottle_$p", () => {
          val fluid = FluidAmountUtil.fromItem(PotionContents.createItemStack(p.getItem, Potions.WATER))
          fillBottle(fluid, p.getItem)
        }))
    }

    def fillBottle(fluid: FluidAmount, potionItem: Item): Unit = {
      val filled = PlatformFluidAccess.getInstance().fillItem(fluid, new ItemStack(Items.GLASS_BOTTLE), null, null, true)
      assertEquals(fluid.setAmount(GenericUnit.ONE_BOTTLE), filled.moved())
      assertTrue(filled.shouldMove())
      assertEquals(potionItem, filled.toReplace.getItem)
      assertEquals(Potions.WATER, PotionUtils.getPotion(filled.toReplace))
    }
  }
}
