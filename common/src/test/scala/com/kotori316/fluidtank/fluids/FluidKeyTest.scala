package com.kotori316.fluidtank.fluids

import cats.implicits.catsSyntaxEq
import com.kotori316.fluidtank.BeforeMC
import com.kotori316.fluidtank.contents.GenericUnit
import net.minecraft.core.component.{DataComponentPatch, DataComponents}
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.{DynamicNode, DynamicTest, Test, TestFactory}

class FluidKeyTest extends BeforeMC {

  @Test
  def createKey(): Unit = {
    val key = FluidLikeKey(FluidLike.FLUID_WATER, Option.empty)
    assertNotNull(key)
    assertFalse(key.isEmpty)
    assertTrue(key.isDefined)
  }

  @Test
  def equal1(): Unit = {
    val key1 = FluidLikeKey(FluidLike.FLUID_WATER, Option.empty)
    val key2 = FluidLikeKey(FluidLike.FLUID_WATER, Option.empty)
    assertEquals(key1, key2)
    assertTrue(key1 == key2)
    assertTrue(key1 === key2)
  }

  @TestFactory
  def notEqual(): Array[DynamicNode] = {
    val keys = Seq(
      FluidLikeKey(FluidLike.FLUID_WATER, Option.empty),
      FluidLikeKey(FluidLike.FLUID_EMPTY, Option.empty),
      FluidLikeKey(FluidLike.FLUID_LAVA, Option.empty),
      FluidLikeKey(FluidLike.POTION_NORMAL, Option.empty),
      FluidLikeKey(FluidLike.POTION_NORMAL, Option(DataComponentPatch.EMPTY)),
      FluidLikeKey(FluidLike.POTION_SPLASH, Option(DataComponentPatch.EMPTY)),
      FluidLikeKey(FluidLike.POTION_LINGERING, Option(DataComponentPatch.EMPTY)),
    )
    keys.combinations(2)
      .map { case s1 +: s2 +: _ =>
        DynamicTest.dynamicTest(s"$s1, $s2", () => {
          assertNotEquals(s1, s2, s"$s1 should not equal to $s2")
        })
      }
      .toArray
  }

  @Test
  def empty(): Unit = {
    val key = FluidLikeKey(FluidLike.FLUID_EMPTY, Option.empty)
    assertTrue(key.isEmpty)
    assertFalse(key.isDefined)
  }

  @Test
  def tagChange(): Unit = {
    val tag = new CompoundTag()
    tag.putString("A", "a")
    val expected: CompoundTag = tag.copy()
    val key = FluidLikeKey(FluidLike.FLUID_EMPTY, Option(DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build()))
    tag.putString("A", "b")

    assertEquals(FluidLikeKey(FluidLike.FLUID_EMPTY, Option(DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(expected)).build())), key)
    assertNotEquals(FluidLikeKey(FluidLike.FLUID_EMPTY, Option(DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build())), key)
  }

  @Test
  def fromAmountEmpty(): Unit = {
    val key = FluidLikeKey(FluidLike.FLUID_EMPTY, Option.empty)
    assertEquals(key, FluidLikeKey.from(FluidAmountUtil.EMPTY))
  }

  @Test
  def fromAmount(): Unit = {
    val expected = FluidLikeKey(FluidLike.FLUID_WATER, Option.empty)
    assertEquals(expected, FluidLikeKey.from(FluidAmountUtil.BUCKET_WATER))
    assertEquals(expected, FluidLikeKey.from(FluidAmountUtil.BUCKET_WATER.setAmount(GenericUnit.fromForge(2000))))
    assertNotEquals(expected, FluidLikeKey.from(FluidAmountUtil.BUCKET_LAVA))
  }

  @Test
  def testToAmount(): Unit = {
    val key = FluidLikeKey(FluidLike.FLUID_WATER, Option.empty)
    assertEquals(FluidAmountUtil.BUCKET_WATER, key.toAmount(GenericUnit.ONE_BUCKET))
    val two = GenericUnit.fromForge(2000)
    assertEquals(FluidAmountUtil.BUCKET_WATER.setAmount(two), key.toAmount(two))
  }
}
