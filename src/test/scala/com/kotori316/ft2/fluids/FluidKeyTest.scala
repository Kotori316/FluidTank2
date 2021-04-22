package com.kotori316.ft2.fluids

import cats.implicits.catsSyntaxEq
import com.kotori316.ft2.BeforeAllTest
import net.minecraft.fluid.{Fluid, Fluids}
import net.minecraft.nbt.CompoundNBT
import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import scala.util.chaining._

object FluidKeyTest extends BeforeAllTest {
  @Test
  def emptyTest(): Unit = {
    assertTrue(FluidKey.EMPTY.isEmpty)
  }

  @Test
  def nonEmptyTest(): Unit = {
    assertFalse(FluidKey.WATER.isEmpty)
    assertFalse(FluidKey.LAVA.isEmpty)
  }

  @Test
  def registryNameTest(): Unit = {
    assertEquals("minecraft:water", FluidKey.WATER.getRegistryName)
  }

  @Test
  def asKeyTest(): Unit = {
    val map = Map(
      FluidKey.EMPTY -> "Empty",
      FluidKey.WATER -> "Water",
    )
    assertAll(
      () => assertTrue(Option("Empty") === map.get(FluidKey.EMPTY)),
      () => assertTrue(Option("Water") === map.get(FluidKey.WATER)),
      () => assertTrue(Option("Water") === map.get(FluidKey(Fluids.WATER, None))),
      () => assertTrue(Option.empty[String] === map.get(FluidKey.LAVA)),
    )
  }

  def fluids = Array(Fluids.WATER, Fluids.EMPTY, Fluids.LAVA)

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.ft2.fluids.FluidKeyTest#fluids"))
  def nbtTest(f: Fluid): Unit = {
    val a = FluidKey(f, None)
    val b = FluidKey(f, Some(new CompoundNBT()))
    assertFalse(a === b)
    assertTrue(a.getRegistryName === b.getRegistryName)
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.ft2.fluids.FluidKeyTest#fluids"))
  def nbtTest2(f: Fluid): Unit = {
    val a = FluidKey(f, Some(new CompoundNBT().tap(_.putInt("a", 1))))
    val b = FluidKey(f, Some(new CompoundNBT().tap(_.putInt("b", 1))))
    assertFalse(a === b)
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.ft2.fluids.FluidKeyTest#fluids"))
  def nbtTest3(f: Fluid): Unit = {
    val a = FluidKey(f, Some(new CompoundNBT().tap(_.putInt("a", 1))))
    val b = FluidKey(f, Some(new CompoundNBT().tap(_.putInt("a", 2))))
    assertFalse(a === b)
  }

  @ParameterizedTest
  @MethodSource(Array("com.kotori316.ft2.fluids.FluidKeyTest#fluids"))
  def nbtTest4(f: Fluid): Unit = {
    val a = FluidKey(f, Some(new CompoundNBT().tap(_.putInt("a", 12))))
    val b = FluidKey(f, Some(new CompoundNBT().tap(_.putInt("a", 12))))
    assertTrue(a === b)
  }

  @Test
  def nbtTest5(): Unit = {
    val a = FluidKey(Fluids.WATER, Some(new CompoundNBT().tap(_.putInt("a", 12))))
    val b = FluidKey(Fluids.LAVA, Some(new CompoundNBT().tap(_.putInt("a", 12))))
    assertTrue(a =!= b)
  }
}
